package com.tecknobit.javadocky.listeners;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.tecknobit.javadocky.JavaDockyConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.psi.PsiManager.getInstance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.SETTER;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.reachMethodType;
import static com.tecknobit.javadocky.JavaDockyDocuManager.formatFieldTemplate;

/**
 * The {@code DocumentationChangeListener} class is useful to detect when the current class where the user is working on
 * changes the documentation of the fields, this allows this listener to modify all the occurrences of that field of all
 * documentation comments that appear in the class
 *
 * @author N7ghtm4r3 - Tecknobit
 * @apiNote this when the {@link JavaDockyConfiguration.Tag#params} is used in the configured template
 * @see BulkFileListener
 */
public class DocumentationChangeListener implements BulkFileListener {

    /**
     * {@code currentDocus} list of the current documentation comments of the class
     * @apiNote when the class changes will be recreated
     */
    private static ArrayList<String> currentDocus = new ArrayList<>();

    /**
     * {@code tempDocus} check list of the current documentation comments of the class
     * @apiNote when the class changes will be recreated
     */
    private static ArrayList<String> tempDocus = new ArrayList<>();

    /**
     * {@code OPEN_CURLY_BRACKET_REGEX} regex for the "{" character
     */
    private static final String OPEN_CURLY_BRACKET_REGEX = "OCBR";

    /**
     * {@code OPEN_CURLY_BRACKET_REGEX} regex for the "}" character
     */
    private static final String CLOSE_CURLY_BRACKET_REGEX = "CCBR";

    /**
     * {@code OPEN_CURLY_BRACKET_REGEX} regex for the "*" character
     */
    private static final String STAR_REGEX = "STRX";

    /**
     * {@code listenerProject} instance of the project to work on
     */
    public static Project listenerProject;

    /**
     * {@code currentClass} instance of the current class to work on
     */
    private PsiClass currentClass;

    /**
     * Method to compute after an event happened
     *
     * @param events: list of events happened
     */
    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        if (listenerProject != null) {
            VirtualFile virtualFile = events.get(events.size() - 1).getFile();
            if (virtualFile != null) {
                PsiJavaFile javaFile = (PsiJavaFile) getInstance(listenerProject).findFile(virtualFile);
                if (javaFile != null) {
                    PsiClass[] classes = javaFile.getClasses();
                    if (classes.length > 0) {
                        if (currentClass != classes[0]) {
                            currentDocus = new ArrayList<>();
                            tempDocus = new ArrayList<>();
                            currentClass = classes[0];
                        }
                        rewriteDocu(currentClass);
                    }
                }
            }
        }
    }

    /**
     * Method to rewrite all the documentation comments of the field (or multiple fields at the same time) where the
     * documentation has been changed
     *
     * @param sourceClass: class where rewrite the documentation
     */
    private void rewriteDocu(PsiClass sourceClass) {
        if (execute(currentClass)) {
            PsiClass tmpClass = PsiElementFactory.getInstance(listenerProject).createClassFromText(
                    insertDanglingMetaCharacters(getChanges(sourceClass, sourceClass.getText())),
                    null).getInnerClasses()[0];
            for (PsiMethod tmpMethod : tmpClass.getMethods()) {
                boolean isSetter = reachMethodType(tmpMethod) == SETTER;
                if (tmpMethod.getName().equals(tmpClass.getName()) || isSetter) {
                    for (PsiMethod method : sourceClass.getMethods()) {
                        if (method.getName().equals(sourceClass.getName()) || isSetter) {
                            if (tmpMethod.getBody().getText().equals(method.getBody().getText())) {
                                PsiDocComment methodDocComment = method.getDocComment();
                                PsiDocComment tmpMethodDocComment = tmpMethod.getDocComment();
                                if ((methodDocComment != null && tmpMethodDocComment != null)
                                        && (!methodDocComment.getText().equals(tmpMethodDocComment.getText()))) {
                                    DumbService.getInstance(listenerProject).smartInvokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            WriteCommandAction.runWriteCommandAction(listenerProject, new Runnable() {
                                                @Override
                                                public void run() {
                                                    methodDocComment.replace(tmpMethodDocComment);
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }
            for (PsiClass innerClass : sourceClass.getInnerClasses())
                rewriteDocu(innerClass);
            currentDocus = new ArrayList<>(new ArrayList<>(tempDocus));
            tempDocus = new ArrayList<>();
        }
    }

    /**
     * Method to check if the rewrite of the documentation must execute
     *
     * @param sourceClass: the class where check if execute the rewrite of the documentation
     * @return whether the rewrite of the documentation must execute
     */
    private boolean execute(PsiClass sourceClass) {
        populateTempDocus(sourceClass);
        return !Arrays.deepEquals(currentDocus.toArray(), tempDocus.toArray());
    }

    /**
     * Method to assemble the {@link #tempDocus}
     *
     * @param sourceClass: the class where fetch the documentation comments of the fields
     */
    private void populateTempDocus(PsiClass sourceClass) {
        for (PsiField field : sourceClass.getFields()) {
            PsiDocComment docComment = field.getDocComment();
            if (docComment != null) {
                String docuText = docComment.getText();
                if (!tempDocus.contains(docuText))
                    tempDocus.add(docuText);
            }
        }
        for (PsiClass innerClass : sourceClass.getInnerClasses())
            populateTempDocus(innerClass);
    }

    /**
     * Method to get if the documentation of the fields of the class has been changed
     *
     * @param sourceClass: the class from fetch the fields and their documentation comment
     */
    private String getChanges(PsiClass sourceClass, String changes) {
        for (PsiField field : sourceClass.getFields()) {
            String fieldName = field.getName();
            PsiDocComment docComment = field.getDocComment();
            if (docComment != null) {
                String description = removeDanglingMetaCharacters(docComment.getText().replaceAll("\\*", "")
                        .replace("\\\\", "").replace("/", "")
                        .replaceAll("\n", "")).trim()
                        .replace(removeDanglingMetaCharacters(formatFieldTemplate(fieldName)).trim(), "");
                String regex = getRegex(changes, fieldName);
                if (!description.isEmpty() && !description.equals(regex)) {
                    changes = removeDanglingMetaCharacters(changes).replaceAll(removeDanglingMetaCharacters("* @param "
                            + fieldName + ": " + regex), "* @param " + fieldName + ":" + description);
                }
            }
        }
        return changes;
    }

    /**
     * Method from fetch the regex to use for rewrite the documentation
     *
     * @param classText: the text of the current class
     * @param fieldName: the field name from fetch the regex
     * @return the regex to rewrite the new documentation comment of the field inserted
     */
    private String getRegex(String classText, String fieldName) {
        String regex = "* @param " + fieldName + ": ";
        for (String line : insertDanglingMetaCharacters(classText).split("\n")) {
            if (line.contains(regex)) {
                return removeDanglingMetaCharacters(line).replaceAll(removeDanglingMetaCharacters(regex), "")
                        .trim();
            }
        }
        return null;
    }

    /**
     * Method to remove the dangling meta characters from the text inserted
     *
     * @param text: the text from remove the dangling meta characters
     * @return text without the dangling meta characters as {@link String}
     * @apiNote this method is useful during the replace operation on a {@link String} to obtain a clean value to use,
     * in this method are used the: {@link #OPEN_CURLY_BRACKET_REGEX}, {@link #CLOSE_CURLY_BRACKET_REGEX} and
     * {@link #STAR_REGEX} regex
     */
    private String removeDanglingMetaCharacters(String text) {
        return text.replaceAll("\\{", OPEN_CURLY_BRACKET_REGEX)
                .replaceAll("}", CLOSE_CURLY_BRACKET_REGEX)
                .replaceAll("\\*", STAR_REGEX);
    }

    /**
     * Method to insert the dangling meta characters from the text inserted
     *
     * @param text: the text where insert the dangling meta characters
     * @return text with the dangling meta characters as {@link String}
     * @apiNote this method is useful during the replace operation on a {@link String} to obtain a clean value to use in
     * the correct way, in this method are used the: {@link #OPEN_CURLY_BRACKET_REGEX}, {@link #CLOSE_CURLY_BRACKET_REGEX}
     * and {@link #STAR_REGEX} regex
     */
    private String insertDanglingMetaCharacters(String text) {
        return text.replaceAll(OPEN_CURLY_BRACKET_REGEX, "{")
                .replaceAll(CLOSE_CURLY_BRACKET_REGEX, "}")
                .replaceAll(STAR_REGEX, "*");
    }

}

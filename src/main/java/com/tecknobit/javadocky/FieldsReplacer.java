package com.tecknobit.javadocky;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;

import java.util.ArrayList;
import java.util.Arrays;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.SETTER;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.reachMethodType;
import static com.tecknobit.javadocky.JavaDockyDocuManager.formatFieldTemplate;

/**
 * The {@code FieldsReplacer} class is useful to modify all the documentation comments that appear in the class of the
 * fields that has been changed
 *
 * @author N7ghtm4r3 - Tecknobit
 * @apiNote this when the {@link JavaDockyConfiguration.Tag#params} is used in the configured template
 */
public class FieldsReplacer {

    /**
     * {@code currentDocus} list of the current documentation comments of the class
     *
     * @apiNote when the class changes will be recreated
     */
    private static ArrayList<String> currentDocus = new ArrayList<>();

    /**
     * {@code tempDocus} check list of the current documentation comments of the class
     *
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
     * {@code project} instance of the project to work on
     */
    private final Project project;

    /**
     * {@code document} the document to work on
     */
    private Document document;

    /**
     * {@code documentManager} the document manager
     */
    private final PsiDocumentManager documentManager;

    /**
     * {@code currentClass} instance of the current class to work on
     */
    private PsiClass currentClass;

    /**
     * Constructor to init a {@link FieldsReplacer} object
     *
     * @param project: instance of the project to work on
     */
    public FieldsReplacer(Project project) {
        this.project = project;
        documentManager = PsiDocumentManager.getInstance(project);
    }

    /**
     * Method to replace the fields documentation
     *
     * @param document: the document to work on
     */
    public void replaceFields(Document document) {
        this.document = document;
        PsiJavaFile javaFile;
        try {
            javaFile = (PsiJavaFile) documentManager.getCachedPsiFile(document);
        } catch (ClassCastException classCastException) {
            javaFile = null;
        }
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

    /**
     * Method to rewrite all the documentation comments of the field (or multiple fields at the same time) where the
     * documentation has been changed
     *
     * @param sourceClass: class where rewrite the documentation
     */
    private void rewriteDocu(PsiClass sourceClass) {
        if (execute(currentClass)) {
            PsiClass tmpClass = PsiElementFactory.getInstance(project).createClassFromText(
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
                                    getApplication().invokeLater(() ->
                                            runWriteCommandAction(project, () -> {
                                                documentManager.commitDocument(document);
                                                methodDocComment.replace(tmpMethodDocComment);
                                            })
                                    );
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

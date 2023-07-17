package com.tecknobit.javadocky.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.psi.PsiManager.getInstance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.SETTER;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.reachMethodType;
import static com.tecknobit.javadocky.JavaDockyDocuManager.formatFieldTemplate;

public class DocumentationChangeListener implements BulkFileListener {

    private static ArrayList<String> currentDocus = new ArrayList<>();

    private static ArrayList<String> tempDocus = new ArrayList<>();

    private static final String OPEN_CURLY_BRACKET_REGEX = "OCBR";

    private static final String CLOSE_CURLY_BRACKET_REGEX = "CCBR";

    private static final String STAR_REGEX = "STRX";

    public static Project listenerProject;

    private PsiClass currentClass;

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

    private void rewriteDocu(PsiClass sourceClass) {
        PsiClass tmpClass = PsiElementFactory.getInstance(listenerProject).createClassFromText(
                insertDanglingMetaCharacters(getChanges(sourceClass, sourceClass.getText())), null).getInnerClasses()[0];
        if (execute(currentClass)) {
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
                                    ApplicationManager.getApplication().invokeLater(() ->
                                            WriteCommandAction.runWriteCommandAction(listenerProject, () -> {
                                                PsiDocumentManager.getInstance(listenerProject).commitAllDocuments();
                                                methodDocComment.replace(tmpMethodDocComment);
                                            })
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
        for (PsiClass innerClass : sourceClass.getInnerClasses())
            rewriteDocu(innerClass);
    }

    private boolean execute(PsiClass sourceClass) {
        populateTempDocus(sourceClass);
        boolean execute = !Arrays.deepEquals(currentDocus.toArray(), tempDocus.toArray());
        if (execute) {
            currentDocus = new ArrayList<>(new ArrayList<>(tempDocus));
            tempDocus = new ArrayList<>();
        }
        return execute;
    }

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

    private String getChanges(PsiClass psiClass, String changes) {
        for (PsiField field : psiClass.getFields()) {
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
        for (PsiClass innerClass : psiClass.getInnerClasses())
            return getChanges(innerClass, changes);
        return changes;
    }

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

    private String removeDanglingMetaCharacters(String text) {
        return text.replaceAll("\\{", OPEN_CURLY_BRACKET_REGEX)
                .replaceAll("}", CLOSE_CURLY_BRACKET_REGEX)
                .replaceAll("\\*", STAR_REGEX);
    }

    private String insertDanglingMetaCharacters(String text) {
        return text.replaceAll(OPEN_CURLY_BRACKET_REGEX, "{")
                .replaceAll(CLOSE_CURLY_BRACKET_REGEX, "}")
                .replaceAll(STAR_REGEX, "*");
    }

}

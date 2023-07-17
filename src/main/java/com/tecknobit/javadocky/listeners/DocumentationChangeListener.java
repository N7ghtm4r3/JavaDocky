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

import java.util.List;

import static com.intellij.psi.PsiManager.getInstance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.SETTER;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.reachMethodType;
import static com.tecknobit.javadocky.JavaDockyDocuManager.formatFieldTemplate;

public class DocumentationChangeListener implements BulkFileListener {

    private static final String OPEN_CURLY_BRACKET_REGEX = "OCBR";

    private static final String CLOSE_CURLY_BRACKET_REGEX = "CCBR";

    private static final String STAR_REGEX = "STRX";

    public static Project listenerProject;

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        if (listenerProject != null) {
            VirtualFile virtualFile = events.get(events.size() - 1).getFile();
            PsiClass[] classes = ((PsiJavaFile) getInstance(listenerProject).findFile(virtualFile)).getClasses();
            if (classes.length > 0)
                rewriteDocu(classes[0]);
        }
    }

    private void rewriteDocu(PsiClass sourceClass) {
        PsiClass tmpClass = PsiElementFactory.getInstance(listenerProject).createClassFromText(
                insertDanglingMetaCharacters(getChanges(sourceClass, sourceClass.getText())), null).getInnerClasses()[0];
        for (PsiMethod tmpMethod : tmpClass.getMethods()) {
            boolean isSetter = reachMethodType(tmpMethod) == SETTER;
            if (tmpMethod.getName().equals(tmpClass.getName()) || isSetter) {
                for (PsiMethod method : sourceClass.getMethods()) {
                    if (method.getName().equals(sourceClass.getName()) || isSetter) {
                        if (tmpMethod.getBody().getText().equals(method.getBody().getText())) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    WriteCommandAction.runWriteCommandAction(listenerProject, () -> {
                                        PsiDocumentManager.getInstance(listenerProject).commitAllDocuments();
                                        method.getDocComment().replace(tmpMethod.getDocComment());
                                    })
                            );
                        }
                    }
                }
            }
        }
        for (PsiClass innerClass : sourceClass.getInnerClasses())
            rewriteDocu(innerClass);
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

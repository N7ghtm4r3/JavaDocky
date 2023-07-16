package com.tecknobit.javadocky.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.*;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.psi.PsiManager.getInstance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.SETTER;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.reachMethodType;
import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.instance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

public class DocumentationChangeListener implements BulkFileListener {

    private static final String OPEN_CURLY_BRACKET_REGEX = "OCBR";

    private static final String CLOSE_CURLY_BRACKET_REGEX = "CCBR";

    private static final String STAR_REGEX = "STRX";

    public static Project listenerProject;

    private String prevClass;

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        if (listenerProject != null) {
            VirtualFile virtualFile = events.get(events.size() - 1).getFile();
            PsiClass javaClass = ((PsiJavaFile) getInstance(listenerProject).findFile(virtualFile)).getClasses()[0];
            String currentClass = javaClass.getText();
            for (PsiField field : javaClass.getFields()) {
                String fieldName = field.getName();
                String description = field.getDocComment().getText().replaceAll("\\*", "")
                        .replace("\\\\", "").replace("/", "")
                        .replaceAll("\n", "").trim();
                String regex = removeCurlyBrackets(configuration.getFieldTemplate().replaceAll("/\\**\n *", "")
                        .replaceAll("\\*/", "")
                        .replaceAll("\\* ", "")
                        .replaceAll(instance.getTag(), fieldName)
                        .replaceAll("\n", "")).trim();
                if (!removeCurlyBrackets(description).equals(regex)) {
                    currentClass = removeCurlyBrackets(currentClass).replaceAll("\\* @param " + fieldName + ": "
                            + regex, "* @param " + fieldName + ":" + removeCurlyBrackets(description)
                            .replaceAll(regex, ""));
                }
            }
            if (prevClass == null || !prevClass.equals(currentClass)) {
                rewriteDocu(javaClass, currentClass);
                prevClass = currentClass;
            }
        }
    }

    private String removeCurlyBrackets(String text) {
        return text.replaceAll("\\{", OPEN_CURLY_BRACKET_REGEX)
                .replaceAll("}", CLOSE_CURLY_BRACKET_REGEX);
    }

    private String insertCurlyBrackets(String text) {
        return text.replaceAll(OPEN_CURLY_BRACKET_REGEX, "{")
                .replaceAll(CLOSE_CURLY_BRACKET_REGEX, "}");
    }

    private void rewriteDocu(PsiClass currentClass, String newDocu) {
        PsiClass tmpClass = PsiElementFactory.getInstance(listenerProject).createClassFromText(insertCurlyBrackets(newDocu),
                null).getInnerClasses()[0];
        for (PsiMethod tmpMethod : tmpClass.getMethods()) {
            boolean isSetter = reachMethodType(tmpMethod) == SETTER;
            if (tmpMethod.getName().equals(tmpClass.getName()) || isSetter) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    for (PsiMethod method : currentClass.getMethods()) {
                        if (!isSetter) {
                            if (method.getName().equals(currentClass.getName())) {
                                if (method.getBody().getText().equals(tmpMethod.getBody().getText())) {
                                    try {
                                        WriteCommandAction.writeCommandAction(listenerProject).run((ThrowableRunnable<Throwable>) () -> {
                                            PsiDocumentManager.getInstance(listenerProject).commitAllDocuments();
                                            method.getDocComment().replace(tmpMethod.getDocComment());
                                        });
                                    } catch (Throwable e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        } else {
                            if (tmpMethod.getBody().getText().equals(method.getBody().getText())) {
                                try {
                                    WriteCommandAction.writeCommandAction(listenerProject).run((ThrowableRunnable<Throwable>) () -> {
                                        PsiDocumentManager.getInstance(listenerProject).commitAllDocuments();
                                        method.getDocComment().replace(tmpMethod.getDocComment());
                                    });
                                } catch (Throwable e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

}

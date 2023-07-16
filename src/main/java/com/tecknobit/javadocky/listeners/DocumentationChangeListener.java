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

import java.util.HashMap;
import java.util.List;

import static com.intellij.psi.PsiManager.getInstance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.instance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

public class DocumentationChangeListener implements BulkFileListener {

    private static final HashMap<VirtualFile, HashMap<String, String>> docus = new HashMap<>();

    private static final String OPEN_CURLY_BRACKET_REGEX = "OCBR";

    private static final String CLOSE_CURLY_BRACKET_REGEX = "CCBR";

    private static final String STAR_REGEX = "STRX";

    public static Project listenerProject;

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        if (listenerProject != null) {
            VirtualFile virtualFile = events.get(events.size() - 1).getFile();
            PsiClass javaClass = ((PsiJavaFile) getInstance(listenerProject).findFile(virtualFile)).getClasses()[0];
            HashMap<String, String> currentDocus = docus.get(virtualFile);
            if (currentDocus == null)
                currentDocus = new HashMap<>();
            for (PsiField field : javaClass.getFields()) {
                String fieldName = field.getName();
                String currentDocuComment = currentDocus.get(fieldName);
                String description = field.getDocComment().getText().replaceAll("\\*", "")
                        .replace("\\\\", "").replace("/", "")
                        .replaceAll("\n", "").trim();
                String regex = removeCurlyBrackets(configuration.getFieldTemplate().replaceAll("/\\**\n *", "")
                        .replaceAll("\\*/", "")
                        .replaceAll("\\* ", "")
                        .replaceAll(instance.getTag(), fieldName)
                        .replaceAll("\n", "")).trim();
                String replacer = "* @param " + fieldName + ":" + removeCurlyBrackets(description).replaceAll(regex, "");
                if (!replacer.equals(currentDocuComment)) {
                    rewriteDocu(javaClass, removeCurlyBrackets(javaClass.getText()).replaceAll("\\* @param " + fieldName
                            + ": " + regex, replacer));
                    currentDocus.put(fieldName, replacer);
                }
            }
            docus.put(virtualFile, currentDocus);
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
        for (PsiMethod tmpConstructor : tmpClass.getMethods()) {
            if (tmpConstructor.getName().equals(tmpClass.getName())) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    for (PsiMethod constructor : currentClass.getConstructors()) {
                        if (constructor.getName().equals(currentClass.getName())) {
                            if (constructor.getBody().getText().equals(tmpConstructor.getBody().getText())) {
                                try {
                                    WriteCommandAction.writeCommandAction(listenerProject).run((ThrowableRunnable<Throwable>) () -> {
                                        PsiDocumentManager.getInstance(listenerProject).commitAllDocuments();
                                        constructor.getDocComment().replace(tmpConstructor.getDocComment());
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

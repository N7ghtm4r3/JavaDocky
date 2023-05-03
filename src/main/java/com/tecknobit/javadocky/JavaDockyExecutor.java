package com.tecknobit.javadocky;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

public class JavaDockyExecutor extends AnAction {

    private PsiClass currentClass;
    private Project project;
    private JavaDockyDocuManager docuManager;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        currentClass = ((PsiJavaFile) e.getData(PlatformDataKeys.PSI_FILE)).getClasses()[0];
        docuManager = new JavaDockyDocuManager(project, currentClass);
        execJavaDocky();
    }

    private void execJavaDocky() {
        try {
            useClassesDocuTemplate();
            useFieldsDocuTemplate();
            useConstructorsTemplate();
            useMethodsTemplate();
            navigateInnerClasses(currentClass);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    private void useClassesDocuTemplate() throws Throwable {
        useClassesDocuTemplate(currentClass);
    }

    private void useClassesDocuTemplate(PsiClass psiClass) throws Throwable {
        if (configuration.isClassTemplateEnabled()) {
            useDocuTemplate(() -> addPsiElement(docuManager.createClassDocu(psiClass), psiClass.getFirstChild()));
        }
    }

    private void useFieldsDocuTemplate() throws Throwable {
        useFieldsDocuTemplate(currentClass);
    }

    private void useFieldsDocuTemplate(PsiClass psiClass) throws Throwable {
        if (configuration.isFieldTemplateEnabled()) {
            useDocuTemplate(() -> {
                for (PsiField field : psiClass.getFields())
                    addPsiElement(docuManager.createFieldDocu(field), field.getSourceElement());
            });
        }
    }

    private void useConstructorsTemplate() throws Throwable {
        useConstructorsTemplate(currentClass);
    }

    private void useConstructorsTemplate(PsiClass psiClass) throws Throwable {
        if (configuration.isConstructorTemplateEnabled()) {
            useDocuTemplate(() -> {
                for (PsiMethod constructor : psiClass.getConstructors())
                    addPsiElement(docuManager.createConstructorDocu(constructor), constructor.getSourceElement());
            });
        }
    }

    private void useMethodsTemplate() throws Throwable {
        useMethodsTemplate(currentClass);
    }

    private void useMethodsTemplate(PsiClass psiClass) throws Throwable {
        String className = psiClass.getName();
        if (configuration.isMethodTemplateEnabled()) {
            useDocuTemplate(() -> {
                for (PsiMethod method : psiClass.getMethods())
                    if (!method.getName().equals(className))
                        addPsiElement(docuManager.createMethodDocu(method), method.getSourceElement());
            });
        }
    }

    private void navigateInnerClasses(PsiClass innerClass) throws Throwable {
        for (PsiClass inner : innerClass.getInnerClasses()) {
            useClassesDocuTemplate(inner);
            useFieldsDocuTemplate(inner);
            useConstructorsTemplate(inner);
            useMethodsTemplate(inner);
            navigateInnerClasses(inner);
        }
    }

    private void addPsiElement(PsiDocComment docu, PsiElement psiElement) {
        if (docu != null && psiElement != null && !psiElement.getText().startsWith("/**"))
            currentClass.addBefore(docu, psiElement);
    }

    private void useDocuTemplate(ThrowableRunnable<Throwable> action) throws Throwable {
        WriteCommandAction.writeCommandAction(project).run(action);
    }

}

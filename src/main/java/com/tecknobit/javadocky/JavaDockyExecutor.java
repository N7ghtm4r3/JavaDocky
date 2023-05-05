package com.tecknobit.javadocky;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ThrowableRunnable;
import com.tecknobit.javadocky.JavaDockyConfiguration.JavaDockyItem;
import org.jetbrains.annotations.NotNull;

import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

/**
 * The {@code JavaDockyExecutor} class is useful to execute the {@code JavaDocky}'s plugin
 *
 * @author N7ghtm4r3 - Tecknobit
 * @see AnAction
 **/
public class JavaDockyExecutor extends AnAction {

    /**
     * {@code currentClass} current class to document
     **/
    private PsiClass currentClass;

    /**
     * {@code project} current project to document
     **/
    private Project project;

    /**
     * {@code docuManager} instance to manage the docu-templates and insert in the {@link #currentClass}
     **/
    private JavaDockyDocuManager docuManager;

    /**
     * {@inheritDoc}
     **/
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        currentClass = ((PsiJavaFile) e.getData(PlatformDataKeys.PSI_FILE)).getClasses()[0];
        docuManager = new JavaDockyDocuManager(project, currentClass);
        execJavaDocky();
    }

    /**
     * Method to execute the {@code JavaDocky}'s tool
     * Any-params required
     **/
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

    /**
     * Method to use the {@link JavaDockyItem#Classes}'s template <br>
     * Any-params required
     **/
    private void useClassesDocuTemplate() throws Throwable {
        useClassesDocuTemplate(currentClass);
    }

    /**
     * Method to use the {@link JavaDockyItem#Classes}'s template
     *
     * @param psiClass: class where use the docu-template
     **/
    private void useClassesDocuTemplate(PsiClass psiClass) throws Throwable {
        if (configuration.isClassTemplateEnabled()) {
            useDocuTemplate(() -> addPsiElement(docuManager.createClassDocu(psiClass), psiClass.getFirstChild()));
        }
    }

    /**
     * Method to use the {@link JavaDockyItem#Fields}'s template <br>
     * Any-params required
     **/
    private void useFieldsDocuTemplate() throws Throwable {
        useFieldsDocuTemplate(currentClass);
    }

    /**
     * Method to use the {@link JavaDockyItem#Fields}'s template
     *
     * @param psiClass: class where use the docu-template
     **/
    private void useFieldsDocuTemplate(PsiClass psiClass) throws Throwable {
        if (configuration.isFieldTemplateEnabled()) {
            useDocuTemplate(() -> {
                for (PsiField field : psiClass.getFields())
                    addPsiElement(docuManager.createFieldDocu(field), field.getSourceElement());
            });
        }
    }

    /**
     * Method to use the {@link JavaDockyItem#Constructors}'s template <br>
     * Any-params required
     **/
    private void useConstructorsTemplate() throws Throwable {
        useConstructorsTemplate(currentClass);
    }

    /**
     * Method to use the {@link JavaDockyItem#Constructors}'s template
     *
     * @param psiClass: class where use the docu-template
     **/
    private void useConstructorsTemplate(PsiClass psiClass) throws Throwable {
        if (configuration.isConstructorTemplateEnabled()) {
            useDocuTemplate(() -> {
                for (PsiMethod constructor : psiClass.getConstructors())
                    addPsiElement(docuManager.createConstructorDocu(constructor), constructor.getSourceElement());
            });
        }
    }

    /**
     * Method to use the {@link JavaDockyItem#Methods}'s template <br>
     * Any-params required
     **/
    private void useMethodsTemplate() throws Throwable {
        useMethodsTemplate(currentClass);
    }

    /**
     * Method to use the {@link JavaDockyItem#Methods}'s template
     *
     * @param psiClass: class where use the docu-template
     **/
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

    /**
     * Method to use a {@link JavaDockyItem}'s template
     *
     * @param action: action to execute to add the docu-template
     **/
    private void useDocuTemplate(ThrowableRunnable<Throwable> action) throws Throwable {
        WriteCommandAction.writeCommandAction(project).run(action);
    }

    /**
     * Method to add a {@link JavaDockyItem}'s template
     *
     * @param docu:       the docu-comment to add
     * @param psiElement: the element where add the docu-comment
     **/
    private void addPsiElement(PsiDocComment docu, PsiElement psiElement) {
        if (docu != null && psiElement != null && !psiElement.getText().startsWith("/**"))
            currentClass.addBefore(docu, psiElement);
    }

    /**
     * Method to navigate and use the different docu-templates in each inner classe
     *
     * @param innerClass: inner class where use the docu-template
     **/
    private void navigateInnerClasses(PsiClass innerClass) throws Throwable {
        for (PsiClass inner : innerClass.getInnerClasses()) {
            useClassesDocuTemplate(inner);
            useFieldsDocuTemplate(inner);
            useConstructorsTemplate(inner);
            useMethodsTemplate(inner);
            navigateInnerClasses(inner);
        }
    }

}

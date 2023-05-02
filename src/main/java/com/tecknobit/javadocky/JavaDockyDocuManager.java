package com.tecknobit.javadocky;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;

import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.className;
import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.instance;
import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

public class JavaDockyDocuManager {

    private final PsiElementFactory factory;

    public JavaDockyDocuManager(Project project) {
        this.factory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    public PsiDocComment createClassDocu(PsiClass currentClass) {
        return createDocuComment(configuration.getClassTemplate().replaceAll(className.getTag(), currentClass.getName()));
    }

    public PsiDocComment createFieldDocu(PsiField field) {
        return createDocuComment(configuration.getFieldTemplate().replaceAll(instance.getTag(), field.getName()));
    }

    public PsiDocComment createConstructorDocu(PsiMethod constructor) {
        // TODO: 02/05/2023 MANAGE ALL CONSTRUCTOR  TAGS BEHAVIOURS FOR THE TEMPLATE
        return createDocuComment(configuration.getConstructorTemplate().replaceAll(className.getTag(), constructor.getName()));
    }

    private PsiDocComment createDocuComment(String template) {
        // TODO: 02/05/2023 MANAGE ALL COMMON TAGS BEHAVIOURS FOR THE TEMPLATE
        return factory.createDocCommentFromText(template);
    }

}

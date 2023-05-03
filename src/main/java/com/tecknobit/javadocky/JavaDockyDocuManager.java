package com.tecknobit.javadocky;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.tecknobit.javadocky.JavaDockyConfiguration.MethodType;
import com.tecknobit.javadocky.JavaDockyConfiguration.Tag;

import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

public class JavaDockyDocuManager {

    private final PsiElementFactory factory;
    private final PsiClass psiClass;

    public JavaDockyDocuManager(Project project, PsiClass psiClass) {
        this.factory = JavaPsiFacade.getInstance(project).getElementFactory();
        this.psiClass = psiClass;
    }

    public PsiDocComment createClassDocu(PsiClass currentClass) {
        return createDocuComment(formatClassNameTag(configuration.getClassTemplate(), currentClass));
    }

    public PsiDocComment createFieldDocu(PsiField field) {
        return createDocuComment(formatInstanceTag(configuration.getFieldTemplate(), field));
    }

    public PsiDocComment createConstructorDocu(PsiMethod constructor) {
        return formatParamsTag(constructor, formatClassNameTag(configuration.getConstructorTemplate(), constructor));
    }

    public PsiDocComment createMethodDocu(PsiMethod method) {
        MethodType methodType = reachMethodType(method.getName());
        if (methodType != CUSTOM) {
            String template = configuration.getMethodTemplate(methodType, null);
            if (template != null) {
                if (methodType != SETTER) {
                    return formatParamsTag(method, formatReturnTypeTag(template.replaceAll(instance.getTag(),
                            getReturnInstanceName(method)), method));
                } else {
                    return formatParamsTag(method, template.replaceAll(instance.getTag(), getSetterInstanceName(method)));
                }
            }
        }
        // TODO: 03/05/2023 MAKE DOCU ALSO FOR CUSTOM METHODS
        return null;
    }

    private String getReturnInstanceName(PsiMethod method) {
        String methodBody = method.getBody().getText();
        if (methodBody.contains("return")) {
            return methodBody.split("return")[1]
                    .replace(";", "")
                    .replaceAll(" ", "")
                    .replace("}", "")
                    .replaceAll("\n", "");
        }
        return "";
    }

    private String getSetterInstanceName(PsiMethod method) {
        String methodBody = method.getBody().getText();
        if (methodBody.contains("this"))
            return methodBody.split("this.")[1].split("=")[0].replaceAll(" ", "");
        else {
            String methodName = method.getName().replace("set", "");
            for (PsiField field : psiClass.getFields()) {
                String fName = field.getName();
                if (fName.equalsIgnoreCase(methodName))
                    return fName;
            }
        }
        return "";
    }

    private String formatClassNameTag(String template, NavigationItem item) {
        return formatTag(className, template, item);
    }

    private String formatInstanceTag(String template, NavigationItem item) {
        return formatTag(instance, template, item);
    }

    private String formatReturnTypeTag(String template, PsiMethod method) {
        return template.replaceAll(returnType.getTag(), method.getReturnType().getCanonicalText());
    }

    private String formatTag(Tag tag, String template, NavigationItem item) {
        return template.replaceAll(tag.getTag(), item.getName());
    }

    private PsiDocComment formatParamsTag(PsiMethod method, String template) {
        boolean fieldsTemplateEnabled = configuration.isFieldTemplateEnabled();
        StringBuilder lParams = new StringBuilder();
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (lParams.toString().isEmpty())
                lParams.append("@param ");
            else
                lParams.append(" @param ");
            lParams.append(parameter.getName());
            if (fieldsTemplateEnabled)
                lParams.append(": ").append(formatFieldTemplate(parameter));
            else
                lParams.append(":\n");
            lParams.append("*");
        }
        return createDocuComment(template.replaceFirst(params.getTag(), lParams.toString()));
    }

    private String formatFieldTemplate(PsiParameter parameter) {
        return configuration.getFieldTemplate()
                .replaceAll("/\\**\n *", "")
                .replaceAll("\\*/", "")
                .replaceAll("\\* ", "")
                .replaceAll(instance.getTag(), parameter.getName());
    }

    private PsiDocComment createDocuComment(String template) {
        return factory.createDocCommentFromText(template);
    }

}
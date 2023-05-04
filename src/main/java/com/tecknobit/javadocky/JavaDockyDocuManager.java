package com.tecknobit.javadocky;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.tecknobit.javadocky.JavaDockyConfiguration.MethodType;
import com.tecknobit.javadocky.JavaDockyConfiguration.Tag;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;

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

    public PsiDocComment createMethodDocu(PsiMethod method) throws BackingStoreException {
        String methodName = method.getName();
        MethodType methodType = reachMethodType(methodName);
        if (methodType != CUSTOM) {
            String template = configuration.getMethodTemplate(methodType, null);
            if (template != null) {
                if (methodType != SETTER)
                    return formatInstanceMethodTag(template, method);
                else
                    return formatParamsTag(method, template.replaceAll(instance.getTag(), getSetterInstanceName(method)));
            }
        } else {
            for (String template : configuration.getCustomMethodTemplates()) {
                boolean useDocuTemplate = true;
                String nameContains = Tag.nameContains.getTag();
                if (template.contains(nameContains)) {
                    String nameContainsValue = getTagValue(template, nameContains);
                    boolean useDefName = nameContainsValue.isEmpty();
                    if (useDefName)
                        nameContainsValue = configuration.getCustomMethodName(methodName);
                    if (!methodName.contains(nameContainsValue))
                        useDocuTemplate = false;
                    else {
                        if (useDefName)
                            nameContainsValue = "";
                        template = removeTagFromTemplate(template, nameContains, nameContainsValue);
                    }
                }
                if (useDocuTemplate) {
                    String returnTypeIs = Tag.returnTypeIs.getTag();
                    if (template.contains(returnTypeIs)) {
                        String returnTypeValue = getTagValue(template, returnTypeIs);
                        if (!method.getReturnTypeElement().getText().equals(returnTypeValue))
                            useDocuTemplate = false;
                        else
                            template = removeTagFromTemplate(template, returnTypeIs, returnTypeValue);
                    }
                    if (useDocuTemplate) {
                        String hasP = Tag.hasP.getTag();
                        if (template.contains(hasP)) {
                            String replacer = getTagValue(template, hasP, false);
                            String psiParameters = Arrays.toString(method.getParameterList().getParameters());
                            for (String iHasP : replacer.replaceAll(" ", "").split(","))
                                useDocuTemplate = psiParameters.contains(iHasP);
                            if (useDocuTemplate)
                                template = removeTagFromTemplate(template, hasP, replacer);
                        }
                        if (useDocuTemplate)
                            return formatInstanceMethodTag(template, method);
                    }
                }
            }
        }
        return null;
    }

    private PsiDocComment formatInstanceMethodTag(String template, PsiMethod method) {
        String instanceReplacer = null;
        String methodBody = method.getBody().getText();
        String instanceTag = instance.getTag();
        if (methodBody.contains("return")) {
            instanceReplacer = methodBody.split("return")[1]
                    .replace(";", "")
                    .replaceAll(" ", "")
                    .replace("}", "")
                    .replaceAll("\n", "");
        }
        if (instanceReplacer != null)
            template = template.replaceAll(instanceTag, instanceReplacer);
        else {
            String returnTypeTag = returnType.getTag();
            template = removeTagFromTemplate(template, returnTypeTag, getTagValue(template, returnTypeTag));
            template = removeTagFromTemplate(template, instanceTag, getTagValue(template, instanceTag));
        }
        return formatParamsTag(method, formatReturnTypeTag(template, method));
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

    private String getTagValue(String template, String tag) {
        return getTagValue(template, tag, true);
    }

    private String getTagValue(String template, String tag, boolean removeBlankSpaces) {
        String tagValue = template.split(tag)[1].split("\n")[0];
        if (removeBlankSpaces)
            return tagValue.replaceAll(" ", "");
        return tagValue;
    }

    private String removeTagFromTemplate(String template, String tag, String value) {
        return template.replaceAll(tag + value, "").replaceAll(tag + " " + value, "");
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

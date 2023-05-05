package com.tecknobit.javadocky;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.tecknobit.javadocky.JavaDockyConfiguration.JavaDockyItem;
import com.tecknobit.javadocky.JavaDockyConfiguration.MethodType;
import com.tecknobit.javadocky.JavaDockyConfiguration.Tag;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;

import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.configuration;

/**
 * The {@code JavaDockyDocuManager} class is useful to manage and create the {@code JavaDocky}'s docu-comments
 *
 * @author N7ghtm4r3 - Tecknobit
 **/
public class JavaDockyDocuManager {

    /**
     * {@code factory} useful to add the docu-comment in the {@link #psiClass}
     **/
    private final PsiElementFactory factory;

    /**
     * {@code psiClass} current class where the plugin is working on
     **/
    private final PsiClass psiClass;

    /**
     * Constructor to init {@link JavaDockyDocuManager}
     *
     * @param project:  current project where the plugin is working on
     * @param psiClass: current class where the plugin is working on
     **/
    public JavaDockyDocuManager(Project project, PsiClass psiClass) {
        this.factory = JavaPsiFacade.getInstance(project).getElementFactory();
        this.psiClass = psiClass;
    }

    /**
     * Method to create a docu-comment with the {@link JavaDockyItem#Classes}'s template
     *
     * @param currentClass: current class where add the docu-comment
     * @return the docu-comment created with the {@link JavaDockyItem#Classes}'s template as {@link PsiDocComment}
     **/
    public PsiDocComment createClassDocu(PsiClass currentClass) {
        return createDocuComment(formatClassNameTag(configuration.getClassTemplate(), currentClass));
    }

    /**
     * Method to create a docu-comment with the {@link JavaDockyItem#Fields}'s template
     *
     * @param field: field where add the docu-comment
     * @return the docu-comment created with the {@link JavaDockyItem#Fields}'s template as {@link PsiDocComment}
     **/
    public PsiDocComment createFieldDocu(PsiField field) {
        return createDocuComment(formatInstanceTag(configuration.getFieldTemplate(), field));
    }

    /**
     * Method to create a docu-comment with the {@link JavaDockyItem#Constructors}'s template
     *
     * @param constructor: constructor where add the docu-comment
     * @return the docu-comment created with the {@link JavaDockyItem#Constructors}'s template as {@link PsiDocComment}
     **/
    public PsiDocComment createConstructorDocu(PsiMethod constructor) {
        return formatParamsTag(constructor, formatClassNameTag(configuration.getConstructorTemplate(), constructor));
    }

    /**
     * Method to create a docu-comment with the {@link JavaDockyItem#Methods}'s template
     *
     * @param method: method where add the docu-comment
     * @return the docu-comment created with the {@link JavaDockyItem#Methods}'s template as {@link PsiDocComment}
     * @apiNote this method will automatically fetch if the template to use is {@link MethodType#CUSTOM} or not
     **/
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

    /**
     * Method to format the {@link Tag#instance}'s tag to create a docu-comment from the template
     * with the return instance value if the method is not void, in that case the {@link Tag#instance}'s tag will
     * be deleted:
     * <ul>
     *     <li>
     *         <b>not_void_method</b> ->
     * <pre>
     *   {@code
     *       // <instance> in the docu-comment
     *       public Object getAnyObject() {
     *           return anyObject;
     *       }
     *   }
     *  </pre>
     *        the {@link Tag#instance}'s tag will be replaced with the {@code "anyObject"} value
     *     </li>
     *     <li>
     *         <b>void_method</b> ->
     * <pre>
     *   {@code
     *       // <instance> in the docu-comment
     *       public void anyMethod() {
     *           // your code here
     *       }
     *   }
     *  </pre>
     *        the {@link Tag#instance}'s tag will be deleted
     *     </li>
     * </ul>
     *
     * @param template: template of the method to format
     * @param method:   method where add the docu-comment
     * @return the docu-comment created with the template formatted as {@link PsiDocComment}
     * @apiNote after will be invoked also the following methods:
     * <ul>
     *     <li>
     *         {@link #formatParamsTag(PsiMethod, String)}
     *     </li>
     *     <li>
     *         {@link #formatReturnTypeTag(String, PsiMethod)}
     *     </li>
     * </ul>
     **/
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

    /**
     * Method to fetch from a setter method the setter instance:
     * <ul>
     *     <li>
     *         <b>standard setter's template</b> ->
     * <pre>
     *   {@code
     *       // <instance> in the docu-comment
     *       public void setAnyObject(Object anyObject) {
     *           this.anyObject = anyObject;
     *       }
     *   }
     *  </pre>
     *        the {@link Tag#instance}'s tag will be replaced with the {@code "anyObject"} value
     *     </li>
     *     <li>
     *         <b>not standard setter's template</b> ->
     * <pre>
     *   {@code
     *       // <instance> in the docu-comment
     *       public void setAnyObject(Object differentAnyObjectInstanceName) {
     *           anyObject = differentAnyObjectInstanceName;
     *       }
     *   }
     *  </pre>
     *        the {@link Tag#instance}'s tag will be replaced with the {@code "anyObject"} value searched from the
     *        fields of the class
     *     </li>
     * </ul>
     *
     * @param method: method from fetch the setter instance
     * @return setter instance as {@link String}
     **/
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

    /**
     * Method to fetch from a template a {@link Tag}'s value
     *
     * @param template: template from fetch the value of a {@link Tag}
     * @param tag:      the tag from fetch the value
     * @return tag value as {@link String}
     **/
    private String getTagValue(String template, String tag) {
        return getTagValue(template, tag, true);
    }

    /**
     * Method to fetch from a template a {@link Tag}'s value
     *
     * @param template:          template from fetch the value of a {@link Tag}
     * @param tag:               the tag from fetch the value
     * @param removeBlankSpaces: whether remove the blank spaces
     * @return tag value as {@link String}
     **/
    private String getTagValue(String template, String tag, boolean removeBlankSpaces) {
        String tagValue = template.split(tag)[1].split("\n")[0];
        if (removeBlankSpaces)
            return tagValue.replaceAll(" ", "");
        return tagValue;
    }

    /**
     * Method to remove from a template a {@link Tag} and its value
     *
     * @param template: template from remove a {@link Tag} and its value
     * @param tag:      the tag to remove
     * @param value:    the value of a tag to remove
     * @return template as {@link String}
     **/
    private String removeTagFromTemplate(String template, String tag, String value) {
        return template.replaceAll(tag + value, "").replaceAll(tag + " " + value, "");
    }

    /**
     * Method to format the {@link Tag#className}'s tag to create a docu-comment from the template
     *
     * @param template: template to format
     * @param item:     item where add the docu-comment
     * @return the template formatted as {@link String}
     **/
    private String formatClassNameTag(String template, NavigationItem item) {
        return formatTag(className, template, item);
    }

    /**
     * Method to format the {@link Tag#instance}'s tag to create a docu-comment from the template
     *
     * @param template: template to format
     * @param item:     item where add the docu-comment
     * @return the template formatted as {@link String}
     **/
    private String formatInstanceTag(String template, NavigationItem item) {
        return formatTag(instance, template, item);
    }

    /**
     * Method to format a {@link Tag} to create a docu-comment from the template
     *
     * @param tag:      tag to format
     * @param template: template to format
     * @param item:     item where add the docu-comment
     * @return the template formatted as {@link String}
     **/
    private String formatTag(Tag tag, String template, NavigationItem item) {
        return template.replaceAll(tag.getTag(), item.getName());
    }

    /**
     * Method to format the {@link Tag#returnType}'s tag to create a docu-comment from the template
     *
     * @param template: template of the method to format
     * @param method    method from fetch the value of the {@link Tag#returnType}
     * @return the template formatted as {@link String}
     **/
    private String formatReturnTypeTag(String template, PsiMethod method) {
        return template.replaceAll(returnType.getTag(), method.getReturnType().getCanonicalText());
    }

    /**
     * Method to fetch from a method the params list
     * <pre>
     *   {@code
     *       // <params> in the docu-comment
     *       public void anyMethod(Object anyObject, String anyString) {
     *           // your code here
     *       }
     *   }
     *  </pre>
     * the {@link Tag#params}'s tag will be replaced with the list of the method's parameters:
     * <ul>
     *     <li>
     *         <b>if the {@link JavaDockyItem#Fields}</b> is disabled -> <br>
     *         {@code @param anyObject} <br>
     *         {@code @param anyString} <br>
     *     </li>
     *     <li>
     *         <b>if the {@link JavaDockyItem#Fields}</b> is enabled -> <br>
     *         {@code @param anyObject - field template} <br>
     *         {@code @param anyString - field template} <br>
     *     </li>
     * </ul>
     *
     * @param method:   the method from fetch the params list
     * @param template: the template from fetch the params list
     * @return the docu-comment created with the template formatted as {@link PsiDocComment}
     **/
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

    /**
     * Method to fetch from a field the docu-template value
     * <pre>
     *   {@code
     *       // its template for the docu-comment -> instance to make any actions
     *       Object anyObject;
     *   }
     *  </pre>
     * will be fetched {@code "instance to make any actions"}
     *
     * @param parameter: the parameter from fetch the docu-template value
     * @return the docu-template value of the field as {@link String}
     **/
    private String formatFieldTemplate(PsiParameter parameter) {
        return configuration.getFieldTemplate()
                .replaceAll("/\\**\n *", "")
                .replaceAll("\\*/", "")
                .replaceAll("\\* ", "")
                .replaceAll(instance.getTag(), parameter.getName());
    }

    /**
     * Method to create a docu-comment with a template
     *
     * @param template: the template to use to create the docu-comment
     * @return the docu-comment created with the template as {@link PsiDocComment}
     **/
    private PsiDocComment createDocuComment(String template) {
        return factory.createDocCommentFromText(template);
    }

}

package com.tecknobit.javadocky;

import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.tecknobit.javadocky.JavaDockyConfiguration.JavaDockyItem.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.*;

/**
 * The {@code JavaDockyConfiguration} class is useful to manage the {@code JavaDocky}'s configuration
 *
 * @author N7ghtm4r3 - Tecknobit
 **/
public class JavaDockyConfiguration {

    /**
     * {@code Tag} list of available tags to use to give directions to {@code JavaDocky}
     **/
    public enum Tag {

        /**
         * {@code className} tag -> use to fetch the name of the class
         **/
        className("className"),

        /**
         * {@code instance} tag -> use to link to an instance of the class
         **/
        instance("instance"),

        /**
         * {@code params} tag -> use to insert the linked params
         **/
        params("params"),

        /**
         * {@code returnType} tag -> use to fetch the return type of method
         **/
        returnType("returnType");

        /**
         * {@code tag} value
         **/
        private final String tag;

        /**
         * Constructor to init {@link Tag}
         *
         * @param tag: tag value
         **/
        Tag(String tag) {
            this.tag = "<" + tag + ">";
        }

        /**
         * Method to get {@link #tag} instance <br>
         * No-any params required
         *
         * @return {@link #tag} instance as {@link String}
         **/
        public String getTag() {
            return tag;
        }

    }

    /**
     * {@code JavaDockyItem} list of available {@code JavaDocky}'s items
     **/
    public enum JavaDockyItem {

        /**
         * {@code Classes} item -> to create and use a docu-template for all the classes
         **/
        Classes,

        /**
         * {@code Fields} item -> to create and use a docu-template for all the fields
         **/
        Fields,

        /**
         * {@code Constructors} item -> to create and use a docu-template for all the constructors
         **/
        Constructors,

        /**
         * {@code Methods} item -> to create and use a docu-template for methods
         **/
        Methods

    }

    /**
     * {@code MethodType} list of available {@code JavaDocky}'s method types
     **/
    public enum MethodType {

        /**
         * {@code HASH_CODE} -> to create and use a docu-template for all the HASH_CODE methods
         **/
        HASH_CODE,

        /**
         * {@code EQUALS} -> to create and use a docu-template for all the EQUALS methods
         **/
        EQUALS,

        /**
         * {@code CLONE} -> to create and use a docu-template for all the CLONE methods
         **/
        CLONE,

        /**
         * {@code TO_STRING} -> to create and use a docu-template for all the TO_STRING methods
         **/
        TO_STRING,

        /**
         * {@code GETTER} -> to create and use a docu-template for all the GETTER methods
         **/
        GETTER,

        /**
         * {@code SETTER} -> to create and use a docu-template for all the SETTER methods
         **/
        SETTER,

        /**
         * {@code CUSTOM} -> to create and use a docu-template for all the CUSTOM methods that the user choose
         **/
        CUSTOM;

        /**
         * Method to check the validity of a method
         *
         * @param method: method to check the validity
         * @return whether a method is valid
         **/
        public static boolean isValidMethod(String method) {
            try {
                JavaDockyItem.valueOf(method);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        }

    }

    /**
     * {@code configuration} instance to manage the {@code JavaDocky}'s configuration
     **/
    public static final JavaDockyConfiguration configuration = new JavaDockyConfiguration();

    /**
     * {@code preferences} useful to manage the data stored by this plugin
     **/
    private static final Preferences preferences = Preferences.userRoot().node("/user/javadocky");

    /**
     * {@code defDocuTemplate} default docu-template
     **/
    public static final String defDocuTemplate = "/**\n *\n **/";

    /**
     * Method to add a docu-template
     *
     * @param item:     the item where use the docu-template
     * @param template: template value for the specified item
     **/
    public <T> void addDocuTemplate(T item, String template) {
        preferences.put(item.toString(), template);
    }

    /**
     * Method to remove a docu-template
     *
     * @param item: the item from remove the docu-template
     **/
    public <T> void removeDocuTemplate(T item) {
        preferences.remove(item.toString());
    }

    /**
     * Method to get a docu-template
     *
     * @param item: the item where use the docu-template
     * @return item template as {@link String}
     **/
    public String getItemTemplate(JavaDockyItem item) {
        return getItemTemplate(item, defDocuTemplate);
    }

    /**
     * Method to get a docu-template
     *
     * @param item: the item where use the docu-template
     * @param def:  def value to return if not exits yet
     * @return item template as {@link String}
     **/
    public String getItemTemplate(JavaDockyItem item, String def) {
        return preferences.get(item.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Classes}'s item <br>
     * No-any params required
     *
     * @return {@link JavaDockyItem#Classes}'s template as {@link String}
     **/
    public String getClassTemplate() {
        return getClassTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Classes}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link JavaDockyItem#Classes}'s template as {@link String}
     **/
    public String getClassTemplate(String def) {
        return preferences.get(Classes.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Fields}'s item <br>
     * No-any params required
     *
     * @return {@link JavaDockyItem#Fields}'s template as {@link String}
     **/
    public String getFieldTemplate() {
        return getFieldTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Fields}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link JavaDockyItem#Fields}'s template as {@link String}
     **/
    public String getFieldTemplate(String def) {
        return preferences.get(Fields.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Constructors}'s item <br>
     * No-any params required
     *
     * @return {@link JavaDockyItem#Constructors}'s template as {@link String}
     **/
    public String getConstructorTemplate() {
        return getConstructorTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Constructors}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link JavaDockyItem#Constructors}'s template as {@link String}
     **/
    public String getConstructorTemplate(String def) {
        return preferences.get(Constructors.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Methods}'s item
     *
     * @param type: type of the method to fetch the template
     * @return {@link JavaDockyItem#Methods}'s template as {@link String}
     **/
    public <T> String getMethodTemplate(T type) {
        return getMethodTemplate(type, defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link JavaDockyItem#Methods}'s item
     *
     * @param type: type of the method to fetch the template
     * @param def:  def value to return if not exits yet
     * @return {@link JavaDockyItem#Methods}'s template as {@link String}
     **/
    public <T> String getMethodTemplate(T type, String def) {
        return preferences.get(type.toString(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#HASH_CODE}'s item <br>
     * No-any params required
     *
     * @return {@link MethodType#HASH_CODE}'s template as {@link String}
     **/
    public String getHashCodeTemplate() {
        return getHashCodeTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#HASH_CODE}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link MethodType#HASH_CODE}'s template as {@link String}
     **/
    public String getHashCodeTemplate(String def) {
        return preferences.get(HASH_CODE.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#EQUALS}'s item <br>
     * No-any params required
     *
     * @return {@link MethodType#EQUALS}'s template as {@link String}
     **/
    public String getEqualsTemplate() {
        return getEqualsTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#EQUALS}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link MethodType#EQUALS}'s template as {@link String}
     **/
    public String getEqualsTemplate(String def) {
        return preferences.get(EQUALS.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#CLONE}'s item <br>
     * No-any params required
     *
     * @return {@link MethodType#CLONE}'s template as {@link String}
     **/
    public String getCloneTemplate() {
        return getCloneTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#CLONE}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link MethodType#CLONE}'s template as {@link String}
     **/
    public String getCloneTemplate(String def) {
        return preferences.get(CLONE.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#TO_STRING}'s item <br>
     * No-any params required
     *
     * @return {@link MethodType#TO_STRING}'s template as {@link String}
     **/
    public String getToStringTemplate() {
        return getToStringTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#TO_STRING}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link MethodType#TO_STRING}'s template as {@link String}
     **/
    public String getToStringTemplate(String def) {
        return preferences.get(TO_STRING.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#GETTER}'s item <br>
     * No-any params required
     *
     * @return {@link MethodType#GETTER}'s template as {@link String}
     **/
    public String getGetterTemplate() {
        return getGetterTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#GETTER}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link MethodType#GETTER}'s template as {@link String}
     **/
    public String getGetterTemplate(String def) {
        return preferences.get(GETTER.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#SETTER}'s item <br>
     * No-any params required
     *
     * @return {@link MethodType#SETTER}'s template as {@link String}
     **/
    public String getSetterTemplate() {
        return getSetterTemplate(defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#SETTER}'s item
     *
     * @param def: def value to return if not exits yet
     * @return {@link MethodType#SETTER}'s template as {@link String}
     **/
    public String getSetterTemplate(String def) {
        return preferences.get(SETTER.name(), def);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#CUSTOM}'s item
     *
     * @param methodName: method name of the {@link MethodType#CUSTOM} method to fetch the template
     * @return {@link MethodType#CUSTOM}'s template as {@link String}
     **/
    public String getCustomMethodTemplate(String methodName) {
        return getCustomMethodTemplate(methodName, defDocuTemplate);
    }

    /**
     * Method to get the docu-template for the {@link MethodType#CUSTOM}'s item
     *
     * @param methodName: method name of the {@link MethodType#CUSTOM} method to fetch the template
     * @param def:        def value to return if not exits yet
     * @return {@link MethodType#CUSTOM}'s template as {@link String}
     **/
    public String getCustomMethodTemplate(String methodName, String def) {
        return preferences.get(CUSTOM.name() + methodName, def);
    }

    /**
     * Method to get the list of custom method templates <br>
     * No-any params required
     *
     * @return list of custom method templates as {@link ArrayList} of {@link String}
     **/
    public ArrayList<String> getCustomMethodTemplates() throws BackingStoreException {
        ArrayList<String> templates = new ArrayList<>();
        for (String method : preferences.keys())
            if (method.startsWith(CUSTOM.name()))
                templates.add(method.replace(CUSTOM.name(), ""));
        return templates;
    }

    /**
     * Method to get the custom method menu items <br>
     * No-any params required
     *
     * @return custom method menu items as array of {@link String}
     **/
    public String[] getCustomMethodMenuItems() throws BackingStoreException {
        ArrayList<String> templates = getCustomMethodTemplates();
        templates.add("Add custom method");
        return templates.toArray(new String[0]);
    }

    /**
     * Method to remove a single template for a method
     *
     * @param method: method identifier to remove the corresponding method
     **/
    public void removeMethodTemplate(String method) {
        if (getCustomMethodTemplate(method, null) != null)
            method = CUSTOM.name() + method;
        preferences.remove(method);
    }

    /**
     * Method to remove all the method templates <br>
     * No-any params required
     **/
    public void removeAllMethodTemplates() {
        preferences.remove(Methods.name());
        try {
            for (String method : preferences.keys())
                if (MethodType.isValidMethod(method))
                    preferences.remove(method);
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

}

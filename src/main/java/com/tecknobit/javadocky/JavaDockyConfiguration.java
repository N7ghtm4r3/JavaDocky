package com.tecknobit.javadocky;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.tecknobit.javadocky.JavaDockyConfiguration.JavaDockyItem.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.*;

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

    public enum JavaDockyItem {

        Classes,
        Fields,
        Constructors,
        Methods

    }

    public enum MethodType {

        HASH_CODE,
        EQUALS,
        CLONE,
        TO_STRING,
        GETTER,
        SETTER,
        CUSTOM

    }

    public static final JavaDockyConfiguration configuration = new JavaDockyConfiguration();

    /**
     * {@code preferences} useful to manage the data stored by this plugin
     **/
    private static final Preferences preferences = Preferences.userRoot().node("/user/javadocky");

    private static final String defDocuTemplate = "/**\n *\n **/";

    public JavaDockyConfiguration() {

    }

    public <T extends Enum<?>> void addDocuTemplate(T item, String template) {
        preferences.put(item.name(), template);
    }

    public <T extends Enum<?>> void removeDocuTemplate(T item) {
        preferences.remove(item.name());
    }

    public String getItemTemplate(JavaDockyItem item) {
        return getItemTemplate(item, defDocuTemplate);
    }

    public String getItemTemplate(JavaDockyItem item, String def) {
        return preferences.get(item.name(), def);
    }

    public String getClassTemplate() {
        return getClassTemplate(defDocuTemplate);
    }

    public String getClassTemplate(String def) {
        return preferences.get(Classes.name(), def);
    }

    public String getFieldTemplate() {
        return getFieldTemplate(defDocuTemplate);
    }

    public String getFieldTemplate(String def) {
        return preferences.get(Fields.name(), def);
    }

    public String getConstructorTemplate() {
        return getConstructorTemplate(defDocuTemplate);
    }

    public String getConstructorTemplate(String def) {
        return preferences.get(Constructors.name(), def);
    }

    public String getMethodTemplate(MethodType type) {
        return getMethodTemplate(type, defDocuTemplate);
    }

    public String getMethodTemplate(MethodType type, String def) {
        return preferences.get(type.name(), def);
    }

    public String getHashCodeTemplate() {
        return getHashCodeTemplate(defDocuTemplate);
    }

    public String getHashCodeTemplate(String def) {
        return preferences.get(HASH_CODE.name(), def);
    }

    public String getEqualsTemplate() {
        return getEqualsTemplate(defDocuTemplate);
    }

    public String getEqualsTemplate(String def) {
        return preferences.get(EQUALS.name(), def);
    }

    public String getCloneTemplate() {
        return getCloneTemplate(defDocuTemplate);
    }

    public String getCloneTemplate(String def) {
        return preferences.get(CLONE.name(), def);
    }

    public String getToStringTemplate() {
        return getToStringTemplate(defDocuTemplate);
    }

    public String getToStringTemplate(String def) {
        return preferences.get(TO_STRING.name(), def);
    }

    public String getGetterTemplate() {
        return getGetterTemplate(defDocuTemplate);
    }

    public String getGetterTemplate(String def) {
        return preferences.get(GETTER.name(), def);
    }

    public String getSetterTemplate() {
        return getSetterTemplate(defDocuTemplate);
    }

    public String getSetterTemplate(String def) {
        return preferences.get(SETTER.name(), def);
    }

    public String getCustomMethodTemplate(String methodName) {
        return getCustomMethodTemplate(methodName, defDocuTemplate);
    }

    public String getCustomMethodTemplate(String methodName, String def) {
        return preferences.get(CUSTOM.name() + methodName, def);
    }

    public ArrayList<String> getCustomMethodsTemplates() throws BackingStoreException {
        ArrayList<String> templates = new ArrayList<>();
        // TODO: 28/04/2023 MANAGE TO GET THE CORRECT CUSTOM LIST 
        System.out.println(Arrays.toString(preferences.keys()));
        return null;
    }

}

package com.tecknobit.javadocky;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.*;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.table.JBTable;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.openapi.ui.Messages.showInputDialog;
import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;
import static com.intellij.util.ui.JBUI.Borders.empty;
import static com.tecknobit.javadocky.JavaDockyConfiguration.*;
import static com.tecknobit.javadocky.JavaDockyConfiguration.JavaDockyItem.Methods;
import static com.tecknobit.javadocky.JavaDockyConfiguration.MethodType.CUSTOM;
import static com.tecknobit.javadocky.JavaDockyConfiguration.Tag.*;
import static java.awt.Color.getColor;
import static java.awt.Font.*;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.SOUTH;

/**
 * The {@code JavaDockyDashboard} class is useful to manage the {@code JavaDocky}'s configuration and set all the
 * docu-string templates to use
 *
 * @author N7ghtm4r3 - Tecknobit
 * @see ToolWindowFactory
 **/
public class JavaDockyDashboard implements ToolWindowFactory {

    /**
     * Method to create the toolwindow to insert the plugin UI
     *
     * @param project:    the current project
     * @param toolWindow: toolwindow where set the content
     **/
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            JavaDockyContent javaDockyContent = new JavaDockyContent(project);
            Content content = getInstance().createContent(javaDockyContent.getContent(), "", false);
            toolWindow.getContentManager().addContent(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The {@code JavaDockyContent} class is useful to manage the plugin's UI content and manage the dashboard
     *
     * @author N7ghtm4r3 - Tecknobit
     **/
    private static class JavaDockyContent {

        /**
         * {@code contentPanel} main panel to contain the ui
         **/
        private final JPanel contentPanel = new JPanel();

        /**
         * {@code project} the project in use
         **/
        private final Project project;

        /**
         * {@code methodTextField} the editor text field for the method item
         **/
        private EditorTextField methodTextField;

        /**
         * {@code customTemplates} the combobox for the custom method templates
         **/
        private ComboBox<String> customTemplates;

        /**
         * Constructor to init {@link JavaDockyContent}
         *
         * @param project: the current project
         **/
        public JavaDockyContent(Project project) throws Exception {
            this.project = project;
            contentPanel.setLayout(new VerticalLayout(10));
            contentPanel.setBorder(empty(10));
            contentPanel.add(getHeaderTitle("Tags"));
            setTagsLayout();
            contentPanel.add(getHeaderTitle("Configuration"));
            setConfigurationLayout();
        }

        /**
         * Method to get a header title
         *
         * @param title: title of the label
         * @return header title as {@link JLabel}
         **/
        private JLabel getHeaderTitle(String title) {
            JLabel lTitle = new JLabel(title);
            lTitle.setFont(getFontText(20));
            return lTitle;
        }

        /**
         * Method to set the tags layout <br>
         * No-any params required
         **/
        private void setTagsLayout() {
            final HashMap<Tag, String> descriptions = new HashMap<>();
            descriptions.put(className, "Fetch the name of the class");
            descriptions.put(instance, "Link to an instance of the class");
            descriptions.put(params, "Insert the linked params");
            descriptions.put(returnType, "Fetch the return type of method");
            descriptions.put(hasP, "Document methods with the specified params");
            descriptions.put(returnTypeIs, "Document methods with the specified return type");
            descriptions.put(nameContains, "Document methods if contains the specified name");
            JBTable table = new JBTable();
            table.setDefaultEditor(Object.class, null);
            table.setRowSelectionAllowed(false);
            table.setCellSelectionEnabled(true);
            table.setFont(getFontText(SANS_SERIF, 15));
            DefaultTableModel model = new DefaultTableModel(new String[]{"Tag", "Description"}, 0);
            for (Tag tag : Tag.values())
                model.addRow(new Object[]{tag.getTag(), descriptions.get(tag)});
            table.setModel(model);
            contentPanel.add(new JBScrollPane(table));
        }

        /**
         * Method to set the configuration items layout
         **/
        private void setConfigurationLayout() throws Exception {
            for (JavaDockyItem item : JavaDockyItem.values()) {
                JPanel container = new JPanel(new VerticalLayout());
                container.setBorder(createLineBorder(getColor("#f5f5f5"), 1));
                ComboBox<MethodType> comboBox;
                EditorTextField docuText;
                JPanel itemPanel = new JPanel(new HorizontalLayout(50));
                itemPanel.setBorder(empty(10));
                JCheckBox itemCheckBox = new JCheckBox(item.name());
                itemCheckBox.setFont(getFontText(15));
                itemCheckBox.setSelected(configuration.getItemTemplate(item, null) != null);
                itemPanel.add(itemCheckBox);
                BasicArrowButton arrowButton = new BasicArrowButton(SOUTH) {
                    /**
                     * {@inheritDoc}
                     **/
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(25, 25);
                    }
                };
                arrowButton.setBorder(createLineBorder(new Color(0f, 0f, 0f, 0f)));
                arrowButton.setVisible(itemCheckBox.isSelected());
                if (item == Methods) {
                    docuText = null;
                    comboBox = new ComboBox<>(MethodType.values());
                    setDefComboBoxLayout(comboBox, false);
                    final JPanel[] customMethodPanel = {null};
                    itemCheckBox.addActionListener(new ActionListener() {
                        /**
                         * {@inheritDoc}
                         **/
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            boolean isSelected = itemCheckBox.isSelected();
                            if (isSelected)
                                configuration.addDocuTemplate(item, "");
                            else
                                configuration.removeAllMethodTemplates();
                            setButtonVisibility(arrowButton, isSelected);
                            setDefComboBoxLayout(comboBox, arrowButton.getDirection() != SOUTH);
                            if (customMethodPanel[0] != null) {
                                container.remove(customMethodPanel[0]);
                                customMethodPanel[0] = null;
                            }
                            removeMethodTextField(container);
                        }
                    });
                    arrowButton.addActionListener(e -> {
                        boolean isVisible = arrowButton.getDirection() == SOUTH;
                        setDefComboBoxLayout(comboBox, isVisible);
                        setButtonDirection(arrowButton);
                        if (!isVisible) {
                            if (customMethodPanel[0] != null) {
                                container.remove(customMethodPanel[0]);
                                customMethodPanel[0] = null;
                            }
                            removeMethodTextField(container);
                        }
                    });
                    comboBox.addActionListener(e -> {
                        try {
                            if (comboBox.getSelectedIndex() != -1) {
                                MethodType methodType = (MethodType) comboBox.getSelectedItem();
                                if (methodType != CUSTOM) {
                                    if (customMethodPanel[0] != null) {
                                        container.remove(customMethodPanel[0]);
                                        customMethodPanel[0] = null;
                                    }
                                    manageMethodText(methodType, container);
                                } else {
                                    removeMethodTextField(container);
                                    if (customMethodPanel[0] == null) {
                                        customMethodPanel[0] = new JPanel(new VerticalLayout());
                                        customMethodPanel[0].setBorder(empty(15));
                                        JLabel jTitle = new JLabel("Current custom methods templates");
                                        jTitle.setFont(getFontText(15));
                                        customMethodPanel[0].add(jTitle);
                                        customTemplates = new ComboBox<>(configuration.getCustomMethodMenuItems());
                                        customTemplates.addActionListener(e1 -> {
                                            try {
                                                String selectedItem = (String) customTemplates.getSelectedItem();
                                                if (selectedItem.equals("Add custom method")) {
                                                    String name = showInputDialog(project, "Insert a name for " +
                                                            "the custom method to add", "Custom Method Name", null);
                                                    if (name != null) {
                                                        if (!name.isEmpty()) {
                                                            if (configuration.getCustomMethodTemplate(name, null)
                                                                    == null) {
                                                                configuration.addDocuTemplate(methodType.name()
                                                                        + name, defDocuTemplate);
                                                                customTemplates.removeAllItems();
                                                                for (String method : configuration.getCustomMethodMenuItems())
                                                                    customTemplates.addItem(method);
                                                                customTemplates.setEditable(true);
                                                                customTemplates.setSelectedItem(name);
                                                                customTemplates.setEditable(false);
                                                                manageMethodText(name, container);
                                                            } else {
                                                                showErrorDialog("The name inserted is already used",
                                                                        "Name Already Exists");
                                                                setDefCustomTemplatesLayout(container);
                                                            }
                                                        } else {
                                                            showErrorDialog("You must insert a valid name for " +
                                                                    "the custom method", "Wrong Name");
                                                            setDefCustomTemplatesLayout(container);
                                                        }
                                                    } else
                                                        setDefCustomTemplatesLayout(container);
                                                } else
                                                    manageMethodText(selectedItem, container);
                                            } catch (Exception exception) {
                                                throw new RuntimeException(exception);
                                            }
                                        });
                                        customMethodPanel[0].add(customTemplates);
                                        container.add(customMethodPanel[0]);
                                    }
                                }
                            }
                        } catch (ClassCastException ignore) {
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                } else {
                    comboBox = null;
                    docuText = createTextEditor(false);
                    itemCheckBox.addActionListener(new ActionListener() {
                        /**
                         * {@inheritDoc}
                         **/
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDocuTextLayout(docuText, false, item);
                            setButtonVisibility(arrowButton, itemCheckBox.isSelected());
                            configuration.removeDocuTemplate(item);
                        }
                    });
                    arrowButton.addActionListener(new ActionListener() {
                        /**
                         * {@inheritDoc}
                         **/
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDocuTextLayout(docuText, arrowButton.getDirection() != NORTH, item);
                            setButtonDirection(arrowButton);
                        }
                    });
                    addEditorListener(docuText, item);
                }
                itemPanel.add(arrowButton);
                container.add(itemPanel);
                if (comboBox != null)
                    container.add(comboBox);
                else
                    container.add(docuText);
                contentPanel.add(container);
            }
        }

        /**
         * Method to get the font for a {@link JComponent}
         *
         * @param size: size of the text
         * @return font as {@link Font}
         **/
        private Font getFontText(int size) {
            return getFontText(DIALOG, size);
        }

        /**
         * Method to get the font for a {@link JComponent}
         *
         * @param font: font to use
         * @param size: size of the text
         * @return font as {@link Font}
         **/
        private Font getFontText(String font, int size) {
            return new Font(font, PLAIN, size);
        }

        /**
         * Method to set the arrow button visibility
         *
         * @param arrowButton: arrow button
         * @param isVisible:   whether the arrow button must be visible
         **/
        private void setButtonVisibility(BasicArrowButton arrowButton, boolean isVisible) {
            arrowButton.setVisible(isVisible);
            if (!isVisible)
                arrowButton.setDirection(SOUTH);
        }

        /**
         * Method to set the arrow button direction
         *
         * @param arrowButton: arrow button to set the direction of the arrow
         **/
        private void setButtonDirection(BasicArrowButton arrowButton) {
            if (arrowButton.getDirection() == NORTH)
                arrowButton.setDirection(SOUTH);
            else
                arrowButton.setDirection(NORTH);
        }

        /**
         * Method to create an editor text field
         *
         * @param isVisible: whether the editor text field must be visible
         * @return editor text field as {@link EditorTextField}
         **/
        private EditorTextField createTextEditor(boolean isVisible) throws Exception {
            // TODO: 02/05/2023 FIX WHEN THE IDE HAS NOT GOT ANY CLASSES OR FILES OPENED BECAUSE THIS WILL BE NULL
            //  AND THE DASHBOARD WILL CRASH
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null)
                throw new Exception("Cannot execute JavaDocky");
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (psiFile == null)
                throw new Exception("Cannot execute JavaDocky");
            PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
            PsiExpressionCodeFragment code = JavaCodeFragmentFactory.getInstance(project)
                    .createExpressionCodeFragment("", element, null, true);
            Document document = PsiDocumentManager.getInstance(project).getDocument(code);
            EditorTextField editorTextField = new EditorTextField(document, editor.getProject(), JavaFileType.INSTANCE);
            editorTextField.setFont(getFontText(13));
            editorTextField.setPreferredWidth(300);
            editorTextField.setOneLineMode(false);
            editorTextField.setVisible(isVisible);
            return editorTextField;
        }

        /**
         * Method to set the default layout of the combobox
         *
         * @param comboBox:  combo box to work on
         * @param isVisible: whether the combobox must be visible
         **/
        private void setDefComboBoxLayout(ComboBox<MethodType> comboBox, boolean isVisible) {
            comboBox.setVisible(isVisible);
            comboBox.setEditable(true);
            comboBox.setSelectedIndex(-1);
            comboBox.setSelectedItem("CHOOSE DOCU-TEMPLATE FOR A METHOD");
            comboBox.setEditable(false);
        }

        /**
         * Method to manage the method panel
         *
         * @param method:    method to work on
         * @param container: the panel of the method layout
         **/
        private <T> void manageMethodText(T method, JPanel container) throws Exception {
            removeMethodTextField(container);
            try {
                MethodType.valueOf(method.toString());
            } catch (IllegalArgumentException e) {
                method = (T) (CUSTOM.name() + method);
            }
            methodTextField = createTextEditor(true);
            methodTextField.setText(configuration.getMethodTemplate(method));
            addEditorListener(methodTextField, method);
            container.add(methodTextField);
        }

        /**
         * Method to remove the method textfield from the container panel
         *
         * @param container: the panel where remove the container panel
         **/
        private void removeMethodTextField(JPanel container) {
            if (methodTextField != null)
                container.remove(methodTextField);
        }

        /**
         * Method to set the default {@link #customTemplates} layout
         *
         * @param container: the panel where set the default {@link #customTemplates} layout
         **/
        private void setDefCustomTemplatesLayout(JPanel container) {
            customTemplates.setSelectedItem(0);
            removeMethodTextField(container);
        }

        /**
         * Method to set the docu-text layout
         *
         * @param docuText:  the editor where insert the template
         * @param isVisible: whether the arrow button is visible
         * @param item:      the item of the panel
         **/
        private void setDocuTextLayout(EditorTextField docuText, boolean isVisible, JavaDockyItem item) {
            docuText.setVisible(isVisible);
            docuText.setText(configuration.getItemTemplate(item));
        }

        /**
         * Method to add a listener on a text field to get the template for an item
         *
         * @param textField: the editor text field to get the template for an item
         * @param item:      the item used for the textfield
         **/
        private <T> void addEditorListener(EditorTextField textField, T item) {
            boolean deleteIfEmpty = MethodType.isValidMethod(item.toString());
            String sItem = item.toString().replace(CUSTOM.name(), "");
            textField.addDocumentListener(new DocumentListener() {
                /**
                 * Called after the text of the document has been changed.
                 *
                 * @param event the event containing the information about the change.
                 */
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    DocumentListener.super.documentChanged(event);
                    String vDocu = textField.getText();
                    if (vDocu.startsWith("/**") && vDocu.endsWith("*/"))
                        configuration.addDocuTemplate(item, vDocu);
                    else if (vDocu.isEmpty() && deleteIfEmpty) {
                        configuration.removeMethodTemplate(sItem);
                        try {
                            MethodType.valueOf(sItem);
                        } catch (IllegalArgumentException e) {
                            customTemplates.removeItem(sItem);
                            customTemplates.setSelectedItem(0);
                        }
                    }
                }
            });
        }

        /**
         * Method to get {@link #contentPanel} instance <br>
         * No-any params required
         *
         * @return {@link #contentPanel} instance as {@link JBScrollPane} to make it scrollable
         **/
        public JBScrollPane getContent() {
            return new JBScrollPane(contentPanel);
        }

    }

}

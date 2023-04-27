package com.tecknobit.javadocky;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.components.Service;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.prefs.Preferences;

import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;
import static com.intellij.util.ui.JBUI.Borders.empty;
import static com.tecknobit.javadocky.JavaDockyDashboard.Tag.*;
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
@Service
public class JavaDockyDashboard implements ToolWindowFactory {

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

    public enum MethodType {

        HASH_CODE,
        EQUALS,
        CLONE,
        TO_STRING,
        GETTER,
        SETTER,
        CUSTOM

    }

    /**
     * {@code preferences} useful to manage the data stored by this plugin
     **/
    private static final Preferences preferences = Preferences.userRoot().node("/user/javadocky");

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
         * {@code items} available to create the docu-strings templates
         **/
        private static final String[] items = new String[]{"Classes", "Fields", "Constructors", "Methods"};

        /**
         * {@code contentPanel} main panel to contain the ui
         **/
        private final JPanel contentPanel = new JPanel();

        private final Project project;

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
            for (String item : items) {
                JPanel container = new JPanel(new VerticalLayout());
                ComboBox<MethodType> comboBox = null;
                EditorTextField docuText = null;
                container.setBorder(createLineBorder(getColor("#f5f5f5"), 1));
                JPanel itemPanel = new JPanel(new HorizontalLayout(50));
                itemPanel.setBorder(empty(10));
                JCheckBox itemCheckBox = new JCheckBox(item);
                itemCheckBox.setFont(getFontText(15));
                itemCheckBox.setSelected(preferences.get(item, null) != null);
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
                arrowButton.setVisible(itemCheckBox.isSelected());
                arrowButton.setBorder(createLineBorder(new Color(0f, 0f, 0f, 0f)));
                arrowButton.addActionListener(new ActionListener() {
                    /**
                     * {@inheritDoc}
                     **/
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (arrowButton.getDirection() == NORTH)
                            setPanelLayout(arrowButton, docuText, SOUTH, false, item);
                        else
                            setPanelLayout(arrowButton, docuText, NORTH, true, item);
                    }
                });
                itemCheckBox.addActionListener(new ActionListener() {
                    /**
                     * {@inheritDoc}
                     **/
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setPanelLayout(arrowButton, docuText, SOUTH, false, item);
                        boolean isSelected = itemCheckBox.isSelected();
                        arrowButton.setVisible(isSelected);
                        if (!isSelected)
                            preferences.remove(item);
                    }
                });
                if (item.equals("Methods")) {
                    comboBox = new ComboBox<>(MethodType.values());
                    comboBox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            try {
                                System.out.println(e.getItem());
                                createTextEditor();
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });
                    comboBox.setVisible(false);
                } else {
                    docuText = createTextEditor();
                    docuText.addDocumentListener(new DocumentListener() {
                        /**
                         * Called after the text of the document has been changed.
                         *
                         * @param event the event containing the information about the change.
                         */
                        @Override
                        public void documentChanged(@NotNull DocumentEvent event) {
                            DocumentListener.super.documentChanged(event);
                            String vDocu = docuText.getText();
                            if (vDocu.startsWith("/**") && vDocu.endsWith("**/"))
                                preferences.put(item, vDocu);
                        }
                    });
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

        private EditorTextField createTextEditor() throws Exception {
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
            editorTextField.setVisible(false);
            return editorTextField;
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
         * Method to set the panel layout
         *
         * @param arrowButton: arrow button to work on
         * @param docuText:    the editor where insert the template
         * @param direction:   the direction of the arrow button
         * @param isVisible:   whether the arrow button is visible
         * @param item:        the item of the panel
         **/
        private void setPanelLayout(BasicArrowButton arrowButton, EditorTextField docuText, int direction,
                                    boolean isVisible, String item) {
            arrowButton.setDirection(direction);
            if (!item.equals("Methods")) {
                docuText.setVisible(isVisible);
                docuText.setText(preferences.get(item, "/**\n *\n **/"));
            }
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

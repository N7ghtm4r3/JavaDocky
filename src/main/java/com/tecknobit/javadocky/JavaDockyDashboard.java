package com.tecknobit.javadocky;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.*;
import com.intellij.ui.EditorTextField;
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
import java.util.prefs.Preferences;

import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;
import static com.intellij.util.ui.JBUI.Borders.empty;
import static com.tecknobit.javadocky.JavaDockyDashboard.Tag.instance;
import static com.tecknobit.javadocky.JavaDockyDashboard.Tag.returnType;
import static java.awt.Color.getColor;
import static java.awt.Font.DIALOG;
import static java.awt.Font.PLAIN;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.SOUTH;

@Service
public class JavaDockyDashboard implements ToolWindowFactory {

    public enum Tag {

        instance("instance"),
        returnType("returnType");

        private final String tag;

        Tag(String tag) {
            this.tag = "<" + tag + ">";
        }

        public String getTag() {
            return tag;
        }

    }

    private static final Preferences preferences = Preferences.userRoot().node("/user/javadocky");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            JavaDockyContent javaDockyContent = new JavaDockyContent(project);
            Content content = getInstance().createContent(javaDockyContent.getContentPanel(), "", false);
            toolWindow.getContentManager().addContent(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class JavaDockyContent {

        private static final String[] items = new String[]{"Classes", "Fields", "Constructors", "Methods"};
        private final JPanel contentPanel = new JPanel();

        public JavaDockyContent(Project project) throws Exception {
            contentPanel.setLayout(new VerticalLayout(10));
            contentPanel.setBorder(empty(10));
            contentPanel.add(getHeaderTitle("Tags"));
            setTagsLayout();
            contentPanel.add(getHeaderTitle("Configuration"));
            setConfigurationLayout(project);
        }

        private JLabel getHeaderTitle(String title) {
            JLabel lTitle = new JLabel(title);
            lTitle.setFont(new Font(DIALOG, PLAIN, 20));
            return lTitle;
        }

        private void setTagsLayout() {
            final HashMap<Tag, String> descriptions = new HashMap<>();
            descriptions.put(instance, "Link to an instance of the class");
            descriptions.put(returnType, "Fetch the return type of a method");
            JBTable table = new JBTable();
            table.setBorder(createLineBorder(getColor("#f5f5f5"), 1));
            table.setFont(new Font(DIALOG, PLAIN, 15));
            DefaultTableModel model = new DefaultTableModel(new String[]{"Tag", "Description"}, 0);
            model.addRow(new Object[]{"Tag", "Description"});
            for (Tag tag : Tag.values())
                model.addRow(new Object[]{tag.getTag(), descriptions.get(tag)});
            table.setModel(model);
            contentPanel.add(table);
        }

        private void setConfigurationLayout(Project project) throws Exception {
            for (String item : items) {
                JPanel container = new JPanel(new GridLayout(1, 1));
                container.setBorder(createLineBorder(getColor("#f5f5f5"), 1));
                JPanel itemPanel = new JPanel(new HorizontalLayout(50));
                itemPanel.setBorder(empty(10));
                JCheckBox itemCheckBox = new JCheckBox(item);
                itemCheckBox.setFont(new Font(DIALOG, PLAIN, 15));
                itemCheckBox.setSelected(preferences.get(item, null) != null);
                itemPanel.add(itemCheckBox);
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
                EditorTextField docuText = new EditorTextField(document, editor.getProject(), JavaFileType.INSTANCE);
                docuText.setFont(new Font(DIALOG, PLAIN, 13));
                docuText.setPreferredWidth(300);
                docuText.setOneLineMode(false);
                docuText.setVisible(false);
                BasicArrowButton arrowButton = new BasicArrowButton(SOUTH) {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(25, 25);
                    }
                };
                arrowButton.setVisible(itemCheckBox.isSelected());
                arrowButton.setBorder(createLineBorder(new Color(0f, 0f, 0f, 0f)));
                arrowButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (arrowButton.getDirection() == NORTH)
                            setPanelLayout(arrowButton, docuText, container, SOUTH, false, 1, item);
                        else
                            setPanelLayout(arrowButton, docuText, container, NORTH, true, 2, item);
                    }
                });
                itemCheckBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setPanelLayout(arrowButton, docuText, container, SOUTH, false, 1, item);
                        boolean isSelected = itemCheckBox.isSelected();
                        arrowButton.setVisible(isSelected);
                        if (!isSelected)
                            preferences.remove(item);
                    }
                });
                docuText.addDocumentListener(new DocumentListener() {
                    /**
                     * Called after the text of the document has been changed.
                     *
                     * @param event the event containing the information about the change.
                     */
                    @Override
                    public void documentChanged(@NotNull DocumentEvent event) {
                        DocumentListener.super.documentChanged(event);
                        preferences.put(item, docuText.getText());
                    }
                });
                itemPanel.add(arrowButton);
                container.add(itemPanel);
                container.add(docuText);
                contentPanel.add(container);
            }
        }

        private void setPanelLayout(BasicArrowButton arrowButton, EditorTextField docuText, JPanel container,
                                    int direction, boolean isVisible, int rows, String item) {
            container.setLayout(new GridLayout(rows, 1));
            arrowButton.setDirection(direction);
            docuText.setVisible(isVisible);
            docuText.setText(preferences.get(item, "/**\n *\n **/"));
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }

    }

}

package com.tecknobit.javadocky;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.*;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.JBUI;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;
import static java.awt.Color.getColor;
import static java.awt.Font.DIALOG;
import static java.awt.Font.PLAIN;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.SOUTH;

@Service
public class JavaDockyDashboard implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JavaDockyContent javaDockyContent = new JavaDockyContent(project);
        Content content = getInstance().createContent(javaDockyContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class JavaDockyContent {

        private static final Color TRANSPARENT = new Color(0f, 0f, 0f, 0f);
        private static final String[] items = new String[]{"Classes", "Fields", "Constructors", "Methods"};
        private final JPanel contentPanel = new JPanel();

        public JavaDockyContent(Project project) {
            contentPanel.setLayout(new VerticalLayout(10));
            for (String item : items) {
                JPanel container = new JPanel(new GridLayout(1, 1));
                container.setBorder(createLineBorder(getColor("#f5f5f5"), 1));
                JPanel itemPanel = new JPanel(new HorizontalLayout(50));
                itemPanel.setBorder(JBUI.Borders.empty(10));
                JCheckBox itemCheckBox = new JCheckBox(item);
                itemCheckBox.setFont(new Font(DIALOG, PLAIN, 15));
                itemPanel.add(itemCheckBox);
                // TODO: 26/04/2023 TO MANAGER 
                Editor editor = null;
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
                PsiExpressionCodeFragment code = JavaCodeFragmentFactory.getInstance(project).createExpressionCodeFragment("", element, null, true);
                Document document = PsiDocumentManager.getInstance(project).getDocument(code);
                EditorTextField myInput = new EditorTextField(document, project, JavaFileType.INSTANCE);
                BasicArrowButton arrowButton = new BasicArrowButton(SOUTH) {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(25, 25);
                    }
                };
                arrowButton.setVisible(false);
                arrowButton.setBorder(createLineBorder(TRANSPARENT));
                arrowButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (arrowButton.getDirection() == NORTH)
                            setPanelLayout(arrowButton, myInput, container, SOUTH, false, 1);
                        else {
                            setPanelLayout(arrowButton, myInput, container, NORTH, true, 2);
                            // TODO: 26/04/2023 STORING WORKFLOW 
                        }
                    }
                });
                itemCheckBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setPanelLayout(arrowButton, myInput, container, SOUTH, false, 1);
                        arrowButton.setVisible(itemCheckBox.isSelected());
                    }
                });
                itemPanel.add(arrowButton);
                container.add(itemPanel);
                container.add(myInput);
                contentPanel.add(container);
            }
        }

        private void setPanelLayout(BasicArrowButton arrowButton, EditorTextField editorTextField, JPanel container,
                                    int direction, boolean isVisible, int rows) {
            container.setLayout(new GridLayout(rows, 1));
            arrowButton.setDirection(direction);
            editorTextField.setVisible(isVisible);
            // TODO: 26/04/2023 FETCHING FROM MEMORY WHAT TO SET
            editorTextField.setText("/**\n*\n**/");
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }

    }

}

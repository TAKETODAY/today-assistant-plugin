/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.assistant.app.run.editor;

import com.intellij.DynamicBundle;
import com.intellij.application.options.ModuleDescriptionsComboBox;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.execution.ui.ShortenCommandLineModeCombo;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.UIUtil;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.lang.Nullable;

public class EnvironmentSettingsPanel implements PanelWithAnchor {
  private JPanel myRootPanel;
  private CommonJavaParametersPanel myCommonProgramParameters;
  private LabeledComponent<ModuleDescriptionsComboBox> myModule;
  private LabeledComponent<JBCheckBox> myIncludeProvidedCheckbox;
  private JrePathEditor myJrePathEditor;
  private LabeledComponent<ShortenCommandLineModeCombo> myShortenCommandLineModeCombo;
  private final ConfigurationModuleSelector myModuleSelector;
  private InfraApplicationRunConfiguration myConfiguration;
  private JComponent myAnchor;

  interface ModuleChangeListener {
    void moduleChanged(Module module);
  }

  private void $$$setupUI$$$() {
    createUIComponents();
    JPanel jPanel = new JPanel();
    this.myRootPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    CommonJavaParametersPanel commonJavaParametersPanel = new CommonJavaParametersPanel();
    this.myCommonProgramParameters = commonJavaParametersPanel;
    jPanel.add(commonJavaParametersPanel, new GridConstraints(0, 0, 1, 1, 0, 1, 3, 3, null, null, null));
    jPanel.add(new Spacer(), new GridConstraints(1, 0, 1, 1, 0, 2, 1, 0, null, new Dimension(-1, 10), null));
    LabeledComponent<ModuleDescriptionsComboBox> labeledComponent = new LabeledComponent<>();
    this.myModule = labeledComponent;
    labeledComponent.setComponent(new ModuleDescriptionsComboBox());
    labeledComponent.setLabelLocation("West");
    labeledComponent.setText(DynamicBundle.getBundle("messages/ExecutionBundle", EnvironmentSettingsPanel.class).getString("application.configuration.use.classpath.and.jdk.of.module.label"));
    jPanel.add(labeledComponent, new GridConstraints(2, 0, 1, 1, 0, 1, 3, 0, null, null, null));
    LabeledComponent<JBCheckBox> labeledComponent2 = new LabeledComponent<>();
    this.myIncludeProvidedCheckbox = labeledComponent2;
    labeledComponent2.setLabelLocation("West");
    labeledComponent2.setText("");
    jPanel.add(labeledComponent2, new GridConstraints(3, 0, 1, 1, 8, 0, 3, 0, null, null, null));
    JrePathEditor jrePathEditor = new JrePathEditor();
    this.myJrePathEditor = jrePathEditor;
    jPanel.add(jrePathEditor, new GridConstraints(4, 0, 1, 1, 0, 1, 3, 3, null, null, null));
    LabeledComponent<ShortenCommandLineModeCombo> labeledComponent3 = this.myShortenCommandLineModeCombo;
    labeledComponent3.setLabelLocation("West");
    labeledComponent3.setText(DynamicBundle.getBundle("messages/ExecutionBundle", EnvironmentSettingsPanel.class).getString("application.configuration.shorten.command.line.label"));
    jPanel.add(labeledComponent3, new GridConstraints(5, 0, 1, 1, 0, 1, 3, 0, null, null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myRootPanel;
  }

  EnvironmentSettingsPanel(Project project, EditorTextFieldWithBrowseButton mainClassField) {
    $$$setupUI$$$();
    this.myModuleSelector = new ConfigurationModuleSelector(project, this.myModule.getComponent()) {
      public boolean isModuleAccepted(Module module) {
        return InfraLibraryUtil.hasFrameworkLibrary(module);
      }
    };
    this.myJrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromSourceRootsDependencies(this.myModule.getComponent(), mainClassField));
    this.myCommonProgramParameters.setModuleContext(this.myModuleSelector.getModule());
    addModuleChangeListener(myCommonProgramParameters::setModuleContext);
    this.myIncludeProvidedCheckbox.setComponent(new JBCheckBox(ExecutionBundle.message("application.configuration.include.provided.scope")));
    this.myShortenCommandLineModeCombo.setComponent(new ShortenCommandLineModeCombo(project, this.myJrePathEditor, this.myModule.getComponent()) {
      protected boolean productionOnly() {
        Boolean productionOnly = JavaParametersUtil.isClassInProductionSources(mainClassField.getText(), EnvironmentSettingsPanel.this.myModuleSelector.getModule());
        return productionOnly != null && productionOnly;
      }
    });
    mainClassField.getChildComponent().addDocumentListener(new DocumentListener() {

      public void documentChanged(DocumentEvent event) {
        Module module;
        if (DumbService.getInstance(project).isDumb()) {
          return;
        }
        String mainClass = mainClassField.getText();
        if (EnvironmentSettingsPanel.this.myModuleSelector.getModule() != null && EnvironmentSettingsPanel.this.myModuleSelector.findClass(mainClass) != null) {
          return;
        }
        JavaRunConfigurationModule configurationModule = new JavaRunConfigurationModule(project, false);
        PsiClass psiClass = configurationModule.findClass(mainClass);
        if (psiClass != null && (module = ModuleUtilCore.findModuleForPsiElement(psiClass)) != null && EnvironmentSettingsPanel.this.myModuleSelector.isModuleAccepted(module)) {
          EnvironmentSettingsPanel.this.myModule.getComponent().setSelectedModule(module);
        }
      }
    });
    this.myAnchor = UIUtil.mergeComponentsWithAnchor(
            this.myCommonProgramParameters, this.myJrePathEditor, this.myModule, this.myIncludeProvidedCheckbox, this.myShortenCommandLineModeCombo);
  }

  void resetEditorFrom(InfraApplicationRunConfiguration configuration) {
    this.myConfiguration = configuration;
    this.myCommonProgramParameters.reset(configuration);
    this.myModuleSelector.reset(configuration);
    this.myJrePathEditor.setPathOrName(configuration.getAlternativeJrePath(), configuration.isAlternativeJrePathEnabled());
    this.myIncludeProvidedCheckbox.getComponent().setSelected(configuration.isProvidedScopeIncluded());
    this.myShortenCommandLineModeCombo.getComponent().setSelectedItem(configuration.getShortenCommandLine());
  }

  void applyEditorTo(InfraApplicationRunConfiguration configuration) {
    this.myCommonProgramParameters.applyTo(configuration);
    this.myModuleSelector.applyTo(configuration);
    configuration.setAlternativeJrePath(this.myJrePathEditor.getJrePathOrName());
    configuration.setAlternativeJrePathEnabled(this.myJrePathEditor.isAlternativeJreSelected());
    configuration.setIncludeProvidedScope(this.myIncludeProvidedCheckbox.getComponent().isSelected());
    configuration.setShortenCommandLine(this.myShortenCommandLineModeCombo.getComponent().getSelectedItem());
  }

  ConfigurationModuleSelector getModuleSelector() {
    return this.myModuleSelector;
  }

  void addModuleChangeListener(ModuleChangeListener listener) {
    this.myModule.getComponent().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        listener.moduleChanged(EnvironmentSettingsPanel.this.myModuleSelector.getModule());
      }
    });
  }

  void targetChanged(String targetName) {
    if (this.myJrePathEditor.updateModel(this.myModuleSelector.getProject(), targetName)) {
      resetEditorFrom(this.myConfiguration);
    }
  }

  JPanel getComponent() {
    return this.myRootPanel;
  }

  public JComponent getAnchor() {
    return this.myAnchor;
  }

  public void setAnchor(@Nullable JComponent anchor) {
    this.myAnchor = anchor;
    this.myCommonProgramParameters.setAnchor(anchor);
    this.myModule.setAnchor(anchor);
    this.myIncludeProvidedCheckbox.setAnchor(anchor);
    this.myJrePathEditor.setAnchor(anchor);
    this.myShortenCommandLineModeCombo.setAnchor(anchor);
  }

  private void createUIComponents() {
    this.myShortenCommandLineModeCombo = new LabeledComponent<>();
  }
}

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
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.ui.ClassBrowser;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.TargetAwareRunConfigurationEditor;
import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.UIUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;

import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

public class ApplicationRunConfigurationEditor
        extends SettingsEditor<InfraApplicationRunConfiguration>
        implements PanelWithAnchor, TargetAwareRunConfigurationEditor {

  private static final String EXPAND_JAVA_OPTIONS_PANEL_PROPERTY_KEY = "ExpandInfraJavaOptionsPanel";
  private static final String EXPAND_INFRA_SETTINGS_PANEL_PROPERTY_KEY = "ExpandInfraSettingsPanel";
  private final Project myProject;
  private JPanel myWholePanel;
  private LabeledComponent<EditorTextFieldWithBrowseButton> myMainClass;
  private JPanel myHideableEnvironmentSettingsPanel;
  private EnvironmentSettingsPanel myEnvironmentSettingsPanel;
  private JPanel myHideableInfraSettingPanel;
  private InfraSettingsPanel infraSettings;
  private JComponent myAnchor;

  private void $$$setupUI$$$() {
    createUIComponents();
    JPanel jPanel = new JPanel();
    this.myWholePanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    LabeledComponent<EditorTextFieldWithBrowseButton> labeledComponent = this.myMainClass;
    labeledComponent.setLabelLocation("West");
    labeledComponent.setText(
            DynamicBundle.getBundle("messages/InfraRunBundle", ApplicationRunConfigurationEditor.class).getString("infra.application.run.configuration.main.class"));
    jPanel.add(labeledComponent, new GridConstraints(0, 0, 1, 1, 0, 1, 3, 3, null, null, null));
    jPanel.add(this.myHideableEnvironmentSettingsPanel, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
    jPanel.add(this.myHideableInfraSettingPanel, new GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, null, null, null));
    jPanel.add(new Spacer(), new GridConstraints(2, 0, 1, 1, 0, 2, 1, 0, null, new Dimension(-1, 10), null));
    jPanel.add(new Spacer(), new GridConstraints(4, 0, 1, 1, 0, 2, 1, 6, null, null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myWholePanel;
  }

  public ApplicationRunConfigurationEditor(Project project) {
    this.myProject = project;
    $$$setupUI$$$();
    this.myAnchor = UIUtil.mergeComponentsWithAnchor(this.myMainClass, this.myEnvironmentSettingsPanel, this.infraSettings);
  }

  private void createUIComponents() {
    this.myMainClass = new LabeledComponent<>();
    this.myMainClass.setComponent(new EditorTextFieldWithBrowseButton(this.myProject, true, (declaration, place) -> {
      if (declaration instanceof PsiClass aClass) {
        if (InfraApplicationService.of().isInfraApplication(aClass) && InfraApplicationService.of().hasMainMethod(aClass)) {
          return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE;
        }
      }
      return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE;
    }));

    this.myHideableEnvironmentSettingsPanel = new JPanel(new BorderLayout());
    this.myEnvironmentSettingsPanel = new EnvironmentSettingsPanel(this.myProject, getMainClassField());
    installHideableDecorator(this.myHideableEnvironmentSettingsPanel, this.myEnvironmentSettingsPanel.getComponent(),
            message("infra.application.run.configuration.environment.section"),
            EXPAND_JAVA_OPTIONS_PANEL_PROPERTY_KEY, false);

    this.myHideableInfraSettingPanel = new JPanel(new BorderLayout());
    this.infraSettings = new InfraSettingsPanel(this.myProject);

    installHideableDecorator(this.myHideableInfraSettingPanel, this.infraSettings.getComponent(),
            message("infra.application.run.configuration.framework.section"),
            EXPAND_INFRA_SETTINGS_PANEL_PROPERTY_KEY, true);

    EnvironmentSettingsPanel environmentSettingsPanel = this.myEnvironmentSettingsPanel;
    environmentSettingsPanel.addModuleChangeListener(infraSettings::setModule);
    new InfraClassBrowser(this.myProject, this.myEnvironmentSettingsPanel.getModuleSelector()).setField(getMainClassField());
  }

  public void resetEditorFrom(InfraApplicationRunConfiguration configuration) {
    getMainClassField().setText(configuration.getInfraMainClass() != null ? configuration.getInfraMainClass().replace('$', '.') : "");
    this.myEnvironmentSettingsPanel.resetEditorFrom(configuration);
    this.infraSettings.resetEditorFrom(configuration);
  }

  public void applyEditorTo(InfraApplicationRunConfiguration configuration) {
    String className = getMainClassField().getText();
    PsiClass aClass = this.myEnvironmentSettingsPanel.getModuleSelector().findClass(className);
    configuration.setInfraMainClass(aClass != null ? JavaExecutionUtil.getRuntimeQualifiedName(aClass) : className);
    this.myEnvironmentSettingsPanel.applyEditorTo(configuration);
    this.infraSettings.applyEditorTo(configuration);
  }

  protected JComponent createEditor() {
    return this.myWholePanel;
  }

  public JComponent getAnchor() {
    return this.myAnchor;
  }

  public void setAnchor(@Nullable JComponent anchor) {
    this.myAnchor = anchor;
    this.myMainClass.setAnchor(anchor);
    this.myEnvironmentSettingsPanel.setAnchor(anchor);
    this.infraSettings.setAnchor(anchor);
  }

  private EditorTextFieldWithBrowseButton getMainClassField() {
    return this.myMainClass.getComponent();
  }

  private void installHideableDecorator(JPanel panel, JComponent content, String title, String expandPropertyKey, boolean defaultState) {
    HideableDecorator hideableDecorator = new HideableDecorator(panel, title, false) {
      protected void on() {
        super.on();
        storeState();
      }

      protected void off() {
        super.off();
        storeState();
      }

      private void storeState() {
        PropertiesComponent.getInstance(ApplicationRunConfigurationEditor.this.myProject).setValue(expandPropertyKey, isExpanded(), defaultState);
      }
    };
    hideableDecorator.setOn(PropertiesComponent.getInstance(this.myProject).getBoolean(expandPropertyKey, defaultState));
    hideableDecorator.setContentComponent(content);
  }

  public void targetChanged(String targetName) {
    this.myEnvironmentSettingsPanel.targetChanged(targetName);
  }

  static class InfraClassBrowser extends ClassBrowser.MainClassBrowser<EditorTextField> {

    InfraClassBrowser(Project project, ConfigurationModuleSelector moduleSelector) {
      super(project, moduleSelector, message("infra.choose.class.dialog.title"));
    }

    protected ClassFilter createFilter(Module module) {
      return aClass -> InfraApplicationService.of().isInfraApplication(aClass)
              && ReadAction.compute(() -> InfraApplicationService.of().hasMainMethod(aClass));
    }
  }
}

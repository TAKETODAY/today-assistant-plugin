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
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.TableUtil;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.SmartList;
import com.intellij.util.ui.UIUtil;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.assistant.app.run.update.InfraApplicationUpdatePolicy;
import cn.taketoday.assistant.profiles.InfraProfileCompletionProvider;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

public class InfraSettingsPanel extends JPanel implements PanelWithAnchor {

  private JPanel myRootPanel;
  private JComponent myAnchor;
  private JBCheckBox myEnableDebugOutput;
  private JBCheckBox myEnableLaunchOptimization;
  private JBCheckBox myHideBanner;
  private JBCheckBox myEnableJmxAgent;
  private JPanel myAdditionalParametersPanel;
  private InfraProfileCompletionProvider myProfileCompletionProvider;
  private LabeledComponent<TextFieldWithAutoCompletion<String>> myActiveProfiles;
  private JLabel myDevToolsMessage;
  private LabeledComponent<ComboBox<InfraApplicationUpdatePolicy>> myUpdateActionPolicy;
  private ContextHelpLabel myUpdateActionHelpLabel;
  private LabeledComponent<ComboBox<InfraApplicationUpdatePolicy>> myFrameDeactivationPolicy;
  private ContextHelpLabel myFrameDeactivationHelpLabel;
  private JPanel myUpdatePoliciesPanel;
  private JBLabel myAdditionalParametersLabel;
  private AdditionalParamsTableView myAdditionalParamsTable;
  private final Project myProject;
  private Module myModule;

  private void $$$setupUI$$$() {
    createUIComponents();
    JPanel jPanel = new JPanel();
    this.myRootPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(5, 6, new Insets(0, 0, 0, 0), -1, -1, false, false));
    JBCheckBox jBCheckBox = new JBCheckBox();
    this.myEnableDebugOutput = jBCheckBox;
    $$$loadButtonText$$$(jBCheckBox, DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.debug.output"));
    jBCheckBox.setToolTipText(ResourceBundle.getBundle("messages/InfraRunBundle").getString("infra.application.run.configuration.debug.output.tooltip"));
    jPanel.add(jBCheckBox, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    jPanel.add(new Spacer(), new GridConstraints(0, 5, 1, 1, 0, 1, 6, 1, null, null, null));
    JPanel jPanel2 = new JPanel();
    this.myAdditionalParametersPanel = jPanel2;
    jPanel2.setLayout(new BorderLayout(0, 0));
    jPanel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
    jPanel.add(jPanel2, new GridConstraints(4, 0, 1, 6, 0, 3, 3, 3, null, null, null));
    LabeledComponent<TextFieldWithAutoCompletion<String>> labeledComponent = this.myActiveProfiles;
    labeledComponent.setLabelLocation("West");
    labeledComponent.setText(DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.active.profiles"));
    labeledComponent.setToolTipText(ResourceBundle.getBundle("messages/InfraRunBundle").getString("infra.application.run.configuration.active.profiles.tooltip"));
    jPanel.add(labeledComponent, new GridConstraints(2, 0, 1, 6, 0, 1, 3, 3, null, null, null));
    JLabel jLabel = new JLabel();
    this.myDevToolsMessage = jLabel;
    jLabel.setText("");
    jPanel.add(jLabel, new GridConstraints(0, 4, 1, 1, 8, 0, 0, 0, null, null, null));
    JBCheckBox jBCheckBox2 = new JBCheckBox();
    this.myEnableLaunchOptimization = jBCheckBox2;
    $$$loadButtonText$$$(jBCheckBox2,
            DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.launch.optimization"));
    jBCheckBox2.setToolTipText(ResourceBundle.getBundle("messages/InfraRunBundle").getString("infra.application.run.configuration.launch.optimization.tooltip"));
    jPanel.add(jBCheckBox2, new GridConstraints(0, 2, 1, 1, 0, 0, 0, 0, null, null, null));
    JBCheckBox jBCheckBox3 = new JBCheckBox();
    this.myHideBanner = jBCheckBox3;
    $$$loadButtonText$$$(jBCheckBox3, DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.hide.banner"));
    jBCheckBox3.setToolTipText(ResourceBundle.getBundle("messages/InfraRunBundle").getString("infra.application.run.configuration.hide.banner.tooltip"));
    jPanel.add(jBCheckBox3, new GridConstraints(0, 1, 1, 1, 8, 0, 0, 0, null, null, null));
    JBCheckBox jBCheckBox4 = new JBCheckBox();
    this.myEnableJmxAgent = jBCheckBox4;
    $$$loadButtonText$$$(jBCheckBox4, DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.jmx.agent"));
    jBCheckBox4.setToolTipText(ResourceBundle.getBundle("messages/InfraRunBundle").getString("infra.application.run.configuration.jmx.agent.tooltip"));
    jPanel.add(jBCheckBox4, new GridConstraints(0, 3, 1, 1, 0, 0, 0, 0, null, null, null));
    JPanel jPanel3 = new JPanel();
    this.myUpdatePoliciesPanel = jPanel3;
    jPanel3.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel.add(jPanel3, new GridConstraints(1, 0, 1, 3, 0, 3, 3, 3, null, null, null, 1));
    LabeledComponent<ComboBox<InfraApplicationUpdatePolicy>> labeledComponent2 = this.myUpdateActionPolicy;
    labeledComponent2.setLabelLocation("West");
    labeledComponent2.setText(DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.on.update.action"));
    jPanel3.add(labeledComponent2, new GridConstraints(0, 0, 1, 1, 0, 1, 3, 3, null, null, null));
    LabeledComponent<ComboBox<InfraApplicationUpdatePolicy>> labeledComponent3 = this.myFrameDeactivationPolicy;
    labeledComponent3.setLabelLocation("West");
    labeledComponent3.setText(DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.on.frame.deactivation"));
    jPanel3.add(labeledComponent3, new GridConstraints(1, 0, 1, 1, 0, 1, 3, 3, null, null, null));
    jPanel3.add(this.myUpdateActionHelpLabel, new GridConstraints(0, 1, 1, 1, 0, 0, 3, 3, null, null, null));
    jPanel3.add(this.myFrameDeactivationHelpLabel, new GridConstraints(1, 1, 1, 1, 0, 0, 3, 3, null, null, null));
    JBLabel jBLabel = new JBLabel();
    this.myAdditionalParametersLabel = jBLabel;
    $$$loadLabelText$$$(jBLabel, DynamicBundle.getBundle("messages/InfraRunBundle", InfraSettingsPanel.class).getString("infra.application.run.configuration.override.parameters"));
    jPanel.add(jBLabel, new GridConstraints(3, 0, 1, 3, 8, 0, 0, 0, null, null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myRootPanel;
  }

  private void $$$loadLabelText$$$(JLabel jLabel, String str) {
    StringBuffer stringBuffer = new StringBuffer();
    boolean z = false;
    char c = 0;
    int i = -1;
    int i2 = 0;
    while (i2 < str.length()) {
      if (str.charAt(i2) == '&') {
        i2++;
        if (i2 == str.length()) {
          break;
        }
        else if (!z && str.charAt(i2) != '&') {
          z = true;
          c = str.charAt(i2);
          i = stringBuffer.length();
        }
      }
      stringBuffer.append(str.charAt(i2));
      i2++;
    }
    jLabel.setText(stringBuffer.toString());
    if (z) {
      jLabel.setDisplayedMnemonic(c);
      jLabel.setDisplayedMnemonicIndex(i);
    }
  }

  private void $$$loadButtonText$$$(AbstractButton abstractButton, String str) {
    StringBuffer stringBuffer = new StringBuffer();
    boolean z = false;
    char c = 0;
    int i = -1;
    int i2 = 0;
    while (i2 < str.length()) {
      if (str.charAt(i2) == '&') {
        i2++;
        if (i2 == str.length()) {
          break;
        }
        else if (!z && str.charAt(i2) != '&') {
          z = true;
          c = str.charAt(i2);
          i = stringBuffer.length();
        }
      }
      stringBuffer.append(str.charAt(i2));
      i2++;
    }
    abstractButton.setText(stringBuffer.toString());
    if (z) {
      abstractButton.setMnemonic(c);
      abstractButton.setDisplayedMnemonicIndex(i);
    }
  }

  public InfraSettingsPanel(Project project) {
    this.myProject = project;
    $$$setupUI$$$();
    initAdditionalParamsTable();
    if (AdvancedSettings.getBoolean("compiler.automake.allow.when.app.running")) {
      this.myDevToolsMessage.setIcon(AllIcons.General.BalloonError);
      this.myDevToolsMessage.setText(message("infra.run.config.settings.background.compilation.enabled"));
    }
    UIUtil.mergeComponentsWithAnchor(this.myUpdateActionPolicy, this.myFrameDeactivationPolicy);
    this.myUpdatePoliciesPanel.setBorder(IdeBorderFactory.createTitledBorder(message("infra.run.config.settings.running.application.update.policies")));
  }

  private void initAdditionalParamsTable() {
    this.myAdditionalParamsTable = new AdditionalParamsTableView(this.myProject);
    this.myAdditionalParametersPanel.add(ToolbarDecorator.createDecorator(this.myAdditionalParamsTable).setAddAction(new AnActionButtonRunnable() {
      public void run(AnActionButton button) {
        myAdditionalParamsTable.addAdditionalParameter();
      }
    }).setRemoveAction(new AnActionButtonRunnable() {
      public void run(AnActionButton button) {
        TableUtil.removeSelectedItems(InfraSettingsPanel.this.myAdditionalParamsTable);
      }
    }).createPanel(), "Center");
    this.myAdditionalParametersLabel.setLabelFor(this.myAdditionalParamsTable);
  }

  void resetEditorFrom(InfraApplicationRunConfiguration configuration) {
    this.myEnableDebugOutput.setSelected(configuration.isDebugMode());
    this.myEnableLaunchOptimization.setSelected(configuration.isEnableLaunchOptimization());
    this.myHideBanner.setSelected(configuration.isHideBanner());
    this.myEnableJmxAgent.setSelected(configuration.isEnableJmxAgent());
    this.myActiveProfiles.getComponent().setText(configuration.getActiveProfiles());
    this.myUpdateActionPolicy.getComponent().setSelectedItem(configuration.getUpdateActionUpdatePolicy());
    this.myFrameDeactivationPolicy.getComponent().setSelectedItem(configuration.getFrameDeactivationUpdatePolicy());
    this.myAdditionalParamsTable.setAdditionalParameters(configuration.getAdditionalParameters());
  }

  void applyEditorTo(InfraApplicationRunConfiguration configuration) {
    configuration.setDebugMode(this.myEnableDebugOutput.isSelected());
    configuration.setEnableLaunchOptimization(this.myEnableLaunchOptimization.isSelected());
    configuration.setHideBanner(this.myHideBanner.isSelected());
    configuration.setEnableJmxAgent(this.myEnableJmxAgent.isSelected());
    configuration.setActiveProfiles(this.myActiveProfiles.getComponent().getText());
    configuration.setUpdateActionUpdatePolicy((InfraApplicationUpdatePolicy) this.myUpdateActionPolicy.getComponent().getSelectedItem());
    configuration.setFrameDeactivationUpdatePolicy((InfraApplicationUpdatePolicy) this.myFrameDeactivationPolicy.getComponent().getSelectedItem());
    configuration.setAdditionalParameters(this.myAdditionalParamsTable.getAdditionalParameters());
  }

  JComponent getComponent() {
    return this.myRootPanel;
  }

  public JComponent getAnchor() {
    return this.myAnchor;
  }

  public void setAnchor(@Nullable JComponent anchor) {
    this.myAnchor = anchor;
    this.myActiveProfiles.setAnchor(anchor);
  }

  public void setModule(Module module) {
    this.myModule = module;
    this.myAdditionalParamsTable.setModule(module);
    this.myEnableJmxAgent.setEnabled(this.myModule == null);
    this.myProfileCompletionProvider.setContext(module == null ? Collections.emptyList() : new SmartList<>(module));
  }

  private void createUIComponents() {
    this.myProfileCompletionProvider = new InfraProfileCompletionProvider(false);
    TextFieldWithAutoCompletion<String> field = new TextFieldWithAutoCompletion<>(this.myProject, this.myProfileCompletionProvider, true, "");
    this.myActiveProfiles = LabeledComponent.create(field, message("infra.application.run.configuration.active.profiles"));
    ComboBox<InfraApplicationUpdatePolicy> updateActionComboBox = new ComboBox<>();
    initUpdatePolicyComboBox(updateActionComboBox, false);
    this.myUpdateActionPolicy = LabeledComponent.create(updateActionComboBox, message("infra.run.config.settings.on.update.action"));
    this.myUpdateActionHelpLabel = ContextHelpLabel.create(getHelpLabelDescription(updateActionComboBox));
    ComboBox<InfraApplicationUpdatePolicy> frameDeactivationComboBox = new ComboBox<>();
    initUpdatePolicyComboBox(frameDeactivationComboBox, true);
    this.myFrameDeactivationPolicy = LabeledComponent.create(frameDeactivationComboBox, message("infra.run.config.settings.on.frame.deactivation"));
    this.myFrameDeactivationHelpLabel = ContextHelpLabel.create(getHelpLabelDescription(frameDeactivationComboBox));
  }

  private static void initUpdatePolicyComboBox(ComboBox<InfraApplicationUpdatePolicy> comboBox, boolean onFrameDeactivation) {
    comboBox.setRenderer(SimpleListCellRenderer.create(message("infra.application.run.configuration.do.nothing"), value -> {
      return UIUtil.removeMnemonic(value.getName());
    }));
    comboBox.addItem(null);
    Collection<? extends InfraApplicationUpdatePolicy> policies = InfraApplicationUpdatePolicy.getAvailablePolicies(onFrameDeactivation);
    for (InfraApplicationUpdatePolicy policy : policies) {
      comboBox.addItem(policy);
    }
    if (comboBox.getItemCount() > 0) {
      comboBox.setSelectedIndex(0);
    }
  }

  private static String getHelpLabelDescription(ComboBox<InfraApplicationUpdatePolicy> updateActionComboBox) {
    StringBuilder description = new StringBuilder();
    for (int i = 0; i < updateActionComboBox.getItemCount(); i++) {
      InfraApplicationUpdatePolicy policy = updateActionComboBox.getItemAt(i);
      if (policy != null) {
        description.append("<p><b>").append(policy.getName()).append("</b><br>").append(policy.getDescription()).append("</p>");
      }
    }
    return description.toString();
  }
}

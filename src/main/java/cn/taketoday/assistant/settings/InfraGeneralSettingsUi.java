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

package cn.taketoday.assistant.settings;

import com.intellij.DynamicBundle;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import java.awt.Insets;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

class InfraGeneralSettingsUi implements ConfigurableUi<InfraGeneralSettings> {
  private JPanel rootPanel;
  private JBCheckBox showProfilesPanel;
  private JBCheckBox showMultiContextsPanel;
  private JBCheckBox allowAutoConfiguration;

  private void $$$setupUI$$$() {
    JPanel jPanel = new JPanel();
    this.rootPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel.add(new Spacer(), new GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    JPanel jPanel2 = new JPanel();
    jPanel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
    jPanel.add(jPanel2, new GridConstraints(1, 0, 2, 2, 0, 3, 3, 3, null, null, null));
    jPanel2.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(null,
            DynamicBundle.getBundle("messages/InfraBundle", InfraGeneralSettingsUi.class).getString("settings.configuration.file.editor"), 1, 0, null, null));
    JBCheckBox jBCheckBox = new JBCheckBox();
    this.showProfilesPanel = jBCheckBox;
    $$$loadButtonText$$$(jBCheckBox, DynamicBundle.getBundle("messages/InfraBundle", InfraGeneralSettingsUi.class).getString("settings.profiles.panel"));
    jBCheckBox.setToolTipText(ResourceBundle.getBundle("messages/InfraBundle").getString("settings.profiles.panel.tooltip"));
    jPanel2.add(jBCheckBox, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    JBCheckBox jBCheckBox2 = new JBCheckBox();
    this.showMultiContextsPanel = jBCheckBox2;
    $$$loadButtonText$$$(jBCheckBox2, DynamicBundle.getBundle("messages/InfraBundle", InfraGeneralSettingsUi.class).getString("settings.multiple.context.panel"));
    jBCheckBox2.setToolTipText(ResourceBundle.getBundle("messages/InfraBundle").getString("settings.multiple.context.panel.tooltip"));
    jPanel2.add(jBCheckBox2, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    JPanel jPanel3 = new JPanel();
    jPanel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel3.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
    jPanel.add(jPanel3, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
    jPanel3.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(null,
            DynamicBundle.getBundle("messages/InfraBundle", InfraGeneralSettingsUi.class).getString("settings.common"), 1, 0, null, null));
    JBCheckBox jBCheckBox3 = new JBCheckBox();
    this.allowAutoConfiguration = jBCheckBox3;
    $$$loadButtonText$$$(jBCheckBox3, DynamicBundle.getBundle("messages/InfraBundle", InfraGeneralSettingsUi.class).getString("settings.auto.configure"));
    jBCheckBox3.setToolTipText("");
    jPanel3.add(jBCheckBox3, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.rootPanel;
  }

  private void $$$loadButtonText$$$(AbstractButton abstractButton, String str) {
    StringBuilder stringBuffer = new StringBuilder();
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

  public InfraGeneralSettingsUi() {
    $$$setupUI$$$();
  }

  public void reset(InfraGeneralSettings settings) {
    this.showProfilesPanel.setSelected(settings.isShowProfilesPanel());
    this.showMultiContextsPanel.setSelected(settings.isShowMultipleContextsPanel());
    this.allowAutoConfiguration.setSelected(settings.isAllowAutoConfigurationMode());
  }

  public boolean isModified(InfraGeneralSettings settings) {
    return hasModifiedEditorPanelSettings(settings);
  }

  private boolean hasModifiedEditorPanelSettings(InfraGeneralSettings settings) {
    return settings.isShowProfilesPanel() != this.showProfilesPanel.isSelected() || settings.isShowMultipleContextsPanel() != this.showMultiContextsPanel.isSelected() || settings.isAllowAutoConfigurationMode() != this.allowAutoConfiguration.isSelected();
  }

  public void apply(InfraGeneralSettings settings) {
    if (hasModifiedEditorPanelSettings(settings)) {
      settings.setShowProfilesPanel(this.showProfilesPanel.isSelected());
      settings.setShowMultipleContextsPanel(this.showMultiContextsPanel.isSelected());
      settings.setAllowAutoConfigurationMode(this.allowAutoConfiguration.isSelected());
      EditorNotifications.updateAll();
    }
  }

  public JComponent getComponent() {
    return rootPanel;
  }
}

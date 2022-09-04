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

package cn.taketoday.assistant.app.run.lifecycle.tabs;

import com.intellij.DynamicBundle;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
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

import cn.taketoday.assistant.app.options.InfrastructureSettings;

final class InfraSettingsUi implements ConfigurableUi<InfrastructureSettings> {
  private JPanel myRootPanel;
  private JBCheckBox myAdditionalConfigJsonShowPanel;
  private JBCheckBox myInitializrAutoCreateRunConfiguration;
  private JPanel myDashboardEndpointsPanel;
  private final Project myProject;
  static final boolean $assertionsDisabled;

  private void $$$setupUI$$$() {
    JPanel jPanel = new JPanel();
    this.myRootPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel.add(new Spacer(), new GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    JPanel jPanel2 = new JPanel();
    jPanel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
    jPanel.add(jPanel2, new GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
    jPanel2.setBorder(IdeBorderFactory.PlainSmallWithoutIndent.createTitledBorder(null,
            DynamicBundle.getBundle("messages/InfraAppBundle", InfraSettingsUi.class).getString("infra.settings.initializr"), 0, 0, null, null));
    JBCheckBox jBCheckBox = new JBCheckBox();
    this.myInitializrAutoCreateRunConfiguration = jBCheckBox;
    $$$loadButtonText$$$(jBCheckBox, DynamicBundle.getBundle("messages/InfraAppBundle", InfraSettingsUi.class).getString("infra.settings.create.run.configuration"));
    jBCheckBox.setToolTipText(ResourceBundle.getBundle("messages/InfraAppBundle").getString("infra.settings.create.run.configuration.tooltip"));
    jPanel2.add(jBCheckBox, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    JBCheckBox jBCheckBox2 = new JBCheckBox();
    this.myAdditionalConfigJsonShowPanel = jBCheckBox2;
    $$$loadButtonText$$$(jBCheckBox2, DynamicBundle.getBundle("messages/InfraAppBundle", InfraSettingsUi.class).getString("infra.settings.show.notification.panel"));
    jBCheckBox2.setToolTipText(ResourceBundle.getBundle("messages/InfraAppBundle").getString("infra.settings.show.notification.panel.tooltip"));
    jPanel.add(jBCheckBox2, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    JPanel jPanel3 = new JPanel();
    this.myDashboardEndpointsPanel = jPanel3;
    jPanel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel3.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithoutIndent");
    jPanel.add(jPanel3, new GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
  }

  public JComponent $$$getRootComponent$$$() {
    return this.myRootPanel;
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

  static {
    $assertionsDisabled = !InfraSettingsUi.class.desiredAssertionStatus();
  }

  InfraSettingsUi(Project project) {
    this.myProject = project;
    $$$setupUI$$$();
  }

  public void reset(InfrastructureSettings settings) {
    this.myAdditionalConfigJsonShowPanel.setSelected(settings.isShowAdditionalConfigNotification());
    this.myInitializrAutoCreateRunConfiguration.setSelected(settings.isAutoCreateRunConfiguration());
    for (EndpointTabConfigurable tabConfigurable : getEndpointTabConfigurables()) {
      tabConfigurable.reset();
    }
  }

  public boolean isModified(InfrastructureSettings settings) {
    if (hasModifiedEditorSettings(settings) || this.myInitializrAutoCreateRunConfiguration.isSelected() != settings.isAutoCreateRunConfiguration()) {
      return true;
    }
    for (EndpointTabConfigurable tabConfigurable : getEndpointTabConfigurables()) {
      if (tabConfigurable.isModified()) {
        return true;
      }
    }
    return false;
  }

  private boolean hasModifiedEditorSettings(InfrastructureSettings settings) {
    return this.myAdditionalConfigJsonShowPanel.isSelected() != settings.isShowAdditionalConfigNotification();
  }

  public void apply(InfrastructureSettings settings) throws ConfigurationException {
    if (hasModifiedEditorSettings(settings)) {
      settings.setShowAdditionalConfigNotification(this.myAdditionalConfigJsonShowPanel.isSelected());
      EditorNotifications.updateAll();
    }
    settings.setAutoCreateRunConfiguration(this.myInitializrAutoCreateRunConfiguration.isSelected());
    for (EndpointTabConfigurable tabConfigurable : getEndpointTabConfigurables()) {
      tabConfigurable.apply();
    }
  }

  public JComponent getComponent() {
    int row = 0;
    for (EndpointTabConfigurable tabConfigurable : getEndpointTabConfigurables()) {
      JComponent component = tabConfigurable.createComponent();
      if (!$assertionsDisabled && !(component instanceof JPanel)) {
        throw new AssertionError(tabConfigurable);
      }
      GridConstraints constraint = new GridConstraints();
      int i = row;
      row++;
      constraint.setRow(i);
      constraint.setAnchor(9);
      this.myDashboardEndpointsPanel.add(component, constraint);
    }
    return this.myRootPanel;
  }

  private EndpointTabConfigurable[] getEndpointTabConfigurables() {
    return EndpointTabConfigurable.EP_NAME.getExtensions(this.myProject);
  }
}

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

package cn.taketoday.assistant.app.run.lifecycle.health.tab;

import com.intellij.DynamicBundle;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import java.awt.Insets;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import cn.taketoday.assistant.app.run.lifecycle.tabs.InfraEndpointsTabSettings;

class HealthEndpointTabConfigurableUi implements ConfigurableUi<HealthEndpointTabSettings> {
  private static final long MIN_DELAY = 1;
  private static final long MAX_DELAY = TimeUnit.MINUTES.toSeconds(30);
  private final Project myProject;
  private JPanel myRootPanel;
  private JBCheckBox myHealthCheckEnabled;
  private JSpinner myHealthCheckDelay;

  private void $$$setupUI$$$() {
    createUIComponents();
    JPanel jPanel = new JPanel();
    this.myRootPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1, false, false));
    jPanel.add(new Spacer(), new GridConstraints(1, 0, 1, 4, 0, 2, 1, 6, null, null, null));
    JBCheckBox jBCheckBox = new JBCheckBox();
    this.myHealthCheckEnabled = jBCheckBox;
    $$$loadButtonText$$$(jBCheckBox, DynamicBundle.getBundle("messages/InfraRunBundle", HealthEndpointTabConfigurableUi.class).getString("infra.application.endpoints.health.check"));
    jBCheckBox.setToolTipText(ResourceBundle.getBundle("messages/InfraRunBundle").getString("infra.application.endpoints.health.check.tooltip"));
    jPanel.add(jBCheckBox, new GridConstraints(0, 0, 1, 1, 0, 0, 0, 0, null, null, null));
    jPanel.add(new Spacer(), new GridConstraints(0, 3, 1, 1, 0, 1, 6, 1, null, null, null));
    jPanel.add(this.myHealthCheckDelay, new GridConstraints(0, 1, 1, 1, 8, 0, 6, 0, null, null, null));
    JLabel jLabel = new JLabel();
    $$$loadLabelText$$$(jLabel, DynamicBundle.getBundle("messages/InfraRunBundle", HealthEndpointTabConfigurableUi.class).getString("infra.application.endpoints.health.check.unit"));
    jPanel.add(jLabel, new GridConstraints(0, 2, 1, 1, 8, 0, 0, 0, null, null, null));
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
    StringBuilder builder = new StringBuilder();
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
          i = builder.length();
        }
      }
      builder.append(str.charAt(i2));
      i2++;
    }
    abstractButton.setText(builder.toString());
    if (z) {
      abstractButton.setMnemonic(c);
      abstractButton.setDisplayedMnemonicIndex(i);
    }
  }

  HealthEndpointTabConfigurableUi(Project project) {
    this.myProject = project;
    $$$setupUI$$$();
    this.myHealthCheckEnabled.addActionListener(e -> {
      this.myHealthCheckDelay.setEnabled(this.myHealthCheckEnabled.isSelected());
    });
  }

  public void reset(HealthEndpointTabSettings settings) {
    this.myHealthCheckEnabled.setSelected(settings.isCheckHealth());
    long seconds = TimeUnit.MILLISECONDS.toSeconds(settings.getHealthCheckDelay());
    if (seconds > MAX_DELAY) {
      seconds = MAX_DELAY;
    }
    else if (seconds < MIN_DELAY) {
      seconds = 1;
    }
    this.myHealthCheckDelay.setValue(seconds);
    this.myHealthCheckDelay.setEnabled(settings.isCheckHealth());
  }

  public boolean isModified(HealthEndpointTabSettings settings) {
    return myHealthCheckEnabled.isSelected() != settings.isCheckHealth()
            || getHealthCheckDelay() != settings.getHealthCheckDelay();
  }

  public void apply(HealthEndpointTabSettings settings) {
    boolean isModified = isModified(settings);
    settings.setCheckHealth(this.myHealthCheckEnabled.isSelected());
    settings.setHealthCheckDelay(getHealthCheckDelay());
    if (isModified) {
      InfraEndpointsTabSettings endpointsTabSettings = InfraEndpointsTabSettings.getInstance(this.myProject);
      endpointsTabSettings.fireSettingsChanged("HEALTH_CHECK");
    }
  }

  public JComponent getComponent() {
    return this.myRootPanel;
  }

  private void createUIComponents() {
    this.myHealthCheckDelay = new JSpinner(new SpinnerNumberModel(Long.valueOf(MIN_DELAY), Long.valueOf(MIN_DELAY), Long.valueOf(MAX_DELAY), Long.valueOf(MIN_DELAY)));
  }

  private long getHealthCheckDelay() {
    return TimeUnit.SECONDS.toMillis(((SpinnerNumberModel) this.myHealthCheckDelay.getModel()).getNumber().longValue());
  }

}

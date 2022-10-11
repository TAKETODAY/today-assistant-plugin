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

package cn.taketoday.assistant.app.run;

import com.intellij.DynamicBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.textCompletion.TextFieldWithCompletion;
import com.intellij.util.ui.JBDimension;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationUrlUtil;
import cn.taketoday.lang.Nullable;

public class InfraApplicationUrlPathConfigurable extends DialogWrapper {
  private JPanel myRootPanel;
  private JPanel myFieldPanel;
  private TextFieldWithCompletion myPathField;

  private final Project myProject;

  private final InfraApplicationRunConfig myRunConfiguration;

  private final InfraApplicationInfo myInfo;

  private void $$$setupUI$$$() {
    createUIComponents();
    JPanel jPanel = new JPanel();
    this.myRootPanel = jPanel;
    jPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1, false, false));
    JBLabel jBLabel = new JBLabel();
    $$$loadLabelText$$$(jBLabel, DynamicBundle.getBundle("messages/InfraRunBundle", InfraApplicationUrlPathConfigurable.class).getString("infra.application.url.path.label"));
    jPanel.add(jBLabel, new GridConstraints(0, 0, 1, 1, 0, 0, 0, 0, null, null, null));
    jPanel.add(new Spacer(), new GridConstraints(1, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    jPanel.add(this.myFieldPanel, new GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
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

  public InfraApplicationUrlPathConfigurable(Project project, InfraApplicationRunConfig runConfiguration, InfraApplicationInfo info) {
    super(project);
    this.myProject = project;
    this.myRunConfiguration = runConfiguration;
    this.myInfo = info;
    $$$setupUI$$$();
    init();
    setTitle(InfraRunBundle.message("infra.application.url.path.dialog.title"));
  }

  private void createUIComponents() {
    this.myFieldPanel = new JPanel(new BorderLayout());
    this.myFieldPanel.setPreferredSize(new JBDimension(300, -1));
    String text = this.myRunConfiguration.getUrlPath();
    if (text == null) {
      text = "";
    }
    TextFieldWithAutoCompletionListProvider<?> provider = InfraApplicationUrlPathProviderFactory.EP_NAME.extensions()
            .findFirst().map(factory -> {
              return factory.createCompletionProvider(this.myInfo);
            }).orElse(null);
    if (provider == null) {
      provider = new TextFieldWithAutoCompletionListProvider<String>(Collections.emptyList()) {

        public String getLookupString(String item) {
          return item;
        }
      };
    }
    this.myPathField = new TextFieldWithAutoCompletion(this.myProject, provider, true, text);
    this.myFieldPanel.add(this.myPathField, "Center");
  }

  protected JComponent createCenterPanel() {
    return this.myRootPanel;
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return this.myPathField;
  }

  protected void doOKAction() {
    InfraApplicationUrlUtil.getInstance().updatePath(this.myProject, this.myRunConfiguration, this.myPathField.getText());
    super.doOKAction();
  }
}

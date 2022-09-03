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

// ChangeActiveProfileDialogUi.java
package cn.taketoday.assistant.profiles;

import com.intellij.openapi.ui.DialogPanel;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.ComponentsKt;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.layout.CCFlags;
import com.intellij.ui.layout.LCFlags;
import com.intellij.ui.layout.LayoutBuilder;
import com.intellij.ui.layout.LayoutImplKt;
import com.intellij.ui.layout.LayoutKt;
import com.intellij.ui.layout.RowBuilder.DefaultImpls;

import javax.swing.JLabel;

import kotlin.Unit;

import static cn.taketoday.assistant.InfraBundle.message;

public final class ChangeActiveProfileDialogUi {
  private final DialogPanel mainPanel;
  private final JBRadioButton projectRadioButton;
  private final JBRadioButton moduleRadioButton;
  private final JBRadioButton contextRadioButton;
  private final TextFieldWithAutoCompletion profilesText;

  public JBRadioButton getProjectRadioButton() {
    return this.projectRadioButton;
  }

  public JBRadioButton getModuleRadioButton() {
    return this.moduleRadioButton;
  }

  public JBRadioButton getContextRadioButton() {
    return this.contextRadioButton;
  }

  public DialogPanel getMainPanel() {
    return this.mainPanel;
  }

  public TextFieldWithAutoCompletion getProfilesText() {
    return this.profilesText;
  }

  public ChangeActiveProfileDialogUi(TextFieldWithAutoCompletion profilesText) {
    this.profilesText = profilesText;
    this.projectRadioButton = new JBRadioButton(message("ChangeActiveProfileDialogUi.radio.button.project"));
    this.moduleRadioButton = new JBRadioButton(message("ChangeActiveProfileDialogUi.radio.button.module"));
    this.contextRadioButton = new JBRadioButton(message("ChangeActiveProfileDialogUi.radio.button.context"));
    LCFlags[] constraints$iv = new LCFlags[0];
    LayoutBuilder builder = LayoutImplKt.createLayoutBuilder();
    DefaultImpls.row(builder, (JLabel) null, false, row -> {
      DefaultImpls.row(row, (JLabel) null, false, label -> {
                String message = message("ChangeActiveProfileDialogUi.radio.button.scope");
                label.label(message, null, null, false);
                label.buttonGroup(() -> {
                  DefaultImpls.row(label, (JLabel) null, false, row13 -> {
                    row13.invoke(projectRadioButton, new CCFlags[0], null, null);
                    return Unit.INSTANCE;
                  });
                  DefaultImpls.row(label, (JLabel) null, false, row12 -> {
                    row12.invoke(moduleRadioButton, new CCFlags[0], null, null);
                    return Unit.INSTANCE;
                  });
                  DefaultImpls.row(label, (JLabel) null, false, row1 -> {
                    row1.invoke(getContextRadioButton(), new CCFlags[0], null, null);
                    return Unit.INSTANCE;
                  });
                  return Unit.INSTANCE;
                });
                return null;
              }
      );

      DefaultImpls.row(row, message("ChangeActiveProfileDialogUi.radio.button.profiles"), false, $this$row -> {
        $this$row.invoke(profilesText, new CCFlags[] { $this$row.getGrowX() }, null,
                message("ChangeActiveProfileDialogUi.radio.button.comment"));
        return Unit.INSTANCE;
      });

      return Unit.INSTANCE;
    });
    DialogPanel dialogPanel = ComponentsKt.DialogPanel(null, null);
    builder.getBuilder().build(dialogPanel, constraints$iv);
    LayoutKt.initPanel(builder, dialogPanel);
    this.mainPanel = dialogPanel;
  }
}






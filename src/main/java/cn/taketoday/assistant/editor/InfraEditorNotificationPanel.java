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

package cn.taketoday.assistant.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ClickListener;
import com.intellij.ui.EditorNotificationPanel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraEditorNotificationPanel extends EditorNotificationPanel {

  public InfraEditorNotificationPanel(@Nullable FileEditor fileEditor, Color backgroundColor) {
    super(fileEditor, backgroundColor, null);
  }

  /**
   * Opens general "TODAT" settings.
   *
   * @param project Current project.
   */
  public void installOpenSettingsButton(Project project) {
    installOpenSettingsButton(project, message("settings.displayName"));
  }

  /**
   * Opens settings with given name.
   *
   * @param project Current project.
   * @param nameToSelect Settings name to open.
   */
  public void installOpenSettingsButton(Project project, String nameToSelect) {
    myGearLabel.setIcon(AllIcons.General.Settings);
    myGearLabel.setToolTipText(message("editor.panel.edit.settings"));
    myGearLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    new ClickListener() {
      @Override
      public boolean onClick(MouseEvent e, int clickCount) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, nameToSelect);
        return true;
      }
    }.installOn(myGearLabel);
  }
}

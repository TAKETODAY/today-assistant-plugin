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

package cn.taketoday.assistant.profiles;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.util.xml.DomChangeAdapter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;

import java.util.Set;
import java.util.function.Function;

import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.editor.InfraEditorNotificationPanel;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.util.InfraUtils;

import static cn.taketoday.assistant.InfraBundle.message;

final class InfraProfilesEditorNotificationsProvider implements EditorNotificationProvider {

  InfraProfilesEditorNotificationsProvider(Project project) {
    DomManager.getDomManager(project).addDomEventListener(new DomChangeAdapter() {
      protected void elementChanged(DomElement element) {
        DomFileElement domFileElement = DomUtil.getParentOfType(element, DomFileElement.class, false);
        if (domFileElement != null && domFileElement.isValid() && (domFileElement.getRootElement() instanceof Beans)) {
          EditorNotifications.getInstance(project).updateAllNotifications();
        }
      }
    }, InfraGeneralSettings.from(project));
  }

  public Function<? super FileEditor, ? extends JComponent> collectNotificationData(Project project, VirtualFile file) {
    if (!InfraGeneralSettings.from(project).isShowProfilesPanel()) {
      return CONST_NULL;
    }
    else if (!InfraUtils.hasFacets(project)) {
      return CONST_NULL;
    }
    else {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile == null) {
        return CONST_NULL;
      }
      InfraModel infraModel = ChangeActiveProfilesAction.getInfraModel(psiFile);
      if (infraModel == null || infraModel.getFileSet() == null) {
        return CONST_NULL;
      }
      Set<String> allProfiles = InfraModelVisitorUtils.getProfiles(infraModel);
      Set<String> activeProfiles = infraModel.getActiveProfiles();
      if ((activeProfiles == null || activeProfiles.isEmpty()) && !allProfiles.isEmpty() && allProfiles.size() <= InfraProfile.DEFAULT_PROFILE_NAMES.size() && InfraProfile.DEFAULT_PROFILE_NAMES.containsAll(
              allProfiles)) {
        return CONST_NULL;
      }
      else if (allProfiles.isEmpty()) {
        return CONST_NULL;
      }
      else {
        String profilesText = ProfileUtils.profilesAsString(activeProfiles);
        boolean inActive = (psiFile instanceof XmlFile) || InfraModelVisitorUtils.hasConfigFile(infraModel, psiFile);
        return fileEditor -> new InfraProfilesPanel(project, fileEditor, profilesText, inActive);
      }
    }
  }

  public static final class InfraProfilesPanel extends InfraEditorNotificationPanel {

    InfraProfilesPanel(Project project, FileEditor fileEditor, String profilesText, boolean isActive) {
      super(fileEditor, isActive ? LightColors.SLIGHTLY_GREEN : JBColor.lightGray);
      text(profilesText);
      icon(Icons.SpringProfile);
      this.myLabel.setToolTipText(message("editor.panel.profiles.tooltip"));
      createActionLabel(message("editor.panel.profiles.tooltip.action.name"), ChangeActiveProfilesAction.ACTION_ID);
      installOpenSettingsButton(project);
    }
  }
}

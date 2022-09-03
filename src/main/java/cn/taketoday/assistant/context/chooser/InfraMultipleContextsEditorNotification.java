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

package cn.taketoday.assistant.context.chooser;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.JComponent;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.settings.InfraGeneralSettings;

final class InfraMultipleContextsEditorNotification implements EditorNotificationProvider {

  InfraMultipleContextsEditorNotification(Project project) {
    project.getMessageBus().connect().subscribe(FacetManager.FACETS_TOPIC, new FacetManagerAdapter() {

      public void facetAdded(Facet facet) {
        facetChanged(facet);
      }

      public void facetRemoved(Facet facet) {
        facetChanged(facet);
      }

      public void facetConfigurationChanged(Facet facet) {
        facetChanged(facet);
      }

      private void facetChanged(Facet facet) {
        if (facet.getTypeId() == InfraFacet.FACET_TYPE_ID) {
          EditorNotifications.getInstance(project).updateAllNotifications();
        }
      }
    });
  }

  public Function<? super FileEditor, ? extends JComponent> collectNotificationData(Project project, VirtualFile file) {
    if (!InfraGeneralSettings.from(project).isShowMultipleContextsPanel()) {
      return CONST_NULL;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (!(psiFile instanceof XmlFile) || !InfraDomUtils.isInfraXml((XmlFile) psiFile)) {
      return CONST_NULL;
    }
    List<InfraContextDescriptor> descriptors = InfraMultipleContextsManager.of().getAllContextDescriptors(psiFile);
    if (descriptors.size() <= 1) {
      return CONST_NULL;
    }
    List<InfraContextDescriptor> allDescriptors = new ArrayList<>();
    allDescriptors.add(InfraContextDescriptor.LOCAL_CONTEXT);
    allDescriptors.add(InfraContextDescriptor.ALL_CONTEXTS);
    allDescriptors.addAll(descriptors);
    InfraContextDescriptor currentContext = getUpdatedCurrentContext(psiFile, allDescriptors);
    return fileEditor -> {
      return new InfraMultipleContextsPanel(fileEditor, psiFile, allDescriptors, currentContext);
    };
  }

  private static InfraContextDescriptor getUpdatedCurrentContext(PsiFile psiFile,
          List<InfraContextDescriptor> allDescriptors) {
    InfraContextDescriptor userDefinedDescriptor = InfraMultipleContextsManager.of()
            .getUserDefinedContextDescriptor(psiFile);
    if (userDefinedDescriptor == null) {
      return InfraMultipleContextsManager.of().getContextDescriptor(psiFile);
    }
    for (InfraContextDescriptor existing : allDescriptors) {
      if (userDefinedDescriptor.getQualifiedName().equals(existing.getQualifiedName())) {
        return existing;
      }
    }
    return InfraContextDescriptor.DEFAULT;
  }
}

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


package cn.taketoday.assistant.app.facet;

import com.intellij.facet.FacetManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.containers.ContainerUtil;

import org.jdom.Attribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileNameContributor;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetEditorCustomization;
import cn.taketoday.assistant.facet.beans.CustomSetting;

public final class AppInfraFileSetEditorCustomization extends InfraFileSetEditorCustomization {
  public static final Key<CustomSetting.BOOLEAN> NON_STRICT_SETTING = Key.create("infra_app_non_strict_conditional_eval");

  public AppInfraFileSetEditorCustomization() {
    InfraModelConfigFileNameContributor.EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
      public void extensionAdded(InfraModelConfigFileNameContributor contributor, PluginDescriptor pluginDescriptor) {

        forEachFacet(facet -> {
          Set<CustomSetting> customSettings = facet.getConfiguration().getCustomSettings();
          boolean modified = customSettings.add(contributor.getCustomNameSettingDescriptor().createCustomSetting());
          modified = customSettings.add(contributor.getCustomFilesSettingDescriptor().createCustomSetting()) || modified;
          if (modified) {
            this.setModified(facet);
          }
        });
      }

      private void forEachFacet(Consumer<InfraFacet> consumer) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

        for (Project project : openProjects) {
          Module[] modules = ModuleManager.getInstance(project).getModules();

          for (Module module : modules) {
            InfraFacet facet = InfraFacet.from(module);
            if (facet != null) {
              consumer.accept(facet);
            }
          }
        }

      }

      private void setModified(InfraFacet facet) {
        facet.getConfiguration().setModified();
        FacetManager.getInstance(facet.getModule()).facetConfigurationChanged(facet);
      }
    }, null);
  }

  public boolean isApplicable(InfraFileSet fileSet) {
    return ContainerUtil.exists(InfraModelConfigFileNameContributor.EP_NAME.getExtensions(), (contributor) -> {
      return contributor.accept(fileSet);
    });
  }

  public List<InfraFileSetEditorCustomization.CustomConfigFileGroup> getCustomConfigFileGroups(InfraFileSet fileSet) {
    Module module = fileSet.getFacet().getModule();
    List<VirtualFile> files = InfraConfigurationFileService.of().findConfigFiles(module, false, (contributor) -> contributor.accept(fileSet));

    var allContributorFiles = new HashSet<>(files);
    allContributorFiles.addAll(InfraConfigurationFileService.of().collectImports(module, files));
    var configFiles = new LinkedHashSet<VirtualFilePointer>();

    for (VirtualFilePointer pointer : fileSet.getFiles()) {
      if (pointer.isValid()) {
        VirtualFile file = pointer.getFile();
        if (file != null && allContributorFiles.contains(file)) {
          configFiles.add(pointer);
        }
      }
    }

    if (configFiles.isEmpty()) {
      return Collections.emptyList();
    }
    else {
      Icon configFileGroupIcon = Icons.Today;
      InfraModelConfigFileNameContributor[] extensions = InfraModelConfigFileNameContributor.EP_NAME.getExtensions();

      for (InfraModelConfigFileNameContributor fileNameContributor : extensions) {
        if (fileNameContributor.accept(fileSet)) {
          configFileGroupIcon = fileNameContributor.getFileIcon();
          break;
        }
      }

      return Collections.singletonList(new InfraFileSetEditorCustomization.CustomConfigFileGroup("Configuration Files", configFileGroupIcon, configFiles));
    }
  }

  public List<CustomSetting> getCustomSettings() {
    List<CustomSetting> customSettings = ContainerUtil.newArrayList(
            new CustomSetting[] { new CustomSetting.BOOLEAN(NON_STRICT_SETTING, InfraAppBundle.message("infra.non-strict.conditional.evaluation"), true) });
    InfraModelConfigFileNameContributor[] extensions = InfraModelConfigFileNameContributor.EP_NAME.getExtensions();

    for (InfraModelConfigFileNameContributor fileNameContributor : extensions) {
      customSettings.add(fileNameContributor.getCustomNameSettingDescriptor().createCustomSetting());
      customSettings.add(fileNameContributor.getCustomFilesSettingDescriptor().createCustomSetting());
    }

    return customSettings;
  }

  public AnAction[] getExtraActions() {
    return new AnAction[] { new CustomizeInfraAction() };
  }

  static class InfraCustomConfigFilesSettingPathMacroFilter extends PathMacroFilter {

    @Override
    public boolean recursePathMacros(Attribute attribute) {
      if ("configuration".equals(attribute.getParent().getName())) {
        for (var contributor : InfraModelConfigFileNameContributor.EP_NAME.getExtensions()) {
          if (contributor.getCustomFilesSettingDescriptor().key.toString().equals(attribute.getName())) {
            return true;
          }
        }
      }
      return false;
    }
  }

  private static final class CustomizeInfraAction extends AnAction {
    private CustomizeInfraAction() {
      super(InfraAppBundle.message("infra.customization.action.name"),
              InfraAppBundle.message("infra.customization.action.description"), Icons.Today);
    }

    public void actionPerformed(AnActionEvent e) {

      InfraFileSet fileSet = DataManager.getInstance().loadFromDataContext(e.getDataContext(), cn.taketoday.assistant.facet.InfraFileSetEditorCustomization.EXTRA_ACTION_FILESET);
      if (fileSet != null) {
        var extensions = InfraModelConfigFileNameContributor.EP_NAME.getExtensions();
        for (var fileNameContributor : extensions) {
          if (fileNameContributor.accept(fileSet)) {
            InfraCustomizationDialog dialog = new InfraCustomizationDialog(e.getProject(), fileSet, fileNameContributor);
            dialog.show();
            return;
          }
        }

      }
    }
  }
}

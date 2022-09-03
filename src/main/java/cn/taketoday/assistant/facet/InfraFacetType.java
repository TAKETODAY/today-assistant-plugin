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

package cn.taketoday.assistant.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.beans.CustomSetting;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 18:11
 */
public class InfraFacetType extends FacetType<InfraFacet, InfraFacetConfiguration> {

  public InfraFacetType() {
    super(InfraFacet.FACET_TYPE_ID, "Today", InfraBundle.message("infra"));
    InfraFileSetEditorCustomization.EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
      @Override
      public void extensionAdded(InfraFileSetEditorCustomization customization, PluginDescriptor pluginDescriptor) {

        this.forEachTodayFacet((facet) -> {
          Set<CustomSetting> customSettings = facet.getConfiguration().getCustomSettings();
          boolean modified = customSettings.addAll(customization.getCustomSettings());
          if (modified) {
            this.setModified(facet);
          }

        });
      }

      private void forEachTodayFacet(Consumer<InfraFacet> consumer) {
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

  @Override
  public InfraFacetConfiguration createDefaultConfiguration() {
    return new InfraFacetConfiguration();
  }

  @Override
  public InfraFacet createFacet(Module module, String name,
          InfraFacetConfiguration configuration, Facet underlyingFacet) {
    return new InfraFacet(this, module, name, configuration, underlyingFacet);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public Icon getIcon() {
    return Icons.Today;
  }

  @Override
  public String getHelpTopic() {
    return "IntelliJ.IDEA.Procedures.Java.EE.Development.Managing.Facets.Facet.Specific.Settings.Today";
  }

}

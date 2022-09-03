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

import com.intellij.facet.FacetManager;
import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.library.DownloadableLibraryService;
import com.intellij.framework.library.FrameworkSupportWithLibrary;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurableBase;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.components.JBCheckBox;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraFrameworkSupportProvider extends FacetBasedFrameworkSupportProvider<InfraFacet> {

  private static final String CONFIG_XML = "today-config.xml";
  private boolean myCreateEmptySpringXml;

  public InfraFrameworkSupportProvider() {
    super(InfraFacet.getFacetType());
  }

  public String[] getPrecedingFrameworkProviderIds() {
    return new String[] { "facet:web", "facet:jsf" };
  }

  public String getTitle() {
    return InfraBundle.message("framework.title");
  }

  public String[] getProjectCategories() {
    return new String[] { InfraFacet.FACET_TYPE_ID.toString() };
  }

  public FrameworkRole[] getRoles() {
    return new FrameworkRole[] { InfraProjectCategory.ROLE };
  }

  @Override
  public FrameworkSupportConfigurableBase createConfigurable(FrameworkSupportModel model) {
    return new InfraFrameworkSupportConfigurable(model);
  }

  public void setupConfiguration(InfraFacet facet, ModifiableRootModel rootModel, FrameworkVersion version) {
  }

  public boolean isEnabledForModuleBuilder(ModuleBuilder builder) {
    return InfraProjectCategory.LEGACY_MODULE_BUILDER_ID.equals(builder.getBuilderId());
  }

  public void onFacetCreated(InfraFacet facet, ModifiableRootModel rootModel, FrameworkVersion version) {
    Module module = facet.getModule();
    StartupManager.getInstance(module.getProject()).runAfterOpened(() -> {
      DumbService.getInstance(module.getProject()).runWhenSmart(() -> {
        if (!module.isDisposed()) {
          boolean configured = false;

          for (InfraConfigurator configurator : InfraConfigurator.EP_NAME.getExtensionList()) {
            configured = configurator.configure(module);
            if (configured) {
              break;
            }
          }

          if (!configured && this.myCreateEmptySpringXml) {
            try {
              VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
              if (sourceRoots.length > 0) {
                PsiDirectory directory = PsiManager.getInstance(module.getProject()).findDirectory(sourceRoots[0]);
                if (directory != null && directory.findFile(CONFIG_XML) == null) {
                  FileTemplate template = InfraUtils.getXmlTemplate(module);
                  PsiElement psiElement = FileTemplateUtil.createFromTemplate(template, CONFIG_XML, null, directory);
                  if (!(psiElement instanceof XmlFile)) {
                    return;
                  }

                  VirtualFile file = ((XmlFile) psiElement).getVirtualFile();

                  assert file != null : psiElement;

                  InfraFileSetService fileSetService = InfraFileSetService.of();
                  Set<InfraFileSet> existingSets = Collections.emptySet();
                  String defaultUniqueId = fileSetService.getUniqueId(existingSets);
                  String uniqueName = fileSetService.getUniqueName(InfraBundle.message("facet.context.default.name"), existingSets);
                  InfraFileSet fileSet = facet.addFileSet(defaultUniqueId, uniqueName);
                  fileSet.addFile(file);
                  FacetManager.getInstance(module).facetConfigurationChanged(facet);
                }
              }
            }
            catch (Exception var14) {
              Logger.getInstance(InfraFrameworkSupportProvider.class).error(var14);
            }
          }

        }
      });
    });
  }

  public class InfraFrameworkSupportConfigurable extends FrameworkSupportConfigurableBase implements FrameworkSupportWithLibrary {

    InfraFrameworkSupportConfigurable(FrameworkSupportModel model) {
      super(InfraFrameworkSupportProvider.this, model, InfraFrameworkSupportProvider.this.getVersions(), InfraFrameworkSupportProvider.this.getVersionLabelText());
    }

    public JComponent getComponent() {
      JPanel allPanel = new JPanel(new BorderLayout());
      JBCheckBox createEmptySpringXml = new JBCheckBox(InfraBundle.message("framework.support.provider.create.empty.config", InfraFrameworkSupportProvider.CONFIG_XML));
      createEmptySpringXml.addChangeListener(e -> {
        InfraFrameworkSupportProvider.this.myCreateEmptySpringXml = createEmptySpringXml.isSelected();
      });
      allPanel.add(createEmptySpringXml, "North");
      allPanel.add(Objects.requireNonNull(super.getComponent()), "Center");
      return allPanel;
    }

    public void addSupport(Module module, ModifiableRootModel rootModel, @Nullable Library library) {
      super.addSupport(module, rootModel, library);
    }

    public CustomLibraryDescription createLibraryDescription() {
      return DownloadableLibraryService.getInstance().createDescriptionForType(InfraLibraryType.class);
    }

    public boolean isLibraryOnly() {
      return false;
    }
  }
}

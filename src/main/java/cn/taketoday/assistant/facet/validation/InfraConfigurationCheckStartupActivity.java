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

package cn.taketoday.assistant.facet.validation;

import com.intellij.facet.FacetManager;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.Strings;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.profile.ProfileChangeAdapter;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.DigestUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFrameworkDetector;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

final class InfraConfigurationCheckStartupActivity implements StartupActivity {
  public static final String CONFIGURATION_CHECKSUM_KEY = "infra.configuration.checksum";

  @Override
  public void runActivity(Project project) {
    checkConfiguration(project, false);
  }

  public void checkConfiguration(Project project, boolean force) {
    Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
      return;
    }
    if (!force && (Boolean.TRUE.equals(project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT)) || InfraGeneralSettings.from(project).isAllowAutoConfigurationMode())) {
      return;
    }
    ReadAction.nonBlocking(() -> {
      if (DetectionExcludesConfiguration.getInstance(project).isExcludedFromDetection(InfraFrameworkDetector.getSpringFrameworkType())) {
        return new InfraProjectChecksum(false);
      }
      if (!force && ModuleManager.getInstance(project).getModules().length > 100) {
        return new InfraProjectChecksum(false);
      }
      if (!InfraUtils.hasFacets(project) && !InfraLibraryUtil.hasLibrary(project)) {
        return new InfraProjectChecksum(false);
      }
      return new InfraProjectChecksum(true, computeConfigurationChecksum(project));
    }).inSmartMode(project).finishOnUiThread(ModalityState.NON_MODAL, currentProjectChecksum -> {
      if (!currentProjectChecksum.isInfraProject) {
        return;
      }
      if (!force && Objects.equals(getCurrentConfigurationChecksum(project), currentProjectChecksum.checksum)) {
        Logger.getInstance(InfraConfigurationCheckStartupActivity.class).debug("Spring configuration checksum has not changed");
        return;
      }
      setCurrentConfigurationChecksum(project, Strings.notNullize(currentProjectChecksum.checksum));
      if (ProjectInspectionProfileManager.getInstance(project).isCurrentProfileInitialized()) {
        queueTask(project);
      }
      else {
        project.getMessageBus().connect().subscribe(ProfileChangeAdapter.TOPIC, new ProfileChangeAdapter() {
          public void profilesInitialized() {
            InfraConfigurationCheckStartupActivity.queueTask(project);
          }
        });
      }
    }).submit(AppExecutorUtil.getAppExecutorService());
  }

  private static void queueTask(Project project) {
    ApplicationManager.getApplication().invokeLater(() -> {
      new InfraConfigurationCheckTask(project).queue();
    }, project.getDisposed());
  }

  public static class InfraProjectChecksum {
    public final boolean isInfraProject;
    @Nullable
    public final String checksum;

    InfraProjectChecksum(boolean isInfraProject, @Nullable String checksum) {
      this.isInfraProject = isInfraProject;
      this.checksum = checksum;
    }

    InfraProjectChecksum(boolean isInfraProject) {
      this(isInfraProject, null);
    }
  }

  public static String computeConfigurationChecksum(Project project) {
    StringBuilder checksumContent = new StringBuilder();
    for (Module module : ModuleManager.getInstance(project).getSortedModules()) {
      Collection<InfraFacet> facets = FacetManager.getInstance(module).getFacetsByType(InfraFacet.FACET_TYPE_ID);
      if (!facets.isEmpty()) {
        checksumContent.append(module.getName());
        for (InfraFacet facet : facets) {
          for (InfraFileSet fileSet : facet.getFileSets()) {
            for (String profile : fileSet.getActiveProfiles()) {
              checksumContent.append(profile);
            }
            checksumContent.append(fileSet.getId());
            checksumContent.append(fileSet.getQualifiedName());
            checksumContent.append(fileSet.isAutodetected());
            for (VirtualFilePointer file : fileSet.getFiles()) {
              checksumContent.append(file.getUrl());
            }
          }
        }
      }
      else if (hasInfraJars(module)) {
        checksumContent.append(module.getName());
      }
    }
    return DigestUtil.md5Hex(checksumContent.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static boolean hasInfraJars(Module module) {
    Ref<Boolean> hasSpringCore = Ref.create(false);
    OrderEnumerator.orderEntries(module).forEachLibrary(library -> {
      String libraryName = library.getName();
      if (libraryName != null && libraryName.contains("cn.taketoday:today-core:")) {
        hasSpringCore.set(true);
        return false;
      }
      return true;
    });
    return hasSpringCore.get();
  }

  @Nullable
  public static String getCurrentConfigurationChecksum(Project project) {
    return PropertiesComponent.getInstance(project).getValue(CONFIGURATION_CHECKSUM_KEY);
  }

  public static void setCurrentConfigurationChecksum(Project project, String checksum) {
    PropertiesComponent.getInstance(project).setValue(CONFIGURATION_CHECKSUM_KEY, checksum);
  }
}

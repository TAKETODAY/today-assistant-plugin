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

package cn.taketoday.assistant.app.mvc.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.facet.InfraAutodetectedFileSet;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.service.InfraModelProvider;
import cn.taketoday.lang.Nullable;

public abstract class InfraAutoConfiguredModelContributor implements InfraModelProvider {

  public String getName() {
    return getClass().getSimpleName();
  }

  public final List<? extends InfraAutodetectedFileSet> getFilesets(InfraFacet infraFacet) {
    Module module = infraFacet.getModule();
    if (DumbService.isDumb(module.getProject()) || !isLibraryConfigured(module)) {
      return Collections.emptyList();
    }
    PsiClass application = findSingleApplication(module);
    if (application == null) {
      return Collections.emptyList();
    }
    return getFilesets(infraFacet, application);
  }

  protected List<? extends InfraAutodetectedFileSet> getFilesets(InfraFacet facet, PsiClass application) {
    return Collections.emptyList();
  }

  protected LocalAnnotationModel getInfraBootLocalModel(PsiClass application, Module module) {
    return LocalModelFactory.of().getOrCreateLocalAnnotationModel(application, module, Collections.emptySet());
  }

  protected boolean isLibraryConfigured(Module module) {
    return false;
  }

  @Nullable
  public static PsiClass findSingleApplication(Module module) {
    List<PsiClass> results = InfraApplicationService.of().getInfraApplications(module);
    if (results.size() == 1) {
      return ContainerUtil.getFirstItem(results);
    }
    return null;
  }
}

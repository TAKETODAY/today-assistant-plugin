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

import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.CommonServletMapping;
import com.intellij.javaee.web.WebModelContributor;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.config.anno.PsiBasedServlet;

public class InfraDispatcherServletWebModelContributor extends WebModelContributor {

  private static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

  public List<CommonServlet> getServlets(Module module) {
    return new ArrayList<>(getInfraBootDispatcherServlets(module));
  }

  public List<CommonServletMapping<CommonServlet>> getServletMappings(Module module) {
    return new ArrayList<>(getInfraBootDispatcherServlets(module));
  }

  private static List<PsiBasedServlet> getInfraBootDispatcherServlets(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      return CachedValueProvider.Result.create(getCommonServlets(module), InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies());
    });
  }

  private static List<PsiBasedServlet> getCommonServlets(Module module) {
    if (!InfraUtils.hasFacets(module.getProject())) {
      return Collections.emptyList();
    }
    else if (!InfraLibraryUtil.hasWebMvcLibrary(module)) {
      return Collections.emptyList();
    }
    else {
      PsiClass application = InfraAutoConfiguredModelContributor.findSingleApplication(module);
      if (application == null) {
        return Collections.emptyList();
      }
      LocalAnnotationModel localBootModel = LocalModelFactory.of().getOrCreateLocalAnnotationModel(application, module, Collections.emptySet());
      if (localBootModel == null) {
        return Collections.emptyList();
      }
      BeanPointer<?> dispatcherServletRegistrationConfiguration = InfraModelSearchers.findBean(localBootModel, getDispatcherServletRegistrationConfigurationBeanName(module));
      if (dispatcherServletRegistrationConfiguration == null) {
        return Collections.emptyList();
      }
      BeanPointer<?> dispatcherServlet = InfraModelSearchers.findBean(localBootModel, DISPATCHER_SERVLET_NAME);
      if (dispatcherServlet == null) {
        return Collections.emptyList();
      }
      CommonInfraBean infraBean = dispatcherServlet.getBean();
      if (!(infraBean instanceof JamPsiMemberInfraBean)) {
        return Collections.emptyList();
      }
      PsiElement dispatcherServletMethod = dispatcherServlet.getPsiElement();
      return Collections.singletonList(
              new PsiBasedServlet(DISPATCHER_SERVLET_NAME, dispatcherServlet.getBeanClass(), dispatcherServletMethod, dispatcherServletMethod, "/"));
    }
  }

  private static String getDispatcherServletRegistrationConfigurationBeanName(Module module) {
    return "dispatcherServletAutoConfiguration.DispatcherServletRegistrationConfiguration";
  }
}

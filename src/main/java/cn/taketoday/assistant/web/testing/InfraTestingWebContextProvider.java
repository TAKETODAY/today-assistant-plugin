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

package cn.taketoday.assistant.web.testing;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.BeansInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraImplicitBean;
import cn.taketoday.assistant.model.jam.testContexts.ContextConfiguration;
import cn.taketoday.assistant.model.jam.testContexts.TestingImplicitContextsProvider;
import cn.taketoday.assistant.web.InfraWebConstant;
import cn.taketoday.lang.Nullable;

final class InfraTestingWebContextProvider extends TestingImplicitContextsProvider {

  @Override
  public Collection<CommonInfraModel> getModels(@Nullable Module module, ContextConfiguration configuration, Set<String> activeProfiles) {
    if (module != null) {
      ArrayList<CommonInfraModel> infraModels = new ArrayList<>();
      GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true);
      if (isAnnotated(configuration, module, InfraWebConstant.WEB_APP_CONFIGURATION)) {
        infraModels.add(new BeansInfraModel(module, NotNullLazyValue.lazy(() -> {
          ArrayList<BeanPointer<?>> smartList2 = new ArrayList<>();
          ContainerUtil.addIfNotNull(smartList2, createMockBean(module, searchScope, "mockServletContext", InfraWebConstant.MOCK_SERVLET_CONTEXT));
          ContainerUtil.addIfNotNull(smartList2, createMockBean(module, searchScope, "mockHttpSession", InfraWebConstant.MOCK_HTTP_SESSION));
          ContainerUtil.addIfNotNull(smartList2, createMockBean(module, searchScope, "mockHttpServletRequest", InfraWebConstant.MOCK_HTTP_SERVLET_REQUEST));
          return smartList2;
        })));
      }
      return infraModels;
    }
    return Collections.emptyList();
  }

  @Nullable
  private static BeanPointer<?> createMockBean(Module module, GlobalSearchScope searchScope, String beanName, String beanClass) {
    InfraImplicitBean bean = InfraImplicitBean.create("MVC Testing Beans", JavaPsiFacade.getInstance(module.getProject())
            .findClass(beanClass, searchScope), beanName);
    if (bean != null) {
      return InfraBeanService.of().createBeanPointer(bean);
    }
    return null;
  }
}

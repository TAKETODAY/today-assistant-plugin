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

package cn.taketoday.assistant.beans;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.BeansInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraImplicitBean;
import cn.taketoday.assistant.model.jam.testContexts.ContextConfiguration;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:48
 */
public class TestingImplicitContextsProvider extends cn.taketoday.assistant.model.jam.testContexts.TestingImplicitContextsProvider {

  @Override
  public Collection<CommonInfraModel> getModels(
          @Nullable Module module, ContextConfiguration configuration, Set<String> activeProfiles) {
    LinkedHashSet<CommonInfraModel> models = new LinkedHashSet<>();

    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true);
    if (isAnnotated(configuration, module, "cn.taketoday.test.context.web.WebAppConfiguration")) {
      models.add(new BeansInfraModel(module, NotNullLazyValue.lazy(() -> {
        Collection<BeanPointer<?>> pointers = new SmartList<>();
        ContainerUtil.addIfNotNull(pointers, createMockBean(module, searchScope, "mockServletContext", "cn.taketoday.mock.web.MockServletContext"));
        ContainerUtil.addIfNotNull(pointers, createMockBean(module, searchScope, "mockHttpSession", "cn.taketoday.mock.web.MockHttpSession"));
        ContainerUtil.addIfNotNull(pointers, createMockBean(module, searchScope, "mockHttpServletRequest", "cn.taketoday.mock.web.MockHttpServletRequest"));
        return pointers;
      })));
    }

    return models;
  }

  //

  @Nullable
  private static BeanPointer<?> createMockBean(Module module,
          GlobalSearchScope searchScope, String beanName, String beanClass) {
    InfraImplicitBean bean = InfraImplicitBean.create("MVC Testing Beans", JavaPsiFacade.getInstance(module.getProject())
            .findClass(beanClass, searchScope), beanName);
    return bean != null ? InfraBeanService.of().createBeanPointer(bean) : null;
  }

}

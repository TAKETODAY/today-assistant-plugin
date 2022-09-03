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
package cn.taketoday.assistant.model.custom;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

import java.util.Collection;
import java.util.HashSet;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraImplicitBeansProviderBase;

/**
 * @see InfraImplicitBeansProviderBase
 */
public abstract class CustomModuleComponentsDiscoverer {

  public static final ExtensionPointName<CustomModuleComponentsDiscoverer> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.customModuleComponentsDiscoverer");

  public abstract Collection<CommonInfraBean> getCustomComponents(Module module);

  public abstract String getProviderName();

  /**
   * @see CachedValueProvider.Result#getDependencyItems()
   */
  public abstract Object[] getDependencies(Module module);

  public boolean accepts(Module module) {
    return true;
  }

  public static CommonInfraModel getCustomBeansModel(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      CustomComponentDiscovererBeansModel model =
              new CustomComponentDiscovererBeansModel(module, "") {
                @Override
                public Collection<BeanPointer<?>> getLocalBeans() {
                  Collection<CommonInfraBean> beans = new HashSet<>();
                  for (CustomModuleComponentsDiscoverer discoverer : EP_NAME.getExtensionList()) {
                    if (discoverer.accepts(module)) {
                      beans.addAll(discoverer.getCustomComponents(module));
                    }
                  }
                  return InfraBeanService.of().mapBeans(beans);
                }
              };
      return CachedValueProvider.Result.createSingleDependency(model, ProjectRootManager.getInstance(module.getProject()));
    });
  }
}

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

package cn.taketoday.assistant.context.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiClass;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.lang.Nullable;

public final class BeansInfraModel extends CacheableCommonInfraModel {
  private final NotNullLazyValue<Collection<? extends BeanPointer<?>>> myPointers;
  private final Module myModule;

  public BeansInfraModel(@Nullable Module module, Set<? extends PsiClass> classes) {
    this(module, NotNullLazyValue.lazy(() -> {
      InfraBeanService beanService = InfraBeanService.of();
      Set<BeanPointer<?>> pointers = new LinkedHashSet<>(classes.size());
      for (PsiClass psiClass : classes) {
        if (psiClass.isValid()) {
          pointers.add(beanService.createBeanPointer(new CustomInfraComponent(psiClass)));
        }
      }
      return pointers;
    }));
  }

  public BeansInfraModel(@Nullable Module module, NotNullLazyValue<Collection<? extends BeanPointer<?>>> pointers) {
    this.myModule = module;
    this.myPointers = pointers;
  }

  @Override
  public Collection<BeanPointer<?>> getLocalBeans() {
    return (Collection) this.myPointers.getValue();
  }

  @Override
  @Nullable
  public Module getModule() {
    return this.myModule;
  }
}

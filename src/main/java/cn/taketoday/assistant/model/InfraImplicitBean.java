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
package cn.taketoday.assistant.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;

import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraImplicitBean extends CustomInfraComponent implements InfraImplicitBeanMarker {

  @Nullable
  public static InfraImplicitBean create(@Nullable Module module, @Nullable String providerName, String className, String beanName) {
    if (module == null)
      return null;
    final PsiClass libraryClass = InfraUtils.findLibraryClass(module, className);
    if (libraryClass == null)
      return null;
    return new InfraImplicitBean(providerName == null ? "" : providerName, libraryClass, beanName);
  }

  @Nullable
  public static InfraImplicitBean create(@Nullable String providerName, @Nullable PsiClass psiClass, String beanName) {
    if (psiClass == null)
      return null;
    return new InfraImplicitBean(providerName == null ? "" : providerName, psiClass, beanName);
  }

  private final String myBeanName;

  private final String myProviderName;

  public InfraImplicitBean(String providerName,
          PsiClass psiClass,
          String beanName) {
    super(psiClass);
    myProviderName = providerName;
    myBeanName = beanName;
  }

  @Override
  public String getBeanName() {
    return myBeanName;
  }

  @Override
  public String getProviderName() {
    return myProviderName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    InfraImplicitBean bean = (InfraImplicitBean) o;

    if (!myBeanName.equals(bean.myBeanName))
      return false;
    if (!myProviderName.equals(bean.myProviderName))
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myBeanName.hashCode();
    result = 31 * result + myProviderName.hashCode();
    return result;
  }
}

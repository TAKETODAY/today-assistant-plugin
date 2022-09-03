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

import com.intellij.jam.JavaLibraryUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Processor;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.lang.Nullable;

public class ObjectFactoryEffectiveTypeProvider extends BeanEffectiveTypeProvider {

  @Override
  public boolean processEffectiveTypes(CommonInfraBean bean, Processor<PsiType> processor) {
    PsiType factoryEffectiveType = getObjectFactoryEffectiveType(bean.getBeanType());
    if (factoryEffectiveType != null) {
      return processor.process(factoryEffectiveType);
    }
    return true;
  }

  @Nullable
  public static PsiType getObjectFactoryEffectiveType(@Nullable PsiType beanType) {
    if (beanType instanceof PsiClassType type) {
      if (isObjectFactory(type)) {
        return getObjectFactoryType(type);
      }
      if (isJavaxInjectProvider(type)) {
        return getJavaxInjectProviderType(type);
      }
      if (isJakartaInjectProvider(type)) {
        return getJakartaInjectProviderType(type);
      }
    }
    return null;
  }

  public static boolean isObjectFactory(PsiClassType psiClassType) {
    return isInheritor(psiClassType, InfraConstant.OBJECT_FACTORY_CLASS);
  }

  public static boolean isJavaxInjectProvider(PsiClassType psiClassType) {
    return isInheritor(psiClassType, InfraConstant.JAVAX_INJECT_PROVIDER_CLASS);
  }

  public static boolean isJakartaInjectProvider(PsiClassType psiClassType) {
    return isInheritor(psiClassType, InfraConstant.JAKARTA_INJECT_PROVIDER_CLASS);
  }

  private static boolean isInheritor(PsiClassType psiClassType, String fqn) {
    PsiClass resolve = psiClassType.resolve();
    return resolve != null && JavaLibraryUtils.hasLibraryClass(resolve.getProject(), fqn) && InheritanceUtil.isInheritor(resolve, fqn);
  }

  @Nullable
  public static PsiType getJavaxInjectProviderType(PsiClassType psiClassType) {
    return PsiUtil.substituteTypeParameter(psiClassType, InfraConstant.JAVAX_INJECT_PROVIDER_CLASS, 0, false);
  }

  public static PsiType getJakartaInjectProviderType(PsiClassType psiClassType) {
    return PsiUtil.substituteTypeParameter(psiClassType, InfraConstant.JAKARTA_INJECT_PROVIDER_CLASS, 0, false);
  }

  @Nullable
  public static PsiType getObjectFactoryType(PsiClassType psiClassType) {
    return PsiUtil.substituteTypeParameter(psiClassType, InfraConstant.OBJECT_FACTORY_CLASS, 0, false);
  }
}

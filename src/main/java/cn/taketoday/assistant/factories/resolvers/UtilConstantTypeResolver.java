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

package cn.taketoday.assistant.factories.resolvers;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.factories.ObjectTypeResolver;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.util.InfraConstant;
import cn.taketoday.lang.Nullable;

public class UtilConstantTypeResolver implements ObjectTypeResolver {

  private static final String FACTORY_CLASS = "cn.taketoday.beans.factory.config.FieldRetrievingFactoryBean";

  private static final char SEPARATOR = '.';

  @Override
  public Set<PsiType> getObjectType(@Nullable CommonInfraBean context) {
    PsiField psiField;
    if (context instanceof InfraConstant constant) {
      String staticField = StringUtil.notNullize(constant.getStaticField().getStringValue());
      int lastDotIndex = staticField.lastIndexOf(SEPARATOR);
      if (lastDotIndex != -1) {
        String className = staticField.substring(0, lastDotIndex);
        String fieldName = staticField.substring(lastDotIndex + 1);
        PsiClass psiClass = findClassByExternalName(context, className);
        if (psiClass != null && (psiField = psiClass.findFieldByName(fieldName, true)) != null) {
          return Collections.singleton(psiField.getType());
        }
      }
    }
    return Collections.emptySet();
  }

  @Nullable
  private static PsiClass findClassByExternalName(CommonInfraBean context, String externalName) {
    Module module = context.getModule();
    if (module != null) {
      GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
      PsiManager psiManager = context.getPsiManager();
      String className = externalName.replace('$', '.');
      return JavaPsiFacade.getInstance(psiManager.getProject()).findClass(className, scope);
    }
    return null;
  }

  @Override
  public boolean accept(String factoryClassName) {
    return factoryClassName.equals(FACTORY_CLASS);
  }
}

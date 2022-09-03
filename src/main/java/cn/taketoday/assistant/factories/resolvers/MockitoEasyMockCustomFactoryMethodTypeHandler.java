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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.factories.CustomFactoryMethodTypeHandler;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class MockitoEasyMockCustomFactoryMethodTypeHandler extends CustomFactoryMethodTypeHandler {
  private static final Map<String, String> METHOD_TO_CLASS_MAP = Map.of(
          "mock", "org.mockito.Mockito",
          "createStrictMock", "org.easymock.EasyMock"
  );

  @Nullable
  public PsiType getFactoryMethodType(PsiMethod psiMethod, @Nullable CommonInfraBean contextBean) {
    if (contextBean instanceof InfraBean) {
      String methodName = psiMethod.getName();
      if (METHOD_TO_CLASS_MAP.containsKey(methodName)) {
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass != null) {
          for (Map.Entry<String, String> entry : METHOD_TO_CLASS_MAP.entrySet()) {
            if (entry.getKey().equals(methodName) && entry.getValue().equals(containingClass.getQualifiedName())) {
              List<ConstructorArg> constructorArgs = ((InfraBean) contextBean).getConstructorArgs();
              if (constructorArgs.size() == 1) {
                String classFqn = constructorArgs.get(0).getValueAsString();
                if (classFqn != null) {
                  JavaPsiFacade facade = JavaPsiFacade.getInstance(psiMethod.getProject());
                  PsiClass psiClass = facade.findClass(classFqn, psiMethod.getResolveScope());
                  if (psiClass != null) {
                    return facade.getElementFactory().createType(psiClass);
                  }
                }
              }
            }
          }

        }
      }
    }
    return null;
  }
}

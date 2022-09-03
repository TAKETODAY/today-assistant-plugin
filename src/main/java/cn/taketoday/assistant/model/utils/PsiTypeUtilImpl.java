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

package cn.taketoday.assistant.model.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Nullable;

public class PsiTypeUtilImpl extends PsiTypeUtil {
  private final Map<Class, Class[]> listOrSetConverters = new HashMap();
  private final Project myProject;

  public PsiTypeUtilImpl(Project project) {
    this.myProject = project;
    Class[] classes = { Object[].class, boolean[].class, byte[].class, short[].class, int[].class, long[].class, float[].class, double[].class };
    this.listOrSetConverters.put(Set.class, classes);
    this.listOrSetConverters.put(List.class, classes);
  }

  @Override
  @Nullable
  public PsiType findType(Class<?> aClass) {
    if (aClass.isArray()) {
      Class<?> componentType = aClass.getComponentType();
      PsiType componentClassType = findType(componentType);
      if (componentClassType == null) {
        return null;
      }
      return componentClassType.createArrayType();
    }
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(this.myProject);
    if (aClass.isPrimitive()) {
      return javaPsiFacade.getElementFactory().createPrimitiveType(aClass.getName());
    }
    PsiClass psiClass = javaPsiFacade.findClass(aClass.getName(), GlobalSearchScope.allScope(this.myProject));
    if (psiClass != null) {
      return javaPsiFacade.getElementFactory().createType(psiClass);
    }
    return null;
  }

  @Override
  public boolean isCollectionType(PsiType psiType) {
    PsiType collectionType = findType(Collection.class);
    return collectionType != null && collectionType.isAssignableFrom(psiType);
  }

  @Override
  public boolean isConvertable(PsiType from, List<? extends PsiType> types) {
    for (PsiType psiType : types) {
      if (psiType != null && isConvertable(from, psiType)) {
        return true;
      }
    }
    return false;
  }

  private boolean isConvertable(PsiType from, PsiType to) {
    if (to instanceof PsiClassType) {
      to = ((PsiClassType) to).rawType();
    }
    if (to.isAssignableFrom(from)) {
      return true;
    }
    if (from.equalsToText("java.lang.String") && isStringConvertable(to)) {
      return true;
    }
    if (!from.equalsToText("java.util.Set") || !to.equalsToText("java.util.List")) {
      if (to.equalsToText("java.util.Set") && from.equalsToText("java.util.List")) {
        return true;
      }
      if (to instanceof PsiArrayType) {
        PsiType type = ((PsiArrayType) to).getComponentType();
        if (type.isAssignableFrom(from)) {
          return true;
        }
        if (from instanceof PsiClassType) {
          PsiClass resolved = ((PsiClassType) from).resolve();
          if (InheritanceUtil.isInheritor(resolved, "java.util.Collection")) {
            return true;
          }
        }
      }
      if ((to instanceof PsiClassType) && InheritanceUtil.isInheritor(((PsiClassType) to).resolve(), "java.util.Properties")) {
        return (from instanceof PsiClassType) && InheritanceUtil.isInheritor(((PsiClassType) from).resolve(), "java.util.Map");
      }
      for (Class registeredClass : this.listOrSetConverters.keySet()) {
        PsiType registeredFromType = findType(registeredClass);
        if (registeredFromType != null && from.isAssignableFrom(registeredFromType)) {
          Class<?>[] classes = this.listOrSetConverters.get(registeredClass);
          for (Class<?> aClass : classes) {
            PsiType registeredTooType = findType(aClass);
            if (registeredTooType != null && (registeredTooType.equals(to) || registeredTooType.isAssignableFrom(to))) {
              return true;
            }
          }
        }
      }
      return false;
    }
    return true;
  }

  private static boolean isStringConvertable(PsiType requiredType) {
    if (requiredType instanceof PsiClassType) {
      PsiClass psiClass = ((PsiClassType) requiredType).resolve();
      if (psiClass != null) {
        for (PsiMethod constructor : psiClass.getConstructors()) {
          PsiParameterList parameterList = constructor.getParameterList();
          if (parameterList.getParametersCount() == 1) {
            PsiParameter parameter = parameterList.getParameters()[0];
            if (String.class.getCanonicalName().equals(parameter.getType().getCanonicalText())) {
              return true;
            }
          }
        }
        return false;
      }
      return false;
    }
    else if (requiredType instanceof PsiPrimitiveType) {
      PsiType[] convertable = { PsiType.BOOLEAN, PsiType.BYTE, PsiType.CHAR, PsiType.DOUBLE, PsiType.FLOAT, PsiType.INT, PsiType.LONG, PsiType.SHORT };
      for (PsiType psiType : convertable) {
        if (requiredType.equals(psiType)) {
          return true;
        }
      }
      return false;
    }
    else {
      return false;
    }
  }
}

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

package cn.taketoday.assistant.model.xml.beans.impl;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraMap;
import cn.taketoday.assistant.model.xml.beans.TypeHolder;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

public abstract class InfraEntryImpl extends InfraInjectionImpl implements InfraEntry {

  @Override
  public List<PsiType> getRequiredTypes() {
    InfraMap map = (InfraMap) getParent();
    List<PsiType> psiTypes = TypedCollectionImpl.getRequiredTypes(map);
    if (!psiTypes.isEmpty()) {
      return psiTypes;
    }
    PsiClass valueType = getValueType().getValue();
    if (valueType != null) {
      return Collections.singletonList(JavaPsiFacade.getElementFactory(valueType.getProject()).createType(valueType));
    }
    else {
      return Collections.singletonList(getRequiredTypeFromGenerics(map, 1));
    }
  }

  @Override
  @Nullable
  public PsiClass getRequiredKeyClass() {
    InfraMap map = (InfraMap) getParent();
    PsiClass psiClass = map.getKeyType().getValue();
    if (psiClass != null) {
      return psiClass;
    }
    PsiType requiredTypeFromGenerics = getRequiredTypeFromGenerics(map, 0);
    if (!(requiredTypeFromGenerics instanceof PsiClassType psiClassType)) {
      return null;
    }
    return psiClassType.resolve();
  }

  @Override
  @Nullable
  public PsiType getRequiredKeyType() {
    InfraMap map = (InfraMap) getParent();
    PsiClass psiClass = map.getKeyType().getValue();
    if (psiClass != null) {
      return PsiTypesUtil.getClassType(psiClass);
    }
    return getRequiredTypeFromGenerics(map, 0);
  }

  @Nullable
  private static PsiType getRequiredTypeFromGenerics(InfraMap map, int index) {
    PsiClassType.ClassResolveResult resolveResult;
    PsiClass psiClass;
    DomElement parent = map.getParent();
    if (parent instanceof TypeHolder) {
      List<PsiType> types = TypeHolderUtil.getRequiredTypes((TypeHolder) parent);
      for (PsiType psiType : types) {
        if (psiType instanceof PsiClassType psiClassType
                && (psiClass = (resolveResult = psiClassType.resolveGenerics()).getElement()) != null) {
          PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
          if (typeParameters.length == 2) {
            PsiSubstitutor substitutor = resolveResult.getSubstitutor();
            return substitutor.substitute(typeParameters[index]);
          }
        }
      }
      return null;
    }
    return null;
  }

  @Override
  public GenericAttributeValue<BeanPointer<?>> getRefAttr() {
    return getValueRef();
  }
}

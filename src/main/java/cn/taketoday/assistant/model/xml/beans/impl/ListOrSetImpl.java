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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xml.DomElement;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.beans.TypeHolder;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class ListOrSetImpl extends TypedCollectionImpl implements ListOrSet {

  @Override

  public List<PsiType> getRequiredTypes() {
    PsiType javaLangObject;
    List<PsiType> list = super.getRequiredTypes();
    if (!list.isEmpty()) {
      return list;
    }
    PsiType fromGenerics = getRequiredTypeFromGenerics();
    Project project = getManager().getProject();
    if (fromGenerics != null) {
      javaLangObject = fromGenerics;
    }
    else {
      javaLangObject = PsiType.getJavaLangObject(PsiManager.getInstance(project), GlobalSearchScope.allScope(project));
    }
    PsiType type = javaLangObject;
    return Collections.singletonList(type);
  }

  @Nullable
  private PsiType getRequiredTypeFromGenerics() {
    DomElement parent = getParent();
    if (parent instanceof TypeHolder) {
      List<PsiType> types = ((TypeHolder) parent).getRequiredTypes();
      if (types.isEmpty()) {
        return null;
      }
      PsiType psiType = types.get(0);
      if (psiType instanceof PsiClassType psiClassType) {
        List<PsiType> list = InfraUtils.resolveGenerics(psiClassType);
        if (list.size() != 1) {
          return null;
        }
        return list.get(0);
      }
      else if (psiType instanceof PsiArrayType) {
        return ((PsiArrayType) psiType).getComponentType();
      }
      else {
        return null;
      }
    }
    return null;
  }
}

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
package cn.taketoday.assistant.model.xml.beans;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public final class TypeHolderUtil {
  private static final Key<CachedValue<List<PsiType>>> TYPE_HOLDER_TYPES = Key.create("TYPE_HOLDER_TYPES");

  private TypeHolderUtil() {
  }

  @Nullable
  public static PsiType getRequiredType(TypeHolder typeHolder) {
    final List<PsiType> psiTypes = getRequiredTypes(typeHolder);
    return ContainerUtil.getFirstItem(psiTypes);
  }

  public static List<PsiType> getRequiredTypes(final TypeHolder typeHolder) {
    if (!DomUtil.hasXml(typeHolder) ||
            !typeHolder.isValid()) {
      return Collections.emptyList();
    }

    return CachedValuesManager.getManager(typeHolder.getManager().getProject())
            .getCachedValue(typeHolder, TYPE_HOLDER_TYPES, () -> {
              List<PsiType> requiredTypes = typeHolder.getRequiredTypes();
              return Result.create(requiredTypes, DomUtil.getFile(typeHolder));
            }, false);
  }

  @Nullable
  public static PsiClassType getRequiredClassType(TypeHolder typeHolder) {
    final PsiType injectionType = getRequiredType(typeHolder);
    if (injectionType instanceof PsiClassType) {
      return (PsiClassType) injectionType;
    }
    return null;
  }
}

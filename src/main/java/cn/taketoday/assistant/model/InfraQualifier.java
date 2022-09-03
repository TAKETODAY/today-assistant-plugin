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

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public interface InfraQualifier extends CommonModelElement {

  @Override
  PsiElement getIdentifyingPsiElement();

  @Nullable
  PsiClass getQualifierType();

  @Nullable
  String getQualifierValue();

  List<? extends QualifierAttribute> getQualifierAttributes();

  default boolean compareQualifiers(@Nullable InfraQualifier other, @Nullable Module module) {
    return InfraQualifierComparator.compareQualifiers(this, other);
  }
}

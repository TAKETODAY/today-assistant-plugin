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

package cn.taketoday.assistant.model.values;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import java.util.List;

import cn.taketoday.lang.Nullable;

public interface InfraPlaceholderReferenceResolver {
  ExtensionPointName<InfraPlaceholderReferenceResolver> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.placeholderReferenceResolver");

  Pair<List<PsiElement>, List<VirtualFile>> resolve(PsiReference reference);

  List<LookupElement> getVariants(PsiReference reference);

  boolean isProperty(PsiElement psiElement);

  @Nullable
  String getPropertyValue(PsiReference reference, PsiElement psiElement);
}

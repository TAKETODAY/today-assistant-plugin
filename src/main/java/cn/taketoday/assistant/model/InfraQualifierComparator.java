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
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.util.containers.CollectionFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.model.jam.qualifiers.InfraJamQualifier;
import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

public final class InfraQualifierComparator {

  public static boolean compareQualifiers(@Nullable InfraQualifier one, @Nullable InfraQualifier two) {
    if (one == null || two == null)
      return false;
    if (!Comparing.equal(one.getQualifierType(), two.getQualifierType()))
      return false;
    if (!Objects.equals(one.getQualifierValue(), two.getQualifierValue()))
      return false;
    List<? extends QualifierAttribute> list1 = one.getQualifierAttributes();
    int size1 = list1.size();
    List<? extends QualifierAttribute> list2 = two.getQualifierAttributes();
    int size2 = list2.size();
    if (size1 != size2)
      return false;
    if (size1 == 0)
      return true;
    Set<QualifierAttribute> set = CollectionFactory.createCustomHashingStrategySet(QualifierAttribute.HASHING_STRATEGY);
    set.addAll(list1);
    return set.containsAll(list2);
  }

  public static boolean compareInheritorQualifier(@Nullable InfraQualifier childrenQualifier,
          @Nullable InfraQualifier baseQualifier,
          @Nullable Module module) {
    if (childrenQualifier instanceof InfraInheritableQualifier &&
            baseQualifier instanceof InfraInheritableQualifier) {
      if (Comparing.equal(childrenQualifier.getQualifierType(), baseQualifier.getQualifierType()))
        return false;

      if (module == null)
        return false;

      PsiClass baseType = baseQualifier.getQualifierType();
      PsiClass childrenType = childrenQualifier.getQualifierType();
      if (baseType != null && childrenType != null) {
        String baseAnnoQualifiedName = baseType.getQualifiedName();
        if (baseAnnoQualifiedName != null) {
          Collection<PsiClass> children =
                  JamAnnotationTypeUtil.getAnnotationTypesWithChildren(module, baseAnnoQualifiedName);
          PsiAnnotation definingMetaAnnotation =
                  AliasForUtils.findDefiningMetaAnnotation(childrenType, baseAnnoQualifiedName, children);
          if (definingMetaAnnotation != null) {
            return compareQualifiers(new InfraJamQualifier(definingMetaAnnotation, null), baseQualifier);
          }
        }
      }
    }

    return false;
  }
}

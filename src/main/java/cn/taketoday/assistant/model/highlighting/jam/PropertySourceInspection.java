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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;

import java.util.Collection;

import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.jam.stereotype.PropertySource;
import cn.taketoday.assistant.model.jam.utils.InfraResourceLocationsUtil;
import cn.taketoday.assistant.service.InfraJamService;
import kotlin.jvm.internal.Intrinsics;

public final class PropertySourceInspection extends AbstractInfraLocalInspection {

  public PropertySourceInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement nameIdentifier;
    Intrinsics.checkNotNullParameter(uClass, "uClass");
    Intrinsics.checkNotNullParameter(manager, "manager");
    UElement uastAnchor = uClass.getUastAnchor();
    if (uastAnchor == null || (nameIdentifier = uastAnchor.getSourcePsi()) == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiClass psiClass = uClass.getJavaPsi();
    Collection<PropertySource> propertySources = InfraJamService.of().getPropertySources(psiClass);

    if (!propertySources.isEmpty()) {
      ProblemsHolder holder = new ProblemsHolder(manager, nameIdentifier.getContainingFile(), isOnTheFly);
      for (PropertySource propertySource : propertySources) {
        Intrinsics.checkNotNullExpressionValue(propertySource, "propertySource");
        if (!propertySource.isIgnoreResourceNotFound()) {
          for (JamStringAttributeElement locationAttributeElement : propertySource.getLocationElements()) {
            InfraResourceLocationsUtil infraResourceLocationsUtil = InfraResourceLocationsUtil.INSTANCE;
            infraResourceLocationsUtil.checkResourceLocation(holder, locationAttributeElement);
          }
        }
      }
      return holder.getResultsArray();
    }
    return ProblemDescriptor.EMPTY_ARRAY;
  }
}

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

package cn.taketoday.assistant.model.jam.dependsOn;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.psi.PsiClass;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.highlighting.jam.BeanPointerResolveInspection;
import cn.taketoday.lang.Nullable;

public final class DependsOnUnresolvedBeanInspection extends BeanPointerResolveInspection {

  @Nullable
  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    if (isPlainJavaFileInInfraModule(aClass)) {
      ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
      InfraJamDependsOn jamElement = InfraJamDependsOn.META.getJamElement(aClass);
      if (jamElement != null) {
        for (JamStringAttributeElement<BeanPointer<?>> dependsOnAttribute : jamElement.getDependsOnAttributes()) {
          checkBeanPointerResolve(holder, dependsOnAttribute);
        }
      }
      return holder.getResultsArray();
    }
    return null;
  }
}

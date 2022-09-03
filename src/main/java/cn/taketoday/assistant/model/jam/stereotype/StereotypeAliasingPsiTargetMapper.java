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

package cn.taketoday.assistant.model.jam.stereotype;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.psi.targets.AliasingPsiTargetMapper;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;

public class StereotypeAliasingPsiTargetMapper implements AliasingPsiTargetMapper {

  public Set<AliasingPsiTarget> getTargets(PomTarget psiTarget) {
    if (!(psiTarget instanceof PsiClass)) {
      return Collections.emptySet();
    }
    return ReadAction.compute(() -> {
      PsiClass psiClass = (PsiClass) psiTarget;
      return psiClass.isInterface() ? Collections.emptySet() : (Set) DumbService.getInstance(psiClass.getProject()).runReadActionInSmartMode(() -> {
        if (!InfraUtils.isBeanCandidateClassInProject(psiClass)) {
          return Collections.emptySet();
        }
        InfraStereotypeElement stereotypeElement = InfraJamService.of().findStereotypeElement(psiClass);
        if (stereotypeElement != null
                && psiClass.equals(stereotypeElement.getPsiElement())
                && (stereotypeElement.getPsiTarget() instanceof AliasingPsiTarget)) {
          return Collections.singleton(stereotypeElement.getPsiTarget());
        }
        return Collections.emptySet();
      });
    });
  }
}

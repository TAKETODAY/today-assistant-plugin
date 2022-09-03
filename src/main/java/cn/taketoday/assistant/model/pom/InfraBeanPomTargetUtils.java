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
package cn.taketoday.assistant.model.pom;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamPomTarget;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.targets.AliasingPsiTarget;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public final class InfraBeanPomTargetUtils {

  @Nullable
  public static CommonInfraBean getBean(PsiElement element) {
    if (!(element instanceof PomTargetPsiElement pomTargetPsiElement)) {
      return null;
    }

    PomTarget target = pomTargetPsiElement.getTarget();
    if (!target.isValid()) {
      return null;
    }

    return getBean(target);
  }

  @Nullable
  public static CommonInfraBean getBean(PomTarget target) {
    if (target instanceof AliasingPsiTarget) {
      PsiElement navigationElement = ((AliasingPsiTarget) target).getNavigationElement();
      if (!(navigationElement instanceof PsiClass))
        return null;

      InfraStereotypeElement stereotypeElement = InfraJamService.of().findStereotypeElement((PsiClass) navigationElement);
      if (stereotypeElement != null &&
              stereotypeElement.getPsiTarget() instanceof AliasingPsiTarget) {
        return stereotypeElement;
      }

      return null;
    }

    if (target instanceof BeanPsiTarget) {
      return ((BeanPsiTarget) target).getInfraBean();
    }

    if (target instanceof JamPomTarget) {
      JamElement jamElement = ((JamPomTarget) target).getJamElement();
      if (jamElement instanceof JamPsiMemberInfraBean) {
        return ((JamPsiMemberInfraBean) jamElement);
      }
    }

    return null;
  }
}

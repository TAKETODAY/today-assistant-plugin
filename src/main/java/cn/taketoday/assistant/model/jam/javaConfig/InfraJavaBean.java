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
package cn.taketoday.assistant.model.jam.javaConfig;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNamedElement;

import cn.taketoday.assistant.model.jam.JamPsiMethodInfraBean;

public abstract class InfraJavaBean extends JamPsiMethodInfraBean {

  public InfraJavaBean(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  public PsiNamedElement getIdentifyingPsiElement() {
    return getPsiElement();
  }

  public abstract PsiAnnotation getPsiAnnotation();

  public boolean isPublic() {
    return getPsiElement().getModifierList().hasModifierProperty(PsiModifier.PUBLIC);
  }
}

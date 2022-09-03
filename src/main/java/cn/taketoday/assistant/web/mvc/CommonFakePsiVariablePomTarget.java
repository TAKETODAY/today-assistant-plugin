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

package cn.taketoday.assistant.web.mvc;

import com.intellij.microservices.utils.CommonFakeNavigatablePomTarget;
import com.intellij.openapi.project.Project;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;

import cn.taketoday.lang.Nullable;

public class CommonFakePsiVariablePomTarget extends CommonFakeNavigatablePomTarget implements PsiVariable {

  private final PsiType psiType;

  public final PsiType getPsiType() {
    return this.psiType;
  }

  public CommonFakePsiVariablePomTarget(Project project, PomRenameableTarget<? extends Object> pomRenameableTarget, PsiType psiType) {
    super(project, pomRenameableTarget);
    this.psiType = psiType;
  }

  public boolean hasModifierProperty(String name) {
    return false;
  }

  public void normalizeDeclaration() {
  }

  public PsiType getType() {
    return this.psiType;
  }

  @Nullable
  public PsiTypeElement getTypeElement() {
    return null;
  }

  @Nullable
  public PsiExpression getInitializer() {
    return null;
  }

  public boolean hasInitializer() {
    return true;
  }

  @Nullable
  public PsiIdentifier getNameIdentifier() {
    return null;
  }

  @Nullable
  public PsiModifierList getModifierList() {
    return null;
  }

  @Nullable
  public Object computeConstantValue() {
    return null;
  }

  public String toString() {
    return "CommonFakePsiVariablePomTarget(" + getTarget() + ")";
  }
}

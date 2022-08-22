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

package cn.taketoday.assistant.code.event.beans;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastUtils;

import cn.taketoday.lang.Nullable;

public class MethodInvocationPublisher extends PublishEventPointDescriptor {

  private final UExpression myEventInitialisedExpression;

  public MethodInvocationPublisher(UExpression expression) {
    this.myEventInitialisedExpression = expression;
  }

  public UExpression getEventInitialisedExpression() {
    return this.myEventInitialisedExpression;
  }

  @Override
  @Nullable
  public PsiType getEventType() {
    return this.myEventInitialisedExpression.getExpressionType();
  }

  @Override
  public PsiElement getIdentifyingElement() {
    PsiElement sourcePsi = myEventInitialisedExpression.getSourcePsi();
    if (sourcePsi == null) {
      throw new PsiInvalidElementAccessException(null, "Source PSI of event publishing expression is null");
    }
    return sourcePsi;
  }

  @Override
  public PsiElement getNavigatableElement() {
    UCallExpression callExpression = UastUtils.getUCallExpression(myEventInitialisedExpression.getUastParent());
    if (callExpression == null) {
      return super.getNavigatableElement();
    }
    PsiElement sourcePsi = callExpression.getSourcePsi();
    if (sourcePsi != null) {
      return sourcePsi;
    }
    return super.getNavigatableElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodInvocationPublisher publisher)) {
      return false;
    }
    return this.myEventInitialisedExpression.equals(publisher.myEventInitialisedExpression);
  }

  @Override
  public int hashCode() {
    return this.myEventInitialisedExpression.hashCode();
  }
}

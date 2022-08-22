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

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UExpression;

import cn.taketoday.lang.Nullable;

public abstract class PublishEventPointDescriptor {
  @Nullable
  public abstract PsiType getEventType();

  public abstract PsiElement getIdentifyingElement();

  public static PublishEventPointDescriptor create(UExpression expression) {
    return new MethodInvocationPublisher(expression);
  }

  public static PublishEventPointDescriptor create(PsiMethod expression, Module module) {
    return new EventListenerAnnoPublisher(expression, module);
  }

  public PsiElement getNavigatableElement() {
    return getIdentifyingElement();
  }
}

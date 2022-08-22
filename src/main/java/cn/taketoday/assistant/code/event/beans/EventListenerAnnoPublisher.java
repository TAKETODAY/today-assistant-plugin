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
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import cn.taketoday.assistant.code.event.jam.EventModelUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:30
 */
public class EventListenerAnnoPublisher extends PublishEventPointDescriptor {

  private final PsiAnchor myMethodAnchor;
  @Nullable
  private final PsiType myReturnType;

  public EventListenerAnnoPublisher(PsiMethod psiMethod, Module module) {
    this.myMethodAnchor = PsiAnchor.create(psiMethod);
    this.myReturnType = EventModelUtils.getEventType(psiMethod.getReturnType(), module);
  }

  public PsiMethod getMethod() {
    return (PsiMethod) this.myMethodAnchor.retrieveOrThrow();
  }

  @Override
  @Nullable
  public PsiType getEventType() {
    return this.myReturnType;
  }

  public boolean isValid() {
    PsiElement element = this.myMethodAnchor.retrieve();
    return element != null && element.isValid();
  }

  @Override

  public PsiElement getIdentifyingElement() {
    return getMethod();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventListenerAnnoPublisher publisher = (EventListenerAnnoPublisher) o;
    return this.myMethodAnchor.equals(publisher.myMethodAnchor);
  }

  public int hashCode() {
    return this.myMethodAnchor.hashCode();
  }
}

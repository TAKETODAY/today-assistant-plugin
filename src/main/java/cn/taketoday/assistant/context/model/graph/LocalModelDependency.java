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

package cn.taketoday.assistant.context.model.graph;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomElement;

import cn.taketoday.lang.Nullable;

public final class LocalModelDependency {
  private final LocalModelDependencyType myType;
  private final PsiElement myIdentifyingElement;

  private final String myLabel;

  public static LocalModelDependency create() {
    return new LocalModelDependency("", LocalModelDependencyType.UNKNOWN, null);
  }

  public static LocalModelDependency create(LocalModelDependencyType type, PsiElement identifyingElement) {
    String text = null;
    if (identifyingElement instanceof PsiClass) {
      text = ((PsiClass) identifyingElement).getQualifiedName();
    }
    else if (identifyingElement instanceof PsiAnnotation) {
      String qualifiedName = ((PsiAnnotation) identifyingElement).getQualifiedName();
      if (qualifiedName != null) {
        text = "@" + qualifiedName;
      }
    }
    else {
      text = identifyingElement.getText();
    }
    return new LocalModelDependency(text != null ? text : "", type, identifyingElement);
  }

  public static LocalModelDependency create(String label, LocalModelDependencyType type, final DomElement identifyingElement) {
    return new LocalModelDependency(label, type, new FakePsiElement() {

      private XmlElement getXmlElement() {
        return identifyingElement.getXmlElement();
      }

      public PsiElement getParent() {
        return getXmlElement();
      }

      public void delete() throws IncorrectOperationException {
        getXmlElement().delete();
      }

      public PsiElement getNavigationElement() {
        return getXmlElement();
      }
    });
  }

  public static LocalModelDependency create(String label, LocalModelDependencyType type, @Nullable PsiElement identifyingElement) {
    return new LocalModelDependency(label, type, identifyingElement);
  }

  private LocalModelDependency(String label, LocalModelDependencyType type, @Nullable PsiElement identifyingElement) {
    this.myLabel = label;
    this.myType = type;
    this.myIdentifyingElement = identifyingElement;
  }

  public LocalModelDependencyType getType() {
    return myType;
  }

  @Nullable
  public PsiElement getIdentifyingElement() {
    return this.myIdentifyingElement;
  }

  public String toString() {
    return "LocalModelDependency{" + this.myType + ", " + this.myIdentifyingElement + "}";
  }

  public String getLabel() {
    return myLabel;
  }
}

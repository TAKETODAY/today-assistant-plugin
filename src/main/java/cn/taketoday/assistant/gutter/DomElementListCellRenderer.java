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
package cn.taketoday.assistant.gutter;

import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;

import javax.swing.Icon;

import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class DomElementListCellRenderer extends PsiElementListCellRenderer<PsiElement> {

  protected final String myUnknown;

  public DomElementListCellRenderer(String unknownElementText) {
    myUnknown = unknownElementText;
  }

  @Override
  public String getElementText(PsiElement element) {
    String elementName = null;

    DomElement domElement = getDomElement(element);
    if (domElement != null) {
      elementName = domElement.getPresentation().getElementName();
    }
    else if (element instanceof XmlTag) {
      return ((XmlTag) element).getName();
    }

    return elementName == null ? myUnknown : elementName;
  }

  @Nullable
  public static DomElement getDomElement(@Nullable PsiElement element) {
    if (element instanceof PomTargetPsiElement) {
      return getDomElement(element.getNavigationElement());
    }
    else if (element instanceof XmlTag) {
      return DomManager.getDomManager(element.getProject()).getDomElement((XmlTag) element);
    }
    return null;
  }

  @Override
  protected String getContainerText(PsiElement element, String name) {
    return getContainerText(element);
  }

  public static String getContainerText(PsiElement element) {
    return " (" + element.getContainingFile().getName() + ")";
  }

  @Override
  protected Icon getIcon(PsiElement element) {
    DomElement domElement = getDomElement(element);

    if (domElement != null) {
      Icon presentationIcon = domElement.getPresentation().getIcon();
      if (presentationIcon != null) {
        return presentationIcon;
      }
    }

    return super.getIcon(element);
  }
}
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

package cn.taketoday.assistant.dom;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementsNavigationManager;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.stubs.index.DomElementClassIndex;
import com.intellij.util.xml.stubs.index.DomNamespaceKeyIndex;

import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 13:38
 */
public abstract class InfraDomUtils {

  public static final Condition<PsiFileSystemItem> XML_CONDITION = item -> {
    if (item.isDirectory())
      return false;

    PsiFile file = item.getContainingFile();
    return file instanceof XmlFile &&
            isInfraXml((XmlFile) file);
  };

  public static void navigate(@Nullable DomElement domElement) {
    if (domElement == null)
      return;

    DomElementsNavigationManager navigationManager = DomElementsNavigationManager.getManager(domElement.getManager().getProject());
    navigationManager.getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME).navigate(domElement, true);
  }

  public static boolean isInfraXml(XmlFile configFile) {
    return getDomFileElement(configFile) != null;
  }

  @Nullable
  public static DomFileElement<Beans> getDomFileElement(XmlFile configFile) {
    DomFileElement<Beans> fileElement = DomManager.getDomManager(configFile.getProject()).getFileElement(configFile, Beans.class);
    return fileElement != null && fileElement.isValid() ? fileElement : null;
  }

  public static boolean hasNamespace(DomFileElement domFileElement, String namespaceKey) {
    return DomNamespaceKeyIndex.getInstance().hasStubElementsWithNamespaceKey(domFileElement, namespaceKey);
  }

  /**
   * @see com.intellij.util.xml.StubbedOccurrence
   */
  public static boolean hasElement(DomFileElement domFileElement, Class<? extends DomElement> domElementClazz) {
    return DomElementClassIndex.getInstance().hasStubElementsOfType(domFileElement, domElementClazz);
  }
}

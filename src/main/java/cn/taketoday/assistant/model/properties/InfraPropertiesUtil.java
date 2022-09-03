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

package cn.taketoday.assistant.model.properties;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaBean;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.lang.Nullable;

public final class InfraPropertiesUtil {

  @Nullable
  public static BeanProperty getBeanProperty(DataContext dataContext) {
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    return getBeanProperty(editor, file);
  }

  @Nullable
  public static BeanProperty getBeanProperty(Editor editor, PsiFile file) {
    PsiReference reference;
    if (editor != null && (file instanceof XmlFile)) {
      int offset = editor.getCaretModel().getOffset();
      DomElement value = DomUtil.getContextElement(editor);
      InfraPropertyDefinition property = DomUtil.getParentOfType(value, InfraPropertyDefinition.class, false);
      if (property != null && !isJavaBeanReference(file, offset) && (reference = TargetElementUtil.findReference(editor, offset)) != null) {
        PsiElement resolve = reference.resolve();
        if ((resolve instanceof PsiMethod psiMethod) && resolve.getLanguage() == JavaLanguage.INSTANCE) {
          return BeanProperty.createBeanProperty(psiMethod);
        }
      }
    }
    return null;
  }

  @Nullable
  public static BeanProperty getBeanProperty(PsiElement element) {
    PsiReference reference;
    InfraPropertyDefinition propertyDefinition = DomUtil.findDomElement(element, InfraPropertyDefinition.class, false);
    if (propertyDefinition != null && !isJavaBeanReference(element) && (reference = element.getReference()) != null) {
      PsiElement resolve = reference.resolve();
      if ((resolve instanceof PsiMethod psiMethod) && resolve.getLanguage() == JavaLanguage.INSTANCE) {
        return BeanProperty.createBeanProperty(psiMethod);
      }
    }
    return null;
  }

  private static boolean isJavaBeanReference(PsiFile file, int offset) {
    return isJavaBeanReference(PsiTreeUtil.getParentOfType(file.findElementAt(offset), XmlAttribute.class));
  }

  private static boolean isJavaBeanReference(PsiElement psiElement) {
    GenericAttributeValue<?> value;
    XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(psiElement, XmlAttribute.class);
    if (xmlAttribute != null && (value = DomManager.getDomManager(psiElement.getProject()).getDomElement(xmlAttribute)) != null) {
      Object attributeValue = value.getValue();
      if (attributeValue instanceof BeanPointer) {
        return ((BeanPointer<?>) attributeValue).getBean() instanceof InfraJavaBean;
      }
      return false;
    }
    return false;
  }
}

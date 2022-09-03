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

package cn.taketoday.assistant.refactoring;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.xml.XmlNamespaceHelper;

import java.util.Collections;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;

public class UsePNamespaceIntention implements IntentionAction {

  public String getText() {
    return InfraBundle.message("use.p.namespace");
  }

  public String getFamilyName() {
    return getText();
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    InfraProperty property;
    return (file instanceof XmlFile) && !(file instanceof JspFile) && (property = DomUtil.getContextElement(editor,
            InfraProperty.class)) != null && (property.getParent() instanceof InfraBean) && property.getName().getStringValue() != null && ((property.getValueElement() != null && DomUtil.hasXml(
            property.getValueElement())) || (property.getRefElement() != null && DomUtil.hasXml(property.getRefElement())));
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (InfraUpdateSchemaIntention.requestSchemaUpdate((XmlFile) file)) {
      InfraProperty property = DomUtil.getContextElement(editor, InfraProperty.class);
      if (property.getXmlTag().getNamespaceByPrefix("p").isEmpty()) {
        XmlNamespaceHelper.getHelper(file).insertNamespaceDeclaration((XmlFile) file, editor, Collections.singleton(InfraConstant.P_NAMESPACE), "p", null);
      }
      InfraBean bean = (InfraBean) property.getParent();
      String name = property.getName().getStringValue();
      GenericDomValue<?> valueElement = property.getValueElement();
      String value = valueElement == null ? null : valueElement.getRawText();
      XmlTag tag = bean.getXmlTag();
      if (value != null) {
        property.undefine();
        XmlAttribute attribute = XmlElementFactory.getInstance(project).createXmlAttribute("p:" + name, value);
        tag.add(attribute);
        tag.collapseIfEmpty();
        return;
      }
      GenericDomValue<BeanPointer<?>> refElement = property.getRefElement();
      String ref = refElement == null ? null : refElement.getRawText();
      if (ref != null) {
        property.undefine();
        XmlAttribute attribute2 = XmlElementFactory.getInstance(project).createXmlAttribute("p:" + name + "-ref", ref);
        tag.add(attribute2);
        tag.collapseIfEmpty();
      }
    }
  }

  public boolean startInWriteAction() {
    return true;
  }
}

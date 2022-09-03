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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiNameHelper;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.values.converters.FieldRetrievingFactoryBeanConverterImpl;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InfraBeanNameConventionInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel springModel) {
    GenericAttributeValue<String> springBeanId = infraBean.getId();
    if (!DomUtil.hasXml(springBeanId)) {
      return;
    }
    String beanId = springBeanId.getStringValue();
    if (acceptBean(infraBean, beanId)) {
      checkName(springBeanId, beanId, holder);
    }
  }

  private static boolean acceptBean(InfraBean infraBean, String beanId) {
    return !StringUtil.isEmpty(beanId)
            && !hasSymbols(beanId, ".")
            && !hasSymbols(beanId, "-")
            && (!FieldRetrievingFactoryBeanConverterImpl.isFieldRetrievingFactoryBean(infraBean)
            || !FieldRetrievingFactoryBeanConverterImpl.isResolved(infraBean.getManager().getProject(), beanId));
  }

  private static boolean hasSymbols(String beanId, String smb) {
    if (!beanId.contains(smb)) {
      return false;
    }
    for (String s : StringUtil.split(beanId, smb, true, false)) {
      if (StringUtil.isEmptyOrSpaces(s)) {
        return false;
      }
    }
    return true;
  }

  private static void checkName(DomElement domElement, String name, DomElementAnnotationHolder holder) {
    PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(domElement.getManager().getProject());
    boolean identifier = psiNameHelper.isIdentifier(name);
    if (!identifier) {
      boolean keyword = psiNameHelper.isKeyword(name);
      if (!keyword) {
        holder.createProblem(domElement, message("model.inspection.invalid.identifier.message", name));
      }
    }
    if (Character.isUpperCase(name.charAt(0))) {
      holder.createProblem(domElement, message("model.inspection.invalid.lowercase.name.message", name));
    }
  }
}

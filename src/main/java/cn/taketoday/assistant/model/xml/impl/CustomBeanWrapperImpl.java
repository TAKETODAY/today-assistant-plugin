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

package cn.taketoday.assistant.model.xml.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.dom.InfraToolDomExtender;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBeanImpl;
import cn.taketoday.assistant.model.xml.custom.CustomBeanInfo;
import cn.taketoday.assistant.model.xml.custom.CustomBeanRegistry;
import cn.taketoday.assistant.model.xml.custom.CustomNamespaceInfraBean;
import cn.taketoday.lang.Nullable;

public abstract class CustomBeanWrapperImpl extends DomInfraBeanImpl implements CustomBeanWrapper {
  private final NotNullLazyValue<List<CustomBean>> myBeans = NotNullLazyValue.volatileLazy(() -> {
    XmlTag tag = getXmlTag();
    if (tag == null) {
      LOG.warn("No XML tag found for " + getXmlElement());
      return Collections.emptyList();
    }
    List<CustomBeanInfo> infos = CustomBeanRegistry.getInstance(getPsiManager().getProject()).getParseResult(tag);
    if (infos == null || infos.isEmpty()) {
      CustomBean bean = getExportingBean(tag);
      return bean != null ? Collections.singletonList(bean) : Collections.emptyList();
    }
    ArrayList<CustomBean> result = new ArrayList<>(infos.size());
    Module module = getModule();
    for (CustomBeanInfo info : infos) {
      result.add(new CustomNamespaceInfraBean(info, module, this));
    }
    return result;
  });
  private static final Logger LOG = Logger.getInstance(CustomBeanWrapperImpl.class);

  @Nullable
  private CustomBean getExportingBean(XmlTag tag) {
    XmlElementDescriptor descriptor = tag.getDescriptor();
    if (descriptor == null) {
      return null;
    }
    PsiElement declaration = descriptor.getDeclaration();
    if (!(declaration instanceof XmlTag declTag)) {
      return null;
    }
    XmlTag annotationTag = InfraToolDomExtender.getToolAnnotationTag(declTag, true);
    if (annotationTag == null && "element".equals(declTag.getLocalName())) {
      PsiElement[] findSubTags = declTag.findSubTags("simpleType", "http://www.w3.org/2001/XMLSchema");
      if (findSubTags.length > 0) {
        annotationTag = InfraToolDomExtender.getToolAnnotationTag(findSubTags[0], true);
      }
      if (annotationTag == null) {
        PsiElement[] findSubTags2 = declTag.findSubTags("complexType", "http://www.w3.org/2001/XMLSchema");
        if (findSubTags2.length > 0) {
          annotationTag = InfraToolDomExtender.getToolAnnotationTag(findSubTags2[0], true);
        }
      }
    }
    if (annotationTag == null) {
      return null;
    }
    XmlTag[] exports = annotationTag.findSubTags("exports", InfraConstant.TOOL_NAMESPACE);
    if (exports.length == 0) {
      return null;
    }
    CustomBeanInfo info = new CustomBeanInfo();
    info.beanClassName = exports[0].getAttributeValue("type", InfraConstant.TOOL_NAMESPACE);
    String idPtr = exports[0].getAttributeValue("identifier", InfraConstant.TOOL_NAMESPACE);
    if (idPtr == null) {
      info.idAttribute = "id";
    }
    else if (idPtr.startsWith("@") && !idPtr.contains("/") && !idPtr.contains("[")) {
      info.idAttribute = idPtr.substring(1);
    }
    if (info.idAttribute != null) {
      info.beanName = tag.getAttributeValue(info.idAttribute);
    }
    return new CustomNamespaceInfraBean(info, getModule(), this);
  }

  private List<CustomBean> getCachedValue() {
    return this.myBeans.getValue();
  }

  @Override
  @Nullable
  public String getBeanName() {
    if (!isParsed()) {
      return super.getBeanName();
    }
    return null;
  }

  @Override

  public List<CustomBean> getCustomBeans() {
    return getCachedValue();
  }

  @Override
  public boolean isDummy() {
    return getCachedValue().isEmpty();
  }

  @Override
  public boolean isParsed() {
    List<CustomBean> customBeans = getCachedValue();
    return !customBeans.isEmpty() || customBeans != Collections.EMPTY_LIST;
  }
}

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

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ParameterizedTypeImpl;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.ExtendClassImpl;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtension;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.impl.schema.XmlAttributeDescriptorImpl;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.beans.MetadataPropertyValueConverter;
import cn.taketoday.assistant.model.xml.beans.MetadataRefValue;
import cn.taketoday.assistant.model.xml.beans.MetadataValue;
import cn.taketoday.lang.Nullable;

public class InfraToolDomExtender extends DomExtender<CustomBeanWrapper> {

  @Nullable
  public static XmlTag getToolAnnotationTag(@Nullable PsiElement declaration, boolean allowRecursion) {
    XmlAttributeValue value;
    XmlTag annotationTag;
    if (declaration instanceof XmlTag xmlTag) {
      XmlTag[] tags = xmlTag.findSubTags("annotation", "http://www.w3.org/2001/XMLSchema");
      if (tags.length > 0) {
        XmlTag[] tags1 = tags[0].findSubTags("appinfo", "http://www.w3.org/2001/XMLSchema");
        if (tags1.length > 0) {
          XmlTag[] tags2 = tags1[0].findSubTags("annotation", InfraConstant.TOOL_NAMESPACE);
          if (tags2.length > 0) {
            return tags2[0];
          }
        }
      }
      XmlAttribute attribute = xmlTag.getAttribute("type");
      if (allowRecursion && attribute != null && (value = attribute.getValueElement()) != null) {
        for (PsiReference reference : value.getReferences()) {
          PsiElement element = reference.resolve();
          if ((element instanceof XmlTag) && (annotationTag = getToolAnnotationTag(element, false)) != null) {
            return annotationTag;
          }
        }
        return null;
      }
      return null;
    }
    return null;
  }

  public void registerExtensions(CustomBeanWrapper element, DomExtensionsRegistrar registrar) {
    XmlTag annotationTag;
    String assignableFrom;
    if (!element.isValid()) {
      return;
    }
    XmlTag tag = element.getXmlTag();
    for (XmlAttribute attribute : tag.getAttributes()) {
      XmlAttributeDescriptor descriptor = attribute.getDescriptor();
      if ((descriptor instanceof XmlAttributeDescriptorImpl) && (annotationTag = getToolAnnotationTag(descriptor.getDeclaration(), true)) != null) {
        boolean ref = "ref".equals(annotationTag.getAttributeValue("kind"));
        String expectedType = getExpectedTypeClass(annotationTag);
        XmlName xmlName = new XmlName(attribute.getName());
        if (ref) {
          registrar.registerAttributeChildExtension(xmlName, MetadataRefValue.class).setConverter(new InfraBeanResolveConverter() {

            @Override

            public List<PsiClassType> getRequiredClasses(ConvertContext context) {
              if (DumbService.isDumb(context.getProject())) {
                return Collections.emptyList();
              }
              PsiClass psiClass = DomJavaUtil.findClass(expectedType, context.getInvocationElement());
              if (psiClass == null) {
                return Collections.emptyList();
              }
              return Collections.singletonList(PsiTypesUtil.getClassType(psiClass));
            }
          });
        }
        else if ("java.lang.Class".equals(expectedType)) {
          DomExtension extension = registrar.registerAttributeChildExtension(xmlName, new ParameterizedTypeImpl(GenericAttributeValue.class, PsiClass.class));
          XmlTag[] tags1 = annotationTag.findSubTags("assignable-to", InfraConstant.TOOL_NAMESPACE);
          if (tags1.length > 0 && (assignableFrom = tags1[0].getAttributeValue("type")) != null) {
            String finalAssignableFrom = assignableFrom;
            extension.addCustomAnnotation(new ExtendClassImpl() {
              public String[] value() {
                return new String[] { finalAssignableFrom };
              }
            });
          }
        }
        else {
          registrar.registerAttributeChildExtension(xmlName, MetadataValue.class).setConverter(new MetadataPropertyValueConverter(expectedType));
        }
      }
    }
  }

  private static String getExpectedTypeClass(XmlTag annotationTag) {
    XmlTag[] expectedTypeTags = annotationTag.findSubTags("expected-type", InfraConstant.TOOL_NAMESPACE);
    if (expectedTypeTags.length == 0) {
      expectedTypeTags = annotationTag.findSubTags("assignable-to", InfraConstant.TOOL_NAMESPACE);
    }
    String type = expectedTypeTags.length > 0 ? expectedTypeTags[0].getAttributeValue("type") : null;
    return type == null ? "java.lang.Object" : type;
  }

  public boolean supportsStubs() {
    return false;
  }
}

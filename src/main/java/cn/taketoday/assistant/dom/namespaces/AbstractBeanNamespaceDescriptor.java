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

package cn.taketoday.assistant.dom.namespaces;

import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.meta.PsiPresentableMetaData;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.XmlAttributeDescriptorEx;
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public abstract class AbstractBeanNamespaceDescriptor<T extends PsiElement> extends XmlNSDescriptorImpl {
  static final String REF_SUFFIX = "-ref";

  protected abstract String getNamespace();

  protected abstract PsiType getAttributeType(T t);

  protected abstract BeanAttributeDescriptor createAttributeDescriptor(String str, T t, String str2);

  protected abstract Map<String, T> getAttributes(InfraBean infraBean);

  public XmlAttributeDescriptor getAttribute(String localName, String namespace, XmlTag context) {
    if (getNamespace().equals(namespace)) {
      String sanitizedLocalName = attributeNameToPropertyName(localName);
      for (XmlAttributeDescriptor descriptor : getAttributeDescriptors(context)) {
        if (descriptor.getName().equals(sanitizedLocalName)) {
          return descriptor;
        }
      }
    }
    return super.getAttribute(localName, namespace, context);
  }

  public static String attributeNameToPropertyName(String attributeName) {
    String realAttributeName = StringUtil.trimEnd(attributeName, REF_SUFFIX);
    if (!realAttributeName.contains("-")) {
      return realAttributeName;
    }
    char[] chars = realAttributeName.toCharArray();
    char[] result = new char[chars.length - 1];
    int currPos = 0;
    boolean upperCaseNext = false;
    for (char c : chars) {
      if (c == '-') {
        upperCaseNext = true;
      }
      else if (upperCaseNext) {
        int i = currPos;
        currPos++;
        result[i] = Character.toUpperCase(c);
        upperCaseNext = false;
      }
      else {
        int i2 = currPos;
        currPos++;
        result[i2] = c;
      }
    }
    return new String(result, 0, currPos);
  }

  @Nullable
  protected static InfraBean getSpringBean(XmlTag tag) {
    DomElement element = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
    if (element instanceof InfraBean) {
      return (InfraBean) element;
    }
    return null;
  }

  @Nullable
  public static PsiClass getClass(XmlTag tag) {
    DomElement element = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
    if (element instanceof InfraBean bean) {
      return PsiTypesUtil.getPsiClass(bean.getBeanType(true));
    }
    return null;
  }

  private XmlAttributeDescriptor[] getAttributeDescriptors(XmlTag tag) {
    InfraBean infraBean = getSpringBean(tag);
    if (infraBean == null) {
      return XmlAttributeDescriptor.EMPTY;
    }
    Map<String, T> properties = getAttributesWithoutRecursion(infraBean);
    if (properties.isEmpty()) {
      return XmlAttributeDescriptor.EMPTY;
    }
    List<XmlAttributeDescriptor> result = new ArrayList<>(properties.size() * 2);
    for (String attrName : properties.keySet()) {
      T t = properties.get(attrName);
      result.add(createAttributeDescriptor(attrName, t, ""));
      PsiType attributeType = getAttributeType(t);
      if ((attributeType instanceof PsiClassType) || (attributeType instanceof PsiArrayType)) {
        result.add(createAttributeDescriptor(attrName, t, REF_SUFFIX));
      }
    }
    return result.toArray(XmlAttributeDescriptor.EMPTY);
  }

  private Map<String, T> getAttributesWithoutRecursion(InfraBean infraBean) {

    PsiElement identifyingPsiElement = infraBean.getIdentifyingPsiElement();
    if (identifyingPsiElement != null && identifyingPsiElement.isValid()) {
      Map<String, T> map = RecursionManager.doPreventingRecursion(identifyingPsiElement, false, () -> this.getAttributes(infraBean));
      return map != null ? map : Collections.emptyMap();
    }
    else {
      return Collections.emptyMap();
    }
  }

  public XmlElementDescriptor[] getRootElementsDescriptors(@Nullable XmlDocument doc) {
    return XmlElementDescriptor.EMPTY_ARRAY;
  }

  public XmlAttributeDescriptor[] getRootAttributeDescriptors(XmlTag context) {
    return getAttributeDescriptors(context);
  }

  protected static class PAttributeDescriptor extends BeanAttributeDescriptor<PsiMethod> {

    public PAttributeDescriptor(String attributeName, String suffix, PsiMethod psiMethod, String namespace) {
      super(attributeName, suffix, psiMethod, namespace);
    }

    public String handleTargetRename(String newTargetName) {
      String propertyName = PropertyUtilBase.getPropertyName(newTargetName);
      if (propertyName == null) {
        return null;
      }
      return propertyName + this.mySuffix;
    }
  }

  protected static class CAttributeDescriptor extends BeanAttributeDescriptor<PsiParameter> {

    public CAttributeDescriptor(String attributeName, String suffix, PsiParameter psiMethod, String namespace) {
      super(attributeName, suffix, psiMethod, namespace);
    }

    public String handleTargetRename(String newTargetName) {
      return newTargetName + this.mySuffix;
    }
  }

  public abstract static class BeanAttributeDescriptor<T extends PsiElement> implements XmlAttributeDescriptorEx, PsiPresentableMetaData {
    protected final String myAttributeName;
    protected final String mySuffix;
    protected final T myPsiElement;
    protected final String myNamespace;

    public BeanAttributeDescriptor(String attributeName, String suffix, T t, String namespace) {
      this.myAttributeName = attributeName;
      this.mySuffix = suffix;
      this.myPsiElement = t;
      this.myNamespace = namespace;
    }

    public String getName() {
      return this.myAttributeName + this.mySuffix;
    }

    public void init(PsiElement element) {
      throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
    }

    public PsiElement getDeclaration() {
      return this.myPsiElement;
    }

    public String getName(PsiElement context) {
      String name = getName();
      String prefix = ((XmlTag) context).getPrefixByNamespace(this.myNamespace);
      return (StringUtil.isNotEmpty(prefix) ? prefix + ":" : "") + name;
    }

    public boolean isRequired() {
      return false;
    }

    public boolean isFixed() {
      return false;
    }

    public boolean hasIdType() {
      return false;
    }

    public boolean hasIdRefType() {
      return false;
    }

    @Nullable
    public String getDefaultValue() {
      return null;
    }

    public boolean isEnumerated() {
      return false;
    }

    public String[] getEnumeratedValues() {
      return ArrayUtilRt.EMPTY_STRING_ARRAY;
    }

    @Nullable
    public String validateValue(XmlElement context, String value) {
      return null;
    }

    public String getTypeName() {
      return null;
    }

    @Nullable
    public Icon getIcon() {
      return Icons.SpringProperty;
    }
  }
}

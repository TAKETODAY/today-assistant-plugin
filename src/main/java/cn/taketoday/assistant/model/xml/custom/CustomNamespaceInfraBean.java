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

package cn.taketoday.assistant.model.xml.custom;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.xml.AbstractDomInfraBean;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public class CustomNamespaceInfraBean extends AbstractDomInfraBean implements CustomBean {
  private static final Logger LOG = Logger.getInstance(CustomNamespaceInfraBean.class);
  private final cn.taketoday.assistant.model.xml.custom.CustomBeanInfo info;
  private final Module myModule;
  private final XmlTag sourceTag;
  private final PsiElement myFakePsi;
  private final CustomBeanWrapper customBeanWrapper;
  private final XmlAttribute myIdAttribute;

  public CustomNamespaceInfraBean(CustomBeanInfo info, Module module, CustomBeanWrapper wrapper) {
    this.info = info;
    this.myModule = module;
    this.customBeanWrapper = wrapper;
    XmlTag tag = wrapper.getXmlTag();
    if (tag == null) {
      LOG.error(String.valueOf(wrapper.getParent()));
    }
    this.sourceTag = CustomBeanRegistry.getActualSourceTag(info, tag);
    this.myIdAttribute = this.sourceTag.getAttribute(info.idAttribute);
    this.myFakePsi = new CustomBeanFakePsiElement(this);
  }

  @Override
  @Nullable
  public XmlAttribute getIdAttribute() {
    return this.myIdAttribute;
  }

  @Nullable
  public GenericValue<BeanPointer<?>> getFactoryBean() {
    InfraModel model;
    BeanPointer<?> beanPointer;
    String beanName = this.info.factoryBeanName;
    if (beanName != null && (model = InfraManager.from(getPsiManager().getProject()).getInfraModelByFile(getContainingFile())) != null && (beanPointer = InfraBeanUtils.of()
            .findBean(model, beanName)) != null) {
      return ReadOnlyGenericValue.getInstance(beanPointer);
    }
    return super.getFactoryBean();
  }

  @Override
  public CustomBeanWrapper getWrapper() {
    return customBeanWrapper;
  }

  @Override
  @Nullable
  public GenericValue<PsiMethod> getFactoryMethod() {
    PsiClass beanClass;
    PsiMethod method;
    String name = this.info.factoryMethodName;
    if (name != null && (beanClass = PsiTypesUtil.getPsiClass(getBeanType(false))) != null
            && (method = findMatchingFactoryMethod(name, beanClass)) != null) {
      return ReadOnlyGenericValue.getInstance(method);
    }
    return super.getFactoryMethod();
  }

  @Nullable
  private PsiMethod findMatchingFactoryMethod(String name, PsiClass beanClass) {
    PsiMethod[] findMethodsByName;
    PsiMethod result = null;
    PsiType returnType = null;
    int count = this.info.constructorArgumentCount;
    for (PsiMethod method : beanClass.findMethodsByName(name, true)) {
      if (method.getParameterList().getParametersCount() == count && method.hasModifierProperty("static")) {
        if (returnType == null) {
          result = method;
          returnType = method.getReturnType();
        }
        else if (!returnType.equals(method.getReturnType())) {
          return null;
        }
      }
    }
    return result;
  }

  @Override
  @Nullable
  public String getBeanName() {
    return this.info.beanName;
  }

  @Override
  public String[] getAliases() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  public boolean isValid() {
    return this.sourceTag.isValid();
  }

  @Override
  public XmlTag getXmlTag() {
    return sourceTag;
  }

  public PsiManager getPsiManager() {
    return this.sourceTag.getManager();
  }

  @Override
  @Nullable
  public Module getModule() {
    return this.myModule;
  }

  @Nullable
  public PsiElement getIdentifyingPsiElement() {
    return this.myFakePsi;
  }

  @Override
  public final PsiFile getContainingFile() {
    return this.sourceTag.getContainingFile();
  }

  @Override
  @Nullable
  public String getClassName() {
    return this.info.beanClassName;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CustomNamespaceInfraBean that = (CustomNamespaceInfraBean) o;
    return this.info.equals(that.info) && this.customBeanWrapper.equals(that.customBeanWrapper);
  }

  public int hashCode() {
    int result = this.info.hashCode();
    return (31 * result) + this.customBeanWrapper.hashCode();
  }

  @Override
  public Beans getBeansParent() {
    return DomUtil.getParentOfType(customBeanWrapper, Beans.class, false);
  }
}

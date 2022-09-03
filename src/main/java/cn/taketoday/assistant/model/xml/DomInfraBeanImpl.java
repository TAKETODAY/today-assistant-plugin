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
package cn.taketoday.assistant.model.xml;

import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.DomReflectionUtil;
import com.intellij.util.xml.DomUtil;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.assistant.model.DefaultInfraBeanQualifier;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class DomInfraBeanImpl extends AbstractDomInfraBean implements DomInfraBean {
  /**
   * Use {@link BeanName} to provide custom bean name.
   * Overriding recommended when {@link #setName(String)} is also changed.
   * NB: do not use any resolving when overriding.
   *
   * @return Bean name.
   */
  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public String getBeanName() {
    BeanName beanName = DomReflectionUtil.findAnnotationDFS(getClass(), BeanName.class);
    if (beanName == null) {
      return DomUtil.hasXml(getId()) ? getId().getRawText() : null;
    }

    String value = beanName.value();
    if (!value.isEmpty()) {
      return value;
    }

    // we already check in indexing, but if model version is not incremented such errors might be introduced
    Class<? extends BeanNameProvider> providerClass = beanName.provider();
    if (providerClass == BeanNameProvider.class) {
      throw new IllegalStateException("@BeanName: no value() given, provider() must be set" + getDomElementType());
    }

    return InfraUtils.getBeanNameProvider(providerClass).getBeanName(this);
  }

  @Override
  public void setName(String newName) {
    if (DomUtil.hasXml(getId())) {
      getId().setStringValue(newName);
    }
  }

  /**
   * @return Bean's class name.
   * @see BeanType
   */
  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public final String getClassName() {
    BeanType beanType = DomReflectionUtil.findAnnotationDFS(getClass(), BeanType.class);
    if (beanType == null) {
      return null;
    }

    String value = beanType.value();
    if (!value.isEmpty()) {
      return value;
    }

    // we already check in indexing, but if model version is not incremented such errors might be introduced
    Class<? extends BeanTypeProvider> providerClass = beanType.provider();
    if (providerClass == BeanTypeProvider.class) {
      throw new IllegalStateException("@BeanType: no value() given, provider() must be set" + getDomElementType());
    }

    return InfraUtils.getBeanTypeProvider(providerClass).getBeanType(this);
  }

  @Override

  public PsiFile getContainingFile() {
    return DomUtil.getFile(this);
  }

  @Override
  @Nullable
  public PsiElement getIdentifyingPsiElement() {
    if (!isValid())
      return null;
    return PomService.convertToPsi(getManager().getProject(),
            InfraBeanService.of().createBeanPsiTarget(this));
  }

  @Override
  public String[] getAliases() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  public PsiManager getPsiManager() {
    return PsiManager.getInstance(getManager().getProject());
  }

  @Override
  public Beans getBeansParent() {
    return DomUtil.getParentOfType(this, Beans.class, false);
  }

  @Override
  public Collection<InfraQualifier> getInfraQualifiers() {
    return Collections.singleton(DefaultInfraBeanQualifier.create(this));
  }
}

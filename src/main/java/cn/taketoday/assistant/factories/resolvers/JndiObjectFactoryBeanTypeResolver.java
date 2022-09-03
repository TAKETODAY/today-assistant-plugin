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

package cn.taketoday.assistant.factories.resolvers;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.jee.JndiLookup;
import cn.taketoday.lang.Nullable;

public class JndiObjectFactoryBeanTypeResolver extends AbstractJeeSchemaTypeResolver {

  private static final String[] myProperties = { "proxyInterface", "expectedType" };

  @Override
  public FactoryPropertiesDependentTypeResolver getPropertyDependentResolver() {
    return super.getPropertyDependentResolver();
  }

  @Override
  public boolean accept(String factoryClassName) {
    return super.accept(factoryClassName);
  }

  @Override

  public Set<PsiType> getObjectType(@Nullable CommonInfraBean commonInfraBean) {
    return super.getObjectType(commonInfraBean);
  }

  @Override

  protected Set<PsiType> getJeeObjectType(CommonInfraBean context) {
    if (context instanceof JndiLookup jndiLookup) {
      PsiClass proxyInterface = jndiLookup.getProxyInterface().getValue();
      if (proxyInterface != null) {
        return getSingletonCollection(proxyInterface);
      }
      PsiClass expectedType = jndiLookup.getExpectedType().getValue();
      if (expectedType != null) {
        return getSingletonCollection(expectedType);
      }
    }
    return Collections.emptySet();
  }

  private Set<PsiType> getSingletonCollection(PsiClass proxyInterface) {
    return Collections.singleton(JavaPsiFacade.getElementFactory(proxyInterface.getProject()).createType(proxyInterface));
  }

  @Override
  protected List<String> getProperties() {
    return Arrays.asList(myProperties);
  }

  @Override
  protected List<String> getFactoryClasses() {
    return Collections.singletonList(InfraConstant.JNDI_OBJECT_FACTORY_BEAN);
  }
}

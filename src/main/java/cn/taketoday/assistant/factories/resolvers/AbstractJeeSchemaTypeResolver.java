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

import com.intellij.psi.PsiType;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.factories.ObjectTypeResolver;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.jee.InfraJeeElement;
import cn.taketoday.lang.Nullable;

abstract class AbstractJeeSchemaTypeResolver implements ObjectTypeResolver {
  private FactoryPropertiesDependentTypeResolver myPropertyDependentResolver;

  protected abstract Set<PsiType> getJeeObjectType(CommonInfraBean commonInfraBean);

  protected abstract List<String> getProperties();

  protected abstract List<String> getFactoryClasses();

  @Override

  public Set<PsiType> getObjectType(@Nullable CommonInfraBean context) {
    if (context instanceof InfraJeeElement) {
      return getJeeObjectType(context);
    }
    return getPropertyDependentResolver().getObjectType(context);
  }

  @Override
  public boolean accept(String factoryClassName) {
    return getFactoryClasses().contains(factoryClassName);
  }

  public FactoryPropertiesDependentTypeResolver getPropertyDependentResolver() {
    if (this.myPropertyDependentResolver == null) {
      this.myPropertyDependentResolver = new FactoryPropertiesDependentTypeResolver(getProperties());
    }
    return this.myPropertyDependentResolver;
  }
}

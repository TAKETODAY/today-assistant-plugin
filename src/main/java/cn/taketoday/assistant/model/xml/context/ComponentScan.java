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

package cn.taketoday.assistant.model.xml.context;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.xml.DomInfraBean;

@Presentation(typeName = PresentationConstant.COMPONENT_SCAN)
public interface ComponentScan extends BeansPackagesScanBean, DomInfraBean, InfraContextElement {

  GenericAttributeValue<String> getResourcePattern();

  @Stubbed
  GenericAttributeValue<Boolean> getUseDefaultFilters();

  GenericAttributeValue<Boolean> getAnnotationConfig();

  @ExtendClass({ InfraConstant.BEAN_NAME_GENERATOR })
  GenericAttributeValue<PsiClass> getNameGenerator();

  @ExtendClass({ InfraConstant.SCOPE_METADATA_RESOLVER })
  GenericAttributeValue<PsiClass> getScopeResolver();

  GenericAttributeValue<ScopedProxy> getScopedProxy();
}

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
package cn.taketoday.assistant.model.xml.beans;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiMethod;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.CustomChildren;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.HyphenNameStrategy;
import com.intellij.util.xml.NameStrategyForAttributes;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Stubbed;
import com.intellij.util.xml.SubTagList;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.converters.InfraDefaultInitDestroyRefConverter;
import cn.taketoday.assistant.model.converters.InfraProfilesDomConverter;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;

@NameStrategyForAttributes(HyphenNameStrategy.class)
@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
@Presentation(icon = "JavaUltimateIcons.Jsf.ManagedBean")
@Stubbed
public interface Beans extends DomElement, Description {

  @SubTagList(value = "beans")
  @Stubbed
  List<Beans> getBeansProfiles();

  @Convert(InfraProfilesDomConverter.class)
  @Stubbed
  InfraDomProfile getProfile();

  GenericAttributeValue<Boolean> getDefaultLazyInit();

  GenericAttributeValue<Boolean> getDefaultMerge();

  GenericAttributeValue<DefaultDependencyCheck> getDefaultDependencyCheck();

  @Stubbed
  GenericAttributeValue<Autowire> getDefaultAutowire();

  @Stubbed
  GenericAttributeValue<String> getDefaultAutowireCandidates();

  @Convert(value = InfraDefaultInitDestroyRefConverter.class, soft = true)

  @Stubbed
  GenericAttributeValue<Set<PsiMethod>> getDefaultInitMethod();

  @Convert(value = InfraDefaultInitDestroyRefConverter.class, soft = true)
  @Stubbed
  GenericAttributeValue<Set<PsiMethod>> getDefaultDestroyMethod();

  @Stubbed
  List<InfraImport> getImports();

  InfraImport addImport();

  @SubTagList(value = "alias")
  @Stubbed
  List<Alias> getAliases();

  @Stubbed
  List<InfraBean> getBeans();

  /**
   * Adds new child to the list of bean children.
   *
   * @return created child
   */
  InfraBean addBean();

  @CustomChildren
  List<CustomBeanWrapper> getCustomBeans();
}

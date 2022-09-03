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

package cn.taketoday.assistant.model.xml.util;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericAttributeValue;

import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.beans.ScopedElement;
import cn.taketoday.assistant.model.xml.beans.TypeHolder;

@BeanType(UtilList.CLASS_NAME)
@Presentation(typeName = PresentationConstant.SPRING_LIST)
public interface UtilList extends DomInfraBean, InfraUtilElement, ListOrSet, ScopedElement, TypeHolder {
  public static final String CLASS_NAME = "cn.taketoday.beans.factory.config.ListFactoryBean";

  @ExtendClass({ "java.util.List" })
  GenericAttributeValue<PsiClass> getListClass();
}

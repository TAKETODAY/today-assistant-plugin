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

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Namespace;

import java.util.List;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.converters.InfraDomQualifierTypeConverter;
import cn.taketoday.assistant.model.xml.InfraModelElement;

/**
 * @author Sergey.Vasiliev
 */
@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
public interface InfraDomQualifier extends InfraModelElement, InfraQualifier {

  @NameValue
  GenericAttributeValue<String> getValue();

  @Convert(InfraDomQualifierTypeConverter.class)
  GenericAttributeValue<PsiClass> getType();

  List<InfraDomAttribute> getAttributes();
}
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

// Generated on Thu Nov 09 17:15:14 MSK 2006
// DTD/Schema  :    http://www.springframework.org/schema/beans

package cn.taketoday.assistant.model.xml.beans;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiFile;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Stubbed;

import java.util.Set;

import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.converters.InfraImportResourceConverter;

/**
 * http://www.springframework.org/schema/beans:importElemType interface.
 */
@Presentation(icon = "cn.taketoday.assistant.Icons.SpringConfig", provider = InfraPresentationProvider.class)
public interface InfraImport extends DomElement {

  /**
   * Returns the value of the resource child.
   * <pre>
   * <h3>Attribute null:resource documentation</h3>
   * 	The relative resource location of the XML (bean definition) file to import,
   * 	for example "myImport.xml" or "includes/myImport.xml" or "../myImport.xml".
   * <p/>
   * </pre>
   *
   * @return the value of the resource child.
   */
  @Required
  @Convert(InfraImportResourceConverter.class)
  @Stubbed
  GenericAttributeValue<Set<PsiFile>> getResource();
}

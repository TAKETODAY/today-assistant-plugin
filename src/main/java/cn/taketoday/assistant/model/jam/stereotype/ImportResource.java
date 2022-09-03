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

package cn.taketoday.assistant.model.jam.stereotype;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;

import java.util.List;

public interface ImportResource extends JamElement {
  public static final String LOCATIONS_ATTR_NAME = "locations";
  public static final String VALUE_ATTR_NAME = "value";
  public static final SemKey<ImportResource> IMPORT_RESOURCE_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("ImportResource");

  List<XmlFile> getImportedResources(Module... moduleArr);
}

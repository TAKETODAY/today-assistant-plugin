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

package cn.taketoday.assistant.app.application.properties;

import com.intellij.lang.properties.DuplicatePropertyKeyAnnotationSuppressor;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;

import java.util.List;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraDuplicatePropertyKeyAnnotationSuppressor implements DuplicatePropertyKeyAnnotationSuppressor {

  public boolean suppressAnnotationFor(Property property) {
    String key = property.getKey();
    if (key == null || !InfraUtils.hasFacets(property.getProject())
            || !InfraConfigurationFileService.of().isApplicationConfigurationFile(property.getContainingFile())) {
      return false;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(property);
    if (!InfraApplicationPropertiesUtil.isSupportMultiDocuments(module)) {
      return false;
    }
    List<IProperty> document = InfraApplicationPropertiesUtil.getDocument(property);
    for (IProperty other : document) {
      if (property != other && key.equals(other.getKey())) {
        return false;
      }
    }
    return true;
  }
}

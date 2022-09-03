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

package cn.taketoday.assistant.model.extensions.myBatis;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collection;

import cn.taketoday.assistant.beans.stereotype.ComponentScanImporter;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;

public class InfraMyBatisComponentScanExtender extends ComponentScanImporter {

  @Override
  public Collection<InfraStereotypeElement> getComponents(GlobalSearchScope scope, Module module) {
    JamService service = JamService.getJamService(module.getProject());
    return service.getJamClassElements(InfraMyBatisMapper.META, InfraMyBatisMapper.MAPPER_ANNOTATION, scope);
  }
}

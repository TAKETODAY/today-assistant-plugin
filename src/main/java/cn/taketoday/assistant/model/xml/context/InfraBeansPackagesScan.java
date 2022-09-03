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

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiPackage;

import java.util.Set;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfrastructureBean;
import cn.taketoday.assistant.model.jam.utils.filters.InfraContextFilter;

public interface InfraBeansPackagesScan extends CommonModelElement, InfrastructureBean {

  Set<CommonInfraBean> getScannedElements(Module module);

  Set<PsiPackage> getPsiPackages();

  boolean useDefaultFilters();

  Set<InfraContextFilter.Exclude> getExcludeContextFilters();

  Set<InfraContextFilter.Include> getIncludeContextFilters();
}

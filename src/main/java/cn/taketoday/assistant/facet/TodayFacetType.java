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

package cn.taketoday.assistant.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 18:11
 */
public class TodayFacetType extends FacetType<TodayFacet, TodayFacetConfiguration> {

  public TodayFacetType() {
    super(TodayFacet.FACET_TYPE_ID, "Today", InfraBundle.message("infra"));
  }

  @Override
  public TodayFacetConfiguration createDefaultConfiguration() {
    return new TodayFacetConfiguration();
  }

  @Override
  public TodayFacet createFacet(Module module, String name,
          TodayFacetConfiguration configuration, Facet underlyingFacet) {
    return new TodayFacet(this, module, name, configuration, underlyingFacet);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public Icon getIcon() {
    return Icons.Today;
  }

  @Override
  public String getHelpTopic() {
    return "IntelliJ.IDEA.Procedures.Java.EE.Development.Managing.Facets.Facet.Specific.Settings.Today";
  }

}

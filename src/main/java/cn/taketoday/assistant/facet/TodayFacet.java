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
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 18:05
 */
public class TodayFacet extends Facet<TodayFacetConfiguration> {

  public final static FacetTypeId<TodayFacet> FACET_TYPE_ID = new FacetTypeId<>("today");

  public TodayFacet(FacetType<TodayFacet, TodayFacetConfiguration> facetType,
          Module module, String name, TodayFacetConfiguration configuration, Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  @Nullable
  public static TodayFacet from(Module module) {
    if (module.isDisposed()) {
      return null;
    }
    return FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID);
  }

  public static FacetType<TodayFacet, TodayFacetConfiguration> getFacetType() {
    return FacetTypeRegistry.getInstance().findFacetType(FACET_TYPE_ID);
  }

}


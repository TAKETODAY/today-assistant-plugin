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

package cn.taketoday.assistant.service;

import java.util.List;

import cn.taketoday.assistant.facet.InfraAutodetectedFileSet;
import cn.taketoday.assistant.facet.InfraFacet;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/24 23:16
 */
public interface InfraModelProvider {

  /**
   * NOTE: Results *must* be cached as method is called many times from various places.
   *
   * @param facet Facet to get autodetected filesets for.
   * @return Filesets.
   * @see InfraAutodetectedFileSet
   */
  List<? extends InfraAutodetectedFileSet> getFilesets(InfraFacet facet);

  /**
   * Provides human-readable description of this provider.
   *
   * @return Class name.
   */
  default String getName() {
    return getClass().getSimpleName();
  }

}

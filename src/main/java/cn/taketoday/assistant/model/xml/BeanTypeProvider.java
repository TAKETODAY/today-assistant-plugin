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
package cn.taketoday.assistant.model.xml;

import cn.taketoday.lang.Nullable;

/**
 * Provides dynamic (element/runtime dependent) bean type.
 *
 * @param <T> Must match {@link DomInfraBean} where @BeanType is used.
 * @see BeanType
 */
public interface BeanTypeProvider<T extends DomInfraBean> {

  /**
   * Returns static bean type candidates when element is not available (e.g. for calculating quickfixes).
   *
   * @return Super class(es) of values returned by {@link #getBeanType(DomInfraBean)} or empty array if not determinable.
   */
  String[] getBeanTypeCandidates();

  /**
   * Determines bean type for the given instance.
   *
   * @param t DomSpringBean instance.
   * @return Bean class or {@code null} if not determinable.
   */
  @Nullable
  String getBeanType(T t);
}

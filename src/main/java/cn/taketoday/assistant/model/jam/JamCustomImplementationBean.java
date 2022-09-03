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
package cn.taketoday.assistant.model.jam;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * Allows to override specific meta-annotation's (and its meta-annotations) JAM implementation provided by Spring plugin.
 */
public final class JamCustomImplementationBean {
  static final ExtensionPointName<JamCustomImplementationBean> EP_NAME =
          new ExtensionPointName<>("cn.taketoday.assistant.jam.customMetaImplementation");

  /**
   * Base annotation, e.g. {@code ContextConfiguration}
   */
  @Attribute("baseAnnotationFqn")
  public String baseAnnotationFqn;

  /**
   * Meta-annotation base class requiring custom JAM implementation, e.g. {@code SpringApplicationConfiguration}
   */
  @Attribute("customMetaAnnotationFqn")
  public String customMetaAnnotationFqn;
}

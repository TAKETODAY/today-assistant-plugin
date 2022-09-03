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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.microservices.jvm.url.AnnotationQueryParameterSupport;

import java.util.List;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;

public final class WebMvcUrlResolverKt {

  public static final AnnotationQueryParameterSupport queryParameterSupport =
          new AnnotationQueryParameterSupport(
                  InfraMvcUrlPathSpecification.INSTANCE,
                  InfraMvcConstant.REQUEST_PARAM,
                  List.of("name", RequestMapping.VALUE_ATTRIBUTE)
          );

  public static AnnotationQueryParameterSupport getQueryParameterSupport() {
    return queryParameterSupport;
  }
}

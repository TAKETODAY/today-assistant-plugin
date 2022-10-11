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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl;

import com.intellij.openapi.util.Pair;

import java.util.List;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;

public record LiveRequestMappingPredicate(
        String path, List<String> requestMethods, List<Pair<String, String>> headers,
        List<String> produces, List<String> consumes, List<Pair<String, String>> params) {

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof LiveRequestMappingPredicate that))
      return false;
    return Objects.equals(path, that.path)
            && Objects.equals(params, that.params)
            && Objects.equals(headers, that.headers)
            && Objects.equals(produces, that.produces)
            && Objects.equals(consumes, that.consumes)
            && Objects.equals(requestMethods, that.requestMethods);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("path", path)
            .append("produces", produces)
            .append("consumes", consumes)
            .append("requestMethods", requestMethods)
            .append("params", params)
            .append("headers", headers)
            .toString();
  }

}

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

package cn.taketoday.assistant.web.mvc.providers;

import java.util.List;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

final class WebRequestHeader {

  private final String name;

  private final List<String> values;

  public String component1() {
    return this.name;
  }

  public List<String> component2() {
    return this.values;
  }

  public WebRequestHeader copy(String name, List<String> list) {
    return new WebRequestHeader(name, list);
  }

  public static WebRequestHeader copy$default(WebRequestHeader webRequestHeader, String str, List list, int i, Object obj) {
    if ((i & 1) != 0) {
      str = webRequestHeader.name;
    }
    if ((i & 2) != 0) {
      list = webRequestHeader.values;
    }
    return webRequestHeader.copy(str, list);
  }

  public String toString() {
    return "InfraRequestHeader(name=" + this.name + ", values=" + this.values + ")";
  }

  public int hashCode() {
    String str = this.name;
    int hashCode = (str != null ? str.hashCode() : 0) * 31;
    List<String> list = this.values;
    return hashCode + (list != null ? list.hashCode() : 0);
  }

  public boolean equals(@Nullable Object obj) {
    if (this != obj) {
      if (!(obj instanceof WebRequestHeader webRequestHeader)) {
        return false;
      }
      return Intrinsics.areEqual(this.name, webRequestHeader.name) && Intrinsics.areEqual(this.values, webRequestHeader.values);
    }
    return true;
  }

  public String getName() {
    return this.name;
  }

  public List<String> getValues() {
    return this.values;
  }

  public WebRequestHeader(String name, List<String> list) {
    Intrinsics.checkNotNullParameter(name, "name");
    Intrinsics.checkNotNullParameter(list, "values");
    this.name = name;
    this.values = list;
  }
}

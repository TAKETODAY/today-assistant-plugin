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

package cn.taketoday.assistant.web.mvc.client.rest;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

public final class RestOperation {

  private final String method;

  private final String type;

  public final String component1() {
    return this.method;
  }

  public final String component2() {
    return this.type;
  }

  public final RestOperation copy(String method, String type) {
    Intrinsics.checkNotNullParameter(method, "method");
    Intrinsics.checkNotNullParameter(type, "type");
    return new RestOperation(method, type);
  }

  public static RestOperation copy$default(RestOperation restOperation, String str, String str2, int i, Object obj) {
    if ((i & 1) != 0) {
      str = restOperation.method;
    }
    if ((i & 2) != 0) {
      str2 = restOperation.type;
    }
    return restOperation.copy(str, str2);
  }

  public String toString() {
    return "RestOperation(method=" + this.method + ", type=" + this.type + ")";
  }

  public int hashCode() {
    String str = this.method;
    int hashCode = (str != null ? str.hashCode() : 0) * 31;
    String str2 = this.type;
    return hashCode + (str2 != null ? str2.hashCode() : 0);
  }

  public boolean equals(@Nullable Object obj) {
    if (this != obj) {
      if (!(obj instanceof RestOperation)) {
        return false;
      }
      RestOperation restOperation = (RestOperation) obj;
      return Intrinsics.areEqual(this.method, restOperation.method) && Intrinsics.areEqual(this.type, restOperation.type);
    }
    return true;
  }

  public final String getMethod() {
    return this.method;
  }

  public final String getType() {
    return this.type;
  }

  public RestOperation(String method, String type) {
    Intrinsics.checkNotNullParameter(method, "method");
    Intrinsics.checkNotNullParameter(type, "type");
    this.method = method;
    this.type = type;
  }
}

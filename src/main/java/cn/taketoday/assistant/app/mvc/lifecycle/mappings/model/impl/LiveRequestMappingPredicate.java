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

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

public final class LiveRequestMappingPredicate {

  private final String path;

  private final List<String> requestMethods;

  private final List<Pair<String, String>> headers;

  private final List<String> produces;

  private final List<String> consumes;

  private final List<Pair<String, String>> params;

  public LiveRequestMappingPredicate(String path, List<String> list, List<Pair<String, String>> list2,
          List<String> list3, List<String> list4, List<Pair<String, String>> list5) {
    this.path = path;
    this.requestMethods = list;
    this.headers = list2;
    this.produces = list3;
    this.consumes = list4;
    this.params = list5;
  }

  public LiveRequestMappingPredicate copy(String path, List<String> list, List<Pair<String, String>> list2, List<String> list3, List<String> list4,
          List<Pair<String, String>> list5) {
    return new LiveRequestMappingPredicate(path, list, list2, list3, list4, list5);
  }

  public String component1() {
    return this.path;
  }

  public List<String> component2() {
    return this.requestMethods;
  }

  public List<Pair<String, String>> component3() {
    return this.headers;
  }

  public List<String> component4() {
    return this.produces;
  }

  public List<String> component5() {
    return this.consumes;
  }

  public List<Pair<String, String>> component6() {
    return this.params;
  }

  public static LiveRequestMappingPredicate copy$default(LiveRequestMappingPredicate liveRequestMappingPredicate, String str, List list, List list2, List list3, List list4, List list5, int i,
          Object obj) {
    if ((i & 1) != 0) {
      str = liveRequestMappingPredicate.path;
    }
    if ((i & 2) != 0) {
      list = liveRequestMappingPredicate.requestMethods;
    }
    if ((i & 4) != 0) {
      list2 = liveRequestMappingPredicate.headers;
    }
    if ((i & 8) != 0) {
      list3 = liveRequestMappingPredicate.produces;
    }
    if ((i & 16) != 0) {
      list4 = liveRequestMappingPredicate.consumes;
    }
    if ((i & 32) != 0) {
      list5 = liveRequestMappingPredicate.params;
    }
    return liveRequestMappingPredicate.copy(str, list, list2, list3, list4, list5);
  }

  public String toString() {
    return "LiveRequestMappingPredicate(path=" + this.path + ", requestMethods=" + this.requestMethods + ", headers=" + this.headers + ", produces=" + this.produces + ", consumes=" + this.consumes + ", params=" + this.params + ")";
  }

  public int hashCode() {
    String str = this.path;
    int hashCode = (str != null ? str.hashCode() : 0) * 31;
    List<String> list = this.requestMethods;
    int hashCode2 = (hashCode + (list != null ? list.hashCode() : 0)) * 31;
    List<Pair<String, String>> list2 = this.headers;
    int hashCode3 = (hashCode2 + (list2 != null ? list2.hashCode() : 0)) * 31;
    List<String> list3 = this.produces;
    int hashCode4 = (hashCode3 + (list3 != null ? list3.hashCode() : 0)) * 31;
    List<String> list4 = this.consumes;
    int hashCode5 = (hashCode4 + (list4 != null ? list4.hashCode() : 0)) * 31;
    List<Pair<String, String>> list5 = this.params;
    return hashCode5 + (list5 != null ? list5.hashCode() : 0);
  }

  public boolean equals(@Nullable Object obj) {
    if (this != obj) {
      if (!(obj instanceof LiveRequestMappingPredicate liveRequestMappingPredicate)) {
        return false;
      }
      return Intrinsics.areEqual(this.path, liveRequestMappingPredicate.path) && Intrinsics.areEqual(this.requestMethods, liveRequestMappingPredicate.requestMethods) && Intrinsics.areEqual(
              this.headers, liveRequestMappingPredicate.headers) && Intrinsics.areEqual(this.produces, liveRequestMappingPredicate.produces) && Intrinsics.areEqual(this.consumes,
              liveRequestMappingPredicate.consumes) && Intrinsics.areEqual(this.params, liveRequestMappingPredicate.params);
    }
    return true;
  }

  public String getPath() {
    return this.path;
  }

  public List<String> getRequestMethods() {
    return this.requestMethods;
  }

  public List<Pair<String, String>> getHeaders() {
    return this.headers;
  }

  public List<String> getProduces() {
    return this.produces;
  }

  public List<String> getConsumes() {
    return this.consumes;
  }

  public List<Pair<String, String>> getParams() {
    return this.params;
  }
}

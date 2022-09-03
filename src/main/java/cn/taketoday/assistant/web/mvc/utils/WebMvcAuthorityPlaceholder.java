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

package cn.taketoday.assistant.web.mvc.utils;

import com.intellij.microservices.url.Authority;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

final class WebMvcAuthorityPlaceholder extends Authority.Placeholder {

  private final String moduleName;

  public String component1() {
    return this.moduleName;
  }

  public WebMvcAuthorityPlaceholder copy(String moduleName) {
    return new WebMvcAuthorityPlaceholder(moduleName);
  }

  public String toString() {
    return "WebMvcAuthorityPlaceholder(moduleName=" + this.moduleName + ")";
  }

  public int hashCode() {
    String str = this.moduleName;
    if (str != null) {
      return str.hashCode();
    }
    return 0;
  }

  public boolean equals(@Nullable Object obj) {
    if (this != obj) {
      return (obj instanceof WebMvcAuthorityPlaceholder) && Intrinsics.areEqual(this.moduleName, ((WebMvcAuthorityPlaceholder) obj).moduleName);
    }
    return true;
  }

  public String getModuleName() {
    return this.moduleName;
  }

  public WebMvcAuthorityPlaceholder(String moduleName) {
    this.moduleName = moduleName;
  }
}

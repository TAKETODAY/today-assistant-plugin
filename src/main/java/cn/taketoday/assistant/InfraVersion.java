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

package cn.taketoday.assistant;

import com.intellij.openapi.util.text.StringUtil;

public enum InfraVersion {
  ANY("1.0", "cn.taketoday.beans.factory.BeanFactory"),
  V_4_0("4.0", "cn.taketoday.stereotype.Component");

  private final String version;
  private final String detectionClassFqn;

  InfraVersion(String version, String detectionClassFqn) {
    this.version = version;
    this.detectionClassFqn = detectionClassFqn;
  }

  public boolean isAtLeast(InfraVersion reference) {
    if (reference == ANY) {
      return true;
    }
    else {
      return StringUtil.compareVersionNumbers(this.version, reference.version) >= 0;
    }
  }

  public String getVersion() {
    return this.version;
  }

  public String getDetectionClassFqn() {
    return this.detectionClassFqn;
  }
}

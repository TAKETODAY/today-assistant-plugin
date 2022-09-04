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

package cn.taketoday.assistant.app.run;

import com.intellij.util.xmlb.annotations.Tag;

import java.util.Objects;

@Tag("param")
public class InfraAdditionalParameter {
  private String name;
  private String value;
  private boolean enabled;

  public InfraAdditionalParameter() { }

  public InfraAdditionalParameter(boolean enabled, String name, String value) {
    this.name = name;
    this.value = value;
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return this.value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof InfraAdditionalParameter second)) {
      return false;
    }
    return this.enabled == second.enabled
            && Objects.equals(this.name, second.name)
            && Objects.equals(this.value, second.value);
  }

  public int hashCode() {
    int result = this.enabled ? 1 : 0;
    return (31 * ((31 * result) + (this.name != null ? this.name.hashCode() : 0))) + (this.value != null ? this.value.hashCode() : 0);
  }
}

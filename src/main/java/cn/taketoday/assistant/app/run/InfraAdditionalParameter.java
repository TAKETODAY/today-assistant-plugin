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
  private boolean myEnabled;
  private String myName;
  private String myValue;

  public InfraAdditionalParameter() { }

  public InfraAdditionalParameter(boolean enabled, String name, String value) {
    this.myEnabled = enabled;
    this.myName = name;
    this.myValue = value;
  }

  public boolean isEnabled() {
    return this.myEnabled;
  }

  public void setEnabled(boolean enabled) {
    this.myEnabled = enabled;
  }

  public String getName() {
    return this.myName;
  }

  public void setName(String name) {
    this.myName = name;
  }

  public String getValue() {
    return this.myValue;
  }

  public void setValue(String value) {
    this.myValue = value;
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof InfraAdditionalParameter second)) {
      return false;
    }
    return this.myEnabled == second.myEnabled
            && Objects.equals(this.myName, second.myName)
            && Objects.equals(this.myValue, second.myValue);
  }

  public int hashCode() {
    int result = this.myEnabled ? 1 : 0;
    return (31 * ((31 * result) + (this.myName != null ? this.myName.hashCode() : 0))) + (this.myValue != null ? this.myValue.hashCode() : 0);
  }
}

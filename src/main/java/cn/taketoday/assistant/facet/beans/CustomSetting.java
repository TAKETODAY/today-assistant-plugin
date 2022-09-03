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

package cn.taketoday.assistant.facet.beans;

import com.intellij.openapi.util.Key;

import cn.taketoday.lang.Nullable;

public abstract class CustomSetting {
  private final String myName;
  private final String myDescription;

  public abstract void setStringValue(String str);

  @Nullable
  public abstract String getStringValue();

  public abstract boolean isModified();

  public abstract void apply();

  public abstract void reset();

  public String getName() {
    return this.myName;
  }

  public String getDescription() {
    return this.myDescription;
  }

  protected CustomSetting(String name, String description) {
    this.myName = name;
    this.myDescription = description;
  }

  protected CustomSetting(Key<? extends CustomSetting> key, String description) {
    this.myName = key.toString();
    this.myDescription = description;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CustomSetting setting = (CustomSetting) o;
    return this.myName.equals(setting.myName);
  }

  public int hashCode() {
    return this.myName.hashCode();
  }

  public static class STRING extends CustomSetting {
    private String myValue;
    private final String myDefaultValue;
    private boolean myModified;

    public STRING(Key<STRING> key, String description, String defaultValue) {
      super(key, description);
      this.myDefaultValue = defaultValue;
    }

    public void clear() {
      this.myValue = null;
    }

    public String getDefaultValue() {
      return this.myDefaultValue;
    }

    @Override
    public void setStringValue(String value) {
      if (!value.equals(this.myValue)) {
        this.myValue = value;
        this.myModified = true;
      }
    }

    @Override
    @Nullable
    public String getStringValue() {
      return this.myValue;
    }

    @Override
    public boolean isModified() {
      return this.myModified;
    }

    @Override
    public void apply() {
      this.myModified = false;
    }

    @Override
    public void reset() {
      if (this.myModified) {
        this.myValue = null;
      }
      this.myModified = false;
    }
  }

  public static class BOOLEAN extends CustomSetting {
    private Boolean myValue;
    private final boolean myDefaultValue;
    private boolean myModified;

    public BOOLEAN(Key<BOOLEAN> key, String description, boolean defaultValue) {
      super(key, description);
      this.myValue = null;
      this.myDefaultValue = defaultValue;
    }

    public void setBooleanValue(boolean value) {
      boolean currentValue = getBooleanValue();
      if (value != currentValue) {
        this.myModified = true;
        this.myValue = value;
      }
    }

    public boolean getBooleanValue() {
      return (this.myModified || this.myValue == null) ? this.myDefaultValue : this.myValue;
    }

    @Nullable
    public Boolean getValue() {
      return this.myValue;
    }

    public boolean getDefaultValue() {
      return this.myDefaultValue;
    }

    @Override
    public void setStringValue(String value) {
      setBooleanValue(Boolean.parseBoolean(value));
    }

    @Override
    @Nullable
    public String getStringValue() {
      if (this.myValue == null) {
        return null;
      }
      return Boolean.toString(this.myValue);
    }

    @Override
    public void apply() {
      this.myModified = false;
    }

    @Override
    public void reset() {
      if (this.myModified) {
        this.myValue = null;
      }
      this.myModified = false;
    }

    @Override
    public boolean isModified() {
      return this.myModified;
    }
  }
}

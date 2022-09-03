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

package cn.taketoday.assistant.context.chooser;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;

import java.util.Objects;

import cn.taketoday.lang.Nullable;

public class InfraContextDescriptor {
  public static final Key<InfraContextDescriptor> KEY = Key.create("InfraContextDescriptor.KEY");
  public static final InfraContextDescriptor ALL_CONTEXTS = new InfraContextDescriptor(null, "all.contexts", "All Contexts");
  public static final InfraContextDescriptor LOCAL_CONTEXT = new InfraContextDescriptor(null, "local.file.context", "Local File");
  public static final InfraContextDescriptor DEFAULT = LOCAL_CONTEXT;
  @Nullable
  private final Module myModule;
  private final String myId;
  private final String myName;

  public InfraContextDescriptor(@Nullable Module module, String id, String name) {
    this.myModule = module;
    this.myId = id;
    this.myName = name;
  }

  @Nullable
  public Module getModule() {
    return this.myModule;
  }

  public String getId() {
    return this.myId;
  }

  public String getName() {
    return this.myName;
  }

  public boolean isPredefinedContext() {
    return this == LOCAL_CONTEXT || this == ALL_CONTEXTS;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InfraContextDescriptor that = (InfraContextDescriptor) o;
    if (!this.myId.equals(that.myId)) {
      return false;
    }
    return Objects.equals(this.myModule, that.myModule);
  }

  public int hashCode() {
    int result = this.myModule != null ? this.myModule.hashCode() : 0;
    return (31 * result) + this.myId.hashCode();
  }

  public String getQualifiedName() {
    return myId;
  }
}

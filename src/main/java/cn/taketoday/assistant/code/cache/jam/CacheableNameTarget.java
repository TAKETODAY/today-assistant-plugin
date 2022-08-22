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

package cn.taketoday.assistant.code.cache.jam;

import com.intellij.ide.presentation.Presentation;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.spring.presentation.SpringCorePresentationConstants;

import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
@Presentation(typeName = SpringCorePresentationConstants.CACHE,
              icon = "com.intellij.spring.SpringApiIcons.ShowCacheable")
public final class CacheableNameTarget implements PomRenameableTarget<Object> {
  private String name;

  public CacheableNameTarget(@NonNull String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "CacheableNameTarget(name=" + this.name + ")";
  }

  @Override
  public int hashCode() {
    String str = this.name;
    if (str != null) {
      return str.hashCode();
    }
    return 0;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this != obj) {
      return (obj instanceof CacheableNameTarget) && Intrinsics.areEqual(this.name, ((CacheableNameTarget) obj).name);
    }
    return true;
  }

  @Override
  public void navigate(boolean requestFocus) {
  }

  @Override
  @Nullable
  public Object setName(String newName) {
    Intrinsics.checkNotNullParameter(newName, "newName");
    this.name = newName;
    return null;
  }

  @Override
  public boolean canNavigate() {
    return false;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public boolean isWritable() {
    return true;
  }
}

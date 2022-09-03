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

package cn.taketoday.assistant.model.utils.resources;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;

import java.util.Collection;

import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.lang.Nullable;

public abstract class ResourcesUtil {

  public static ResourcesUtil of() {
    return ApplicationManager.getApplication().getService(ResourcesUtil.class);
  }

  public abstract <V extends PsiFileSystemItem> Collection<V> getResourceItems(InfraProperty property,
          Condition<PsiFileSystemItem> filter);

  public abstract <V extends PsiFileSystemItem> Collection<V> getResourceItems(PsiReference[] references,
          Condition<PsiFileSystemItem> filter);

  public abstract PsiReference[] getReferences(InfraResourcesBuilder builder);

  public abstract PsiReference[] getClassPathReferences(InfraResourcesBuilder builder);

  @Nullable
  public abstract String getResourceFileReferenceString(PsiFile resourceFile);
}

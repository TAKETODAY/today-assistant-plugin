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
package cn.taketoday.assistant.model.values.converters.resources;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;

public class ResourceTypeCondition implements Condition<PsiFileSystemItem> {
  private final String[] myFileExtensions;

  public ResourceTypeCondition(String... fileExtensions) {
    myFileExtensions = fileExtensions;
  }

  @Override
  public boolean value(PsiFileSystemItem psiFileSystemItem) {
    for (String fileExtension : myFileExtensions) {
      if (hasExtension(psiFileSystemItem, fileExtension))
        return true;
    }
    return false;
  }

  private static boolean hasExtension(PsiFileSystemItem psiFileSystemItem, String fileExtension) {
    if (FileReferenceSet.DIRECTORY_FILTER.value(psiFileSystemItem))
      return true;
    VirtualFile virtualFile = psiFileSystemItem.getVirtualFile();

    return virtualFile != null && fileExtension.equals(virtualFile.getExtension());
  }

  public String[] getExpectedExtensions() {
    return myFileExtensions;
  }
}

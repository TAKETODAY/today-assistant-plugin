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

package cn.taketoday.assistant.web.mvc.config;

import com.intellij.javaee.web.CommonServlet;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LayeredIcon;

import org.jetbrains.annotations.NonNls;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.facet.InfraAutodetectedFileSet;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.lang.Nullable;

public class ServletFileSet extends InfraAutodetectedFileSet {
  private static final Icon ICON = new LayeredIcon(Icons.FileSet, Icons.WebOverlay);
  @Nullable
  private final CommonServlet myServlet;

  public ServletFileSet(@NonNls String id, String name, @Nullable CommonServlet servlet, InfraFacet parent) {
    super(id, name, parent, ICON);
    this.myServlet = servlet;
  }

  @Nullable
  public CommonServlet getServlet() {
    return this.myServlet;
  }

  public static void addInFileset(InfraFileSet fileSet, @Nullable PsiFile containingFile) {
    VirtualFile file;
    if (containingFile != null && (file = containingFile.getVirtualFile()) != null) {
      fileSet.addFile(file);
    }
  }
}

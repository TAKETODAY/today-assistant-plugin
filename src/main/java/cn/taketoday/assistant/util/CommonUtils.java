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

package cn.taketoday.assistant.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 19:03
 */
public abstract class CommonUtils {

  public static <T extends PsiFile> List<T> findConfigFilesInMetaInf(Module module, boolean withTests, String filename, Class<T> psiFileType) {
    GlobalSearchScope moduleScope = GlobalSearchScope.moduleRuntimeScope(module, withTests);
    return findConfigFilesInMetaInf(module.getProject(), moduleScope, filename, psiFileType);
  }

  public static <T extends PsiFile> @NotNull List<T> findConfigFilesInMetaInf(Project project, GlobalSearchScope scope, String filename, Class<T> psiFileType) {
    GlobalSearchScope searchScope = getConfigFilesScope(project, scope);
    if (searchScope == null) {
      return Collections.emptyList();
    }
    else {
      PsiFile[] configFiles = FilenameIndex.getFilesByName(project, filename, searchScope);
      if (configFiles.length == 0) {
        return Collections.emptyList();
      }
      else {
        return ContainerUtil.findAll(configFiles, psiFileType);
      }
    }
  }

  public static @Nullable GlobalSearchScope getConfigFilesScope(Project project, GlobalSearchScope scope) {
    PsiPackage metaInfPackage = JavaPsiFacade.getInstance(project).findPackage("META-INF");
    if (metaInfPackage == null) {
      return null;
    }
    else {
      GlobalSearchScope packageScope = PackageScope.packageScope(metaInfPackage, false);
      return scope.intersectWith(packageScope);
    }
  }
}

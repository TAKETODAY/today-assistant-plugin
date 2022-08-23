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

import com.intellij.codeInsight.JavaLibraryModificationTracker;
import com.intellij.jam.JavaLibraryUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 00:25
 */
public class InfraLibraryUtil {

  public static boolean hasLibrary(Project project) {
    return JavaLibraryUtils.hasLibraryClass(project, TodayVersion.ANY.getDetectionClassFqn());
  }

  public static boolean hasLibrary(@Nullable Module module) {
    return isAtLeastVersion(module, TodayVersion.ANY);
  }

  public static boolean isAtLeastVersion(@Nullable Module module, TodayVersion version) {
    if (module == null) {
      return false;
    }
    else if (!hasLibrary(module.getProject())) {
      return false;
    }
    else {
      TodayVersion cached = getCachedVersion(module);
      return cached != null && cached.isAtLeast(version);
    }
  }

  @Nullable
  private static TodayVersion getCachedVersion(Module module) {
    Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, false);
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      TodayVersion detected = null;
      TodayVersion[] versions = ArrayUtil.reverseArray(TodayVersion.values());
      for (TodayVersion version : versions) {
        PsiClass psiClass = javaPsiFacade.findClass(version.getDetectionClassFqn(), scope);
        if (psiClass != null) {
          detected = version;
          break;
        }
      }

      return CachedValueProvider.Result.create(detected, JavaLibraryModificationTracker.getInstance(project));
    });
  }

  public static enum TodayVersion {
    ANY("1.0", "cn.taketoday.beans.factory.BeanFactory"),
    V_4_0("4.0", "cn.taketoday.stereotype.Component");

    private final String myVersion;
    private final String myDetectionClassFqn;

    TodayVersion(String version, String detectionClassFqn) {
      this.myVersion = version;
      this.myDetectionClassFqn = detectionClassFqn;
    }

    boolean isAtLeast(TodayVersion reference) {
      if (reference == ANY) {
        return true;
      }
      else {
        return StringUtil.compareVersionNumbers(this.getVersion(), reference.getVersion()) >= 0;
      }
    }

    String getVersion() {
      return this.myVersion;
    }

    String getDetectionClassFqn() {
      return this.myDetectionClassFqn;
    }
  }
}

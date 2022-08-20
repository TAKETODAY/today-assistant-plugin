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

package cn.taketoday.assistant.core;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairProcessor;

import cn.taketoday.lang.Nullable;

import java.util.List;

import cn.taketoday.assistant.util.CommonUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 18:37
 */
public class StrategiesManager {
  private final Module myModule;

  public StrategiesManager(Module module) {
    this.myModule = module;
  }

  public List<PropertiesFileImpl> getStrategiesFiles(boolean includeTests) {
    return CommonUtils.findConfigFilesInMetaInf(this.myModule, includeTests,
            TodayStrategiesFileType.STRATEGIES_FILE_NAME, PropertiesFileImpl.class);
  }

  public List<PsiClass> getClassesListValue(boolean includeTests, String key) {
    return StrategiesFileIndex.getClasses(this.myModule.getProject(), getScope(includeTests), key);
  }

  public List<PsiClass> getClassesListValue(String key, GlobalSearchScope scope) {
    return StrategiesFileIndex.getClasses(this.myModule.getProject(), scope, key);
  }

  public boolean processClassesListValues(boolean includeTests, @Nullable String valueHint, PairProcessor<IProperty, PsiClass> processor) {
    return StrategiesFileIndex.processValues(this.myModule.getProject(), this.getScope(includeTests), valueHint, processor);
  }

  private GlobalSearchScope getScope(boolean includeTests) {
    return GlobalSearchScope.moduleRuntimeScope(this.myModule, includeTests);
  }

  public static StrategiesManager from(Module module) {
    return module.getService(StrategiesManager.class);
  }

}


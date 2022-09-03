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

package cn.taketoday.assistant.app.spi;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairProcessor;

import java.util.List;

import cn.taketoday.lang.Nullable;

public abstract class InfraImportsManager {

  public static InfraImportsManager getInstance(Module module) {
    return module.getService(InfraImportsManager.class);
  }

  /**
   * Returns all resolved classes configured for given key.
   *
   * @param includeTests Include config files in test source roots.
   * @param key Key to search for.
   * @return Resolved classes or empty list of none found.
   */
  public abstract List<PsiClass> getClasses(boolean includeTests, String key);

  /**
   * Process all resolved classes list values.
   *
   * @param includeTests Process config files in test source roots.
   * @param processor Processor.
   * @param valueHint Only process properties containing given text (class FQN) as value or `null` to process all properties.
   * @return Processing result.
   */
  public abstract boolean processValues(boolean includeTests, @Nullable String valueHint, PairProcessor<PsiElement, PsiClass> processor);

  public abstract List<PsiClass> getAutoConfigurationClasses(boolean includeTests);

  public abstract List<PsiClass> getAutoConfigurationClasses(GlobalSearchScope scope);

}

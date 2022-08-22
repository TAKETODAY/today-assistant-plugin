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

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;

import cn.taketoday.assistant.code.cache.jam.standard.JamCacheConfig;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheEvict;
import cn.taketoday.assistant.code.cache.jam.standard.JamCachePut;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
final class CacheableSemContributor extends SemContributor {

  @Override
  public void registerSemProviders(SemRegistrar registrar, Project project) {
    PsiMethodPattern psiMethod = PsiJavaPatterns.psiMethod().constructor(false);
    JamCacheable.register(registrar, psiMethod);
    JamCacheEvict.register(registrar, psiMethod);
    JamCachePut.register(registrar, psiMethod);

    JamCacheConfig.register(registrar);
    CachingGroup.register(registrar);
  }

}

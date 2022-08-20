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

package cn.taketoday.assistant.code.cache.jam.custom;

import com.intellij.jam.reflect.JamMemberArchetype;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemKey;

public class CustomCacheEvictForMethod extends CustomCacheEvict<PsiMethod> {
  public static final SemKey<CustomCacheEvictForMethod> JAM_KEY = CUSTOM_CACHE_EVICT_JAM_KEY.subKey("SpringJamCustomCacheEvictForMethod");
  public static final JamMethodMeta<CustomCacheEvictForMethod> META = new JamMethodMeta<>((JamMemberArchetype) null, CustomCacheEvictForMethod.class, JAM_KEY);
  public static final SemKey<JamMemberMeta<PsiMethod, CustomCacheEvictForMethod>> META_KEY = META.getMetaKey().subKey("SpringJamCustomCacheEvictForMethod");

  public CustomCacheEvictForMethod(String annoName, PsiMethod psiMethod) {
    super(annoName, psiMethod);
  }
}

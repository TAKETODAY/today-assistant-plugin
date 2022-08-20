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

public class CustomCacheableForMethod extends CustomCacheable<PsiMethod> {
  public static final SemKey<CustomCacheableForMethod> JAM_KEY = CUSTOM_CACHEABLE_JAM_KEY.subKey("SpringJamCustomCacheableForMethod");
  public static final JamMethodMeta<CustomCacheableForMethod> META = new JamMethodMeta<>((JamMemberArchetype) null, CustomCacheableForMethod.class, JAM_KEY);
  public static final SemKey<JamMemberMeta<PsiMethod, CustomCacheableForMethod>> META_KEY = META.getMetaKey().subKey("SpringJamCustomCacheableForMethod");

  public CustomCacheableForMethod(String annoName, PsiMethod method) {
    super(annoName, method);
  }
}

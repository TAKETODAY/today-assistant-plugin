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

public class CustomCachePutForMethod extends CustomCachePut<PsiMethod> {
  public static final SemKey<CustomCachePutForMethod> JAM_KEY = CUSTOM_CACHE_PUT_JAM_KEY.subKey("SpringJamCustomCachePutForMethod");
  public static final JamMethodMeta<CustomCachePutForMethod> META = new JamMethodMeta<>((JamMemberArchetype) null, CustomCachePutForMethod.class, JAM_KEY);
  public static final SemKey<JamMemberMeta<PsiMethod, CustomCachePutForMethod>> META_KEY = META.getMetaKey().subKey("SpringJamCustomCachePutForMethod");

  public CustomCachePutForMethod(String annoName, PsiMethod psiMethod) {
    super(annoName, psiMethod);
  }
}

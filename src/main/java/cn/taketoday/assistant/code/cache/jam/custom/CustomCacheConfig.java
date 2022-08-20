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

import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberArchetype;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CustomCacheableElement;

public class CustomCacheConfig extends CustomCacheableElement<PsiClass> {
  public static final SemKey<CustomCacheConfig> JAM_KEY = CUSTOM_ROOT_JAM_KEY.subKey("SpringJamCustomCacheable");
  public static final JamClassMeta<CustomCacheConfig> META = new JamClassMeta<>((JamMemberArchetype) null, CustomCacheConfig.class, JAM_KEY);
  public static final SemKey<JamMemberMeta<PsiClass, CustomCacheConfig>> META_KEY = META.getMetaKey().subKey("SpringJamCustomCacheConfig");

  public CustomCacheConfig(String annoName, PsiClass aClass) {
    super(annoName, aClass);
  }

  @Override
  protected String getDefiningAnnotation() {
    return CacheableConstant.CACHE_CONFIG;
  }
}

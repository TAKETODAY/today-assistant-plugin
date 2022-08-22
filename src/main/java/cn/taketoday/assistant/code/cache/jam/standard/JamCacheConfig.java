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

package cn.taketoday.assistant.code.cache.jam.standard;

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.JamBaseCacheableElement;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public class JamCacheConfig extends JamBaseCacheableElement<PsiClass> {

  private static final JamAnnotationMeta CACHE_CONFIG_ANNO_META = new JamAnnotationMeta(CacheableConstant.CACHE_CONFIG)
          .addAttribute(CACHE_NAMES_ATTR_META)
          .addAttribute(CACHE_MANAGER_ATTR_META)
          .addAttribute(CACHE_RESOLVER_ATTR_META)
          .addAttribute(KEY_GENERATOR_ATTR_META);
  public static final SemKey<JamCacheConfig> CACHE_CONFIG_JAM_KEY = CACHEABLE_BASE_JAM_KEY.subKey("SpringJamCacheable");
  public static final JamClassMeta<JamCacheConfig> META = new JamClassMeta<>(
          null, JamCacheConfig.class, CACHE_CONFIG_JAM_KEY)
          .addAnnotation(CACHE_CONFIG_ANNO_META);

  public JamCacheConfig(PsiClass aClass) {
    super(CacheableConstant.CACHE_CONFIG, aClass);
  }
}

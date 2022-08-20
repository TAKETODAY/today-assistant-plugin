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

import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNamedElement;
import com.intellij.semantic.SemKey;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CustomCacheableElement;
import cn.taketoday.assistant.code.cache.jam.standard.CacheEvictMarker;

public abstract class CustomCacheEvict<T extends PsiMember & PsiNamedElement> extends CustomCacheableElement<T> implements CacheEvictMarker {
  public static final SemKey<CustomCacheEvict> CUSTOM_CACHE_EVICT_JAM_KEY = CUSTOM_ROOT_JAM_KEY.subKey("SpringJamCustomCacheEvict");

  public CustomCacheEvict(String annoName, T t) {
    super(annoName, t);
  }

  @Override
  protected String getDefiningAnnotation() {
    return CacheableConstant.CACHE_EVICT;
  }
}

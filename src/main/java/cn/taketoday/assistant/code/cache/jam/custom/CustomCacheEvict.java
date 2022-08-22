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
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CustomCacheableElement;
import cn.taketoday.assistant.code.cache.jam.standard.CacheEvictMarker;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public abstract class CustomCacheEvict<T extends PsiMember & PsiNamedElement> extends CustomCacheableElement<T> implements CacheEvictMarker {
  public static final SemKey<CustomCacheEvict> CUSTOM_CACHE_EVICT_JAM_KEY = CUSTOM_ROOT_JAM_KEY.subKey("SpringJamCustomCacheEvict");

  public CustomCacheEvict(String annoName, T t) {
    super(annoName, t);
  }

  @Override
  protected String getDefiningAnnotation() {
    return CacheableConstant.CACHE_EVICT;
  }

  public static class ForMethod extends CustomCacheEvict<PsiMethod> {
    public static final SemKey<ForMethod> JAM_KEY = CUSTOM_CACHE_EVICT_JAM_KEY.subKey("SpringJamCustomCacheEvictForMethod");
    public static final JamMethodMeta<ForMethod> META = new JamMethodMeta<>(null, ForMethod.class, JAM_KEY);
    public static final SemKey<JamMemberMeta<PsiMethod, ForMethod>> META_KEY = META.getMetaKey().subKey("SpringJamCustomCacheEvictForMethod");

    public ForMethod(String annoName, PsiMethod psiMethod) {
      super(annoName, psiMethod);
    }
  }

  public static class ForClass extends CustomCacheEvict<PsiClass> {
    public static final SemKey<ForClass> JAM_KEY = CUSTOM_CACHE_EVICT_JAM_KEY.subKey("SpringJamCustomCacheEvictForClass");
    public static final JamClassMeta<ForClass> META = new JamClassMeta<>(null, ForClass.class, JAM_KEY);
    public static final SemKey<JamMemberMeta<PsiClass, ForClass>> META_KEY = META.getMetaKey().subKey("SpringJamCustomCacheEvictForClass");

    public ForClass(String annoName, PsiClass aClass) {
      super(annoName, aClass);
    }
  }
}

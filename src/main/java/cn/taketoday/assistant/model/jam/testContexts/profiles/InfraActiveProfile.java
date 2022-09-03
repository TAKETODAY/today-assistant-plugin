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
package cn.taketoday.assistant.model.jam.testContexts.profiles;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.semantic.SemKey;

import java.util.Set;

import cn.taketoday.lang.Nullable;

public interface InfraActiveProfile extends JamElement {
  String VALUE_ATTR_NAME = "value";
  String PROFILES_ATTR_NAME = "profiles";
  String[] PROFILES_ATTRS = { PROFILES_ATTR_NAME, VALUE_ATTR_NAME };

  SemKey<JamAnnotationMeta> ACTIVE_PROFILE_JAM_ANNOTATION_KEY = JamService.ANNO_META_KEY.subKey("SpringActiveProfile");
  SemKey<InfraActiveProfile> ACTIVE_PROFILE_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("SpringActiveProfile");
  SemKey<JamMemberMeta> ACTIVE_PROFILE_META_KEY = JamService.getMetaKey(ACTIVE_PROFILE_JAM_KEY);

  @Nullable
  PsiMember getPsiElement();

  @Nullable
  PsiAnnotation getAnnotation();

  @Nullable
  PsiElement getIdentifyingPsiElement();

  Set<String> getActiveProfiles();
}

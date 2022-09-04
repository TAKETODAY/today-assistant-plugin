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
package cn.taketoday.assistant.model.jam.profile;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMember;
import com.intellij.semantic.SemKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.profiles.InfraProfilesFactory;
import cn.taketoday.assistant.profiles.InfraProfilesFactory.MalformedProfileExpressionException;
import cn.taketoday.lang.Nullable;

public interface InfraContextProfile extends JamElement, InfraProfile {
  String VALUE_ATTR_NAME = "value";

  SemKey<JamAnnotationMeta> CONTEXT_PROFILE_JAM_ANNOTATION_KEY = JamService.ANNO_META_KEY.subKey("ContextProfile");
  SemKey<InfraContextProfile> CONTEXT_PROFILE_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("InfraContextProfile");
  SemKey<JamMemberMeta> CONTEXT_PROFILE_META_KEY = JamService.getMetaKey(CONTEXT_PROFILE_JAM_KEY);

  @Nullable
  PsiMember getPsiElement();

  @Nullable
  PsiAnnotation getAnnotation();

  List<JamStringAttributeElement<String>> getValueElements();

  @Override
  default Set<String> getNames() {
    return getExpressions().stream()
            .flatMap(expression -> {
              Set<String> names = new HashSet<>();
              new DelimitedListProcessor(InfraJamProfile.PROFILE_DELIMITERS) {
                @Override
                protected void processToken(int start, int end, boolean delimitersOnly) {
                  String name = expression.substring(start, end);
                  name = StringUtil.trimStart(name, "!").trim();
                  if (!name.isEmpty()) {
                    names.add(name);
                  }
                }
              }.processText(expression);
              return names.stream();
            })
            .collect(Collectors.toSet());
  }

  @Override
  default boolean matches(Set<String> activeProfiles) {
    try {
      Predicate<Set<String>> profiles =
              InfraProfilesFactory.of().parseProfileExpressions(getExpressions());
      return profiles.test(activeProfiles);
    }
    catch (MalformedProfileExpressionException e) {
      return false;
    }
  }
}

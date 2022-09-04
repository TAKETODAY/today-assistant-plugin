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

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.jam.profile.InfraJamProfile;
import cn.taketoday.lang.Nullable;

@Presentation(typeName = PresentationConstant.ACTIVE_PROFILES)
public class InfraJamActiveProfiles extends CommonModelElement.PsiBase implements InfraActiveProfile {

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final PsiMember myPsiMember;

  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = ACTIVE_PROFILE_JAM_ANNOTATION_KEY.subKey("InfraJamActiveProfiles");
  private static final SemKey<InfraJamActiveProfiles> JAM_KEY = ACTIVE_PROFILE_JAM_KEY.subKey("InfraJamActiveProfiles");

  public static final JamClassMeta<InfraJamActiveProfiles> META = new JamClassMeta<>(null, InfraJamActiveProfiles.class, JAM_KEY);
  private static final JamConverter<String> PROFILE_CONVERTER = new InfraJamProfile.InfraProfileConverter("", false);
  private static final JamStringAttributeMeta.Collection<String> VALUE_ATTR_META =
          JamAttributeMeta.collectionString(VALUE_ATTR_NAME, PROFILE_CONVERTER);
  private static final JamStringAttributeMeta.Collection<String> PROFILES_ATTR_META =
          JamAttributeMeta.collectionString(PROFILES_ATTR_NAME, PROFILE_CONVERTER);

  private static final JamAnnotationArchetype ARCHETYPE =
          new JamAnnotationArchetype().addAttribute(VALUE_ATTR_META).addAttribute(PROFILES_ATTR_META);
  public static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.ACTIVE_PROFILES, ARCHETYPE, JAM_ANNO_META_KEY);

  static {
    META.addAnnotation(ANNO_META);
  }

  public InfraJamActiveProfiles(PsiMember psiMember) {
    myPsiMember = psiMember;
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiMember);
  }

  @SuppressWarnings("unused")
  public InfraJamActiveProfiles(PsiAnnotation annotation) {
    myPsiMember = PsiTreeUtil.getParentOfType(annotation, PsiMember.class, true);
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Override

  public Set<String> getActiveProfiles() {
    Set<String> profiles = new LinkedHashSet<>();

    addProfiles(profiles, VALUE_ATTR_META);
    addProfiles(profiles, PROFILES_ATTR_META);

    return profiles;
  }

  private void addProfiles(Set<String> profiles,
          JamStringAttributeMeta.Collection<String> attrMeta) {
    for (JamStringAttributeElement<String> element : ANNO_META.getAttribute(myPsiMember, attrMeta)) {
      if (element != null) {
        String value = element.getValue();
        if (StringUtil.isNotEmpty(value))
          profiles.add(value);
      }
    }
  }

  @Override

  public PsiMember getPsiElement() {
    return myPsiMember;
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }
}

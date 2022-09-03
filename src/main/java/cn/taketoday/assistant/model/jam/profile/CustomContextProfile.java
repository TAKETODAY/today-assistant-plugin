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

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.ref.AnnotationChildLink;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.lang.Nullable;

public class CustomContextProfile implements InfraContextProfile {

  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = CONTEXT_PROFILE_JAM_ANNOTATION_KEY
          .subKey("CustomContextProfile");

  public static final SemKey<CustomContextProfile> JAM_KEY = CONTEXT_PROFILE_JAM_KEY.subKey("CustomContextProfile");
  public static final SemKey<JamMemberMeta<PsiMember, CustomContextProfile>> META_KEY =
          CONTEXT_PROFILE_META_KEY.subKey("CustomContextProfile");

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final AnnotationChildLink myAnnotationChildLink;

  private final PsiAnchor myPsiMemberAnchor;

  private final NullableLazyValue<InfraJamProfile> myDefiningMetaAnnotation =
          new NullableLazyValue<>() {
            @Nullable
            @Override
            protected InfraJamProfile compute() {
              PsiMember element = getPsiElement();
              if (element == null)
                return null;

              PsiAnnotation definingMetaAnnotation =
                      AliasForUtils.findDefiningMetaAnnotation(element, myAnnotationChildLink.getAnnotationQualifiedName(),
                              AnnotationConstant.PROFILE);
              if (definingMetaAnnotation != null) {
                PsiClass annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true);
                if (annotationType != null) {
                  return InfraJamProfile.META.getJamElement(annotationType);
                }
              }
              return null;
            }
          };

  public CustomContextProfile(String anno, PsiMember psiMember) {
    myAnnotationChildLink = new AnnotationChildLink(anno);
    myPsiMemberAnchor = PsiAnchor.create(psiMember);
    myPsiAnnotation = myAnnotationChildLink.createChildRef(psiMember);
  }

  @Override
  public PsiMember getPsiElement() {
    return (PsiMember) myPsiMemberAnchor.retrieve();
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Nullable
  @Override
  public PsiElement getIdentifyingPsiElement() {
    return getPsiElement();
  }

  @Override
  public Set<String> getExpressions() {
    Set<String> profiles = new HashSet<>();
    InfraAliasFor aliasFor = getAliasAttribute(VALUE_ATTR_NAME);
    if (aliasFor != null) {
      for (JamStringAttributeElement<String> element : JamAttributeMeta.collectionString(aliasFor.getMethodName())
              .getJam(myPsiAnnotation)) {
        String value = element.getStringValue();
        if (!StringUtil.isEmptyOrSpaces(value)) {
          profiles.add(value.trim());
        }
      }
    }
    else {
      InfraJamProfile definingProfile = myDefiningMetaAnnotation.getValue();
      if (definingProfile != null)
        return definingProfile.getExpressions();
    }
    return profiles;
  }

  @Nullable
  private InfraAliasFor getAliasAttribute(String attrName) {
    PsiMember element = getPsiElement();
    return element == null ? null : AliasForUtils.findAliasFor(element,
            myAnnotationChildLink.getAnnotationQualifiedName(),
            AnnotationConstant.PROFILE,
            attrName);
  }

  @Override
  public List<JamStringAttributeElement<String>> getValueElements() {
    InfraAliasFor aliasFor = getAliasAttribute(VALUE_ATTR_NAME);
    return aliasFor == null ? Collections.emptyList() :
           JamAttributeMeta.collectionString(aliasFor.getMethodName()).getJam(myPsiAnnotation);
  }
}
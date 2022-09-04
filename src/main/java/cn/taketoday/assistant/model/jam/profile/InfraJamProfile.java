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

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.expressions.UStringConcatenationsFacade;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.profiles.InfraProfilesFactory;
import cn.taketoday.lang.Nullable;

@Presentation(typeName = PresentationConstant.PROFILE)
public class InfraJamProfile extends CommonModelElement.PsiBase implements InfraContextProfile {
  public static final String PROFILE_DELIMITERS = "()&|"; // delimiters are supported since Infra 4.0

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final PsiMember myPsiMember;

  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = CONTEXT_PROFILE_JAM_ANNOTATION_KEY.subKey("JamProfile");
  private static final SemKey<InfraJamProfile> JAM_KEY = InfraContextProfile.CONTEXT_PROFILE_JAM_KEY.subKey("JamProfile");

  public static final JamMemberMeta<PsiMember, InfraJamProfile> META = new JamMemberMeta<>(null, InfraJamProfile.class, JAM_KEY);
  private static final JamStringAttributeMeta.Collection<String> VALUE_ATTR_META =
          JamAttributeMeta.collectionString("value", new InfraProfileConverter(PROFILE_DELIMITERS, true));
  private static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype().addAttribute(VALUE_ATTR_META);

  public static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.PROFILE, ARCHETYPE, JAM_ANNO_META_KEY);

  static {
    META.addAnnotation(ANNO_META);
  }

  @SuppressWarnings("unused")
  public InfraJamProfile(PsiMember psiMember) {
    myPsiMember = psiMember;
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiMember);
  }

  @SuppressWarnings("unused")
  public InfraJamProfile(PsiAnnotation annotation) {
    myPsiMember = PsiTreeUtil.getParentOfType(annotation, PsiMember.class, true);
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Override
  public Set<String> getExpressions() {
    Set<String> profiles = new LinkedHashSet<>();
    for (JamStringAttributeElement<String> element : getValueElements()) {
      String value = element.getStringValue();
      if (!StringUtil.isEmptyOrSpaces(value)) {
        profiles.add(value.trim());
      }
    }
    return profiles;
  }

  @Override
  public List<JamStringAttributeElement<String>> getValueElements() {
    return ANNO_META.getAttribute(myPsiMember, VALUE_ATTR_META);
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

  public static class InfraProfileConverter extends JamConverter<String> {
    private final String myDelimiters;
    private final boolean myIsDefinition;

    public InfraProfileConverter(String delimiters, boolean isDefinition) {
      myDelimiters = delimiters;
      myIsDefinition = isDefinition;
    }

    @Override
    public String fromString(@Nullable String s, JamStringAttributeElement<String> context) {
      return s;
    }

    @Override
    public PsiReference[] createReferences(JamStringAttributeElement<String> context, PsiLanguageInjectionHost injectionHost) {
      return createReferences(injectionHost);
    }

    public PsiReference[] createReferences(PsiLanguageInjectionHost injectionHost) {
      UExpression uExpression = UastContextKt.toUElement(injectionHost, UExpression.class);
      if (uExpression == null)
        return PsiReference.EMPTY_ARRAY;

      UStringConcatenationsFacade facade = UStringConcatenationsFacade.createFromTopConcatenation(uExpression);
      if (facade == null)
        return PsiReference.EMPTY_ARRAY;

      PartiallyKnownString pks = facade.asPartiallyKnownString();
      if (pks.getSegments().size() > 1)
        return PsiReference.EMPTY_ARRAY;

      Module module = ModuleUtilCore.findModuleForPsiElement(injectionHost);
      if (module == null)
        return PsiReference.EMPTY_ARRAY;

      return InfraProfilesFactory.of()
              .getProfilesReferences(module, injectionHost, pks.getValueIfKnown(), 0, myDelimiters, myIsDefinition);
    }
  }
}
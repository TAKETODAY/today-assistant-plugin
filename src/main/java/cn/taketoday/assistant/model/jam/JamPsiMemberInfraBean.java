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
package cn.taketoday.assistant.model.jam;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.jam.JamCommonModelElement;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiType;
import com.intellij.semantic.SemKey;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.DefaultInfraBeanQualifier;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.jam.profile.InfraContextProfile;
import cn.taketoday.assistant.model.jam.qualifiers.InfraJamQualifier;
import cn.taketoday.lang.Nullable;

public abstract class JamPsiMemberInfraBean<T extends PsiMember> extends JamCommonModelElement<T>
        implements JamElement, CommonInfraBean {

  public static final SemKey<JamPsiMemberInfraBean> PSI_MEMBERINFRA_BEAN_JAM_KEY =
          JamService.JAM_ALIASING_ELEMENT_KEY.subKey("PsiMemberSpringBean");

  protected JamPsiMemberInfraBean(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  @Nullable
  public PsiType getBeanType(boolean considerFactories) {
    return getBeanType();
  }

  @Override
  public String[] getAliases() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  protected List<String> getStringNames(List<JamStringAttributeElement<String>> elements) {
    List<String> aliases = new SmartList<>();
    for (JamStringAttributeElement<String> element : elements) {
      String aliasName = element.getStringValue();
      if (!StringUtil.isEmptyOrSpaces(aliasName)) {
        aliases.add(aliasName);
      }
    }
    return aliases;
  }

  @Override
  public Collection<InfraQualifier> getInfraQualifiers() {
    Collection<InfraQualifier> jamQualifiers = getQualifiers();
    return jamQualifiers.isEmpty() ? Collections.singleton(DefaultInfraBeanQualifier.create(this)) : jamQualifiers;
  }

  public Collection<InfraQualifier> getQualifiers() {
    Module module = getModule();
    if (module == null) {
      return Collections.emptySet();
    }

    return InfraJamQualifier.findSpringJamQualifiers(module, getPsiElement());
  }

  @Override
  public boolean isPrimary() {
    return MetaAnnotationUtil.isMetaAnnotated(getPsiElement(), Collections.singleton(AnnotationConstant.PRIMARY));
  }

  @Override
  public InfraProfile getProfile() {
    PsiMember psiElement = getPsiElement();
    InfraContextProfile springProfile = getProfile(psiElement);
    if (springProfile == null) {
      springProfile = getProfile(psiElement.getContainingClass());
    }
    return springProfile != null ? springProfile : InfraProfile.DEFAULT;
  }

  @Nullable
  public InfraContextProfile getProfile(@Nullable PsiMember psiElement) {
    return psiElement == null ? null : JamService.getJamService(getPsiManager().getProject())
            .getJamElement(InfraContextProfile.CONTEXT_PROFILE_JAM_KEY, psiElement);
  }
}

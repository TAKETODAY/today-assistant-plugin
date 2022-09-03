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
package cn.taketoday.assistant.model.jam.converters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.lang.Nullable;

public class InfraBeanReferenceJamConverter extends JamSimpleReferenceConverter<BeanPointer<?>> {

  @Nullable
  private final String myBaseClass;

  /**
   * @param baseClass Required base class or {@code null} (provide custom {@link #getVariants(JamStringAttributeElement)}).
   */
  public InfraBeanReferenceJamConverter(@Nullable String baseClass) {
    myBaseClass = baseClass;
  }

  /**
   * @return Required base class or {@code null}.
   */
  @Nullable
  public String getBaseClass() {
    return myBaseClass;
  }

  @Override
  public BeanPointer<?> fromString(@Nullable String s, JamStringAttributeElement<BeanPointer<?>> context) {
    if (StringUtil.isEmptyOrSpaces(s)) {
      return null;
    }

    CommonInfraModel model = getSpringModel(context.getPsiElement());
    return InfraModelSearchers.findBean(model, s);
  }

  @Override
  public Collection<BeanPointer<?>> getVariants(JamStringAttributeElement<BeanPointer<?>> context) {
    PsiAnnotationMemberValue psiElement = context.getPsiElement();
    if (psiElement == null)
      return Collections.emptyList();

    if (myBaseClass == null) {
      CommonInfraModel model = getSpringModel(psiElement);
      return model.getAllCommonBeans();
    }

    PsiClass psiClass = JavaPsiFacade.getInstance(psiElement.getProject())
            .findClass(myBaseClass, psiElement.getResolveScope());
    if (psiClass == null)
      return Collections.emptyList();

    CommonInfraModel model = getSpringModel(psiElement);
    ModelSearchParameters.BeanClass searchParameters =
            ModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes();
    return InfraModelSearchers.findBeans(model, searchParameters);
  }

  @Override
  protected LookupElement createLookupElementFor(BeanPointer<?> target) {
    LookupElement variant = InfraConverterUtil.createCompletionVariant(target);
    return variant != null ? variant : super.createLookupElementFor(target);
  }

  @Override
  protected PsiElement getPsiElementFor(BeanPointer<?> target) {
    return target.getBean().getIdentifyingPsiElement();
  }

  /**
   * Spring Model for converter.
   *
   * @param psiElement Current element.
   * @return Spring Model for resolving/completion.
   */

  protected CommonInfraModel getSpringModel(PsiElement psiElement) {
    return InfraModelService.of().getModel(psiElement);
  }
}

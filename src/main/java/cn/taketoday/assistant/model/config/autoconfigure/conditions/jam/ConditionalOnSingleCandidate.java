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

package cn.taketoday.assistant.model.config.autoconfigure.conditions.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamEnumAttributeElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.jam.StringLiteralPsiClassConverter;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

public class ConditionalOnSingleCandidate extends JamBaseElement<PsiModifierListOwner> implements ConditionalOnJamElement.NonStrict {
  private static final JamClassAttributeMeta.Single VALUE_ATTRIBUTE = JamAttributeMeta.singleClass("value");
  private static final JamStringAttributeMeta.Single<PsiClass> TYPE_ATTRIBUTE = JamAttributeMeta.singleString(InfraMetadataConstant.TYPE, new StringLiteralPsiClassConverter());
  private static final JamEnumAttributeMeta.Single<ConditionalOnBeanBase.SearchStrategy> SEARCH_STRATEGY_ATTRIBUTE = JamAttributeMeta.singleEnum(
          "search", ConditionalOnBeanBase.SearchStrategy.class);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_SINGLE_CANDIDATE).addAttribute(VALUE_ATTRIBUTE)
          .addAttribute(TYPE_ATTRIBUTE).addAttribute(SEARCH_STRATEGY_ATTRIBUTE);
  private static final SemKey<ConditionalOnSingleCandidate> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnSingleCandidate");
  public static final JamClassMeta<ConditionalOnSingleCandidate> CLASS_META = new JamClassMeta<>(null, ConditionalOnSingleCandidate.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnSingleCandidate> METHOD_META = new JamMethodMeta<>(null, ConditionalOnSingleCandidate.class, SEM_KEY).addAnnotation(
          ANNOTATION_META);

  public ConditionalOnSingleCandidate(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public PsiClass getValue() {
    return ANNOTATION_META.getAttribute(getPsiElement(), VALUE_ATTRIBUTE).getValue();
  }

  @Nullable
  public PsiClass getType() {
    return (PsiClass) ((JamStringAttributeElement) ANNOTATION_META.getAttribute(getPsiElement(), TYPE_ATTRIBUTE)).getValue();
  }

  public ConditionalOnBeanBase.SearchStrategy getSearch() {
    ConditionalOnBeanBase.SearchStrategy searchStrategy = (ConditionalOnBeanBase.SearchStrategy) ((JamEnumAttributeElement) ANNOTATION_META.getAttribute(
            getPsiElement(), SEARCH_STRATEGY_ATTRIBUTE)).getValue();
    return searchStrategy != null ? searchStrategy : ConditionalOnBeanBase.SearchStrategy.ALL;
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    CommonInfraModel infraModel = context.getUserData(ConditionalOnEvaluationContext.MODEL_KEY);
    if (infraModel == null) {
      return ConditionalOnBeanUtils.getMissingModelOutcome();
    }
    List<PsiType> psiTypes = getTypesToMatch();
    if (psiTypes.size() != 1) {
      return ConditionOutcome.noMatch(ConditionMessage.didNotFind("bean"));
    }
    PsiType psiType = psiTypes.get(0);
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byType(psiType).withInheritors().effectiveBeanTypes();
    List<CommonInfraBean> beans = ContainerUtil.map(InfraModelSearchers.findBeans(infraModel, searchParameters), BeanPointer.TO_BEAN);
    CommonInfraBean selfBean = ConditionalOnBeanUtils.getSpringBean(getPsiElement());
    if (selfBean != null) {
      beans.remove(selfBean);
    }
    if (beans.size() == 1 || ContainerUtil.filter(beans, CommonInfraBean::isPrimary).size() == 1) {
      return ConditionOutcome.match(ConditionMessage.found("bean"));
    }
    return ConditionOutcome.noMatch(ConditionMessage.didNotFind("bean"));
  }

  private List<PsiType> getTypesToMatch() {
    SmartList smartList = new SmartList();
    PsiClass psiClass = getValue();
    if (isClassToSearch(psiClass)) {
      smartList.add(PsiTypesUtil.getClassType(psiClass));
    }
    PsiClass psiClass2 = getType();
    if (isClassToSearch(psiClass2)) {
      smartList.add(PsiTypesUtil.getClassType(psiClass2));
    }
    if (!smartList.isEmpty()) {
      return smartList;
    }
    PsiModifierListOwner psiMethod = getPsiElement();
    if (psiMethod instanceof PsiMethod method) {
      ContainerUtil.addIfNotNull(smartList, ConditionalOnBeanUtils.getBeanType(method));
    }
    return smartList;
  }

  private static boolean isClassToSearch(PsiClass candidate) {
    return candidate != null && !Objects.equals(candidate.getQualifiedName(), "java.lang.Object");
  }
}

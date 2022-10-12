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

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.jam.StringLiteralPsiClassConverter;

public class ConditionalOnMissingBean extends ConditionalOnBeanBase implements ConditionalOnJamElement.NonStrict {
  private static final JamClassAttributeMeta.Collection IGNORED_ATTRIBUTE = JamAttributeMeta.classCollection("ignored");
  private static final JamStringAttributeMeta.Collection<PsiClass> IGNORED_TYPE_ATTRIBUTE = JamAttributeMeta.collectionString("ignoredType", new StringLiteralPsiClassConverter());
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_MISSING_BEAN, ARCHETYPE).addAttribute(IGNORED_ATTRIBUTE)
          .addAttribute(IGNORED_TYPE_ATTRIBUTE);
  private static final SemKey<ConditionalOnMissingBean> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnMissingBean");
  public static final JamClassMeta<ConditionalOnMissingBean> CLASS_META = new JamClassMeta<>(null, ConditionalOnMissingBean.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnMissingBean> METHOD_META = new JamMethodMeta<>(null, ConditionalOnMissingBean.class, SEM_KEY).addAnnotation(ANNOTATION_META);

  @Override
  public ConditionalOnBeanBase.SearchStrategy getSearch() {
    return super.getSearch();
  }

  @Override
  public Collection getParametrizedContainer() {
    return super.getParametrizedContainer();
  }

  @Override
  public Collection getName() {
    return super.getName();
  }

  @Override
  public Collection getAnnotation() {
    return super.getAnnotation();
  }

  @Override
  public Collection getType() {
    return super.getType();
  }

  @Override
  public Collection getValue() {
    return super.getValue();
  }

  public ConditionalOnMissingBean(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }

  public Collection<PsiClass> getIgnored() {
    List<JamClassAttributeElement> attribute = getIgnoredElements();
    return ContainerUtil.map(attribute, JamClassAttributeElement::getValue);
  }

  public Collection<PsiClass> getIgnoredType() {
    List<JamStringAttributeElement<PsiClass>> attribute = getIgnoredTypeElements();
    return ContainerUtil.map(attribute, JamStringAttributeElement::getValue);
  }

  private List<JamClassAttributeElement> getIgnoredElements() {
    return ANNOTATION_META.getAttribute(getPsiElement(), IGNORED_ATTRIBUTE);
  }

  private List<JamStringAttributeElement<PsiClass>> getIgnoredTypeElements() {
    return ANNOTATION_META.getAttribute(getPsiElement(), IGNORED_TYPE_ATTRIBUTE);
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    CommonInfraModel infraModel = context.getUserData(ConditionalOnEvaluationContext.MODEL_KEY);
    if (infraModel == null) {
      return ConditionalOnBeanUtils.getMissingModelOutcome();
    }
    Collection<PsiClass> containers = getValidParametrizedContainers();
    List<CommonInfraBean> ignoredBeans = findIgnoredBeans(infraModel, containers);
    ContainerUtil.addIfNotNull(ignoredBeans, ConditionalOnBeanUtils.getSpringBean(getPsiElement()));
    MatchAnyProcessor processor = new MatchAnyProcessor();
    Collection<PsiType> types = getTypesToMatch();
    if (types.isEmpty() && getName().isEmpty() && getAnnotation().isEmpty()) {
      return ConditionOutcome.noMatch("Bean is not specified using type, name or annotation");
    }
    matchBeansByType(infraModel, types, containers, ignoredBeans, processor);
    if (processor.isMatched()) {
      return ConditionOutcome.noMatch(ConditionMessage.found("bean"));
    }
    MatchAnyProcessor processor2 = new MatchAnyProcessor();
    matchBeansByAnnotation(infraModel, ignoredBeans, processor2);
    if (processor2.isMatched()) {
      return ConditionOutcome.noMatch(ConditionMessage.found("bean"));
    }
    MatchAnyProcessor processor3 = new MatchAnyProcessor();
    matchBeansByName(infraModel, ignoredBeans, processor3);
    if (processor3.isMatched()) {
      return ConditionOutcome.noMatch(ConditionMessage.found("bean"));
    }
    return ConditionOutcome.match(ConditionMessage.didNotFind("bean"));
  }

  private List<CommonInfraBean> findIgnoredBeans(CommonInfraModel infraModel, Collection<PsiClass> containers) {
    SmartList smartList = new SmartList();
    Iterable<PsiClass> ignored = ContainerUtil.concat(getIgnored(), getIgnoredType());
    for (PsiClass psiClass : ignored) {
      if (psiClass != null) {
        PsiClassType classType = PsiTypesUtil.getClassType(psiClass);
        smartList.addAll(ConditionalOnBeanUtils.findBeansByType(infraModel, classType));
        for (PsiClass container : containers) {
          PsiClassType containerType = ConditionalOnBeanUtils.getContainerType(container, classType);
          smartList.addAll(ConditionalOnBeanUtils.findBeansByType(infraModel, containerType));
        }
      }
    }
    return smartList.isEmpty() ? new SmartList() : ContainerUtil.map(smartList, BeanPointer.TO_BEAN);
  }

  private static class MatchAnyProcessor implements Processor<Boolean> {
    private boolean myMatched;

    private MatchAnyProcessor() {
    }

    public boolean process(Boolean matched) {
      this.myMatched = matched.booleanValue();
      return !this.myMatched;
    }

    boolean isMatched() {
      return this.myMatched;
    }
  }
}

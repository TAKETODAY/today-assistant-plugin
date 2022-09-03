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

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiType;
import com.intellij.semantic.SemKey;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;

public class ConditionalOnBean extends ConditionalOnBeanBase implements ConditionalOnJamElement.NonStrict {
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_BEAN, ARCHETYPE);
  private static final SemKey<ConditionalOnBean> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnBean");
  public static final JamClassMeta<ConditionalOnBean> CLASS_META = new JamClassMeta<>(null, ConditionalOnBean.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnBean> METHOD_META = new JamMethodMeta<>(null, ConditionalOnBean.class, SEM_KEY).addAnnotation(ANNOTATION_META);

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

  public ConditionalOnBean(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    CommonInfraModel springModel = context.getUserData(ConditionalOnEvaluationContext.MODEL_KEY);
    if (springModel == null) {
      return ConditionalOnBeanUtils.getMissingModelOutcome();
    }
    Collection<PsiClass> containers = getValidParametrizedContainers();
    CommonInfraBean selfBean = ConditionalOnBeanUtils.getSpringBean(getPsiElement());
    List<CommonInfraBean> ignoredBeans = selfBean != null ? new SmartList<>(selfBean) : Collections.emptyList();
    MatchAllProcessor processor = new MatchAllProcessor();
    Collection<PsiType> types = getTypesToMatch();
    if (types.isEmpty() && getName().isEmpty() && getAnnotation().isEmpty()) {
      return ConditionOutcome.noMatch("Bean is not specified using type, name or annotation");
    }
    matchBeansByType(springModel, types, containers, ignoredBeans, processor);
    if (!processor.isMatched()) {
      return ConditionOutcome.noMatch(ConditionMessage.didNotFind("bean"));
    }
    MatchAllProcessor processor2 = new MatchAllProcessor();
    matchBeansByAnnotation(springModel, ignoredBeans, processor2);
    if (!processor2.isMatched()) {
      return ConditionOutcome.noMatch(ConditionMessage.didNotFind("bean"));
    }
    MatchAllProcessor processor3 = new MatchAllProcessor();
    matchBeansByName(springModel, ignoredBeans, processor3);
    if (!processor3.isMatched()) {
      return ConditionOutcome.noMatch(ConditionMessage.didNotFind("bean"));
    }
    return ConditionOutcome.match(ConditionMessage.found("bean"));
  }

  private static class MatchAllProcessor implements Processor<Boolean> {
    private boolean myMatched = true;

    private MatchAllProcessor() {
    }

    public boolean process(Boolean matched) {
      this.myMatched = matched;
      return this.myMatched;
    }

    boolean isMatched() {
      return this.myMatched;
    }
  }
}

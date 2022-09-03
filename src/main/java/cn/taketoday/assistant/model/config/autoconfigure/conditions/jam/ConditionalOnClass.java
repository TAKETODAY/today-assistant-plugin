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

import com.intellij.jam.JamAttributeElement;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.jam.StringLiteralPsiClassConverter;

public class ConditionalOnClass extends JamBaseElement<PsiModifierListOwner> implements ConditionalOnJamElement {
  private static final JamClassAttributeMeta.Collection VALUE_ATTRIBUTE = JamClassAttributeMeta.Collection.CLASS_COLLECTION_VALUE_META;
  private static final JamStringAttributeMeta.Collection<PsiClass> NAME_ATTRIBUTE = JamAttributeMeta.collectionString(InfraMetadataConstant.NAME, new StringLiteralPsiClassConverter());
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_CLASS).addAttribute(VALUE_ATTRIBUTE)
          .addAttribute(NAME_ATTRIBUTE);
  private static final SemKey<ConditionalOnClass> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnClass");
  public static final JamClassMeta<ConditionalOnClass> CLASS_META = new JamClassMeta<>(null, ConditionalOnClass.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnClass> METHOD_META = new JamMethodMeta<>(null, ConditionalOnClass.class, SEM_KEY).addAnnotation(ANNOTATION_META);

  public ConditionalOnClass(PsiElementRef<?> ref) {
    super(ref);
  }

  public Collection<PsiClass> getValue() {
    List<JamClassAttributeElement> attribute = getValueElements();
    return ContainerUtil.map(attribute, JamClassAttributeElement::getValue);
  }

  public Collection<PsiClass> getName() {
    List<JamStringAttributeElement<PsiClass>> attribute = getNameElements();
    return ContainerUtil.map(attribute, JamStringAttributeElement::getValue);
  }

  @Override

  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    Ref<ConditionOutcome> conditionOutcomeRef = Ref.create();
    Processor<JamAttributeElement<PsiClass>> processor = classJamAttributeElement -> {
      PsiClass psiClass = classJamAttributeElement.getValue();
      if (psiClass == null) {
        String value = classJamAttributeElement.getStringValue();
        conditionOutcomeRef.set(ConditionOutcome.noMatch(ConditionMessage.didNotFindClass(value)));
        return false;
      }
      return true;
    };
    if (!ContainerUtil.process(getValueElements(), processor) || !ContainerUtil.process(getNameElements(), processor)) {
      return conditionOutcomeRef.get();
    }
    return ConditionOutcome.match("All classes present");
  }

  private List<JamClassAttributeElement> getValueElements() {
    List<JamClassAttributeElement> list = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_ATTRIBUTE);
    return list;
  }

  private List<JamStringAttributeElement<PsiClass>> getNameElements() {
    List<JamStringAttributeElement<PsiClass>> list = ANNOTATION_META.getAttribute(getPsiElement(), NAME_ATTRIBUTE);
    return list;
  }
}

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
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalContributor;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalContributors;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;

public class Conditional extends JamBaseElement<PsiModifierListOwner> implements ConditionalOnJamElement {
  private static final JamClassAttributeMeta.Collection VALUE_ATTRIBUTE = JamClassAttributeMeta.Collection.CLASS_COLLECTION_VALUE_META;
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta("cn.taketoday.context.annotation.Conditional").addAttribute(VALUE_ATTRIBUTE);
  private static final SemKey<Conditional> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("Conditional");
  public static final JamClassMeta<Conditional> CLASS_META = new JamClassMeta<>(null, Conditional.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<Conditional> METHOD_META = new JamMethodMeta<>(null, Conditional.class, SEM_KEY).addAnnotation(ANNOTATION_META);

  public Conditional(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    boolean allConditionsEvaluated = true;
    List<JamClassAttributeElement> jamElements = getValueElements();
    for (JamClassAttributeElement jamElement : jamElements) {
      PsiClass valueClass = jamElement.getValue();
      String fqn = valueClass != null ? valueClass.getQualifiedName() : null;
      if (fqn == null) {
        return ConditionOutcome.noMatch(ConditionMessage.generic("Unresolved condition", "class", jamElement.getStringValue()));
      }
      ConditionalContributor conditionalContributor = ConditionalContributors.INSTANCE.findSingle(fqn);
      if (conditionalContributor == null) {
        allConditionsEvaluated = false;
      }
      else {
        String conditionClassName = StringUtil.getShortName(fqn);
        ConditionOutcome conditionOutcome = conditionalContributor.matches(context);
        String conditionText = conditionOutcome.getMessage().getText();
        if (!conditionOutcome.isMatch()) {
          return ConditionOutcome.noMatch("Condition " + conditionClassName + " did not match: " + conditionText);
        }
        else if (jamElements.size() == 1) {
          return ConditionOutcome.match("Condition " + conditionClassName + " matched: " + conditionText);
        }
      }
    }
    if (allConditionsEvaluated) {
      return ConditionOutcome.match("All conditions matched");
    }
    return ConditionOutcome.match("All conditions matched (not all evaluated)");
  }

  public Collection<PsiClass> getValue() {
    return ContainerUtil.map(getValueElements(), JamClassAttributeElement::getValue);
  }

  private List<JamClassAttributeElement> getValueElements() {
    return ANNOTATION_META.getAttribute(getPsiElement(), VALUE_ATTRIBUTE);
  }
}

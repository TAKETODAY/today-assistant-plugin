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
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.lang.Nullable;

public class ConditionalOnManagementPort implements ConditionalOnJamElement {
  private static final SemKey<ConditionalOnManagementPort> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnManagementPort");
  private static final JamEnumAttributeMeta.Single<ManagementPortType> VALUE_ATTRIBUTE_META = JamAttributeMeta.singleEnum("value", ManagementPortType.class);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_MANAGEMENT_PORT).addAttribute(VALUE_ATTRIBUTE_META);
  public static final JamClassMeta<ConditionalOnManagementPort> CLASS_META = new JamClassMeta<>(null, ConditionalOnManagementPort.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnManagementPort> METHOD_META = new JamMethodMeta<>(null, ConditionalOnManagementPort.class, SEM_KEY).addAnnotation(ANNOTATION_META);

  private static final String MANAGEMENT_SERVER_PORT = "management.server.port";
  private static final int MANAGEMENT_SERVER_DEFAULT_PORT = 8080;

  private static final String SERVER_PORT = "server.port";
  private final PsiElementRef<PsiAnnotation> myAnnotationRef;

  enum ManagementPortType {
    DISABLED,
    SAME,
    DIFFERENT
  }

  public ConditionalOnManagementPort(PsiModifierListOwner owner) {
    this.myAnnotationRef = ANNOTATION_META.getAnnotationRef(owner);
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    if (!isWebApplication(context)) {
      return ConditionOutcome.noMatch("Not a (reactive) web application");
    }
    ManagementPortType actualType = getValue();
    if (actualType == null) {
      return ConditionOutcome.noMatch("Invalid 'value'");
    }
    ManagementPortType requiredType = ManagementPortType.DIFFERENT;
    String managementServerPortValue = findConfigurationValue(context, MANAGEMENT_SERVER_PORT);
    if (StringUtil.isEmptyOrSpaces(managementServerPortValue)) {
      requiredType = ManagementPortType.SAME;
    }
    else {
      try {
        int managementPort = Integer.parseInt(managementServerPortValue);
        if (managementPort < 0) {
          requiredType = ManagementPortType.DISABLED;
        }
        else {
          String serverPortValue = findConfigurationValue(context, SERVER_PORT);
          if (serverPortValue == null) {
            if (managementPort == MANAGEMENT_SERVER_DEFAULT_PORT) {
              requiredType = ManagementPortType.SAME;
            }
          }
          else if (managementPort != 0) {
            try {
              int serverPort = Integer.parseInt(serverPortValue);
              if (managementPort == serverPort) {
                requiredType = ManagementPortType.SAME;
              }
            }
            catch (NumberFormatException e) {
              return ConditionOutcome.noMatch(ConditionMessage.foundConfigKeyWithValue(SERVER_PORT, serverPortValue));
            }
          }
        }
      }
      catch (NumberFormatException e2) {
        return ConditionOutcome.noMatch(ConditionMessage.foundConfigKeyWithValue(MANAGEMENT_SERVER_PORT, managementServerPortValue));
      }
    }
    if (actualType == requiredType) {
      return ConditionOutcome.match("Management Port Type '" + actualType + "' matched required type");
    }
    return ConditionOutcome.noMatch("Management Port Type '" + actualType + "' did not match required type '" + requiredType + "'");
  }

  private static String findConfigurationValue(ConditionalOnEvaluationContext context, String configKey) {
    Ref<String> configuredValueRef = Ref.create();
    Processor<List<ConfigurationValueResult>> findValue = results -> {
      ConfigurationValueResult item = ContainerUtil.getFirstItem(results);
      if (item != null) {
        configuredValueRef.set(item.getValueText());
        return false;
      }
      return false;
    };
    context.processConfigurationValues(findValue, true, configKey);
    return configuredValueRef.get();
  }

  private static boolean isWebApplication(ConditionalOnEvaluationContext context) {
    ConditionOutcome servletOutcome = ConditionalOnWebApplication.getServletOutcome(context);
    if (servletOutcome.isMatch()) {
      return true;
    }
    ConditionOutcome reactiveOutcome = ConditionalOnWebApplication.getReactiveOutcome(context);
    return reactiveOutcome.isMatch();
  }

  @Nullable
  public ManagementPortType getValue() {
    return VALUE_ATTRIBUTE_META.getJam(this.myAnnotationRef).getValue();
  }
}

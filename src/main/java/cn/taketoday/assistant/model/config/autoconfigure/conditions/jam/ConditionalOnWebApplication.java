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
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.scope.InfraCustomBeanScope;
import cn.taketoday.assistant.util.InfraUtils;

public class ConditionalOnWebApplication implements ConditionalOnJamElement {
  private static final String WEB_APPLICATION_CONTEXT = "cn.taketoday.web.context.support.GenericWebApplicationContext";

  private static final String WEB_FLUX_CONFIGURER = "cn.taketoday.web.reactive.config.WebFluxConfigurer";

  private static final String WEB_MVC_CONFIGURER = "cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer";

  private static final String WEB_APPLICATION_TYPE_KEY = "context.main.application-type";
  private final PsiElementRef<PsiAnnotation> myAnnotationRef;
  private static final JamEnumAttributeMeta.Single<Type> TYPE_ATTRIBUTE_META = JamAttributeMeta.singleEnum(InfraMetadataConstant.TYPE, Type.class);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_WEB_APPLICATION).addAttribute(TYPE_ATTRIBUTE_META);
  private static final SemKey<ConditionalOnWebApplication> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnWebApplication");
  public static final JamClassMeta<ConditionalOnWebApplication> CLASS_META = new JamClassMeta<>(null, ConditionalOnWebApplication.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnWebApplication> METHOD_META = new JamMethodMeta<>(null, ConditionalOnWebApplication.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  private static final Condition<BeanScope> SESSION_SCOPE = scope -> {
    return "session".equals(scope.getValue());
  };
  private static final Key<ConditionOutcome> SERVLET_CONDITION_KEY = Key.create("servlet");
  private static final Function<ConditionalOnEvaluationContext, ConditionOutcome> SERVLET_CONDITION = context -> {
    if (InfraUtils.findLibraryClass(context.getModule(), WEB_APPLICATION_CONTEXT) != null) {
      return ConditionOutcome.match(ConditionMessage.foundClass(WEB_APPLICATION_CONTEXT));
    }
    boolean hasSessionScope = hasSessionScope(context.getAutoConfigClass());
    if (hasSessionScope) {
      return ConditionOutcome.match(ConditionMessage.found("scope", "session"));
    }
    return ConditionOutcome.noMatch("Not a web application");
  };
  private static final Key<ConditionOutcome> REACTIVE_CONDITION_KEY = Key.create("reactive");
  private static final Function<ConditionalOnEvaluationContext, ConditionOutcome> REACTIVE_CONDITION = context -> {
    if (InfraUtils.findLibraryClass(context.getModule(), "reactor.core.publisher.Flux") == null || InfraUtils.findLibraryClass(context.getModule(), WEB_FLUX_CONFIGURER) == null) {
      return ConditionOutcome.noMatch(ConditionMessage.didNotFind("library", "Flux/Reactor"));
    }
    if (InfraUtils.findLibraryClass(context.getModule(), WEB_MVC_CONFIGURER) == null) {
      return ConditionOutcome.match(ConditionMessage.found("library", "Flux/Reactor"));
    }
    boolean exists = !context.processConfigurationValues(results -> {
      return !ContainerUtil.exists(results, result -> {
        return Comparing.strEqual(Type.REACTIVE.name(), result.getValueText(), false);
      });
    }, true, WEB_APPLICATION_TYPE_KEY);
    if (exists) {
      return ConditionOutcome.match(ConditionMessage.foundConfigKeyWithValue(WEB_APPLICATION_TYPE_KEY, "reactive"));
    }
    return ConditionOutcome.noMatch("Not a reactive web application");
  };

  enum Type {
    ANY,
    SERVLET,
    REACTIVE
  }

  private static boolean hasSessionScope(PsiClass aClass) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(aClass.getProject());
    for (InfraCustomBeanScope customBeanScope : InfraCustomBeanScope.EP_NAME.getExtensions()) {
      PsiClass scopeClass = javaPsiFacade.findClass(customBeanScope.getScopeClassName(), aClass.getResolveScope());
      if (scopeClass != null) {
        List<BeanScope> customScopes = new ArrayList<>();
        customBeanScope.process(customScopes, Collections.emptySet(), scopeClass, aClass);
        if (ContainerUtil.exists(customScopes, SESSION_SCOPE)) {
          return true;
        }
      }
    }
    return false;
  }

  public ConditionalOnWebApplication(PsiModifierListOwner owner) {
    this.myAnnotationRef = ANNOTATION_META.getAnnotationRef(owner);
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    if (getType() == Type.ANY) {
      ConditionOutcome servletOutcome = getServletOutcome(context);
      if (servletOutcome.isMatch()) {
        return servletOutcome;
      }
      ConditionOutcome reactiveOutcome = getReactiveOutcome(context);
      if (reactiveOutcome.isMatch()) {
        return reactiveOutcome;
      }
      return ConditionOutcome.noMatch("Not a (reactive) web application");
    }
    else if (getType() == Type.REACTIVE) {
      return getReactiveOutcome(context);
    }
    else {
      return getServletOutcome(context);
    }
  }

  static ConditionOutcome getServletOutcome(ConditionalOnEvaluationContext context) {
    return calcOrGetCached(context, SERVLET_CONDITION_KEY, SERVLET_CONDITION);
  }

  static ConditionOutcome getReactiveOutcome(ConditionalOnEvaluationContext context) {
    return calcOrGetCached(context, REACTIVE_CONDITION_KEY, REACTIVE_CONDITION);
  }

  private static ConditionOutcome calcOrGetCached(ConditionalOnEvaluationContext context, Key<ConditionOutcome> key, Function<ConditionalOnEvaluationContext, ConditionOutcome> condition) {
    ConditionOutcome data = context.getUserData(key);
    if (data == null) {
      data = context.putUserDataIfAbsent(key, condition.fun(context));
    }
    return data;
  }

  public Type getType() {
    Type type = TYPE_ATTRIBUTE_META.getJam(this.myAnnotationRef).getValue();
    if (type != null) {
      return type;
    }
    return Type.ANY;
  }
}

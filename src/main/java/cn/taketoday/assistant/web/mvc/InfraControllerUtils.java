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

package cn.taketoday.assistant.web.mvc;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.beans.stereotype.Controller;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.views.UrlBasedViewResolver;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;
import cn.taketoday.lang.Nullable;

public final class InfraControllerUtils {

  public static boolean isController(PsiClass psiClass) {
    Controller controller = JamService.getJamService(psiClass.getProject()).getJamElement(psiClass, Controller.META);
    return controller != null || isInheritedController(psiClass);
  }

  static boolean isInheritedController(PsiClass psiClass) {
    return CachedValuesManager.getCachedValue(psiClass, () -> {
      return CachedValueProvider.Result.createSingleDependency(InheritanceUtil.isInheritor(psiClass, InfraMvcConstant.SERVLET_MVC_CONTROLLER),
              PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public static boolean hasClassLevelResponseBody(PsiClass aClass) {
    PsiClass metaControllerAnnoClass;
    Controller controller = JamService.getJamService(aClass.getProject()).getJamElement(aClass, Controller.META);
    if (controller == null) {
      return false;
    }
    String definingAnnotation = controller.getDefiningAnnotation();
    return !AnnotationConstant.CONTROLLER.equals(definingAnnotation)
            && (metaControllerAnnoClass = JavaPsiFacade.getInstance(aClass.getProject()).findClass(definingAnnotation, controller.getPsiElement().getResolveScope())) != null
            && AnnotationUtil.isAnnotated(metaControllerAnnoClass, InfraMvcConstant.RESPONSE_BODY, 0);
  }

  public static boolean isRequestHandler(PsiMethod method) {
    return isRequestHandler(method.getContainingClass(), method);
  }

  static boolean isRequestHandler(@Nullable PsiClass controller, PsiMethod method) {
    if (controller != null && isRequestHandlerCandidate(method)) {
      if (!isInheritedController(controller)) {
        return hasRequestMappingJam(method);
      }
      return "handleRequest".equals(method.getName()) || "handleRequestInternal".equals(method.getName());
    }
    return false;
  }

  public static boolean isRequestHandlerCandidate(PsiMethod method) {
    return !method.hasModifierProperty("static") && !method.isConstructor();
  }

  public static boolean isJamRequestHandler(PsiMethod method) {
    return isRequestHandlerCandidate(method) && hasRequestMappingJam(method);
  }

  private static boolean hasRequestMappingJam(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(RequestMapping.METHOD_JAM_KEY, method) != null;
  }

  public static boolean isModelAttributeProvider(PsiMethod method) {
    return AnnotationUtil.isAnnotated(method, InfraMvcConstant.MODEL_ATTRIBUTE, 0);
  }

  public static ViewResolver createURLBasedViewResolver(Module module, BeanPointer<?> templateResolver) {
    CommonInfraBean bean = templateResolver.getBean();
    return createURLBasedViewResolver(module, InfraPropertyUtils.getPropertyStringValue(bean, "prefix"), InfraPropertyUtils.getPropertyStringValue(bean, "suffix"));
  }

  public static ViewResolver createURLBasedViewResolver(Module module, @Nullable String prefix, @Nullable String suffix) {
    return new UrlBasedViewResolver(module, "thymeleaf", "", prefix, suffix);
  }

  @Nullable
  public static String getVariableName(@Nullable PsiType psiType) {
    if (psiType instanceof PsiClassType psiClassType) {
      PsiClass psiClass = psiClassType.resolve();
      if (psiClass == null) {
        return null;
      }
      if ((psiClassType.getParameterCount() == 1 && InheritanceUtil.isInheritor(psiClass, "java.util.List")) || InheritanceUtil.isInheritor(psiClass, "java.util.Set")) {
        PsiType collectionElementType = psiClassType.getParameters()[0];
        return pluralize(getComponentName(collectionElementType));
      }
      return StringUtil.decapitalize(StringUtil.notNullize(psiClass.getName()));
    }
    else if (psiType instanceof PsiArrayType psiArrayType) {
      PsiType arrayElementType = psiArrayType.getComponentType();
      return pluralize(getComponentName(arrayElementType));
    }
    else {
      return null;
    }
  }

  @Nullable
  private static String getComponentName(PsiType psiType) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
    if (psiClass != null) {
      return StringUtil.decapitalize(StringUtil.notNullize(psiClass.getName()));
    }
    return null;
  }

  @Nullable
  private static String pluralize(@Nullable String componentName) {
    if (componentName == null) {
      return null;
    }
    return componentName + "List";
  }
}

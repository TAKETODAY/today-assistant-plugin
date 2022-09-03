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

package cn.taketoday.assistant.web.mvc.model.mappings.processors;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.Processor;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.mappings.UrlMappingPsiBasedElement;
import cn.taketoday.lang.Nullable;

public final class WebMvcConfigurationRequestMappingProcessor {

  public static boolean processWebMvcSupport(Processor<UrlMappingElement> requestMappings, Module module, Collection<InfraModel> models) {
    String[] codeConfigurers = { InfraMvcConstant.WEB_MVC_CONFIGURATION_SUPPORT, InfraMvcConstant.WEB_MVC_CONFIGURER };
    for (String codeConfigurer : codeConfigurers) {
      ModelSearchParameters.BeanClass mvcConfigurationSupportSearchParameters = XmlDefinitionMappingProcessor.createSearchParams(module, codeConfigurer);
      if (mvcConfigurationSupportSearchParameters != null) {
        for (InfraModel model : models) {
          for (BeanPointer pointer : InfraModelSearchers.findBeans(model, mvcConfigurationSupportSearchParameters)) {
            if (!processConfigurationClass(pointer.getBeanClass(), requestMappings)) {
              return false;
            }
          }
        }
        continue;
      }
    }
    return true;
  }

  public static boolean processConfigurationClass(@Nullable PsiClass configClass, Processor<UrlMappingElement> processor) {
    UMethod configureViewResolvers;
    if (configClass == null || (configureViewResolvers = UastContextKt.toUElement(configClass.findMethodBySignature(
            new LightMethodBuilder(configClass.getManager(), "addViewControllers").setModifiers("protected").setMethodReturnType(PsiType.VOID)
                    .addParameter("registry", InfraMvcConstant.VIEW_CONTROLLER_REGISTRY), false), UMethod.class)) == null) {
      return true;
    }
    configureViewResolvers.accept(new AbstractUastVisitor() {

      public boolean visitCallExpression(UCallExpression expression) {
        UExpression qualifierExpression = expression.getReceiver();
        if (qualifierExpression == null) {
          return super.visitCallExpression(expression);
        }
        PsiType qualifierType = qualifierExpression.getExpressionType();
        PsiClass qualifierClass = PsiTypesUtil.getPsiClass(qualifierType);
        if (qualifierClass == null || !InfraMvcConstant.VIEW_CONTROLLER_REGISTRY.equals(qualifierClass.getQualifiedName())) {
          return super.visitCallExpression(expression);
        }
        processControllers(expression, expression.getMethodName());
        return super.visitCallExpression(expression);
      }

      private boolean processControllers(UCallExpression expression, String methodName) {
        return "addViewController".equals(methodName) ? processAddViewController(expression, processor) : "addRedirectViewController".equals(
                methodName) ? processRedirectViewController(expression, processor) : !"addStatusController".equals(
                methodName) || processStatusController(expression, processor);
      }
    });
    return true;
  }

  private static boolean processAddViewController(UCallExpression expression, Processor<UrlMappingElement> processor) {
    List<UExpression> arguments = expression.getValueArguments();
    return arguments.size() != 1 || processUrlFromFirstArgument(arguments.get(0), processor);
  }

  private static boolean processRedirectViewController(UCallExpression expression, Processor<UrlMappingElement> processor) {
    List<UExpression> arguments = expression.getValueArguments();
    return arguments.size() != 2 || processUrlFromFirstArgument(arguments.get(0), processor);
  }

  private static boolean processStatusController(UCallExpression expression, Processor<UrlMappingElement> processor) {
    List<UExpression> arguments = expression.getValueArguments();
    return arguments.size() != 2 || processUrlFromFirstArgument(arguments.get(0), processor);
  }

  private static boolean processUrlFromFirstArgument(UExpression argument, Processor<UrlMappingElement> processor) {
    String urlPathExpr = UastUtils.evaluateString(argument);
    return urlPathExpr == null || processor.process(createUrlMapping(urlPathExpr, argument.getSourcePsi()));
  }

  private static UrlMappingElement createUrlMapping(String path, PsiElement definition, RequestMethod... requestMethods) {
    return new UrlMappingPsiBasedElement(path, definition, null, path, requestMethods);
  }
}

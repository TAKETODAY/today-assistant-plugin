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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ObjectUtils;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.views.BeanNameViewResolverFactory;
import cn.taketoday.assistant.web.mvc.views.UrlBasedViewResolver;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;
import cn.taketoday.assistant.web.mvc.views.ViewResolverFactory;
import cn.taketoday.lang.Nullable;
import one.util.streamex.StreamEx;

public class CodeConfigurationViewResolverParser extends CodeConfigurationParserBase {
  private final List<? super ViewResolver> myViewResolvers;

  public CodeConfigurationViewResolverParser(CommonInfraModel servletModel, BeanPointer<?> configBeanPointer, List<? super ViewResolver> resolvers) {
    super(servletModel, configBeanPointer);
    this.myViewResolvers = resolvers;
  }

  @Override
  protected boolean parseConfigurationClass(PsiClass configClass) {
    UMethod configureViewResolvers = UastContextKt.toUElement(configClass.findMethodBySignature(
            new LightMethodBuilder(configClass.getManager(), "configureViewResolvers")
                    .setModifiers("protected")
                    .setMethodReturnType(PsiType.VOID)
                    .addParameter("registry", InfraMvcConstant.VIEW_RESOLVER_REGISTRY), false), UMethod.class);
    if (configureViewResolvers == null) {
      return false;
    }
    List<ViewResolverFactory> viewResolverFactoryEPs = ViewResolverFactory.EP_NAME.getExtensionList();
    Module module = ModuleUtilCore.findModuleForPsiElement(configClass);
    configureViewResolvers.accept(new AbstractUastVisitor() {

      public boolean visitCallExpression(UCallExpression expression) {
        UExpression qualifierExpression = expression.getReceiver();
        if (qualifierExpression == null) {
          return super.visitCallExpression(expression);
        }
        PsiType qualifierType = qualifierExpression.getExpressionType();
        PsiClass qualifierClass = PsiTypesUtil.getPsiClass(qualifierType);
        if (qualifierClass == null || !InfraMvcConstant.VIEW_RESOLVER_REGISTRY.equals(qualifierClass.getQualifiedName())) {
          return super.visitCallExpression(expression);
        }
        String methodName = expression.getMethodName();
        if ("jsp".equals(methodName)) {
          CodeConfigurationViewResolverParser.this.handleJsp(module, expression);
        }
        else if ("beanName".equals(methodName)) {
          CodeConfigurationViewResolverParser.this.handleBeanName(module);
        }
        else {
          for (ViewResolverFactory viewResolver : viewResolverFactoryEPs) {
            Set<ViewResolver> viewResolvers = viewResolver.handleResolversRegistry(methodName, expression, CodeConfigurationViewResolverParser.this.myServletModel);
            if (!viewResolvers.isEmpty()) {
              CodeConfigurationViewResolverParser.this.myViewResolvers.addAll(viewResolvers);
              break;
            }
          }
        }
        return super.visitCallExpression(expression);
      }
    });
    return true;
  }

  private static StreamEx<UCallExpression> getChainCalls(UCallExpression callExpression) {
    return StreamEx.iterate(callExpression.getUastParent(), UQualifiedReferenceExpression.class::isInstance, UElement::getUastParent)
            .select(UQualifiedReferenceExpression.class)
            .map(UQualifiedReferenceExpression::getSelector)
            .select(UCallExpression.class);
  }

  private void handleJsp(@Nullable Module module, UCallExpression expression) {
    String value;
    String value2;
    if (module == null) {
      return;
    }
    List<UExpression> arguments = expression.getValueArguments();
    if (arguments.size() == 0) {
      String prefix = "";
      String suffix = ".jsp";
      for (UCallExpression callExpression : getChainCalls(expression)) {
        String methodName = callExpression.getMethodName();
        if ("prefix".equals(methodName) && (value2 = ObjectUtils.doIfNotNull(callExpression.getArgumentForParameter(0), UastUtils::evaluateString)) != null) {
          prefix = value2;
        }
        if ("suffix".equals(methodName) && (value = ObjectUtils.doIfNotNull(callExpression.getArgumentForParameter(0), UastUtils::evaluateString)) != null) {
          suffix = value;
        }
      }
      this.myViewResolvers.add(new UrlBasedViewResolver(module, "CodeConfigurationViewResolverParser.jsp()", prefix.isEmpty() ? InfraMvcConstant.WEB_INF : "", prefix, suffix));
    }
    else if (arguments.size() == 2) {
      String prefixExpr = UastUtils.evaluateString(arguments.get(0));
      String suffixExpr = UastUtils.evaluateString(arguments.get(1));
      if (prefixExpr != null && suffixExpr != null) {
        this.myViewResolvers.add(new UrlBasedViewResolver(module, "CodeConfigurationViewResolverParser.jsp()", "", prefixExpr, suffixExpr));
      }
    }
  }

  private void handleBeanName(@Nullable Module module) {
    if (module == null) {
      return;
    }
    this.myViewResolvers.add(new BeanNameViewResolverFactory.BeanNameViewResolver(module, "CodeConfigurationViewResolverParser.beanName()"));
  }
}

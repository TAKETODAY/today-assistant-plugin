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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Processor;

import org.jetbrains.uast.UCallExpression;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.lang.Nullable;

public abstract class ViewResolverFactory {
  public static final ExtensionPointName<ViewResolverFactory> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.viewResolverFactory");

  @Nullable
  protected abstract String getBeanClass();

  public Set<ViewResolver> createResolvers(@Nullable CommonInfraBean bean, PsiClass beanClass, CommonInfraModel model) {
    return isMine(bean, beanClass) ? createViewResolvers(bean, model) : Collections.emptySet();
  }

  public Set<ViewResolver> handleResolversRegistry(String methodName, UCallExpression methodCallExpression, CommonInfraModel servletModel) {
    return Collections.emptySet();
  }

  protected boolean isMine(@Nullable CommonInfraBean bean, PsiClass beanClass) {
    String baseClassName = getBeanClass();
    return baseClassName != null && ReadAction.compute(() -> InheritanceUtil.isInheritor(beanClass, baseClassName));
  }

  protected Set<ViewResolver> createViewResolvers(@Nullable CommonInfraBean bean, CommonInfraModel model) {
    return Collections.emptySet();
  }

  public static void findResolversByClass(PsiClass psiClass, CommonInfraBean viewResolvers, CommonInfraModel model, List<? super ViewResolver> result,
          Predicate<? super ViewResolverFactory> predicate) {
    for (ViewResolverFactory factory : EP_NAME.getExtensionList()) {
      if (predicate.test(factory) && factory.isMine(viewResolvers, psiClass)) {
        result.addAll(factory.createResolvers(viewResolvers, psiClass, model));
        return;
      }
    }
  }

  public static boolean processAllMineFactories(@Nullable CommonInfraBean bean, PsiClass viewResolverClass, Processor<? super ViewResolverFactory> processor) {
    for (ViewResolverFactory factory : ViewResolverRegistry.getInstance().getAllFactories()) {
      if (factory.isMine(bean, viewResolverClass) && !processor.process(factory)) {
        return false;
      }
    }
    return true;
  }
}

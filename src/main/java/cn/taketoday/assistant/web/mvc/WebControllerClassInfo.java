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

import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.intellij.util.containers.MultiMap;

import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastLiteralUtils;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import cn.taketoday.assistant.web.mvc.views.InfraMVCViewUastReferenceProvider;

public final class WebControllerClassInfo {

  public static MultiMap<String, PsiVariable> getVariables(PsiClass psiClass) {
    return ProgressIndicatorUtils.awaitWithCheckCanceled(Promises.asCompletableFuture(getVariablesAsync(psiClass)));
  }

  public static Promise<MultiMap<String, PsiVariable>> getVariablesAsync(PsiClass psiClass) {
    return WebControllerModelVariablesCollector.getVariables(psiClass);
  }

  private static class UastViewsCollector extends AbstractUastVisitor {
    private final MultiMap<String, UMethod> myResult = new MultiMap<>();

    public boolean visitElement(UElement uElement) {
      String name;
      if ((uElement instanceof UReferenceExpression) || UastLiteralUtils.isInjectionHost(uElement)) {
        if (InfraMVCViewUastReferenceProvider.VIEW_PATTERN.accepts(uElement) && (name = UastUtils.evaluateString((UExpression) uElement)) != null) {
          this.myResult.putValue(name, UastUtils.getParentOfType(uElement, UMethod.class));
          return false;
        }
        return false;
      }
      super.visitElement(uElement);
      return false;
    }

    private MultiMap<String, UMethod> getResult() {
      return this.myResult;
    }
  }

  public static MultiMap<String, UMethod> getViews(UMethod method) {
    UastViewsCollector visitor = new UastViewsCollector();
    method.accept(visitor);
    return visitor.getResult();
  }

  public static MultiMap<String, UMethod> getViews(PsiClass myClass) {
    UastViewsCollector visitor = new UastViewsCollector();
    for (PsiElement psiElement : myClass.getAllMethods()) {
      UMethod uMethod = UastContextKt.toUElement(psiElement, UMethod.class);
      if (uMethod != null) {
        uMethod.accept(visitor);
      }
    }
    return visitor.getResult();
  }
}

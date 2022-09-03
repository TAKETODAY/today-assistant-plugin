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

package cn.taketoday.assistant.web.mvc.client.rest;

import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.USimpleNameReferenceExpression;

import java.util.List;

import cn.taketoday.assistant.web.mvc.client.WebClientModel;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;

public abstract class RestTemplatesModel extends WebClientModel {
  @Override
  @Nullable
  public String findHttpMethod(UCallExpression urlNode) {
    Intrinsics.checkNotNullParameter(urlNode, "urlNode");
    PsiMethod resolve = urlNode.resolve();
    String methodName = resolve != null ? resolve.getName() : null;
    if (methodName == null) {
      return null;
    }
    if (Intrinsics.areEqual(methodName, RestOperationsConstants.EXECUTE_METHOD) || Intrinsics.areEqual(methodName, RestOperationsConstants.EXCHANGE_METHOD)) {
      UExpression parameter = urlNode.getArgumentForParameter(1);
      if (parameter instanceof USimpleNameReferenceExpression expression) {
        return expression.getResolvedName();
      }
      if (!(parameter instanceof UQualifiedReferenceExpression)) {
        return null;
      }
      return ((UQualifiedReferenceExpression) parameter).getResolvedName();
    }
    for (RestOperation restOperation : RestOperationsConstants.INSTANCE.getREST_OPERATIONS_METHODS()) {
      if (Intrinsics.areEqual(restOperation.getMethod(), methodName)) {
        return restOperation.getType();
      }
    }
    return null;
  }

  @Override

  public String findBaseUrl(@Nullable UExpression uExpression) {
    return "";
  }

  @Override

  protected List<String> getPackageBlackList() {
    return RestOperationsConstants.INSTANCE.getPACKAGE_SEARCH_BLACK_LIST();
  }
}

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

package cn.taketoday.assistant.web.mvc.pathVariables;

import com.intellij.microservices.jvm.pathvars.PathVariableMethodInspection;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;

import org.jetbrains.uast.UMethod;

import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.model.WebUrlPathSpecificationProviderKt;

public final class MVCPathVariableInspection extends PathVariableMethodInspection {

  @Override
  public boolean isApplicable(Module module) {
    return InfraUtils.isEnabledModule(module);
  }

  @Override
  public UrlPathContext getUrlPathContext(UMethod method) {
    PsiElement javaPsi = method.getJavaPsi();
    UrlPathContext urlPathContext = WebUrlPathSpecificationProviderKt.getFrameworkUrlPathSpecification().getUrlPathContext(javaPsi);
    if (!urlPathContext.isEmpty()) {
      return urlPathContext;
    }
    return InfraExchangeUrlPathSpecification.INSTANCE.getUrlPathContext(javaPsi);
  }

  public String getPathVariableAnnotationFQN() {
    return InfraMvcConstant.PATH_VARIABLE;
  }
}

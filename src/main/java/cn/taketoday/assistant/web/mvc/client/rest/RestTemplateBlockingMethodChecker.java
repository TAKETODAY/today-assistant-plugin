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

import com.intellij.codeInspection.blockingCallsDetection.BlockingMethodChecker;
import com.intellij.codeInspection.blockingCallsDetection.MethodContext;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;

import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public final class RestTemplateBlockingMethodChecker implements BlockingMethodChecker {

  @Override
  public boolean isApplicable(PsiFile file) {
    return InfraLibraryUtil.hasLibrary(file.getProject())
            && JavaPsiFacade.getInstance(file.getProject()).findClass(InfraMvcConstant.REST_OPERATIONS, file.getResolveScope()) != null;
  }

  @Override
  public boolean isMethodBlocking(MethodContext methodContext) {
    Set<RestOperation> restOperations = RestOperationsConstants.REST_OPERATIONS_METHODS;
    if (!restOperations.isEmpty()) {
      for (RestOperation restOperation : restOperations) {

        if (Objects.equals(restOperation.getMethod(), methodContext.getElement().getName())) {
          PsiClass containingClass = methodContext.getElement().getContainingClass();
          String qualifiedClassName = containingClass != null ? containingClass.getQualifiedName() : null;
          return Objects.equals(qualifiedClassName, InfraMvcConstant.REST_OPERATIONS)
                  || Objects.equals(qualifiedClassName, InfraMvcConstant.REST_TEMPLATE);
        }
      }
    }
    return false;
  }
}

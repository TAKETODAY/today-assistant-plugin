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

package cn.taketoday.assistant.model.utils.search.executors;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiType;
import com.intellij.util.Processor;

import java.util.Collection;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;

public abstract class AbstractBeanQueryExecutor extends QueryExecutorBase<BeanPointer<?>, BeanSearchParameters.BeanClass> {

  public static boolean processEffectiveBeanTypes(Collection<? extends BeanPointer<?>> pointers, BeanSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    for (BeanPointer<?> pointer : pointers) {
      ProgressManager.checkCanceled();
      for (PsiType psiType : pointer.getEffectiveBeanTypes()) {
        if (params.matchesClass(psiType) && !processor.process(pointer)) {
          return false;
        }
      }
    }
    return true;
  }
}

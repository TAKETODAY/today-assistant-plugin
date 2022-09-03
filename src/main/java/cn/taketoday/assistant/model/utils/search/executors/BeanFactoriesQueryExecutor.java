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

import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;

import java.util.Collection;

import cn.taketoday.assistant.index.InfraXmlBeansIndex;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;

public class BeanFactoriesQueryExecutor extends AbstractBeanQueryExecutor {
  public static final BeanFactoriesQueryExecutor INSTANCE = new BeanFactoriesQueryExecutor();

  public void processQuery(BeanSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> consumer) {
    if (!processEffectiveBeanTypes(getIndexedFactoryBeans(params), params, consumer)) {
      return;
    }
    processEffectiveBeanTypes(getIndexedFactoryMethods(params), params, consumer);
  }

  private static Collection<BeanPointer<?>> getIndexedFactoryBeans(BeanSearchParameters.BeanClass params) {
    CommonProcessors.CollectProcessor<BeanPointer<?>> collectBeans = new CommonProcessors.CollectProcessor<>();
    InfraXmlBeansIndex.processFactoryBeans(params, collectBeans);
    return collectBeans.getResults();
  }

  private static Collection<BeanPointer<?>> getIndexedFactoryMethods(BeanSearchParameters.BeanClass params) {
    CommonProcessors.CollectProcessor<BeanPointer<?>> collectMethods = new CommonProcessors.CollectProcessor<>();
    InfraXmlBeansIndex.processFactoryMethods(params, collectMethods);
    return collectMethods.getResults();
  }
}

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

package cn.taketoday.assistant.context.model;

import com.intellij.util.ExecutorsQuery;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.lang.Nullable;

abstract class InfraExecutorsQueryNoCacheProcessor<InParams extends ModelSearchParameters, OutParams extends BeanSearchParameters> extends NoCacheProcessor<InParams> {
  protected abstract ExecutorsQuery<BeanPointer<?>, OutParams> createQuery(InParams inparams);

  @Override
  protected Collection<BeanPointer<?>> findPointers(InParams parameters) {
    Collection<BeanPointer<?>> results = new SmartList<>();
    Processor<BeanPointer<?>> collectProcessor = Processors.cancelableCollectProcessor(results);
    createQuery(parameters).forEach(collectProcessor);
    return results.isEmpty() ? Collections.emptyList() : results;
  }

  @Override
  @Nullable
  protected BeanPointer<?> findFirstPointer(InParams parameters) {
    return createQuery(parameters).findFirst();
  }
}

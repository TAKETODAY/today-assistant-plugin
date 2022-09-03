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

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.Processor;
import com.intellij.util.Processors;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;

import static cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext.context;
import static cn.taketoday.assistant.context.model.visitors.InfraModelVisitors.visitRelatedModels;

public abstract class AbstractProcessableModel extends UserDataHolderBase implements CommonInfraModel {

  @Override
  public boolean processByClass(ModelSearchParameters.BeanClass params,
          Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch())
      return true;
    return visitRelatedModels(this, context(processor, (model, p) -> model.processByClass(params, p)), false);
  }

  @Override
  public boolean processByName(ModelSearchParameters.BeanName params,
          Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch())
      return true;
    return visitRelatedModels(this, context(processor, (m, p) -> m.processByName(params, p)), false);
  }

  @Override

  public Collection<BeanPointer<?>> getAllCommonBeans() {
    Set<BeanPointer<?>> pointers = new LinkedHashSet<>();
    processAllBeans(Processors.cancelableCollectProcessor(pointers));
    return Collections.unmodifiableSet(pointers);
  }

  @Override
  public boolean processAllBeans(Processor<? super BeanPointer<?>> processor) {
    return visitRelatedModels(this, context(processor, (m, p) -> m.processAllBeans(p)), false);
  }
}

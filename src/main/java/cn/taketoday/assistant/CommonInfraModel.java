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

package cn.taketoday.assistant;

import com.intellij.openapi.module.Module;
import com.intellij.util.Processor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 13:02
 */
public interface CommonInfraModel {

  /**
   * Infra models:
   * - have connections/links/associations to other models (via imports/component-scans/implicit fileset configurations/auto-configurations/etc.)
   * - contain other models {@link InfraModel}
   *
   * @return related models
   * @see InfraModelVisitors#visitRelatedModels(CommonInfraModel, InfraModelVisitorContext)
   */
  default Set<CommonInfraModel> getRelatedModels() {
    return Collections.emptySet();
  }

  /**
   * @param params Search parameters.
   * @param processor Results processor.
   * @return {@code true} to continue processing or {@code false} to stop.
   * @see InfraModelSearchers
   * @see ModelSearchParameters#canSearch()
   */
  boolean processByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor);

  /**
   * @param params Search parameters.
   * @param processor Results processor.
   * @return {@code true} to continue processing or {@code false} to stop.
   * @see InfraModelSearchers
   * @see ModelSearchParameters#canSearch()
   */
  boolean processByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor);

  /**
   * @param processor Results processor.
   * @return {@code true} to continue processing or {@code false} to stop.
   * @see InfraModelSearchers
   */
  default boolean processAllBeans(Processor<? super BeanPointer<?>> processor) {
    return true;
  }

  /**
   * NOTE: Expensive operation. Consider using {@code process...()} methods instead.
   *
   * @return All beans.
   */
  Collection<BeanPointer<?>> getAllCommonBeans();

  /**
   * Returns the associated module.
   *
   * @return Module containing this model or {@code null} if model is not bound to a specific module (e.g. Project-global).
   */
  @Nullable
  Module getModule();

  @Nullable
  Set<String> getActiveProfiles();

}

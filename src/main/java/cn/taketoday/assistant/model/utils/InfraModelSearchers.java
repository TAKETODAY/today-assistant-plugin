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

package cn.taketoday.assistant.model.utils;

import com.intellij.psi.PsiClass;
import com.intellij.util.CommonProcessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.lang.Nullable;

/**
 * Provides convenience methods to find or check existence of beans with specific type/name.
 * <p/>
 * All methods must be invoked under read-action.
 */
public final class InfraModelSearchers {

  @SuppressWarnings("unchecked")
  public static List<BeanPointer<?>> findBeans(CommonInfraModel model, ModelSearchParameters.BeanClass parameters) {
    if (!parameters.canSearch()) {
      return Collections.emptyList();
    }

    ExplicitRedefinitionAwareBeansCollector processor = new ExplicitRedefinitionAwareBeansCollector();
    model.processByClass(parameters, processor);

    return new ArrayList<>(processor.getResult());
  }

  @Nullable
  public static BeanPointer<?> findBean(CommonInfraModel model, String beanName) {
    var findFirstProcessor = new CommonProcessors.FindFirstProcessor<BeanPointer<?>>();
    model.processByName(ModelSearchParameters.byName(beanName), findFirstProcessor);
    return findFirstProcessor.getFoundValue();
  }

  public static Collection<BeanPointer<?>> findBeans(CommonInfraModel model, String beanName) {
    CommonProcessors.CollectProcessor<BeanPointer<?>> collectProcessor = new CommonProcessors.CollectProcessor<>();

    model.processByName(ModelSearchParameters.byName(beanName), collectProcessor);
    return collectProcessor.getResults();
  }

  public static boolean doesBeanExist(CommonInfraModel model, ModelSearchParameters.BeanClass parameters) {
    return containsBean(model, parameters);
  }

  /**
   * Checks whether a bean with the given class (actual, inheritor or effective type) exists.
   * <p/>
   * NOTE: Expensive operation. Use {@link #doesBeanExist(CommonInfraModel, ModelSearchParameters.BeanClass)} for "plain" class
   * search.
   */
  public static boolean doesBeanExist(CommonInfraModel model, PsiClass beanClass) {
    return containsBean(model, ModelSearchParameters.byClass(beanClass).withInheritors().effectiveBeanTypes());
  }

  private static boolean containsBean(CommonInfraModel model, ModelSearchParameters.BeanClass parameters) {
    if (!parameters.canSearch()) {
      return false;
    }

    CommonProcessors.FindFirstProcessor<BeanPointer<?>> findFirstProcessor =
            new CommonProcessors.FindFirstProcessor<>();

    model.processByClass(parameters, findFirstProcessor);

    return findFirstProcessor.isFound();
  }
}
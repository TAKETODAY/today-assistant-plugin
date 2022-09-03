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

import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.lang.Nullable;

public abstract class NoCacheProcessor<InParams extends ModelSearchParameters> {

  protected abstract Collection<BeanPointer<?>> findPointers(InParams inparams);

  @Nullable
  protected abstract BeanPointer<?> findFirstPointer(InParams inparams);

  public boolean process(InParams params, Processor<? super BeanPointer<?>> processor, Set<String> activeProfiles) {
    if (processor instanceof CommonProcessors.FindFirstProcessor) {
      return processBeansInActiveProfile(processor, findFirstPointer(params), activeProfiles);
    }
    for (BeanPointer pointer : findPointers(params)) {
      if (!processBeansInActiveProfile(processor, pointer, activeProfiles)) {
        return false;
      }
    }
    return true;
  }

  private static boolean processBeansInActiveProfile(Processor<? super BeanPointer<?>> processor, @Nullable BeanPointer<?> pointer, Set<String> activeProfiles) {
    if (pointer == null) {
      return true;
    }
    if (ContainerUtil.isEmpty(activeProfiles)) {
      return processor.process(pointer);
    }
    if (ProfileUtils.isInActiveProfiles(pointer.getBean(), activeProfiles)) {
      return processor.process(pointer);
    }
    return true;
  }
}

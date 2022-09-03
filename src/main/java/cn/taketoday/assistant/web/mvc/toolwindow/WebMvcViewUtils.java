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

package cn.taketoday.assistant.web.mvc.toolwindow;

import com.intellij.openapi.module.Module;
import com.intellij.util.Processor;

import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.services.WebMvcService;
import cn.taketoday.assistant.web.mvc.services.WebMvcUtils;
import cn.taketoday.lang.Nullable;

public final class WebMvcViewUtils {
  public static boolean processControllers(Module module, Processor<? super BeanPointer<?>> processor) {
    for (BeanPointer<?> pointer : WebMvcService.getInstance().getControllers(module)) {
      if (!processor.process(pointer)) {
        return false;
      }
    }
    return true;
  }

  public static boolean processUrls(Module module, @Nullable BeanPointer<?> controllerBeanPointer, Set<RequestMethod> requestMethods, Processor<? super UrlMappingElement> processor) {
    boolean foundMatch = false;
    for (UrlMappingElement variant : WebMvcUtils.getUrlMappings(module)) {
      if (matchesSelectedController(controllerBeanPointer, variant) && matchesRequestMethod(requestMethods, variant)) {
        foundMatch = true;
        if (!processor.process(variant)) {
          return false;
        }
      }
    }
    return !foundMatch;
  }

  private static boolean matchesSelectedController(@Nullable BeanPointer<?> controllerBeanPointer, UrlMappingElement variant) {
    if (controllerBeanPointer == null) {
      return true;
    }
    return variant.isDefinedInBean(controllerBeanPointer);
  }

  private static boolean matchesRequestMethod(Set<RequestMethod> requestMethods, UrlMappingElement variant) {
    RequestMethod[] method;
    if (variant.getMethod().length == 0) {
      return true;
    }
    for (RequestMethod method2 : variant.getMethod()) {
      if (requestMethods.contains(method2)) {
        return true;
      }
    }
    return false;
  }
}

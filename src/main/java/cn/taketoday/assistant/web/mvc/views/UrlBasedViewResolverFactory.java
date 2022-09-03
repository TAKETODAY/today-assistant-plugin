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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.openapi.module.Module;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;

public abstract class UrlBasedViewResolverFactory extends ViewResolverFactory {

  public static class MVC extends UrlBasedViewResolverFactory {
    @Override
    public String getBeanClass() {
      return InfraMvcConstant.URL_BASED_VIEW_RESOLVER;
    }
  }

  @Override
  public Set<ViewResolver> createViewResolvers(@Nullable CommonInfraBean bean, CommonInfraModel model) {
    Module module = model.getModule();
    if (module == null) {
      return Collections.emptySet();
    }
    String beanId = bean != null ? bean.getBeanName() : "<unknown bean>";
    String prefix = InfraPropertyUtils.getPropertyStringValue(bean, "prefix");
    String suffix = InfraPropertyUtils.getPropertyStringValue(bean, "suffix");
    return Collections.singleton(new UrlBasedViewResolver(module, "UrlBasedViewResolverFactory[" + beanId + "]", "", prefix, suffix));
  }
}

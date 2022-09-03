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

package cn.taketoday.assistant.web.mvc.freemarker;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;

import java.util.List;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.model.xml.FreeMarkerConfigurer;
import cn.taketoday.assistant.web.mvc.model.xml.FreeMarkerTemplateLoaderPath;
import cn.taketoday.assistant.web.mvc.views.TemplateViewResolverFactory;
import cn.taketoday.assistant.web.mvc.views.UrlBasedViewResolver;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;
import cn.taketoday.assistant.web.mvc.views.WithPrefixSuffix;

final class FreemarkerViewResolverFactory extends TemplateViewResolverFactory {

  FreemarkerViewResolverFactory() {
    super(InfraMvcConstant.FREEMARKER_VIEW_RESOLVER, InfraMvcConstant.FREEMARKER_CONFIGURER, "templateLoaderPath", ".ftl");
  }

  @Override
  protected String getViewResolverRegistryMethodName() {
    return "freeMarker";
  }

  @Override

  protected ViewResolver handleCustomConfigurer(CommonInfraBean configurer, WithPrefixSuffix resolver) {
    Module configurerModule = configurer.getModule();
    if ((configurer instanceof FreeMarkerConfigurer freeMarkerConfigurer) && configurerModule != null) {
      List<FreeMarkerTemplateLoaderPath> paths = freeMarkerConfigurer.getTemplateLoaderPaths();
      if (paths.size() == 1) {
        FreeMarkerTemplateLoaderPath templateLoaderPath = paths.get(0);
        String location = templateLoaderPath.getLocation().getStringValue();
        return new UrlBasedViewResolver(configurerModule, "FreemarkerViewResolverFactory", "", StringUtil.defaultIfEmpty(location, InfraMvcConstant.WEB_INF), "");
      }
    }
    return super.handleCustomConfigurer(configurer, resolver);
  }
}

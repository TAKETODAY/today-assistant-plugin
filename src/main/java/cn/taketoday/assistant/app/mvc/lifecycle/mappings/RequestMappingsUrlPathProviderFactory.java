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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings;

import com.intellij.ui.TextFieldWithAutoCompletionListProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMappingsModel;
import cn.taketoday.assistant.app.run.InfraApplicationUrlPathProviderFactory;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraWebServerConfig;
import cn.taketoday.lang.Nullable;

public class RequestMappingsUrlPathProviderFactory implements InfraApplicationUrlPathProviderFactory {

  @Override
  public TextFieldWithAutoCompletionListProvider<?> createCompletionProvider(InfraApplicationInfo info) {
    LiveRequestMappingsModel mappingsModel = info.getEndpointData(RequestMappingsEndpoint.getInstance()).getValue();
    Collection<String> mappings =
            mappingsModel == null ? Collections.emptySet() : mappingsModel.getRequestMappings().stream()
                    .filter(LiveRequestMapping::canNavigate)
                    .map(LiveRequestMapping::getPath)
                    .collect(Collectors.toSet());
    return new TextFieldWithAutoCompletionListProvider<>(mappings) {

      public String getLookupString(String item) {
        return item;
      }
    };
  }

  @Override
  @Nullable
  public String getServletPath(InfraApplicationInfo info, String path) {
    InfraWebServerConfig applicationServerConfiguration = info.getServerConfig().getValue();
    String servletPath = applicationServerConfiguration == null ? null : applicationServerConfiguration.servletPath();
    LiveRequestMappingsModel mappingsModel = info.getEndpointData(RequestMappingsEndpoint.getInstance()).getValue();
    if (mappingsModel != null) {
      for (LiveRequestMapping mapping : mappingsModel.getRequestMappings()) {
        if (mapping.getPath().equals(path)) {
          String servletMappingPath = mapping.getDispatcherServlet().getServletMappingPath();
          return servletMappingPath != null ? servletMappingPath : servletPath;
        }
      }
    }
    return servletPath;
  }
}

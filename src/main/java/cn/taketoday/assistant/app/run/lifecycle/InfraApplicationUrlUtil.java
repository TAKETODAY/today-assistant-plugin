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
package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Arrays;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.InfraApplicationUrlPathProviderFactory;
import cn.taketoday.lang.Nullable;

public class InfraApplicationUrlUtil {

  public static InfraApplicationUrlUtil getInstance() {
    return ApplicationManager.getApplication().getService(InfraApplicationUrlUtil.class);
  }

  @Nullable
  public String getServletPath(InfraApplicationInfo info, @Nullable String path) {
    InfraWebServerConfig configuration = info.getServerConfig().getValue();
    String servletPath = configuration == null ? null : configuration.servletPath();
    return StringUtil.isEmpty(path) ? servletPath : Arrays.stream(InfraApplicationUrlPathProviderFactory.EP_NAME.getExtensions())
            .findFirst().map((factory) -> factory.getServletPath(info, path))
            .orElse(servletPath);
  }

  public String getMappingUrl(String applicationUrl, @Nullable String servletPath, @Nullable String mappingPath) {
    return appendPath(appendPath(applicationUrl, servletPath), mappingPath);
  }

  private static String appendPath(String applicationUrl, @Nullable String path) {
    boolean endsWithSlash = StringUtil.endsWith(applicationUrl, "/");
    if (path == null) {
      path = endsWithSlash ? "" : "/";
    }
    else if (endsWithSlash) {
      path = StringUtil.trimStart(path, "/");
    }
    else if (!StringUtil.startsWith(path, "/")) {
      path = "/" + path;
    }

    return applicationUrl + path;
  }

  public void updatePath(Project project, InfraApplicationRunConfig runConfiguration, String path) {

    RunManagerImpl runManager = (RunManagerImpl) RunManager.getInstance(project);
    RunnerAndConfigurationSettings configurationSettings = runManager.findSettings(runConfiguration);
    if (configurationSettings != null) {
      if (configurationSettings.getConfiguration() instanceof InfraApplicationRunConfig) {
        ((InfraApplicationRunConfig) configurationSettings.getConfiguration()).setUrlPath(path);
        runConfiguration.setUrlPath(path);
        runManager.fireRunConfigurationChanged(configurationSettings);
      }

    }
  }
}

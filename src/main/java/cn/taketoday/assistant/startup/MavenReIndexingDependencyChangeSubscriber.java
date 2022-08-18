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

package cn.taketoday.assistant.startup;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.idea.maven.project.MavenImportListener;

import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.assistant.suggestion.ProjectSuggestionService;

import static java.util.stream.Collectors.joining;
import static org.jetbrains.idea.maven.project.MavenImportListener.TOPIC;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 00:03
 */
public class MavenReIndexingDependencyChangeSubscriber implements StartupActivity {
  /**
   * Debug logging can be enabled by adding fully classified class name/package name with # prefix
   * For eg., to enable debug logging, go `Help > Debug log settings` & type `#cn.taketoday.assistant.startup.MavenReIndexingDependencyChangeSubscriber`
   */
  private static final Logger log =
          Logger.getInstance(MavenReIndexingDependencyChangeSubscriber.class);

  @Override
  public void runActivity(Project project) {
    // This will trigger indexing
    ProjectSuggestionService service = project.getService(ProjectSuggestionService.class);

    try {
      log.debug("Subscribing to maven dependency updates for project ", project.getName());
      var con = project.getMessageBus().connect();

      con.<MavenImportListener>subscribe(TOPIC, (importedProjects, newModules) -> {
        boolean proceed = importedProjects.stream().anyMatch(
                p -> project.getName().equals(p.getDisplayName()) && p.getDirectory().equals(project.getBasePath()));

        if (proceed) {
          log.debug("Maven dependencies are updated for project ", project.getName());
          DumbService.getInstance(project).smartInvokeLater(() -> {
            log.debug("Will attempt to trigger indexing for project " + project.getName());
            try {
              Module[] modules = ModuleManager.getInstance(project).getModules();
              if (modules.length > 0) {
                service.reindex(modules);
              }
              else if (log.isDebugEnabled()) {
                log.debug("Skipping indexing for project "
                        + project.getName() + " as there are no modules");
              }
            }
            catch (Throwable e) {
              log.error("Error occurred while indexing project " + project.getName() + " & modules "
                      + moduleNamesAsStrCommaDelimited(newModules, false), e);
            }
          });
        }
        else {
          log.debug("Skipping indexing as none of the imported projects match our project ", project.getName());
        }
      });
    }
    catch (Throwable e) {
      log.error("Failed to subscribe to maven dependency updates for project " + project.getName(), e);
    }
  }

  public static String moduleNamesAsStrCommaDelimited(List<Module> newModules,
          boolean includeProjectName) {
    return moduleNamesAsStrCommaDelimited(newModules.stream(), includeProjectName);
  }

  private static String moduleNamesAsStrCommaDelimited(Stream<Module> moduleStream,
          boolean includeProjectName) {
    return moduleStream.map(module -> includeProjectName ?
                                      module.getProject().getName() + ":" + module.getName() :
                                      module.getName()).collect(joining(", "));
  }

}

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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;

import cn.taketoday.assistant.InfraLibraryUtil;

public final class InfraMetadataCompilerAfterTask implements CompileTask {

  @Override
  public boolean execute(CompileContext context) {
    if (context.isAnnotationProcessorsEnabled()) {
      boolean var10000;
      DumbService dumbService;
      boolean projectHasLib;
      label42:
      {
        dumbService = DumbService.getInstance(context.getProject());
        if (!dumbService.isDumb()) {
          if (!dumbService.runReadActionInSmartMode(() -> InfraLibraryUtil.hasFrameworkLibrary(context.getProject()))) {
            projectHasLib = false;
            break label42;
          }
        }

        projectHasLib = true;
      }

      if (projectHasLib) {
        Module[] affectedModules = context.getCompileScope().getAffectedModules();
        for (Module module : affectedModules) {
          label33:
          {
            if (!dumbService.isDumb()) {
              var10000 = dumbService.runReadActionInSmartMode(new Computable<Boolean>() {
                public Boolean compute() {
                  return false;
                }
              });
              if (!(Boolean) var10000) {
                projectHasLib = false;
                break label33;
              }
            }

            projectHasLib = true;
          }

          boolean hasAnnotationProcessor = projectHasLib;
          if (hasAnnotationProcessor) {
            DaemonCodeAnalyzer.getInstance(context.getProject()).restart();
            break;
          }
        }

      }
    }
    return true;
  }
}

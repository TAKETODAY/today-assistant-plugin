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

package cn.taketoday.assistant.app.run.update;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Set;

public interface TriggerFilePolicy {
  String TRIGGER_FILE_NAME = ".restartTriggerFile";
  Logger LOG = Logger.getInstance(TriggerFilePolicy.class);

  static void updateTriggerFile(Set<? extends Module> modules) {
    Application application = ApplicationManager.getApplication();
    application.invokeLater(() -> {
      application.runWriteAction(() -> {
        for (Module module : modules) {
          if (!module.isDisposed()) {
            VirtualFile[] roots = OrderEnumerator.orderEntries(module).runtimeOnly().productionOnly().withoutLibraries().withoutSdk().getClassesRoots();
            if (roots.length == 0) {
              LOG.debug("Failed to update trigger file: class roots not found");
            }
            else {
              try {
                VirtualFile triggerFile = roots[0].findOrCreateChildData(TriggerFilePolicy.class, TRIGGER_FILE_NAME);
                LocalFileSystem.getInstance().setTimeStamp(triggerFile, System.currentTimeMillis());
              }
              catch (IOException e) {
                LOG.debug("Failed to update trigger file:", e);
              }
            }
          }
        }
      });
    });
  }
}

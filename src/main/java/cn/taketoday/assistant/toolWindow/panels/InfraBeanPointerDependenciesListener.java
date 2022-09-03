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

package cn.taketoday.assistant.toolWindow.panels;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;

class InfraBeanPointerDependenciesListener implements Disposable {
  private final MergingUpdateQueue myModelFilesQueue;
  private final ParameterizedCachedValue<Object, PsiFile> myJavaStructureCachedValueTrigger;

  public InfraBeanPointerDependenciesListener(InfraBeanPointerFinderRecursivePanel panel) {
    this.myModelFilesQueue = new MergingUpdateQueue("Model Files", 1000, true, panel, this);
    Project project = panel.getProject();
    this.myJavaStructureCachedValueTrigger = CachedValuesManager.getManager(project).createParameterizedCachedValue(psiFile -> {
      scheduleModelFilesRecalculation(panel, psiFile);
      return CachedValueProvider.Result.create(null, PsiModificationTracker.MODIFICATION_COUNT);
    }, false);
    PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {

      public void childrenChanged(PsiTreeChangeEvent event) {
        PsiFile file = event.getFile();
        if (panel.knowsAboutConfigurationFile(file)) {
          panel.updatePanel();
        }
        else if (file instanceof PsiClassOwner) {
          InfraBeanPointerDependenciesListener.this.myJavaStructureCachedValueTrigger.getValue(file);
        }
      }
    }, this);
    Disposer.register(panel, this);
  }

  private void scheduleModelFilesRecalculation(InfraBeanPointerFinderRecursivePanel panel, PsiFile psiFile) {
    this.myModelFilesQueue.queue(new Update("Recalculate model files") {
      public void run() {
        Application application = ApplicationManager.getApplication();
        application.executeOnPooledThread(() -> {
          DumbService.getInstance(psiFile.getProject()).runReadActionInSmartMode(() -> {
            panel.getListItems();
            if (panel.knowsAboutConfigurationFile(psiFile)) {
              panel.updatePanel();
            }
          });
        });
      }
    });
  }

  public void dispose() {
    this.myModelFilesQueue.cancelAllUpdates();
  }
}

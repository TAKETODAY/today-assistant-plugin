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

package cn.taketoday.assistant.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.Objects;

public final class InfraKtOutOfCodeBlockModificationTracker extends SimpleModificationTracker {

  private long outOfBlockModCount;
  private final PsiModificationTracker myPsiModificationTracker;
  private final PsiTreeChangeAdapter listener;

  public InfraKtOutOfCodeBlockModificationTracker(Project project, Disposable parent) {
    this.myPsiModificationTracker = PsiModificationTracker.getInstance(project);
    this.outOfBlockModCount = myPsiModificationTracker.getModificationCount();
    this.listener = new PsiTreeChangeAdapter() {
      public void childrenChanged(PsiTreeChangeEvent event) {
        PsiFile language = event.getFile();
        if (language != null) {
          String id = language.getLanguage().getID();
          if (Objects.equals(id, "kotlin")) {
            long count = myPsiModificationTracker.getModificationCount();
            if (outOfBlockModCount != count) {
              outOfBlockModCount = count;
              incModificationCount();
            }
          }
        }

      }
    };

    PsiManager.getInstance(project).addPsiTreeChangeListener(this.listener, parent);
  }
}

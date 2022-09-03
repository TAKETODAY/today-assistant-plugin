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

package cn.taketoday.assistant.model.actions.generate;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;

import cn.taketoday.lang.Nullable;

public class InfraGenerateTemplatesHolder extends ArrayList<Pair<PsiElement, Factory<Template>>> {
  private final Project myProject;

  public InfraGenerateTemplatesHolder(Project project) {
    this.myProject = project;
  }

  public void addTemplateFactory(PsiElement element, Factory<Template> template) {
    add(Pair.create(element, template));
  }

  public void runTemplates() {
    TemplateManager manager = TemplateManager.getInstance(this.myProject);
    runTemplates(manager, 0);
  }

  private void runTemplates(TemplateManager manager, int index) {
    if (index < this.size()) {
      WriteCommandAction.writeCommandAction(this.myProject).run(() -> {
        Pair<PsiElement, Factory<Template>> pair = this.get(index);
        Editor editor = this.getEditor(pair.getFirst());
        if (editor != null) {
          PsiDocumentManager.getInstance(this.myProject).doPostponedOperationsAndUnblockDocument(editor.getDocument());
          Factory<Template> factory = pair.getSecond();
          if (factory != null) {
            Template template = factory.create();
            if (template != null) {
              manager.startTemplate(editor, template, new TemplateEditingAdapter() {
                public void templateFinished(Template template, boolean brokenOff) {
                  if (index + 1 < InfraGenerateTemplatesHolder.this.size()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                      InfraGenerateTemplatesHolder.this.runTemplates(manager, index + 1);
                    });
                  }
                }
              });
            }
          }
        }

      });
    }
  }

  @Nullable
  private Editor getEditor(PsiElement element) {
    VirtualFile virtualFile;
    PsiFile psiFile = element.getContainingFile();
    if (psiFile != null && (virtualFile = psiFile.getVirtualFile()) != null) {
      TextRange range = element.getTextRange();
      int textOffset = range.getStartOffset();
      OpenFileDescriptor descriptor = new OpenFileDescriptor(this.myProject, virtualFile, textOffset);
      return FileEditorManager.getInstance(this.myProject).openTextEditor(descriptor, true);
    }
    return null;
  }
}

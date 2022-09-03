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

package cn.taketoday.assistant.model.actions.patterns.frameworks.ui;

import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;

import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;

import cn.taketoday.assistant.InfraBundle;

public class ChooseTemplatesDialogWrapper extends DialogWrapper {
  private final cn.taketoday.assistant.model.actions.patterns.frameworks.ui.ChooseTemplatesForm myTemplatesForm;

  public ChooseTemplatesDialogWrapper(Project project, List<cn.taketoday.assistant.model.actions.patterns.frameworks.ui.TemplateInfo> infos, LibrariesInfo libInfo, String frameworkTitle) {
    super(project, true);
    this.myTemplatesForm = new cn.taketoday.assistant.model.actions.patterns.frameworks.ui.ChooseTemplatesForm(infos, libInfo);
    this.myTemplatesForm.getLibrariesValidationComponent().addValidityListener(isValid -> ChooseTemplatesDialogWrapper.this.setOKActionEnabled(isValid));
    setOKActionEnabled(this.myTemplatesForm.getLibrariesValidationComponent().isValid());
    setTitle(InfraBundle.message("spring.choose.bean.templates.dialog.title", frameworkTitle));
    init();
  }

  protected Action[] createActions() {
    return new Action[] { getOKAction(), getCancelAction() };
  }

  protected JComponent createCenterPanel() {
    return this.myTemplatesForm.getComponent();
  }

  public JComponent getPreferredFocusedComponent() {
    return this.myTemplatesForm.getComponent();
  }

  public ChooseTemplatesForm getTemplatesForm() {
    return this.myTemplatesForm;
  }

  public List<Template> getSelectedTemplates() {
    List<Template> templates = new LinkedList<>();
    for (TemplateInfo info : this.myTemplatesForm.getTemplateInfos()) {
      if (info.isAccepted()) {
        templates.add(info.getTemplate());
      }
    }
    return templates;
  }

  protected void dispose() {
    Disposer.dispose(this.myTemplatesForm);
    super.dispose();
  }
}

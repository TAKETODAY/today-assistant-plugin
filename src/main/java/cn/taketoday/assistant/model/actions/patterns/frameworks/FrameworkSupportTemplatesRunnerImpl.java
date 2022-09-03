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

package cn.taketoday.assistant.model.actions.patterns.frameworks;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.actions.generate.AbstractDomGenerateProvider;
import com.intellij.xml.util.XmlTagUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.actions.patterns.frameworks.ui.ChooseTemplatesDialogWrapper;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;

public class FrameworkSupportTemplatesRunnerImpl extends FrameworkSupportTemplatesRunner {

  @Override
  public void generateSpringBeans(FrameworkSupportProvider provider, Module module, Editor editor, XmlFile xmlFile) {
    ChooseTemplatesDialogWrapper dialogWrapper = new ChooseTemplatesDialogWrapper(module.getProject(), provider.getTemplateInfos(module), provider.getLibrariesInfo(module), provider.getDescription());
    dialogWrapper.show();
    if (dialogWrapper.getExitCode() != 0 || !FileModificationService.getInstance().preparePsiElementForWrite(xmlFile)) {
      return;
    }
    WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
      provider.addFacet(module);
      dialogWrapper.getTemplatesForm().getLibrariesValidationComponent().setupLibraries();
      moveCaretIfNeeded(module.getProject(), editor, xmlFile);
      runTemplates(module.getProject(), editor, dialogWrapper.getSelectedTemplates(), 0, provider.getPredefinedVars(module, xmlFile));
    });
  }

  private static void moveCaretIfNeeded(Project project, Editor editor, XmlFile xmlFile) {
    XmlTag tag;
    DomInfraBean bean = BeanCoreUtils.getBeanForCurrentCaretPosition(editor, xmlFile);
    if (bean != null) {
      DomInfraBean infraBean = BeanCoreUtils.getTopLevelBean(bean);
      if (infraBean.getXmlTag() != null) {
        TextRange range = infraBean.getXmlTag().getTextRange();
        int offset = range.getEndOffset();
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        return;
      }
      return;
    }
    PsiElement element = xmlFile.findElementAt(editor.getCaretModel().getOffset());
    Beans beans = DomUtil.findDomElement(element, Beans.class);
    if (beans != null && (tag = beans.getXmlTag()) != null) {
      int offset2 = XmlTagUtil.getTrimmedValueRange(tag).getStartOffset();
      editor.getCaretModel().moveToOffset(tag.getTextRange().getStartOffset() + offset2);
      editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }
  }

  public static void runTemplates(Project project, Editor editor, List<Template> templates, int index, Map<String, String> predefinedVars) {
    Template template = templates.get(index);
    template.setToReformat(true);
    TemplateManager.getInstance(project).startTemplate(editor, template, true, predefinedVars, new TemplateEditingAdapter() {

      public void templateFinished(Template template2, boolean brokenOff) {
        if (index + 1 < templates.size()) {
          Application application = ApplicationManager.getApplication();
          Project project2 = project;
          Editor editor2 = editor;
          List list = templates;
          int i = index;
          Map map = predefinedVars;
          application.invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project2, () -> {
              runTemplates(project2, editor2, list, i + 1, map);
            });
          });
        }
      }
    });
  }

  @Override
  public Map<String, String> getPredefinedVars(Module module, XmlFile xmlFile) {
    Map<String, String> vars = new HashMap<>();
    DomFileElement<Beans> domFileElement = InfraDomUtils.getDomFileElement(xmlFile);
    if (domFileElement != null) {
      Beans beans = domFileElement.getRootElement();
      AbstractDomGenerateProvider.addNamespacePrefix(beans, vars);
    }
    return vars;
  }
}

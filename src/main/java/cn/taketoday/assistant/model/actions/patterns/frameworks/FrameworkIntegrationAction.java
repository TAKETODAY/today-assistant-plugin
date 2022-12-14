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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ui.EmptyIcon;

import javax.swing.Icon;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

public abstract class FrameworkIntegrationAction extends AnAction implements FrameworkSupportProvider {

  protected FrameworkIntegrationAction() {

    // TODO we cannot use getTemplatePresentation() and set icon/text once
    // TODO due to BasicDelegateFrameworkIntegrationAction, so reserve space for icon
    Presentation template = getTemplatePresentation();
    template.setIcon(EmptyIcon.ICON_16);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    Module module = getModule(dataContext);
    Editor editor = getEditor(dataContext);
    XmlFile xmlFile = getXmlFile(dataContext);

    if (module != null && editor != null && xmlFile != null) {
      generateSpringBeans(module, editor, xmlFile);
    }
  }

  @Override
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();

    XmlFile file = getXmlFile(dataContext);

    boolean isSpringBeanFile = file != null && InfraDomUtils.isInfraXml(file);

    boolean enabled = isSpringBeanFile && accept(file);

    presentation.setEnabledAndVisible(enabled);
    presentation.setText(getDescription());
    presentation.setIcon(getIcon());
  }

  @Nullable
  protected static XmlFile getXmlFile(DataContext dataContext) {
    return getXmlFile(getProject(dataContext), getEditor(dataContext));
  }

  @Nullable
  protected static XmlFile getXmlFile(Project project, Editor editor) {
    if (project == null || editor == null)
      return null;

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    return psiFile instanceof XmlFile ? (XmlFile) psiFile : null;
  }

  @Nullable
  protected static Editor getEditor(DataContext dataContext) {
    return CommonDataKeys.EDITOR.getData(dataContext);
  }

  @Nullable
  protected static Project getProject(DataContext dataContext) {
    return CommonDataKeys.PROJECT.getData(dataContext);
  }

  @Nullable
  protected static Module getModule(DataContext dataContext) {
    return PlatformCoreDataKeys.MODULE.getData(dataContext);
  }

  protected boolean accept(XmlFile file) {
    return acceptBeansByClassNames(file, getBeansClassNames());
  }

  protected String[] getBeansClassNames() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  private static boolean acceptBeansByClassNames(XmlFile file, String... classNames) {
    if (classNames.length == 0)
      return true;

    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null)
      return false;

    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);

    CommonInfraModel model = InfraManager.from(file.getProject()).getInfraModelByFile(file);
    if (model != null) {
      for (String className : classNames) {
        PsiClass psiClass = javaPsiFacade.findClass(className, searchScope);
        if (psiClass == null)
          continue;

        if (InfraModelSearchers.doesBeanExist(model, psiClass))
          return false;
      }
    }
    return true;
  }

  protected abstract void generateSpringBeans(Module module, Editor editor, XmlFile xmlFile);

  @Nullable
  protected Icon getIcon() {
    return null;
  }
}
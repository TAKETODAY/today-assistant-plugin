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

package cn.taketoday.assistant.model.converters.fixes.bean;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.facet.FacetManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationsManager;

import org.jetbrains.annotations.TestOnly;

import java.util.Set;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFacetConfiguration;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraImport;

import static cn.taketoday.assistant.InfraBundle.message;

public class UseExistingBeanFromOtherContextQuickFix implements LocalQuickFix, HighPriorityAction {
  private final String myBeanId;
  private final SmartPsiElementPointer<PsiFile> myBeanFilePointer;
  private final InfraFileSet mySpringFileSet;
  private final String myFilesetName;
  private static int quickFixActionIndexToInvokeInTest = -1;

  public UseExistingBeanFromOtherContextQuickFix(String beanId, PsiFile beanFile, InfraFileSet springFileSet) {
    this.myBeanId = beanId;
    this.myBeanFilePointer = SmartPointerManager.getInstance(beanFile.getProject()).createSmartPsiElementPointer(beanFile);
    this.mySpringFileSet = springFileSet;
    this.myFilesetName = springFileSet.getName();
  }

  @TestOnly
  public static void setQuickFixActionIndexToInvokeInTest(int actionIdx) {
    quickFixActionIndexToInvokeInTest = actionIdx;
  }

  public boolean startInWriteAction() {
    return false;
  }

  public String getName() {
    return message("use.existing.quick.fixes.use.existing.bean.from.context", this.myFilesetName);
  }

  public String getFamilyName() {
    return message("use.existing.quick.fixes.use.existing.bean.family.name");
  }

  public void applyFix(Project project, ProblemDescriptor descriptor) {

    PsiFile myBeanFile = this.myBeanFilePointer.getElement();
    if (myBeanFile != null) {
      PsiElement element = descriptor.getPsiElement();
      PsiFile file = element.getContainingFile();
      InfraFileSet currentFileSet = getCurrentFileSet(project, file);
      AnAction addDependencyToFileSetAction = new AnAction(
              message("use.existing.quick.fixes.add.dependency.to.context", this.myFilesetName), null, Icons.ParentContext) {
        public void update(AnActionEvent e) {

          e.getPresentation().setEnabled(currentFileSet.getDependencyFileSets().isEmpty());
        }

        public void actionPerformed(AnActionEvent e) {

          currentFileSet.addDependency(UseExistingBeanFromOtherContextQuickFix.this.mySpringFileSet);
          InfraFacet facet = UseExistingBeanFromOtherContextQuickFix.this.mySpringFileSet.getFacet();
          FacetManager.getInstance(facet.getModule()).facetConfigurationChanged(facet);
          rehighlight(file);
        }
      };
      AnAction addSpringXmlToOurFileSetAction = new AnAction(
              message("use.existing.quick.fixes.add.infra.xml.to.fileset.action", myBeanFile.getName(), currentFileSet.getName()), null,
              Icons.FileSet) {

        @Override
        public void actionPerformed(AnActionEvent e) {
          VirtualFile virtualFile = myBeanFile.getVirtualFile();
          assert virtualFile != null;
          currentFileSet.addFile(virtualFile);
          InfraFacet facet = UseExistingBeanFromOtherContextQuickFix.this.mySpringFileSet.getFacet();
          FacetManager.getInstance(facet.getModule()).facetConfigurationChanged(facet);
          rehighlight(file);
        }
      };

      String relativePath = VfsUtilCore.getRelativePath(myBeanFile.getVirtualFile(), file.getContainingDirectory().getVirtualFile(), '/');
      AnAction addImportAction = new AnAction(message("use.existing.quick.fixes.add.import.action", myBeanFile.getName()), null, AllIcons.Nodes.Tag) {

        @Override
        public void update(AnActionEvent e) {
          e.getPresentation().setEnabled(relativePath != null && myBeanFile instanceof XmlFile);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
          if (FileModificationService.getInstance().preparePsiElementForWrite(element)) {
            DomFileElement<Beans> root = InfraDomUtils.getDomFileElement((XmlFile) file);

            assert root != null;

            Beans beans = root.getRootElement();
            if (beans.isValid()) {
              WriteCommandAction.runWriteCommandAction(project, () -> {
                InfraImport infraImport = beans.addImport();
                GenericAttributeValue<Set<PsiFile>> resource = infraImport.getResource();
                resource.setStringValue(relativePath);
                rehighlight(file);
              });
            }
          }
        }
      };
      AnAction openTodayFacetSettings = new AnAction(
              message("use.existing.quick.fixes.open.infra.facet.settings"), null, Icons.Today) {
        @Override
        public void actionPerformed(AnActionEvent e) {
          ModulesConfigurator.showFacetSettingsDialog(getTodayFacet(file), null);
        }
      };
      ActionGroup actionGroup = new DefaultActionGroup(addDependencyToFileSetAction, addSpringXmlToOurFileSetAction, addImportAction, openTodayFacetSettings);
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        assert quickFixActionIndexToInvokeInTest != -1;
        actionGroup.getChildren(null)[quickFixActionIndexToInvokeInTest].actionPerformed(ActionUtil.createEmptyEvent());
      }
      else {
        DataManager.getInstance().getDataContextFromFocusAsync().onSuccess((dataContext) -> {
          JBPopupFactory.getInstance()
                  .createActionGroupPopup(
                          message("use.existing.quick.fixes.choose.fix.for", this.myBeanId),
                          actionGroup, dataContext, JBPopupFactory.ActionSelectionAid.NUMBERING, true)
                  .showInBestPositionFor(dataContext);
        });
      }
    }
  }

  private static void rehighlight(PsiFile psiFile) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    InfraFacet infraFacet = getTodayFacet(psiFile);
    InfraFacetConfiguration configuration = infraFacet.getConfiguration();
    configuration.setModified();
    ApplicationManager.getApplication().invokeLater(() -> {
      DomElementAnnotationsManager.getInstance(psiFile.getProject()).dropAnnotationsCache();
      psiFile.getManager().dropPsiCaches();
    });
  }

  private static InfraFacet getTodayFacet(PsiFile psiFile) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);

    assert module != null;
    InfraFacet infraFacet = InfraFacet.from(module);
    assert infraFacet != null;

    return infraFacet;
  }

  private static InfraFileSet getCurrentFileSet(Project project, PsiFile file) {
    Set<InfraModel> models = InfraManager.from(project).getInfraModelsByFile(file);
    for (InfraModel model : models) {
      InfraFileSet currentFileSet = model.getFileSet();
      if (currentFileSet != null) {
        return currentFileSet;
      }
    }
    throw new IllegalStateException("no fileset for " + file);
  }
}

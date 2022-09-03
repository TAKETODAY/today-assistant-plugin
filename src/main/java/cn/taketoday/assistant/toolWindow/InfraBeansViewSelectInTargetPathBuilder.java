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

package cn.taketoday.assistant.toolWindow;

import com.intellij.ide.SelectInContext;
import com.intellij.jam.JamService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastLanguagePlugin;
import org.jetbrains.uast.UastUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.beans.stereotype.Component;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.context.chooser.InfraContextDescriptor;
import cn.taketoday.assistant.context.chooser.InfraMultipleContextsManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.stereotype.AbstractComponentScan;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

class InfraBeansViewSelectInTargetPathBuilder {
  private Object[] myPath = ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  private Module myModule;
  private InfraBeansViewSettings myViewSettings;

  public InfraBeansViewSelectInTargetPathBuilder(SelectInContext context) {
    PsiElement element = getElement(context);
    if (element != null) {
      calculatePath(element);
    }
  }

  public boolean canSelect() {
    return this.myPath.length != 0;
  }

  public Object[] getPath() {
    return this.myPath;
  }

  private PsiElement getElement(SelectInContext context) {
    Object selector = context.getSelectorInFile();
    if (!(selector instanceof PsiElement element)) {
      return null;
    }
    this.myModule = ModuleUtilCore.findModuleForFile(context.getVirtualFile(), context.getProject());
    if (this.myModule == null) {
      return null;
    }
    this.myViewSettings = InfraBeansViewSettings.from(this.myModule.getProject());
    return element;
  }

  private void calculatePath(PsiElement element) {
    BeanPointer<?> beanPointer = getPointer(element);
    if (beanPointer == null) {
      return;
    }
    FileSetAndConfigFileResult result = determineFileSetAndConfigFile(beanPointer);
    if (result.infraFileSet == null) {
      return;
    }
    SmartList smartList = new SmartList();
    if (this.myViewSettings.isShowModules()) {
      smartList.add(result.infraFileSet.getFacet().getModule());
      if (this.myViewSettings.isShowFileSets()) {
        smartList.add(result.infraFileSet);
        if (this.myViewSettings.isShowFiles()) {
          SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(result.configElement);
          smartList.add(pointer);
        }
      }
    }
    smartList.add(beanPointer);
    this.myPath = ArrayUtil.toObjectArray(smartList);
  }

  @Nullable
  private static BeanPointer<?> getPointer(PsiElement element) {
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) {
      return null;
    }
    if (containingFile instanceof XmlFile xmlFile) {
      return findInfraBeanInXml(element, xmlFile);
    }
    if (UastLanguagePlugin.Companion.byLanguage(containingFile.getLanguage()) != null) {
      return findInfraBeanInCode(element);
    }
    return null;
  }

  @Nullable
  private static BeanPointer<?> findInfraBeanInCode(PsiElement element) {
    JamPsiMemberInfraBean memberInfraBean;
    UClass uClass = UastUtils.findContaining(element, UClass.class);
    PsiClass psiClass = uClass != null ? uClass.getJavaPsi() : null;
    if (!InfraUtils.isBeanCandidateClassInProject(psiClass)) {
      return null;
    }
    JavaClassInfo info = JavaClassInfo.from(psiClass);
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      if (!info.isCalculatedMapped()) {
        return null;
      }
    }
    else if (!info.isMapped()) {
      return null;
    }
    if (info.isMappedDomBean()) {
      return info.getMappedDomBeans().get(0);
    }
    UMethod uMethod = UastUtils.findContaining(element, UMethod.class);
    PsiMethod psiMethod = uMethod != null ? uMethod.getJavaPsi() : null;
    if (psiMethod != null && (memberInfraBean = JamService.getJamService(element.getProject())
            .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiMethod)) != null) {
      return InfraBeanService.of().createBeanPointer(memberInfraBean);
    }
    List<JamBeanPointer> stereoTypeMappedBeans = info.getStereotypeMappedBeans();
    if (stereoTypeMappedBeans.isEmpty()) {
      throw new IllegalStateException("could not find bean for " + element);
    }
    return stereoTypeMappedBeans.get(0);
  }

  @Nullable
  private static BeanPointer<?> findInfraBeanInXml(PsiElement element, XmlFile file) {
    XmlTag tag;
    DomElement domElement;
    DomInfraBean domBean;
    if (InfraDomUtils.isInfraXml(file) && (tag = PsiTreeUtil.getParentOfType(element, XmlTag.class)) != null && (domElement = DomUtil.getDomElement(
            tag)) != null && (domBean = domElement.getParentOfType(DomInfraBean.class, false)) != null) {
      return InfraBeanService.of().createBeanPointer(domBean);
    }
    return null;
  }

  private FileSetAndConfigFileResult determineFileSetAndConfigFile(BeanPointer<?> beanPointer) {
    FileSetAndConfigFileResult result = new FileSetAndConfigFileResult();
    if (!beanPointer.isValid()) {
      return result;
    }
    if (beanPointer instanceof JamBeanPointer) {
      JamPsiMemberInfraBean infraBean = ((JamBeanPointer) beanPointer).getBean();
      PsiElement psiElement = infraBean.getPsiElement();
      PsiClass psiElement2 = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false);
      if (psiElement2 == null) {
        return result;
      }
      PsiElement configElement = getConfigElement(psiElement2);
      if (configElement == null) {
        configElement = psiElement2;
      }
      result.configElement = configElement;
    }
    else {
      result.configElement = beanPointer.getContainingFile();
    }
    InfraContextDescriptor context = InfraMultipleContextsManager.of().getContextDescriptor(result.configElement.getContainingFile());
    if (!context.isPredefinedContext()) {
      String contextName = context.getName();
      Module module = context.getModule();
      InfraFacet infraFacet = InfraFacet.from(module);
      for (InfraFileSet infraFileSet : InfraFileSetService.of().getAllSets(infraFacet)) {
        if (infraFileSet.getName().equals(contextName) && infraFileSet.hasFile(result.configElement.getContainingFile().getVirtualFile())) {
          result.infraFileSet = infraFileSet;
          return result;
        }
      }
    }
    boolean found = ModuleUtilCore.visitMeAndDependentModules(this.myModule, module2 -> {
      InfraFileSet fileSet = InfraFileSetService.of().findFileSet(module2, result.configElement.getContainingFile());
      if (fileSet != null) {
        result.infraFileSet = fileSet;
        return false;
      }
      return true;
    });
    if (beanPointer instanceof JamBeanPointer) {
      PsiClass beanClass = beanPointer.getBeanClass();
      if (!found && beanClass == null) {
        return result;
      }
      JamPsiMemberInfraBean stereotypeBean = ((JamBeanPointer) beanPointer).getBean();
      ModuleUtilCore.visitMeAndDependentModules(this.myModule, module3 -> {
        Set<InfraModel> models = InfraManager.from(module3.getProject()).getAllModels(module3);
        for (InfraModel model : models) {
          if (model.getFileSet() != null) {
            boolean exists = InfraModelSearchers.doesBeanExist(model, ModelSearchParameters.byClass(beanClass));
            if (exists) {
              for (CommonInfraModel process : model.getRelatedModels()) {
                for (InfraBeansPackagesScan scan : InfraModelVisitorUtils.getComponentScans(process)) {
                  Set<CommonInfraBean> scannedBeans = scan.getScannedElements(module3);
                  if (scannedBeans.contains(stereotypeBean)) {
                    setComponentScanResult(result, model, scan);
                    return false;
                  }
                  for (CommonInfraBean bean : scannedBeans) {
                    if ((bean instanceof Configuration) || (bean instanceof Component)) {
                      if (Objects.equals(bean.getContainingFile(), stereotypeBean.getContainingFile())) {
                        setComponentScanResult(result, model, scan);
                        return false;
                      }
                    }
                  }
                }
              }
            }
          }
        }
        return true;
      });
    }
    return result;
  }

  private static void setComponentScanResult(FileSetAndConfigFileResult result, InfraModel model, InfraBeansPackagesScan scan) {
    result.infraFileSet = model.getFileSet();
    if (scan instanceof AbstractComponentScan) {
      result.configElement = Objects.requireNonNull(((AbstractComponentScan) scan).getPsiElement());
    }
    else {
      result.configElement = Objects.requireNonNull(scan.getContainingFile());
    }
  }

  private static PsiClass getConfigElement(PsiClass containingClass) {
    if (InfraUtils.isStereotypeComponentOrMeta(containingClass)) {
      return containingClass;
    }
    Query<PsiClass> inheritorsQuery = ClassInheritorsSearch.search(containingClass, containingClass.getResolveScope(), true, true, false);
    CommonProcessors.FindFirstProcessor<PsiClass> processor = new CommonProcessors.FindFirstProcessor<>() {
      public boolean accept(PsiClass psiClass) {
        return InfraUtils.isStereotypeComponentOrMeta(psiClass);
      }
    };
    inheritorsQuery.forEach(processor);
    return processor.getFoundValue();
  }

  public static class FileSetAndConfigFileResult {
    @Nullable
    private InfraFileSet infraFileSet;
    private PsiElement configElement;

    private FileSetAndConfigFileResult() {
    }
  }
}

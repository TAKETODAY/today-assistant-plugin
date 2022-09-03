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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.DomBeanPointer;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class RequiredAnnotationInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkMethod(PsiMethod method, InspectionManager manager, boolean isOnTheFly) {
    PsiClass psiClass;
    if (!InfraLibraryUtil.hasLibrary(manager.getProject()) || !PropertyUtilBase.isSimplePropertySetter(
            method) || (psiClass = method.getContainingClass()) == null || !JamCommonUtil.isPlainJavaFile(method.getContainingFile()) || !AnnotationUtil.isAnnotated(method,
            AnnotationConstant.REQUIRED, 0) || !InfraUtils.isBeanCandidateClass(psiClass)) {
      return null;
    }
    JavaClassInfo info = JavaClassInfo.from(psiClass);
    if (!info.isMapped()) {
      return null;
    }
    String propertyName = PropertyUtilBase.getPropertyNameBySetter(method);
    PsiType propertyType = PropertyUtilBase.getPropertyType(method);
    if (!info.getMappedProperties(propertyName).isEmpty() || info.isAutowired() || isAutowiredByDefault(method)) {
      return null;
    }
    List<DomBeanPointer> list = info.getMappedDomBeans();
    List<InfraBean> beans = new ArrayList<>(list.size());
    for (DomBeanPointer pointer : list) {
      DomInfraBean infraBean = pointer.getBean();
      if ((infraBean instanceof InfraBean) && !((InfraBean) infraBean).isAbstract()) {
        beans.add((InfraBean) infraBean);
      }
    }
    if (beans.isEmpty()) {
      return null;
    }
    LocalQuickFix fix = new LocalQuickFix() {

      public String getName() {
        return message("create.missing.mappings", propertyName);
      }

      public String getFamilyName() {
        return message("create.missing.mappings.family.name");
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        Set<VirtualFile> files = new HashSet<>();
        for (InfraBean bean : beans) {
          if (bean.isValid()) {
            files.add(bean.getContainingFile().getVirtualFile());
          }
        }
        if (!ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files).hasReadonlyFiles()) {
          for (InfraBean bean2 : beans) {
            if (bean2.isValid()) {
              InfraProperty springProperty = bean2.addProperty();
              springProperty.getName().setStringValue(propertyName);
              RequiredAnnotationInspection.this.setPropertyValue(project, bean2, springProperty, propertyType);
            }
          }
        }
      }
    };
    PsiIdentifier psiIdentifier = method.getNameIdentifier();
    ProblemDescriptor descriptor = manager.createProblemDescriptor(psiIdentifier, message("required.property.not.mapped", propertyName), fix,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly);
    return new ProblemDescriptor[] { descriptor };
  }

  public void setPropertyValue(Project project, InfraBean bean, InfraProperty springProperty, @Nullable PsiType propertyType) {
    InfraModel modelByFile;
    if (propertyType != null && (modelByFile = InfraManager.from(project).getInfraModelByFile(bean.getContainingFile())) != null) {
      List<BeanPointer<?>> beansByType = BeanCoreUtils.getBeansByType(propertyType, modelByFile);
      if (beansByType.size() > 0) {
        springProperty.getRefAttr().setStringValue(beansByType.size() == 1 ? beansByType.get(0).getName() : "");
        return;
      }
    }
    springProperty.getValueAttr().setStringValue("");
  }

  private static boolean isAutowiredByDefault(PsiMethod method) {
    PsiType psiType = PropertyUtilBase.getPropertyType(method);
    return AutowireUtil.isAutowiredByDefault(psiType);
  }
}

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
package cn.taketoday.assistant.gutter;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.jam.JamService;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.pom.InfraBeanPomTargetUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.lang.Nullable;

public final class BeansPsiElementCellRenderer extends DefaultPsiElementCellRenderer {

  private static final DomElementListCellRenderer DOM_RENDERER =
          new DomElementListCellRenderer(InfraBundle.message("bean.with.unknown.name")) {

            @Override
            public String getElementText(PsiElement element) {
              DomElement domElement = getDomElement(element);
              if (domElement instanceof DomInfraBean) {
                return InfraPresentationProvider.getBeanName(((DomInfraBean) domElement));
              }
              return super.getElementText(element);
            }

            @Override
            protected Icon getIcon(PsiElement element) {
              return InfraPresentationProvider.getInfraIcon(getDomElement(element));
            }
          };

  @Override
  public String getElementText(PsiElement element) {
    PsiElement psiElement = getElementToProcess(element);
    if (psiElement instanceof XmlTag) {
      return DOM_RENDERER.getElementText(psiElement);
    }

    CommonInfraBean infraBean;
    if (psiElement instanceof PsiAnnotation) {
      PsiMember member = PsiTreeUtil.getParentOfType(psiElement, PsiMember.class);
      infraBean = member == null ? null : getJamBean(member);
    }
    else {
      infraBean = InfraBeanPomTargetUtils.getBean(element);
    }

    if (infraBean != null) {
      return InfraPresentationProvider.getBeanName(infraBean);
    }

    return super.getElementText(psiElement);
  }

  private static PsiElement getElementToProcess(PsiElement element) {
    if (element instanceof PomTargetPsiElement) {
      PomTarget target = ((PomTargetPsiElement) element).getTarget();
      if (target instanceof BeanPsiTarget) {
        return ((BeanPsiTarget) target).getNavigationElement();
      }
    }
    return element;
  }

  @Override
  public String getContainerText(PsiElement element, String name) {
    PsiElement psiElement = getElementToProcess(element);
    if (psiElement instanceof XmlTag) {
      return getUniqueVirtualFilePath(psiElement);
    }

    if (psiElement instanceof PsiAnnotation) {
      PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
      if (psiClass != null && psiClass.getName() != null) {
        return getUniqueVirtualFilePath(psiClass);
      }
    }

    return getUniqueVirtualFilePath(element);
  }

  private static String getUniqueVirtualFilePath(PsiElement psiElement) {
    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
    return "(" + UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(psiElement.getProject(), virtualFile) + ")";
  }

  @Nullable
  @Override
  protected Icon getIcon(PsiElement element) {
    PsiElement psiElement = getElementToProcess(element);
    if (psiElement instanceof XmlTag) {
      return DOM_RENDERER.getIcon(psiElement);
    }
    else if (psiElement instanceof PsiAnnotation) {
      PsiMember member = PsiTreeUtil.getParentOfType(psiElement, PsiMember.class);
      CommonInfraBean springBean = member == null ? null : getJamBean(member);
      if (springBean != null) {
        return Icons.SpringJavaBean;
      }
    }
    else {
      if (!DumbService.isDumb(element.getProject())) {
        CommonInfraBean bean = InfraBeanPomTargetUtils.getBean(element);
        if (bean != null) {
          return InfraPresentationProvider.getInfraIcon(bean);
        }
      }
    }
    return super.getIcon(psiElement);
  }

  private static JamPsiMemberInfraBean getJamBean(PsiMember member) {
    return JamService.getJamService(member.getProject())
            .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, member);
  }
}

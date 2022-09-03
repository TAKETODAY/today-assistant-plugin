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

package cn.taketoday.assistant.editor;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.documentation.QuickDocUtil;
import com.intellij.codeInsight.javadoc.JavaDocInfoGeneratorFactory;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.usageView.UsageViewTypeLocation;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.pom.InfraBeanPomTargetUtils;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.xml.beans.Description;
import cn.taketoday.assistant.model.xml.beans.ScopedElement;
import cn.taketoday.lang.Nullable;

public class InfraBeanDocumentationProvider extends AbstractDocumentationProvider {

  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    CommonInfraBean springBean = InfraBeanPomTargetUtils.getBean(element);
    if (springBean != null && springBean.isValid()) {
      StringBuilder sb = new StringBuilder("<div class='definition'><pre>");
      sb.append(getSpringBeanTypeName(springBean));
      String beanName = InfraPresentationProvider.getBeanName(springBean);
      sb.append(" <b>").append(StringUtil.escapeXmlEntities(beanName)).append("</b>");
      PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
      if (beanClass != null) {
        sb.append("<br>");
        PsiClassType beanClassType = PsiTypesUtil.getClassType(beanClass);
        JavaDocInfoGeneratorFactory.create(beanClass.getProject(), null).generateType(sb, beanClassType, beanClass, true);
      }

      VirtualFile file = springBean.getContainingFile().getVirtualFile();
      if (file != null) {
        sb.append("<br>");
        sb.append(ProjectUtil.calcRelativeToProjectPath(file, element.getProject()));
      }

      sb.append("</pre></div>");
      sb.append("<div class='content'>");
      if (springBean instanceof Description description) {
        String documentation = description.getDescription().getValue();
        if (StringUtil.isNotEmpty(documentation)) {
          sb.append(documentation);
          sb.append("<br><br>");
        }
      }
      else if (springBean instanceof JamPsiMemberInfraBean<?> jamPsiMemberSpringBean) {
        PsiAnnotation description = AnnotationUtil.findAnnotation(jamPsiMemberSpringBean.getPsiElement(), true,
                "cn.taketoday.context.annotation.Description");
        if (description != null) {
          sb.append(AnnotationUtil.getStringAttributeValue(description, null));
          sb.append("<br><br>");
        }
      }

      sb.append("</div>");
      sb.append("<table class='sections'>");
      String[] aliases = springBean.getAliases();
      if (aliases.length > 0) {
        appendSection(sb, InfraBundle.message("aliases"), StringUtil.join(aliases, ", "));
      }

      InfraProfile profile = springBean.getProfile();
      if (profile != InfraProfile.DEFAULT) {
        appendSection(sb, InfraBundle.message("profile"), StringUtil.join(profile.getExpressions(), ", "));
      }

      if (springBean instanceof ScopedElement) {
        GenericAttributeValue<BeanScope> scope = ((ScopedElement) springBean).getScope();
        if (DomUtil.hasXml(scope)) {
          appendSection(sb, InfraBundle.message("scope"), scope.getStringValue());
        }
      }

      sb.append("</table>");
      return sb.toString();
    }
    else {
      return null;
    }
  }

  private static void appendSection(StringBuilder sb, String sectionName, String sectionContent) {
    sb.append("<tr><td valign='top' class='section'><p>").append(sectionName).append(":").append("</td><td valign='top'>");
    sb.append(sectionContent);
    sb.append("</td>");
  }

  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    return QuickDocUtil.inferLinkFromFullDocumentation(this, element, originalElement, getQuickNavigateInfoInner(element));
  }

  @Nullable
  private static String getQuickNavigateInfoInner(PsiElement element) {
    CommonInfraBean springBean = InfraBeanPomTargetUtils.getBean(element);
    if (springBean != null && springBean.isValid()) {
      String beanName = InfraPresentationProvider.getBeanName(springBean);
      PsiFile containingFile = springBean.getContainingFile();
      StringBuilder sb = (new StringBuilder(getSpringBeanTypeName(springBean))).append(" ").append(beanName).append(" [").append(containingFile.getName()).append("]");
      PsiClass psiClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
      if (psiClass != null) {
        sb.append("\n ").append(psiClass.getQualifiedName());
      }

      return sb.toString();
    }
    else {
      return null;
    }
  }

  private static String getSpringBeanTypeName(CommonInfraBean springBean) {
    PsiElement identifyingPsiElement = springBean.getIdentifyingPsiElement();

    assert identifyingPsiElement != null : springBean;

    return ElementDescriptionUtil.getElementDescription(identifyingPsiElement, UsageViewTypeLocation.INSTANCE);
  }
}

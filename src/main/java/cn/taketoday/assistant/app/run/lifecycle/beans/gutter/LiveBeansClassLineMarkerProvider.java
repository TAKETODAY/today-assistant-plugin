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

package cn.taketoday.assistant.app.run.lifecycle.beans.gutter;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.jam.JamService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UAnnotationUtils;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.app.run.InfraRunIcons;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

final class LiveBeansClassLineMarkerProvider extends RelatedItemLineMarkerProvider {

  public String getId() {
    return "LiveBeansClassLineMarkerProvider";
  }

  public String getName() {
    return InfraAppBundle.message("runtime.beans.class.gutter.icon.name");
  }

  public Icon getIcon() {
    return InfraRunIcons.Gutter.LiveBean;
  }

  public void collectNavigationMarkers(List<? extends PsiElement> elements, Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {
    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement != null && cn.taketoday.assistant.app.run.lifecycle.beans.gutter.LiveBeansNavigationHandler.hasLiveBeansModels(psiElement.getProject())) {
      super.collectNavigationMarkers(elements, result, forNavigation);
    }
  }

  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UDeclaration identifierAnnotationOwner = UAnnotationUtils.getIdentifierAnnotationOwner(psiElement);
    if (identifierAnnotationOwner instanceof UClass uClass) {
      annotateAnno(psiElement, uClass, result);
    }
    else if (identifierAnnotationOwner instanceof UMethod) {
      annotateMethod(psiElement, (UMethod) identifierAnnotationOwner, result);
    }
    else {
      UElement uParentForIdentifier = UastUtils.getUParentForIdentifier(psiElement);
      if (uParentForIdentifier instanceof UClass uClass) {
        annotateClass(psiElement, uClass, result);
      }
    }
  }

  private static void annotateAnno(PsiElement nameIdentifier, UClass uClass, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    String beanName;
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (!InfraUtils.isBeanCandidateClass(psiClass)) {
      return;
    }
    JamPsiMemberInfraBean jamPsiMemberBean = JamService.getJamService(psiClass.getProject())
            .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass);
    if (!(jamPsiMemberBean instanceof InfraStereotypeElement stereotypeElement) || (beanName = jamPsiMemberBean.getBeanName()) == null) {
      return;
    }
    PsiElement psiAnnotationIdentifier = UAnnotationUtils.getNameElement(UastContextKt.toUElement(stereotypeElement.getPsiAnnotation()));
    if (psiAnnotationIdentifier != nameIdentifier) {
      return;
    }
    annotateStereotype(nameIdentifier, beanName, psiClass, result);
  }

  private static void annotateStereotype(PsiElement nameIdentifier, String beanName, PsiClass psiClass, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    Predicate<LiveBean> beanMatcher = liveBean -> {
      LiveResource liveResource = liveBean.getResource();
      if (liveResource == null) {
        return false;
      }
      if (liveResource.hasDescription() && !liveResource.matchesClass(psiClass, true)) {
        return false;
      }
      return liveBean.matches(psiClass);
    };
    cn.taketoday.assistant.app.run.lifecycle.beans.gutter.LiveBeansNavigationHandler.addLiveBeansGutterIcon(beanName, beanMatcher, psiClass.getProject(), nameIdentifier, result);
  }

  private static void annotateMethod(PsiElement nameIdentifier, UMethod uMethod, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    ContextJavaBean contextJavaBean;
    String beanName;
    PsiMethod psiMethod = UElementKt.getAsJavaPsiElement(uMethod, PsiMethod.class);
    if (psiMethod == null) {
      return;
    }
    PsiClass psiClass = psiMethod.getContainingClass();
    if (!InfraUtils.isBeanCandidateClass(psiClass) || psiMethod.isConstructor() || (contextJavaBean = JamService.getJamService(psiMethod.getProject())
            .getJamElement(ContextJavaBean.BEAN_JAM_KEY, psiMethod)) == null || (beanName = contextJavaBean.getBeanName()) == null) {
      return;
    }
    PsiElement psiAnnotationIdentifier = UAnnotationUtils.getNameElement(UastContextKt.toUElement(contextJavaBean.getPsiAnnotation()));
    if (psiAnnotationIdentifier != nameIdentifier) {
      return;
    }
    Predicate<LiveBean> beanMatcher = liveBean -> {
      LiveResource liveResource = liveBean.getResource();
      return liveResource != null && liveResource.matchesClass(psiClass, false);
    };
    cn.taketoday.assistant.app.run.lifecycle.beans.gutter.LiveBeansNavigationHandler.addLiveBeansGutterIcon(beanName, beanMatcher, psiMethod.getProject(), nameIdentifier, result);
  }

  private static void annotateClass(PsiElement nameIdentifier, UClass uClass, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    CustomInfraComponent stereotypeElement;
    String beanName;
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (!InfraUtils.isBeanCandidateClass(psiClass)) {
      return;
    }
    JamPsiMemberInfraBean springBean = JamService.getJamService(psiClass.getProject()).getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass);
    if ((springBean instanceof InfraStereotypeElement)
            || (stereotypeElement = findCustomStereotype(psiClass)) == null
            || !stereotypeElement.isValid()
            || (beanName = stereotypeElement.getBeanName()) == null) {
      return;
    }
    annotateStereotype(nameIdentifier, beanName, psiClass, result);
  }

  @Nullable
  private static CustomInfraComponent findCustomStereotype(PsiClass psiClass) {
    for (JamBeanPointer pointer : JavaClassInfo.from(psiClass).getStereotypeMappedBeans()) {
      CommonInfraBean customSpringComponent = pointer.getBean();
      if (customSpringComponent instanceof CustomInfraComponent component) {
        return component;
      }
    }
    return null;
  }
}

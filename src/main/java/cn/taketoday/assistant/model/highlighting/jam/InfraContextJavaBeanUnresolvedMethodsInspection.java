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

import com.intellij.codeInsight.daemon.impl.quickfix.CreateMethodFixKt;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTypesUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public final class InfraContextJavaBeanUnresolvedMethodsInspection extends AbstractInfraLocalInspection {

  public InfraContextJavaBeanUnresolvedMethodsInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass aClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement sourcePsi;
    if (InfraUtils.isInEnabledModule(aClass) && (sourcePsi = aClass.getSourcePsi()) != null) {
      ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
      for (UMethod uMethod : aClass.getMethods()) {
        checkMethod(uMethod, holder);
      }
      return holder.getResultsArray();
    }
    return null;
  }

  private static void checkMethod(UMethod uMethod, ProblemsHolder holder) {
    ContextJavaBean contextJavaBean = getContextJavaBean(uMethod, holder.getProject());
    if (contextJavaBean != null) {
      checkContextJavaBean(contextJavaBean, holder);
    }
  }

  private static void checkContextJavaBean(ContextJavaBean contextJavaBean, ProblemsHolder holder) {
    checkMethodExists(contextJavaBean, holder, contextJavaBean.getInitMethodAttributeElement());
    checkMethodExists(contextJavaBean, holder, contextJavaBean.getDestroyMethodAttributeElement());
  }

  private static void checkMethodExists(ContextJavaBean contextJavaBean, ProblemsHolder holder, JamStringAttributeElement<PsiMethod> methodAttributeElement) {
    if (StringUtil.isNotEmpty(methodAttributeElement.getStringValue()) && methodAttributeElement.getValue() == null) {
      PsiAnnotationMemberValue psiElement = methodAttributeElement.getPsiElement();
      PsiAnnotation psiAnnotation = psiElement instanceof PsiAnnotation ? (PsiAnnotation) psiElement : contextJavaBean.getPsiAnnotation();
      PsiElement sourcePsiElement = UElementKt.getSourcePsiElement(UastContextKt.toUElement(psiAnnotation));

      holder.registerProblem(sourcePsiElement != null ? sourcePsiElement : psiAnnotation,
              InfraBundle.message("ContextJavaBeanUnresolvedMethodsInspection.cannot.resolve.method"),
              makeFixes(contextJavaBean, methodAttributeElement));
    }
  }

  private static LocalQuickFix[] makeFixes(ContextJavaBean contextJavaBean, JamStringAttributeElement<PsiMethod> methodAttributeElement) {
    String methodName;
    PsiClass clazz = PsiTypesUtil.getPsiClass(contextJavaBean.getBeanType());
    if (clazz == null || (methodName = methodAttributeElement.getStringValue()) == null) {
      return null;
    }
    return CreateMethodFixKt.createVoidMethodFixes(clazz, methodName, JvmModifier.PRIVATE);
  }

  @Nullable
  private static ContextJavaBean getContextJavaBean(UMethod uMethod, Project project) {
    JamPsiMemberInfraBean memberSpringBean = JamService.getJamService(project)
            .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, uMethod.getJavaPsi());
    if (memberSpringBean instanceof ContextJavaBean) {
      return (ContextJavaBean) memberSpringBean;
    }
    return null;
  }
}

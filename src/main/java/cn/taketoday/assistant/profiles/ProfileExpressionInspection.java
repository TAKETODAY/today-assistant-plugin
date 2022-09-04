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

package cn.taketoday.assistant.profiles;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.jam.profile.InfraContextProfile;
import cn.taketoday.assistant.util.InfraUtils;

public class ProfileExpressionInspection extends AbstractInfraLocalInspection {

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement sourcePsi = uClass.getSourcePsi();
    if (sourcePsi == null) {
      return null;
    }
    PsiClass psiClass = uClass.getJavaPsi();
    if (InfraUtils.isBeanCandidateClassInProject(psiClass)) {
      return checkProfile(psiClass, sourcePsi, manager, isOnTheFly);
    }
    return null;
  }

  public ProblemDescriptor[] checkMethod(UMethod uMethod, InspectionManager manager, boolean isOnTheFly) {
    PsiElement sourcePsi = uMethod.getSourcePsi();
    if (sourcePsi == null) {
      return null;
    }
    PsiMethod psiMethod = uMethod.getJavaPsi();
    PsiClass psiClass = psiMethod.getContainingClass();
    if (InfraUtils.isBeanCandidateClassInProject(psiClass)) {
      return checkProfile(psiMethod, sourcePsi, manager, isOnTheFly);
    }
    return null;
  }

  private static ProblemDescriptor[] checkProfile(PsiElement psiElement, PsiElement sourcePsi, InspectionManager manager, boolean isOnTheFly) {
    InfraContextProfile profile = JamService.getJamService(sourcePsi.getProject()).getJamElement(InfraContextProfile.CONTEXT_PROFILE_JAM_KEY, psiElement);
    if (profile == null) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
    for (JamStringAttributeElement<String> attributeElement : profile.getValueElements()) {
      checkProfileAttribute(holder, attributeElement);
    }
    return holder.getResultsArray();
  }

  private static void checkProfileAttribute(ProblemsHolder holder, JamStringAttributeElement<String> attributeElement) {
    PsiElement psiElement;
    UElement uElement = UastContextKt.toUElement(attributeElement.getPsiElement());
    if (uElement == null || (psiElement = uElement.getSourcePsi()) == null) {
      return;
    }
    try {
      InfraProfilesFactory.of().parseProfileExpressions(new SmartList<>(attributeElement.getStringValue()));
    }
    catch (InfraProfilesFactory.MalformedProfileExpressionException e) {
      holder.registerProblem(psiElement, e.getMessage());
    }
  }
}

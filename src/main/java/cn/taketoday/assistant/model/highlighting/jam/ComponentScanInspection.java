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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamReferenceContributorKt;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;

import java.util.Collection;

import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.jam.stereotype.AbstractComponentScan;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;

public class ComponentScanInspection extends AbstractInfraLocalInspection {

  public ComponentScanInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement sourcePsiElement;
    PsiClass aClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (aClass == null || !InfraUtils.isConfigurationOrMeta(aClass) || (sourcePsiElement = UElementKt.getSourcePsiElement(uClass)) == null) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, sourcePsiElement.getContainingFile(), isOnTheFly);
    for (InfraBeansPackagesScan scan : InfraJamService.of().getBeansPackagesScan(aClass)) {
      if (scan instanceof AbstractComponentScan) {
        checkComponentScan(holder, (AbstractComponentScan) scan);
      }
    }
    return holder.getResultsArray();
  }

  private static void checkComponentScan(ProblemsHolder holder, AbstractComponentScan springComponentScan) {
    PsiAnnotation annotation = springComponentScan.getAnnotation();
    if (annotation == null) {
      return;
    }
    PsiElementRef<PsiAnnotation> ref = PsiElementRef.real(annotation);
    for (JamStringAttributeMeta.Collection<Collection<PsiPackage>> packageAttribute : springComponentScan.getPackageJamAttributes()) {
      for (JamStringAttributeElement<Collection<PsiPackage>> stringAttributeElement : packageAttribute.getJam(ref)) {

        PsiAnnotationMemberValue value = stringAttributeElement.getPsiElement();
        if (value instanceof PsiLiteral literal) {
          for (PsiReference reference : literal.getReferences()) {
            PsiReference unwrapReference = JamReferenceContributorKt.unwrapReference(reference);
            if ((unwrapReference instanceof PsiPackageReference psiPackageReference) && psiPackageReference.multiResolve(false).length == 0) {
              holder.registerProblem(reference);
            }
          }
        }
      }
    }
  }
}

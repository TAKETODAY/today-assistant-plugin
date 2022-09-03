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

package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemService;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSql;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSqlGroup;
import cn.taketoday.assistant.model.jam.utils.InfraResourceLocationsUtil;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function0;

public final class TestingSqlInspection extends AbstractInfraLocalInspection {

  public TestingSqlInspection() {
    super(UMethod.class, UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement nameIdentifier;
    UElement uastAnchor = uClass.getUastAnchor();
    if (uastAnchor == null || (nameIdentifier = uastAnchor.getSourcePsi()) == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiClass psiClass = uClass.getJavaPsi();
    return checkElement(psiClass, manager, nameIdentifier, isOnTheFly, () -> InfraTestingSqlGroup.CLASS_META.getJamElement(psiClass));
  }

  public ProblemDescriptor[] checkMethod(UMethod method, InspectionManager manager, boolean isOnTheFly) {
    PsiElement nameIdentifier = UElementKt.getSourcePsiElement(method.getUastAnchor());
    if (nameIdentifier != null) {
      PsiMethod psiMethod = method.getJavaPsi();
      return checkElement(psiMethod, manager, nameIdentifier, isOnTheFly, () -> InfraTestingSqlGroup.METHOD_META.getJamElement(psiMethod));
    }
    return ProblemDescriptor.EMPTY_ARRAY;
  }

  private ProblemDescriptor[] checkElement(PsiModifierListOwner psiElement, InspectionManager manager,
          PsiElement nameIdentifier, boolean isOnTheFly, Function0<? extends InfraTestingSqlGroup> function0) {
    ProblemsHolder holder = new ProblemsHolder(manager, nameIdentifier.getContainingFile(), isOnTheFly);
    for (InfraTestingSql sql : getSqlAnnotations(psiElement)) {
      checkSqlFileReferences(sql, holder);
    }
    for (InfraTestingSql sql2 : getSqlAnnotations(function0.invoke())) {
      checkSqlFileReferences(sql2, holder);
    }
    return holder.getResultsArray();
  }

  private List<InfraTestingSql> getSqlAnnotations(InfraTestingSqlGroup group) {
    if (group != null) {
      List<InfraTestingSql> sqlAnnotations = group.getSqlAnnotations();
      if (sqlAnnotations != null) {
        return sqlAnnotations;
      }
    }
    return Collections.emptyList();
  }

  private List<InfraTestingSql> getSqlAnnotations(PsiModifierListOwner psiElement) {
    SemService service = SemService.getSemService(psiElement.getProject());
    PsiAnnotation[] annotations = AnnotationUtil.findAnnotations(psiElement, SetsKt.setOf(AnnotationConstant.TEST_SQL));

    SmartList<InfraTestingSql> smartList = new SmartList<>();
    for (PsiAnnotation element$iv : annotations) {
      List<InfraTestingSql> list$iv = service.getSemElements(InfraTestingSql.REPEATABLE_ANNO_JAM_KEY, element$iv);
      CollectionsKt.addAll(smartList, list$iv);
    }
    return smartList;
  }

  private void checkSqlFileReferences(InfraTestingSql sql, ProblemsHolder holder) {
    for (JamStringAttributeElement<?> attributeElement : sql.getScriptElements()) {
      InfraResourceLocationsUtil infraResourceLocationsUtil = InfraResourceLocationsUtil.INSTANCE;
      infraResourceLocationsUtil.checkResourceLocation(holder, attributeElement);
    }
  }
}

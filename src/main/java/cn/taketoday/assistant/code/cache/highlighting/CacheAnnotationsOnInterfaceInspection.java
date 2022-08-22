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

package cn.taketoday.assistant.code.cache.highlighting;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.semantic.SemService;

import org.jetbrains.uast.UAnnotated;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public final class CacheAnnotationsOnInterfaceInspection extends AbstractInfraLocalInspection {

  public CacheAnnotationsOnInterfaceInspection() {
    super(UClass.class, UMethod.class);
  }

  @Override
  public ProblemDescriptor[] checkMethod(UMethod uMethod, InspectionManager manager, boolean isOnTheFly) {
    UClass containingClass;
    PsiElement sourcePsi;

    if (!CommonUtils.isInInfraEnabledModule(uMethod)
            || (containingClass = UastUtils.getContainingUClass(uMethod)) == null
            || !containingClass.isInterface()
            || (sourcePsi = containingClass.getSourcePsi()) == null
            || isImplicitlySubclassed(containingClass.getJavaPsi())) {
      return null;
    }

    ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
    PsiMethod method = uMethod.getJavaPsi();
    List<CacheableElement> semElements = SemService.getSemService(sourcePsi.getProject())
            .getSemElements(CacheableElement.CACHEABLE_ROOT_JAM_KEY, method);
    for (CacheableElement<?> cacheableElement : semElements) {
      registerProblemIfAnnotationExists(holder, cacheableElement.getAnnotation());
    }
    registerProblemIfAnnotationExists(holder, findMetaAnnotation(uMethod, CacheableConstant.CACHING));
    return holder.getResultsArray();
  }

  @Nullable
  private static UAnnotation findMetaAnnotation(UAnnotated declaration, String annotationName) {
    for (UAnnotation uAnnotation : declaration.getUAnnotations()) {
      PsiClass annotationClass = uAnnotation.resolve();
      if (annotationClass != null) {
        if (Objects.equals(annotationClass.getQualifiedName(), annotationName)) {
          return uAnnotation;
        }

        if (MetaAnnotationUtil.isMetaAnnotated(annotationClass, Collections.singletonList(annotationName))) {
          return uAnnotation;
        }
      }
    }
    return null;
  }

  private static void registerProblemIfAnnotationExists(ProblemsHolder holder, @Nullable PsiAnnotation annotation) {
    registerProblemIfAnnotationExists(holder, UastContextKt.toUElement(annotation, UAnnotation.class));
  }

  private static void registerProblemIfAnnotationExists(ProblemsHolder holder, @Nullable UAnnotation uAnnotation) {
    PsiElement element = UAnnotationKt.getNamePsiElement(uAnnotation);
    if (element != null) {
      holder.registerProblem(element, InfraBundle.message("cacheable.should.be.defined.on.concrete.method"),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }

  @Override
  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    if (CommonUtils.isInInfraEnabledModule(uClass) && uClass.isInterface() && !uClass.isAnnotationType()) {
      PsiElement sourcePsi = uClass.getSourcePsi();
      if (sourcePsi == null) {
        return null;
      }
      PsiClass aClass = uClass.getJavaPsi();
      if (isImplicitlySubclassed(aClass)) {
        return null;
      }
      ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
      for (CacheableElement<?> cacheableElement : SemService.getSemService(aClass.getProject()).getSemElements(CacheableElement.CACHEABLE_ROOT_JAM_KEY, aClass)) {
        registerProblemIfAnnotationExists(holder, cacheableElement.getAnnotation());
      }
      registerProblemIfAnnotationExists(holder, findMetaAnnotation(uClass, CacheableConstant.CACHING));
      return holder.getResultsArray();
    }
    return null;
  }

  private static boolean isImplicitlySubclassed(PsiClass clazz) {
    return InheritanceUtil.isInheritor(clazz, "cn.taketoday.data.repository.Repository");
  }

}

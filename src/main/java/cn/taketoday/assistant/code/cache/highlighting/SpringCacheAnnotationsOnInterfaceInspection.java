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

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.semantic.SemService;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.el.lexer._SpringELLexer;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import com.intellij.spring.model.highlighting.jam.SpringUastInspectionBase;
import com.intellij.spring.model.utils.SpringCommonUtils;

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

import cn.taketoday.lang.Nullable;

public final class SpringCacheAnnotationsOnInterfaceInspection extends SpringUastInspectionBase {
  private static void $$$reportNull$$$0(int i) {
    Object[] objArr = new Object[3];
    switch (i) {
      case 0:
      default:
        objArr[0] = "uMethod";
        break;
      case 1:
      case 5:
        objArr[0] = "manager";
        break;
      case _SpringELLexer.SELECT:
      case 3:
        objArr[0] = "holder";
        break;
      case _SpringELLexer.OPEN_BRACE:
        objArr[0] = "uClass";
        break;
      case _SpringELLexer.EL_EXPR:
        objArr[0] = "clazz";
        break;
    }
    objArr[1] = "com/intellij/spring/model/cacheable/highlighting/SpringCacheAnnotationsOnInterfaceInspection";
    switch (i) {
      case 0:
      case 1:
      default:
        objArr[2] = "checkMethod";
        break;
      case _SpringELLexer.SELECT:
      case 3:
        objArr[2] = "registerProblemIfAnnotationExists";
        break;
      case _SpringELLexer.OPEN_BRACE:
      case 5:
        objArr[2] = "checkClass";
        break;
      case _SpringELLexer.EL_EXPR:
        objArr[2] = "isImplicitlySubclassed";
        break;
    }
    throw new IllegalArgumentException(String.format("Argument for parameter '%s' of %s.%s must not be null", objArr));
  }

  public SpringCacheAnnotationsOnInterfaceInspection() {
    super(UClass.class, UMethod.class);
  }

  public ProblemDescriptor[] checkMethod(UMethod uMethod, InspectionManager manager, boolean isOnTheFly) {
    UClass containingClass;
    PsiElement sourcePsi;
    if (uMethod == null) {
      $$$reportNull$$$0(0);
    }
    if (manager == null) {
      $$$reportNull$$$0(1);
    }
    if (!SpringCommonUtils.isInSpringEnabledModule(uMethod) || (containingClass = UastUtils.getContainingUClass(
            uMethod)) == null || !containingClass.isInterface() || (sourcePsi = containingClass.getSourcePsi()) == null || isImplicitlySubclassed(containingClass.getJavaPsi())) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
    PsiMethod method = uMethod.getJavaPsi();
    for (CacheableElement<?> cacheableElement : SemService.getSemService(sourcePsi.getProject()).getSemElements(CacheableElement.CACHEABLE_ROOT_JAM_KEY, method)) {
      registerProblemIfAnnotationExists(holder, cacheableElement.getAnnotation());
    }
    registerProblemIfAnnotationExists(holder, findMetaAnnotation(uMethod, CacheableConstant.CACHING));
    return holder.getResultsArray();
  }

  @Nullable
  private static UAnnotation findMetaAnnotation(UAnnotated declaration, String annotationName) {
    for (UAnnotation uAnnotation : declaration.getUAnnotations()) {
      PsiClass annotationClass = uAnnotation.resolve();
      if (annotationClass == null || (!Objects.equals(annotationClass.getQualifiedName(), annotationName) && !MetaAnnotationUtil.isMetaAnnotated(annotationClass,
              Collections.singletonList(annotationName)))) {
      }
      return uAnnotation;
    }
    return null;
  }

  private static void registerProblemIfAnnotationExists(ProblemsHolder holder, @Nullable PsiAnnotation annotation) {
    if (holder == null) {
      $$$reportNull$$$0(2);
    }
    registerProblemIfAnnotationExists(holder, UastContextKt.toUElement(annotation, UAnnotation.class));
  }

  private static void registerProblemIfAnnotationExists(ProblemsHolder holder, @Nullable UAnnotation uAnnotation) {
    if (holder == null) {
      $$$reportNull$$$0(3);
    }
    PsiElement element = UAnnotationKt.getNamePsiElement(uAnnotation);
    if (element != null) {
      holder.registerProblem(element, SpringBundle.message("cacheable.should.be.defined.on.concrete.method", new Object[0]), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new LocalQuickFix[0]);
    }
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    if (uClass == null) {
      $$$reportNull$$$0(4);
    }
    if (manager == null) {
      $$$reportNull$$$0(5);
    }
    if (SpringCommonUtils.isInSpringEnabledModule(uClass) && uClass.isInterface() && !uClass.isAnnotationType()) {
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
    if (clazz == null) {
      $$$reportNull$$$0(6);
    }
    return InheritanceUtil.isInheritor(clazz, "cn.taketoday.data.repository.Repository") || AnnotationUtil.isAnnotated(clazz,
            List.of("cn.taketoday.cloud.netflix.feign.FeignClient", "cn.taketoday.cloud.openfeign.FeignClient"), 0);
  }
}

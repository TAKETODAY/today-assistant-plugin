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

package cn.taketoday.assistant.model.highlighting.providers;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInspection.inheritance.ImplicitSubclassProvider;
import com.intellij.java.analysis.JavaAnalysisBundle;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;

import java.util.HashMap;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.LazyThreadSafetyMode;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InfraImplicitSubclassProvider extends ImplicitSubclassProvider {
  private final List<String> transactionalAnnotations = List.of(
          AnnotationConstant.TRANSACTIONAL,
          "javax.transaction.Transactional",
          "jakarta.transaction.Transactional"
  );
  private final List<String> cacheableAnnotations = List.of(
          CacheableConstant.CACHEABLE,
          CacheableConstant.CACHING,
          CacheableConstant.CACHE_EVICT,
          CacheableConstant.CACHE_PUT,
          CacheableConstant.CACHE_CONFIG
  );
  private final List<String> definitelyOverridableMethods =
          CollectionsKt.plus(CollectionsKt.plus(List.of(AnnotationConstant.ASYNC), transactionalAnnotations), cacheableAnnotations);
  private final List<String> definitelyOverridableClasses = CollectionsKt.plus(
          CollectionsKt.plus(List.of(AnnotationConstant.ASYNC, AnnotationConstant.CONFIGURATION),
                  transactionalAnnotations), cacheableAnnotations);
  private final List<String> configuration = List.of(AnnotationConstant.CONFIGURATION);

  @Nullable
  public ImplicitSubclassProvider.SubclassingInfo getSubclassingInfo(PsiClass psiClass) {
    Lazy<Boolean> hasAspectJTransactionMode = LazyKt.lazy(LazyThreadSafetyMode.NONE, () -> {
      InfraModelService instance = InfraModelService.of();
      PsiElement element = psiClass.getOriginalElement();
      if (element == null) {
        throw new NullPointerException("null cannot be cast to non-null type com.intellij.psi.PsiClass");
      }
      else {
        CommonInfraModel model = instance.getPsiClassModel((PsiClass) element);
        return InfraImplicitSubclassProviderKt.hasAspectJTransactionMode(model);
      }
    });

    HashMap<PsiMethod, OverridingInfo> methodsToOverride = new HashMap<>();

    for (PsiMethod method : psiClass.getMethods()) {
      Intrinsics.checkNotNullExpressionValue(method, "method");
      OverridingInfo overridingInfo = getOverridingInfo(method, hasAspectJTransactionMode);
      if (overridingInfo != null) {
        methodsToOverride.put(method, overridingInfo);
      }
    }

    label70:
    {
      PsiModifierList modifierList = psiClass.getModifierList();
      if (modifierList != null) {
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        if (annotations.length == 0) {
          break label70;
        }
      }

      PsiAnnotation annotation = MetaAnnotationUtil.findMetaAnnotations(psiClass, this.definitelyOverridableClasses).findFirst().orElse(null);
      if (annotation != null) {
        Object[] objects = new Object[1];
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName != null) {
          qualifiedName = StringUtilRt.getShortName(qualifiedName);
        }

        objects[0] = qualifiedName;
        String message = message("ImplicitSubclassInspection.display.forClass.annotated", objects);
        return new ImplicitSubclassProvider.SubclassingInfo(message, methodsToOverride, false);
      }
    }

    if (!methodsToOverride.isEmpty()) {
      String message = JavaAnalysisBundle.message("inspection.implicit.subclass.display.forClass", psiClass.getName());
      return new ImplicitSubclassProvider.SubclassingInfo(message, methodsToOverride, false);
    }
    else {
      return null;
    }
  }

  public boolean isApplicableTo(PsiClass psiClass) {
    Intrinsics.checkNotNullParameter(psiClass, "psiClass");
    return InfraUtils.isBeanCandidateClassInProject(psiClass);
  }

  private OverridingInfo getOverridingInfo(PsiMethod method, Lazy<Boolean> lazy) {
    PsiModifierList modifierList = method.getModifierList();
    PsiAnnotation[] annotations = modifierList.getAnnotations();
    if (annotations.length == 0) {
      return null;
    }
    PsiAnnotation it = MetaAnnotationUtil.findMetaAnnotations(method, this.definitelyOverridableMethods)
            .filter(annotation -> {
              boolean isTransactional = CollectionsKt.contains(this.transactionalAnnotations, annotation.getQualifiedName());
              return !isTransactional || !lazy.getValue();
            })
            .findFirst()
            .orElse(null);
    if (it != null) {
      Object[] objArr = new Object[1];
      String qualifiedName = it.getQualifiedName();
      if (qualifiedName != null) {
        objArr[0] = StringUtilRt.getShortName(qualifiedName);
      }
      String message = message("ImplicitSubclassInspection.display.forMethod.annotated", objArr);
      return new OverridingInfo(message, false);
    }
    else if (beanInConfiguration(method)) {
      String message2 = message("ImplicitSubclassInspection.display.bean.in.configuration");
      return new OverridingInfo(message2, false);
    }
    else {
      return null;
    }
  }

  private boolean beanInConfiguration(PsiMethod method) {
    PsiModifierListOwner containingClass;
    return !method.hasModifierProperty("static")
            && (containingClass = method.getContainingClass()) != null
            && MetaAnnotationUtil.isMetaAnnotated(containingClass, this.configuration)
            && MetaAnnotationUtil.isMetaAnnotated(method, List.of(AnnotationConstant.BEAN));
  }
}

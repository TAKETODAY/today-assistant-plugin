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
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamService;
import com.intellij.jam.model.util.JamAnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UIdentifier;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastCallKind;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class ConfigurationProxyMethodsInspection extends AbstractInfraLocalInspection {

  public ConfigurationProxyMethodsInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass aClass, InspectionManager manager, boolean isOnTheFly) {

    Module module = ModuleUtilCore.findModuleForPsiElement(aClass.getJavaPsi());
    if (module == null || !InfraUtils.isEnabledModule(module)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiClass psiClass = aClass.getJavaPsi();
    if (!InfraUtils.isBeanCandidateClass(psiClass)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    Configuration configurationAnnotation = Configuration.META.getJamElement(psiClass);
    if (configurationAnnotation != null && configurationAnnotation.isProxyBeanMethods()) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    else if (!isComponentOrBeanSource(psiClass)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    else {
      ProblemsHolder holder = new ProblemsHolder(manager, psiClass.getContainingFile(), isOnTheFly);
      Set<String> beanProviderMethodNames = getAllBeanProviderMethodNames(module);
      if (beanProviderMethodNames.isEmpty()) {
        return ProblemDescriptor.EMPTY_ARRAY;
      }
      for (UMethod uMethod : aClass.getMethods()) {
        UExpression uastBody = uMethod.getUastBody();
        PsiElement methodBody = uastBody != null ? uastBody.getSourcePsi() : null;
        if (methodBody != null) {
          methodBody.accept(new PsiRecursiveElementVisitor() {
            public void visitElement(PsiElement element) {

              UCallExpression node = UastContextKt.toUElement(element, UCallExpression.class);
              if (Intrinsics.areEqual(node != null ? node.getKind() : null, UastCallKind.METHOD_CALL)
                      && CollectionsKt.contains(beanProviderMethodNames, node.getMethodName())) {
                ConfigurationProxyMethodsInspection.this.checkMethodCall(node, configurationAnnotation, psiClass, holder);
              }
              super.visitElement(element);
            }
          });
        }
      }
      return holder.getResultsArray();
    }
  }

  private boolean isComponentOrBeanSource(PsiClass sourcePsiClass) {
    if (!InfraUtils.isConfigurationOrMeta(sourcePsiClass) && !InfraUtils.isComponentOrMeta(sourcePsiClass)) {
      InfraManager infraManager = InfraManager.from(sourcePsiClass.getProject());
      InfraModel model = infraManager.getInfraModelByFile(sourcePsiClass.getContainingFile());
      if (model != null) {
        return InfraModelSearchers.doesBeanExist(model, sourcePsiClass);
      }
      return false;
    }
    return true;
  }

  public void checkMethodCall(UCallExpression node, Configuration configurationAnnotation, PsiClass sourcePsiClass, ProblemsHolder holder) {
    PsiElement methodName;
    PsiMethod resolve;
    PsiClass callTargetClass;
    String message;
    UIdentifier methodIdentifier = node.getMethodIdentifier();
    if (methodIdentifier == null || (methodName = methodIdentifier.getSourcePsi()) == null || (resolve = node.resolve()) == null || (callTargetClass = resolve.getContainingClass()) == null) {
      return;
    }
    if (ContextJavaBean.METHOD_META.getJamElement(resolve) == null || !InheritanceUtil.isInheritorOrSelf(sourcePsiClass, callTargetClass, true)) {
      return;
    }
    if (configurationAnnotation != null) {
      message = InfraBundle.message("bean.method.called.from.configuration.without.proxy");
    }
    else {
      message = InfraBundle.message("bean.method.called.without.proxy");
    }
    String message2 = message;
    holder.registerProblem(methodName, message2, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }

  public static Set<String> getAllBeanProviderMethodNames(Module module) {
    return CachedValuesManager.getManager(module.getProject())
            .getCachedValue(module, () -> {
              JamService jamService = JamService.getJamService(module.getProject());
              HashSet<String> names = new HashSet<>();
              GlobalSearchScope scope = GlobalSearchScope.moduleScope(module);
              for (String annotation : JamAnnotationUtil.getChildAnnotations(module, AnnotationConstant.COMPONENT)) {
                List<ContextJavaBean> annotatedMethods = jamService.getJamMethodElements(ContextJavaBean.METHOD_META, annotation, scope);
                ArrayList<String> collection = new ArrayList<>(Math.max(annotatedMethods.size(), 10));
                for (ContextJavaBean it : annotatedMethods) {
                  PsiMethod psiElement = it.getPsiElement();
                  collection.add(psiElement.getName());
                }
                names.addAll(collection);
              }
              return CachedValueProvider.Result.createSingleDependency(CollectionsKt.toSet(names), PsiModificationTracker.MODIFICATION_COUNT);
            });
  }
}

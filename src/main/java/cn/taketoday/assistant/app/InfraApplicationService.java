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

package cn.taketoday.assistant.app;

import com.intellij.jam.JamService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.config.autoconfigure.jam.EnableAutoConfiguration;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraApplicationService {

  public static InfraApplicationService of() {
    return ApplicationManager.getApplication().getService(InfraApplicationService.class);
  }

  public boolean isInfraApplication(PsiClass psiClass) {
    return isNotPrivateMain(psiClass)
            && EnableAutoConfiguration.find(psiClass) != null;
  }

  /**
   * @param module module to search classes
   * @return the list of all classes in production scope of the given module
   * for which {@link #isInfraApplication(PsiClass)} returns {@code true}.
   */
  public List<PsiClass> getInfraApplications(Module module) {
    return !module.isDisposed() && !DumbService.isDumb(module.getProject())
                   && InfraUtils.findLibraryClass(module, AnnotationConstant.EnableAutoConfiguration) != null
           ? CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      JamService jamService = JamService.getJamService(module.getProject());
      GlobalSearchScope scope = module.getModuleScope(false);
      ArrayList<String> fqns = new ArrayList<>(EnableAutoConfiguration.getAnnotations().fun(module));
      fqns.add(AnnotationConstant.EnableAutoConfiguration);
      SmartList<PsiClass> applications = new SmartList<>();

      for (String fqn : fqns) {
        List<EnableAutoConfiguration> elements = jamService.getJamClassElements(EnableAutoConfiguration.JAM_KEY, fqn, scope);

        for (EnableAutoConfiguration element : elements) {
          PsiClass annotatedClass = element.getPsiElement();
          if (isNotPrivateMain(annotatedClass)) {
            applications.add(annotatedClass);
          }
        }
      }

      return CachedValueProvider.Result.create(applications, PsiModificationTracker.MODIFICATION_COUNT);
    }) : List.of();
  }

  /**
   * @param appClass infra application class
   * @return {@code true} if candidate class is found by {@link #findMainClassCandidate(PsiClass)} for the given Infra application,
   * and it contains {@code public static void main} method, otherwise {@code false}.
   */
  public boolean hasMainMethod(PsiClass appClass) {
    return UastUtils.findMainInClass(UastContextKt.toUElement(findMainClassCandidate(appClass), UClass.class)) != null;
  }

  /**
   * @param psiClass Spring Boot application class
   * @return class itself or candidate class which may contain main method to start the given Spring Boot application
   */
  @Nullable
  public PsiClass findMainClassCandidate(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return null;
    }
    else {
      UFile file = UastContextKt.getUastParentOfType(psiClass, UFile.class);
      if (file == null) {
        return psiClass;
      }
      else {
        SmartList<UMethod> mainMethodCandidates = new SmartList<>();
        for (UClass uClass : file.getClasses()) {
          UMethod[] methods = uClass.getMethods();

          for (UMethod method : methods) {
            if (UastUtils.getMainMethodClass(method) != null) {
              mainMethodCandidates.add(method);
            }
          }
        }

        if (mainMethodCandidates.size() == 1) {
          PsiMethod psiMethod = mainMethodCandidates.get(0).getJavaPsi();
          return psiMethod.getContainingClass();
        }
        else {
          return psiClass;
        }
      }
    }
  }

  private static boolean isNotPrivateMain(PsiClass psiClass) {
    return PsiMethodUtil.MAIN_CLASS.value(psiClass)
            && !psiClass.hasModifierProperty("private");
  }

}

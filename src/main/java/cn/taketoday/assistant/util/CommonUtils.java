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

package cn.taketoday.assistant.util;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.JavaLibraryModificationTracker;
import com.intellij.facet.FacetFinder;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.CharFilter;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.psi.util.PsiUtil;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringComponent;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.TodayLibraryUtil;
import cn.taketoday.assistant.beans.ConfigurationBean;
import cn.taketoday.assistant.facet.TodayFacet;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 19:03
 */
public abstract class CommonUtils {
  public static final String SPRING_DELIMITERS = ",; ";

  public static final CharFilter ourFilter = ch -> SPRING_DELIMITERS.indexOf(ch) >= 0;
  private static final Key<ParameterizedCachedValue<Boolean, Module>> IS_SPRING_CONFIGURED_KEY = Key.create("IS_SPRING_CONFIGURED_IN_MODULE");
  private static final ParameterizedCachedValueProvider<Boolean, Module> IS_SPRING_CONFIGURED_PROVIDER
          = new ParameterizedCachedValueProvider<>() {
    @Override
    public CachedValueProvider.Result<Boolean> compute(Module module) {
      return CachedValueProvider.Result.create(isInfraConfigured(module),
              ProjectRootManager.getInstance(module.getProject()),
              FacetFinder.getInstance(module.getProject()).getAllFacetsOfTypeModificationTracker(TodayFacet.FACET_TYPE_ID));
    }

    private boolean isInfraConfigured(Module module) {
      if (hasFacet(module)) {
        return true;
      }

      for (Module dependent : ModuleUtilCore.getAllDependentModules(module)) {
        if (hasFacet(dependent)) {
          return true;
        }
      }
      Set<Module> dependencies = new HashSet<>();
      ModuleUtilCore.getDependencies(module, dependencies);
      for (Module dependency : dependencies) {
        if (hasFacet(dependency)) {
          return true;
        }
      }

      return false;
    }
  };

  public static <T extends PsiFile> List<T> findConfigFilesInMetaInf(Module module, boolean withTests, String filename, Class<T> psiFileType) {
    GlobalSearchScope moduleScope = GlobalSearchScope.moduleRuntimeScope(module, withTests);
    return findConfigFilesInMetaInf(module.getProject(), moduleScope, filename, psiFileType);
  }

  public static <T extends PsiFile> List<T> findConfigFilesInMetaInf(Project project, GlobalSearchScope scope, String filename, Class<T> psiFileType) {
    GlobalSearchScope searchScope = getConfigFilesScope(project, scope);
    if (searchScope == null) {
      return Collections.emptyList();
    }
    else {
      PsiFile[] configFiles = FilenameIndex.getFilesByName(project, filename, searchScope);
      if (configFiles.length == 0) {
        return Collections.emptyList();
      }
      else {
        return ContainerUtil.findAll(configFiles, psiFileType);
      }
    }
  }

  @Nullable
  public static GlobalSearchScope getConfigFilesScope(Project project, GlobalSearchScope scope) {
    PsiPackage metaInfPackage = JavaPsiFacade.getInstance(project).findPackage("META-INF");
    if (metaInfPackage == null) {
      return null;
    }
    else {
      GlobalSearchScope packageScope = PackageScope.packageScope(metaInfPackage, false);
      return scope.intersectWith(packageScope);
    }
  }

  public static PsiClass findLibraryClass(@Nullable Module module, String className) {
    if (module != null && !module.isDisposed()) {
      Project project = module.getProject();
      Map<String, PsiClass> cache = CachedValuesManager.getManager(project).getCachedValue(module, () -> {
        Map<String, PsiClass> map = ConcurrentFactoryMap.createMap((key) -> findLibraryClass(project, key, GlobalSearchScope.moduleRuntimeScope(module, false)));
        return CachedValueProvider.Result.createSingleDependency(map, JavaLibraryModificationTracker.getInstance(module.getProject()));
      });
      return cache.get(className);
    }
    else {
      return null;
    }
  }

  @Nullable
  private static PsiClass findLibraryClass(Project project,
          String fqn, GlobalSearchScope searchScope) {
    return DumbService.getInstance(project)
            .runReadActionInSmartMode(() -> JavaPsiFacade.getInstance(project).findClass(fqn, searchScope));
  }

  public static boolean hasFacets(Project project) {
    return ProjectFacetManager.getInstance(project).hasFacets(TodayFacet.FACET_TYPE_ID);
  }

  public static boolean hasFacet(@Nullable Module module) {
    return module != null && TodayFacet.from(module) != null;
  }

  /**
   * Returns whether the given class has {@value AnnotationConstant#CONFIGURATION} annotation, <em>excluding</em> meta-annotations.
   *
   * @see #isConfigurationOrMeta(PsiClass)
   */
  public static boolean isConfiguration(@NotNull PsiClass psiClass) {
    return isBeanCandidateClass(psiClass)
            && AnnotationUtil.isAnnotated(psiClass, AnnotationConstant.CONFIGURATION, 0);
  }

  /**
   * Returns whether the given class is <em>(meta-)</em>annotated with {@value AnnotationConstant#CONFIGURATION}.
   *
   * @param psiClass Class to check.
   * @return {@code true} if class annotated.
   */
  public static boolean isConfigurationOrMeta(@NotNull PsiClass psiClass) {
    if (!isBeanCandidateClass(psiClass))
      return false;
    return JamService.getJamService(psiClass.getProject())
            .getJamElement(ConfigurationBean.JAM_KEY, psiClass) != null;
  }

  /**
   * @param psiClass Class to check.
   * @return whether the given class is annotated with {@value AnnotationConstant#COMPONENT}.
   * <p>NOTE: it doesn't consider {@value AnnotationConstant#SERVICE}, {@value AnnotationConstant#REPOSITORY} and so on</p>
   * @see #isStereotypeComponentOrMeta(PsiClass)
   */
  public static boolean isComponentOrMeta(@NotNull PsiClass psiClass) {
    if (!isBeanCandidateClass(psiClass))
      return false;
    return JamService.getJamService(psiClass.getProject())
            .getJamElement(psiClass, SpringComponent.META) != null;
  }

  /**
   * Returns whether the given class is <em>(meta-)</em>annotated with {@value AnnotationConstant#COMPONENT, AnnotationConstant#SERVICE, , AnnotationConstant#REPOSITORY}.
   *
   * @param psiClass Class to check.
   * @return {@code true} if class annotated.
   */
  public static boolean isStereotypeComponentOrMeta(@NotNull PsiClass psiClass) {
    if (!isBeanCandidateClass(psiClass))
      return false;
    return JamService.getJamService(psiClass.getProject())
            .getJamElement(JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY, psiClass) != null;
  }

  /**
   * Returns whether the given PsiClass (or its inheritors) could possibly be mapped as Spring Bean.
   *
   * @param psiClass PsiClass to check.
   * @return {@code true} if yes.
   */
  public static boolean isBeanCandidateClass(@Nullable PsiClass psiClass) {
    return psiClass != null
            && psiClass.isValid()
            && !(psiClass instanceof PsiTypeParameter)
            && !psiClass.hasModifierProperty(PsiModifier.PRIVATE)
            && !psiClass.isAnnotationType()
            && psiClass.getQualifiedName() != null
            && !PsiUtil.isLocalOrAnonymousClass(psiClass);
  }

  /**
   * Returns true if the given class is a bean candidate, today library is present in project and
   * Spring facet in current/dependent module(s) (or at least one in Project for PsiClass located in JAR) exists.
   *
   * @param psiClass Class to check.
   * @return true if all conditions apply
   */
  public static boolean isBeanCandidateClassInProject(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return false;
    }

    if (!hasFacets(psiClass.getProject())) {
      return false;
    }

    if (!TodayLibraryUtil.hasLibrary(psiClass.getProject())) {
      return false;
    }

    if (!isBeanCandidateClass(psiClass)) {
      return false;
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (isInfraEnabledModule(module)) {
      return true;
    }

    // located in JAR
    return module == null;
  }

  public static boolean isInfraConfigured(@Nullable Module module) {
    if (module == null) {
      return false;
    }

    return CachedValuesManager.getManager(module.getProject()).getParameterizedCachedValue(module, IS_SPRING_CONFIGURED_KEY,
            IS_SPRING_CONFIGURED_PROVIDER, false, module);
  }

  public static boolean isInfraEnabledModule(@Nullable Module module) {
    return isInfraConfigured(module) || SpringModelUtils.getInstance().hasAutoConfiguredModels(module);
  }

  public static boolean isInInfraEnabledModule(@Nullable UElement uElement) {
    PsiElement psi = UElementKt.getSourcePsiElement(uElement);
    if (psi == null)
      return false;
    return isInfraEnabledModule(ModuleUtilCore.findModuleForPsiElement(psi));
  }

}

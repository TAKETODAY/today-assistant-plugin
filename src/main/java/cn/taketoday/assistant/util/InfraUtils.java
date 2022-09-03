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
import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.facet.FacetFinder;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.jam.JamService;
import com.intellij.model.search.SearchContext;
import com.intellij.model.search.SearchService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.JarVersionDetectionUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.CharFilter;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UIdentifier;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.beans.stereotype.Component;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.facet.InfraSchemaVersion;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.BeanNameProvider;
import cn.taketoday.assistant.model.xml.BeanTypeProvider;
import cn.taketoday.assistant.model.xml.DomBeanPointer;
import cn.taketoday.assistant.model.xml.beans.CollectionElements;
import cn.taketoday.lang.Nullable;

public final class InfraUtils {

  public static List<String> tokenize(String str) {
    return tokenize(str, INFRA_DELIMITERS);
  }

  public static List<String> tokenize(String str, String delimiters) {
    List<String> list = new SmartList<>();
    StringTokenizer st = new StringTokenizer(str, delimiters);
    while (st.hasMoreTokens()) {
      String token = st.nextToken().trim();
      if (!token.isEmpty()) {
        list.add(token);
      }
    }
    return list;
  }

  public static List<PsiType> resolveGenerics(PsiClassType classType) {
    PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
    PsiClass psiClass = resolveResult.getElement();
    if (psiClass != null) {
      PsiSubstitutor substitutor = resolveResult.getSubstitutor();
      List<PsiType> generics = new SmartList<>();
      for (PsiTypeParameter typeParameter : psiClass.getTypeParameters()) {
        generics.add(substitutor.substitute(typeParameter));
      }
      return generics;
    }
    return Collections.emptyList();
  }

  public static PsiElement createXmlConfigFile(String newName, PsiDirectory directory) throws Exception {
    Module module = ModuleUtilCore.findModuleForPsiElement(directory);
    FileTemplate template = getXmlTemplate(module);
    String fileName = FileUtilRt.getExtension(newName).length() == 0 ? newName + ".xml" : newName;
    return FileTemplateUtil.createFromTemplate(template, fileName, null, directory);
  }

  public static FileTemplate getXmlTemplate(Module... modules) {
    for (Module module : modules) {
      String version = JarVersionDetectionUtil.detectJarVersion(InfraConstant.INFRA_VERSION_CLASS, module);
      if (version != null) {
        return version.startsWith("1") ?
               InfraSchemaVersion.INFRA_1_DTD.getTemplate(module.getProject()) :
               InfraSchemaVersion.INFRA_SCHEMA.getTemplate(module.getProject());
      }
    }
    return InfraSchemaVersion.INFRA_SCHEMA.getTemplate(modules[0].getProject());
  }

  public static boolean isEnabledModule(@Nullable Module module) {
    return isInfraConfigured(module) || InfraModelService.of().hasAutoConfiguredModels(module);
  }

  public static boolean isInEnabledModule(@Nullable UElement uElement) {
    PsiElement psi = UElementKt.getSourcePsiElement(uElement);
    if (psi == null)
      return false;
    return isEnabledModule(ModuleUtilCore.findModuleForPsiElement(psi));
  }

  public static Set<UExpression> findParameterExpressionInMethodCalls(PsiMethod psiMethod,
          int forParam,
          @Nullable SearchScope searchScope) {
    if (searchScope == null)
      return Collections.emptySet();
    return findMethodCalls(psiMethod, searchScope).stream()
            .map(uCallExpression -> uCallExpression.getArgumentForParameter(forParam))
            .filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static Set<UCallExpression> findMethodCalls(PsiMethod psiMethod,
          @Nullable SearchScope searchScope,
          LocalSearchScope... excludeScopes) {
    //return CachedValuesManager.getCachedValue(psiMethod, () -> {
    return MethodReferencesSearch.search(psiMethod, searchScope, true).findAll().stream().
            map(ref -> {
              return UastContextKt.getUastParentOfType(ref.getElement(), UCallExpression.class);
            }).filter(Objects::nonNull).collect(Collectors.toSet());

    //return Result.create(callExpressions, PsiModificationTracker.MODIFICATION_COUNT);
    //});
  }

  public static List<UExpression> getReturnedUExpression(@Nullable PsiMethod method) {
    UMethod uMethod = UastContextKt.toUElement(method, UMethod.class);
    if (uMethod == null)
      return Collections.emptyList();
    List<UExpression> result = new SmartList<>();
    uMethod.accept(new AbstractUastVisitor() {
      @Override
      public boolean visitReturnExpression(UReturnExpression node) {
        if (node.getJumpTarget() == uMethod) {
          ContainerUtil.addIfNotNull(result, node.getReturnExpression());
        }
        return super.visitReturnExpression(node);
      }
    });
    return result;
  }

  @Nullable
  public static String evaluateStringExpression(@Nullable UExpression expression) {
    if (expression == null)
      return null;
    String evaluateString = UastUtils.evaluateString(expression);
    if (evaluateString == null) {
      // in some cases StringExpressionHelper evaluate string expressions better (for java only)
      PsiElement sourcePsi = expression.getSourcePsi();
      if (sourcePsi instanceof PsiExpression) {
        Pair<PsiElement, String> pair = StringExpressionHelper.evaluateExpression(sourcePsi);
        if (pair != null)
          return pair.second;
      }
    }
    return evaluateString;
  }

  public static BeanTypeProvider getBeanTypeProvider(Class<? extends BeanTypeProvider> providerClass) {
    try {
      return providerClass.getDeclaredConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException("Couldn't instantiate " + providerClass, e);
    }
  }

  public static BeanNameProvider getBeanNameProvider(Class<? extends BeanNameProvider> providerClass) {
    try {
      return providerClass.getDeclaredConstructor().newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException("Couldn't instantiate " + providerClass, e);
    }
  }

  public static Collection<? extends UCallExpression> findMethodCallsByPattern(Project project,
          String methodName,
          SearchScope scope,
          UCallExpressionPattern... patterns) {
    return SearchService.getInstance().searchWord(project, methodName)
            .inContexts(SearchContext.IN_CODE)
            .inScope(scope).buildQuery(occurrence -> {
              if (occurrence.getOffsetInStart() != 0) {
                return Collections.emptySet();
              }
              UCallExpression uCallExpression = getUCallExpression(occurrence.getStart());
              if (uCallExpression == null) {
                return Collections.emptySet();
              }
              for (UCallExpressionPattern callExpressionPattern : patterns) {
                if (callExpressionPattern.accepts(uCallExpression)) {
                  return Collections.singleton(uCallExpression);
                }
              }

              return Collections.emptySet();
            }).findAll();
  }

  @Nullable
  private static UCallExpression getUCallExpression(PsiElement leafNode) {
    UElement element = UastContextKt.toUElement(leafNode);
    if (element instanceof UCallExpression)
      return (UCallExpression) element;
    if (element instanceof UIdentifier) {
      UElement uastParent = element.getUastParent();
      if (uastParent instanceof UCallExpression)
        return (UCallExpression) uastParent;
    }
    return null; // ??? UastContextKt.getUastParentOfType(start, UCallExpression.class);
  }

  public static <V> V withProgress(Computable<V> callable) {
    ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    if (progressIndicator != null && progressIndicator.isRunning()) {
      return callable.compute();
    }
    else {
      return ProgressManager.getInstance().runProcess(callable, new EmptyProgressIndicator());
    }
  }

  public static boolean hasXmlConfigs(@Nullable Module module) {
    if (module == null)
      return false;

    Collection<InfraFacet> facets = FacetManager.getInstance(module).getFacetsByType(InfraFacet.FACET_TYPE_ID);
    for (InfraFacet facet : facets) {
      if (facet.hasXmlMappings())
        return true;
    }
    return false;
  }

  //

  public static final String INFRA_DELIMITERS = ",; ";

  public static final CharFilter ourFilter = ch -> INFRA_DELIMITERS.indexOf(ch) >= 0;
  private static final Key<ParameterizedCachedValue<Boolean, Module>> IS_INFRA_CONFIGURED_KEY = Key.create("IS_INFRA_CONFIGURED_IN_MODULE");
  private static final ParameterizedCachedValueProvider<Boolean, Module> IS_INFRA_CONFIGURED_PROVIDER
          = new ParameterizedCachedValueProvider<>() {
    @Override
    public CachedValueProvider.Result<Boolean> compute(Module module) {
      return CachedValueProvider.Result.create(isInfraConfigured(module),
              ProjectRootManager.getInstance(module.getProject()),
              FacetFinder.getInstance(module.getProject()).getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID));
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

  @Nullable
  public static PsiClass findLibraryClass(@Nullable Module module, String className) {
    if (module == null || module.isDisposed()) {
      return null;
    }
    else {
      Project project = module.getProject();
      Map<String, PsiClass> cache = CachedValuesManager.getManager(project).getCachedValue(module, () -> {
        var map = ConcurrentFactoryMap.<String, PsiClass>createMap(key ->
                findLibraryClass(project, key, GlobalSearchScope.moduleRuntimeScope(module, false)));
        return CachedValueProvider.Result.createSingleDependency(map, JavaLibraryModificationTracker.getInstance(project));
      });
      return cache.get(className);
    }
  }

  @Nullable
  private static PsiClass findLibraryClass(Project project, String fqn, GlobalSearchScope searchScope) {
    return DumbService.getInstance(project)
            .runReadActionInSmartMode(() -> JavaPsiFacade.getInstance(project).findClass(fqn, searchScope));
  }

  public static boolean hasFacets(Project project) {
    return ProjectFacetManager.getInstance(project).hasFacets(InfraFacet.FACET_TYPE_ID);
  }

  public static boolean hasFacet(@Nullable Module module) {
    return module != null && InfraFacet.from(module) != null;
  }

  /**
   * Returns whether the given class has {@value AnnotationConstant#CONFIGURATION} annotation, <em>excluding</em> meta-annotations.
   *
   * @see #isConfigurationOrMeta(PsiClass)
   */
  public static boolean isConfiguration(PsiClass psiClass) {
    return isBeanCandidateClass(psiClass)
            && AnnotationUtil.isAnnotated(psiClass, AnnotationConstant.CONFIGURATION, 0);
  }

  /**
   * Returns whether the given class is <em>(meta-)</em>annotated with {@value AnnotationConstant#CONFIGURATION}.
   *
   * @param psiClass Class to check.
   * @return {@code true} if class annotated.
   */
  public static boolean isConfigurationOrMeta(PsiClass psiClass) {
    if (!isBeanCandidateClass(psiClass))
      return false;
    return JamService.getJamService(psiClass.getProject())
            .getJamElement(Configuration.JAM_KEY, psiClass) != null;
  }

  /**
   * @param psiClass Class to check.
   * @return whether the given class is annotated with {@value AnnotationConstant#COMPONENT}.
   * <p>NOTE: it doesn't consider {@value AnnotationConstant#SERVICE}, {@value AnnotationConstant#REPOSITORY} and so on</p>
   * @see #isStereotypeComponentOrMeta(PsiClass)
   */
  public static boolean isComponentOrMeta(PsiClass psiClass) {
    if (!isBeanCandidateClass(psiClass))
      return false;
    return JamService.getJamService(psiClass.getProject())
            .getJamElement(psiClass, Component.META) != null;
  }

  /**
   * Returns whether the given class is <em>(meta-)</em>annotated with {@value AnnotationConstant#COMPONENT, AnnotationConstant#SERVICE, , AnnotationConstant#REPOSITORY}.
   *
   * @param psiClass Class to check.
   * @return {@code true} if class annotated.
   */
  public static boolean isStereotypeComponentOrMeta(PsiClass psiClass) {
    if (!isBeanCandidateClass(psiClass))
      return false;
    return JamService.getJamService(psiClass.getProject())
            .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass) != null;
  }

  /**
   * Returns whether the given PsiClass (or its inheritors) could possibly be mapped as Infra Bean.
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
   * Infra facet in current/dependent module(s) (or at least one in Project for PsiClass located in JAR) exists.
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

    if (!InfraLibraryUtil.hasLibrary(psiClass.getProject())) {
      return false;
    }

    if (!isBeanCandidateClass(psiClass)) {
      return false;
    }

    Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
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

    return CachedValuesManager.getManager(module.getProject()).getParameterizedCachedValue(module, IS_INFRA_CONFIGURED_KEY,
            IS_INFRA_CONFIGURED_PROVIDER, false, module);
  }

  public static boolean isInfraEnabledModule(@Nullable Module module) {
    return isInfraConfigured(module) || InfraModelService.of().hasAutoConfiguredModels(module);
  }

  public static boolean isInInfraEnabledModule(@Nullable UElement uElement) {
    PsiElement psi = UElementKt.getSourcePsiElement(uElement);
    if (psi == null)
      return false;
    return isInfraEnabledModule(ModuleUtilCore.findModuleForPsiElement(psi));
  }

  public static Set<BeanPointer<?>> filterInnerClassBeans(Collection<BeanPointer<?>> pointers) {
    return pointers.stream()
            .filter(pointer -> !isDefinedAsCollectionElement(pointer))
            .collect(Collectors.toSet());
  }

  public static boolean isDefinedAsCollectionElement(@Nullable BeanPointer<?> pointer) {
    if (pointer instanceof DomBeanPointer) {
      // bean is defined in collection and can't be autowired
      return (((DomBeanPointer) pointer).getBean()).getParent() instanceof CollectionElements;
    }
    return false;
  }

}

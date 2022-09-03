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

package cn.taketoday.assistant.web.mvc.client;

import com.intellij.microservices.jvm.cache.ProjectCacheValueHolder;
import com.intellij.microservices.jvm.cache.ScopedCacheValueHolder;
import com.intellij.microservices.utils.EndpointsViewUtils;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.uast.UastModificationTracker;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastLanguagePlugin;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.expressions.UStringConcatenationsFacade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.utils.InfraFunctionalSearchersUtils;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public abstract class WebClientModel {

  protected abstract List<PsiMethod> getUrlApiMethods(Module module);

  public abstract List<PsiClass> getApiClasses(ScopedCacheValueHolder<?> scopedCacheValueHolder);

  public abstract List<PsiMethod> getApiMethods(Project project);

  @Nullable
  public abstract String findBaseUrl(@Nullable UExpression uExpression);

  @Nullable
  public abstract String findHttpMethod(UCallExpression uCallExpression);

  protected abstract List<String> getPackageBlackList();

  protected abstract String getWebClientPackage();

  public final boolean hasUsages(Project project) {
    GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
    return hasApiClassUsages(new ProjectCacheValueHolder(project), projectScope);
  }

  public final List<WebClientHolder> findHolders(Module module, GlobalSearchScope scope) {
    return InfraUtils.withProgress(() -> {
      return findHoldersUnderProgress(module, scope);
    });
  }

  public final List<WebClientHolder> findHoldersUnderProgress(Module module, GlobalSearchScope scope) {
    return findHoldersUnderProgress(getUrlApiMethods(module), module, scope);
  }

  private boolean hasApiClassUsages(ScopedCacheValueHolder<?> scopedCacheValueHolder, GlobalSearchScope scope) {
    List apiClasses = getApiClasses(scopedCacheValueHolder);
    if (apiClasses.isEmpty()) {
      return false;
    }
    GlobalSearchScope webClientScope = getWebClientScope(scopedCacheValueHolder.getProject(), scope);
    return !Intrinsics.areEqual(webClientScope, GlobalSearchScope.EMPTY_SCOPE);
  }

  public final List<WebClientUrl> getEndpoints(WebClientHolder group) {
    Intrinsics.checkNotNullParameter(group, "group");
    PsiElement psiElement = group.getPsiElement();
    Project project = psiElement.getProject();
    Intrinsics.checkNotNullExpressionValue(project, "psiElement.project");
    PsiFile containingFile = psiElement.getContainingFile();
    Intrinsics.checkNotNullExpressionValue(containingFile, "psiElement.containingFile");
    VirtualFile virtualFile = containingFile.getVirtualFile();
    CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(project);
    if (ProjectScope.getLibrariesScope(project).contains(virtualFile)) {
      return cachedValuesManager.getCachedValue(psiElement, new CachedValueProvider<List<WebClientUrl>>() {
        @Nullable
        public CachedValueProvider.Result<List<WebClientUrl>> compute() {
          return Result.createSingleDependency(findEndpoints(psiElement), ProjectRootManager.getInstance(project));
        }
      });
    }
    return cachedValuesManager.getCachedValue(psiElement, new CachedValueProvider<>() {
      @Nullable
      public CachedValueProvider.Result<List<WebClientUrl>> compute() {
        return Result.createSingleDependency(findEndpoints(psiElement), UastModificationTracker.Companion.getInstance(project));
      }
    });
  }

  public final List<WebClientUrl> findEndpoints(PsiElement holderPsi) {
    if (UastContextKt.toUElement(holderPsi) != null) {
      Project project = holderPsi.getProject();
      Intrinsics.checkNotNullExpressionValue(project, "holderPsi.project");
      List<PsiMethod> apiMethods = getApiMethods(project);
      if (apiMethods.isEmpty()) {
        return CollectionsKt.emptyList();
      }
      HashSet<UCallExpression> methodCalls = new HashSet<>();
      for (PsiMethod apiMethod : apiMethods) {
        methodCalls.addAll(InfraFunctionalSearchersUtils.findMethodCalls(apiMethod, new LocalSearchScope(holderPsi)));
      }
      Collection<WebClientUrl> list = new ArrayList<>();
      for (UCallExpression it : methodCalls) {
        CollectionsKt.addAll(list, findUrl(it));
      }
      return CollectionsKt.toList(list);
    }
    return CollectionsKt.emptyList();
  }

  private final List<WebClientUrl> findUrl(UCallExpression uriMethodCall) {
    UExpression argument;
    PsiElement sourcePsi;
    String calculatedUri;
    if (uriMethodCall.getValueArgumentCount() != 0
            && (argument = uriMethodCall.getArgumentForParameter(0)) != null
            && (sourcePsi = argument.getSourcePsi()) != null && (calculatedUri = getExpressionValue(argument)) != null) {
      ArrayList<WebClientUrl> urlCollector = new ArrayList<>();
      String findHttpMethod = findHttpMethod(uriMethodCall);
      if (findHttpMethod == null) {
        findHttpMethod = "GET";
      }
      String httpMethod = findHttpMethod;
      String findBaseUrl = findBaseUrl(UastUtils.getOutermostQualified(uriMethodCall));
      if (findBaseUrl == null) {
        findBaseUrl = "";
      }
      String baseUrl = findBaseUrl;
      urlCollector.add(new WebClientUrl(sourcePsi, baseUrl + calculatedUri, httpMethod));
      return urlCollector;
    }
    return CollectionsKt.emptyList();
  }

  @Nullable
  protected final String getExpressionValue(@Nullable UExpression argument) {
    if (argument == null) {
      return null;
    }
    String exactValue = UastUtils.evaluateString(argument);
    if (exactValue != null) {
      return exactValue;
    }
    var facade = UStringConcatenationsFacade.Companion.createFromUExpression(argument, false);
    if (facade != null && facade.getUastOperands().iterator().hasNext()) {
      return EndpointsViewUtils.getEndpointUrlPresentation(facade.asPartiallyKnownString());
    }
    return null;
  }

  private List<WebClientHolder> findHoldersUnderProgress(List<? extends PsiMethod> list, Module module, GlobalSearchScope scope) {
    if (list.isEmpty()) {
      return CollectionsKt.emptyList();
    }
    Project project = module.getProject();
    GlobalSearchScope webClientScope = getWebClientScope(project, scope);
    if (Intrinsics.areEqual(webClientScope, GlobalSearchScope.EMPTY_SCOPE)) {
      return CollectionsKt.emptyList();
    }
    HashSet<PsiElement> holders = new HashSet<>();
    for (PsiMethod method : list) {
      searchMethodHolders(method, webClientScope, holders);
    }
    ArrayList<WebClientHolder> clientHolders = new ArrayList<>(Math.max(holders.size(), 10));
    for (PsiElement it : holders) {
      clientHolders.add(new WebClientHolder(it));
    }
    return clientHolders;
  }

  private void searchMethodHolders(PsiMethod method, GlobalSearchScope webClientScope, Set<PsiElement> set) {
    CacheManager cacheManager = CacheManager.getInstance(method.getProject());
    VirtualFile[] methodIdFiles = cacheManager.getVirtualFilesWithWord(method.getName(), (short) 1, webClientScope, true);
    ArrayList<PsiFile> ret = new ArrayList<>();
    for (VirtualFile virtualFile : methodIdFiles) {
      PsiFile findFile = PsiManager.getInstance(method.getProject()).findFile(virtualFile);
      if (findFile != null) {
        ret.add(findFile);
      }
    }
    set.addAll(ret);
  }

  private final GlobalSearchScope getUastScope(GlobalSearchScope originalScope) {
    Collection<UastLanguagePlugin> languagePlugins = UastLanguagePlugin.Companion.getInstances();
    ArrayList<LanguageFileType> fileTypes = new ArrayList<>(Math.max(languagePlugins.size(), 10));
    for (UastLanguagePlugin it : languagePlugins) {
      fileTypes.add(it.getLanguage().getAssociatedFileType());
    }
    LanguageFileType[] array = fileTypes.toArray(new LanguageFileType[0]);
    return GlobalSearchScope.getScopeRestrictedByFileTypes(originalScope, Arrays.copyOf(array, array.length));
  }

  private GlobalSearchScope unionScope(List<? extends GlobalSearchScope> list) {
    if (list.isEmpty()) {
      return GlobalSearchScope.EMPTY_SCOPE;
    }
    return GlobalSearchScope.union(list);
  }

  private GlobalSearchScope excludePackages(Project project, GlobalSearchScope scope) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    List<String> packageBlackList = getPackageBlackList();
    ArrayList<GlobalSearchScope> collection = new ArrayList<>();

    for (String pkg : packageBlackList) {
      PsiPackage it = javaPsiFacade.findPackage(pkg);
      GlobalSearchScope packageScope = it != null ? PackageScope.packageScope(it, false) : null;
      if (packageScope != null) {
        collection.add(packageScope);
      }
    }
    GlobalSearchScope excludedPackagesScope = unionScope(collection);
    return scope.intersectWith(GlobalSearchScope.notScope(excludedPackagesScope));
  }

  private GlobalSearchScope getWebClientScope(Project project, GlobalSearchScope baseScope) {
    GlobalSearchScope nonBlackListScope = excludePackages(project, getUastScope(baseScope));
    CacheManager cacheManager = CacheManager.getInstance(project);
    VirtualFile[] containingFiles = cacheManager.getVirtualFilesWithWord(getWebClientPackage(), (short) 1, nonBlackListScope, true);
    if (containingFiles.length == 0) {
      return GlobalSearchScope.EMPTY_SCOPE;
    }
    return GlobalSearchScope.filesScope(project, ArraysKt.toList(containingFiles));
  }
}

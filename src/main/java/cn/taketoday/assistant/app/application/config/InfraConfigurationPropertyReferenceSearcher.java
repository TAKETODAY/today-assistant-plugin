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

package cn.taketoday.assistant.app.application.config;

import com.intellij.jam.JamService;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.QuerySearchRequest;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.app.application.metadata.InfraConfigKetPathBeanPropertyResolver;
import cn.taketoday.assistant.app.application.metadata.InfraConfigKeyReferenceSearcher;
import cn.taketoday.assistant.model.config.properties.ConfigurationProperties;
import cn.taketoday.assistant.model.config.properties.NestedConfigurationProperty;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraConfigurationPropertyReferenceSearcher extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {
  private static final CachedValueProvider.Result<String> NULL_PREFIX_RESULT = CachedValueProvider.Result.create(null, PsiModificationTracker.MODIFICATION_COUNT);

  public InfraConfigurationPropertyReferenceSearcher() {
    super(true);
  }

  public void processQuery(MethodReferencesSearch.SearchParameters queryParameters, Processor<? super PsiReference> consumer) {
    PsiMethod psiMethod;
    String prefix;
    SearchScope scope = queryParameters.getEffectiveSearchScope();
    if (!(scope instanceof GlobalSearchScope)) {
      return;
    }
    Project project = queryParameters.getProject();
    if (!InfraUtils.hasFacets(project)
            || !InfraLibraryUtil.hasFrameworkLibrary(project)
            || (prefix = getPrefixIfRelevantPropertyMethod((psiMethod = queryParameters.getMethod()), true)) == null) {
      return;
    }
    if (GlobalSearchScope.EMPTY_SCOPE.equals(scope)) {
      scope = ProjectScope.getProjectScope(project);
    }
    List<MetaConfigKey> metaConfigKeys = findMetaConfigKeys(prefix, psiMethod);
    for (MetaConfigKey metaConfigKey : metaConfigKeys) {
      PsiElement keyDeclaration = metaConfigKey.getDeclaration();
      SearchRequestCollector requestCollector = new SearchRequestCollector(queryParameters.getOptimizer().getSearchSession());

      queryParameters.getOptimizer().searchQuery(new QuerySearchRequest(ReferencesSearch.search(keyDeclaration, scope),
              requestCollector, true, (reference, p) -> consumer.process(reference)));
    }
  }

  private static List<MetaConfigKey> findMetaConfigKeys(String prefix, PsiMethod psiMethod) {
    SmartList<MetaConfigKey> smartList = new SmartList<>();
    Module currentModule = ModuleUtilCore.findModuleForPsiElement(psiMethod);
    if (currentModule != null) {
      smartList.addAll(findMetaConfigKeysInModule(currentModule, prefix, psiMethod));
      return smartList;
    }
    for (Module module : InfraConfigKeyReferenceSearcher.getRelevantModules(psiMethod.getProject(), null)) {
      if (InfraUtils.hasFacet(module) && InfraLibraryUtil.hasFrameworkLibrary(module)) {
        smartList.addAll(findMetaConfigKeysInModule(module, prefix, psiMethod));
      }
    }
    return smartList;
  }

  private static List<MetaConfigKey> findMetaConfigKeysInModule(Module module, String prefix, PsiMethod psiMethod) {
    SmartList<MetaConfigKey> smartList = new SmartList<>();
    boolean constructorBinding = psiMethod.isConstructor();
    for (MetaConfigKey key : InfraApplicationMetaConfigKeyManager.getInstance().getAllMetaConfigKeys(module)) {
      if (key.getDeclarationResolveResult() == MetaConfigKey.DeclarationResolveResult.PROPERTY
              && StringUtil.startsWith(key.getName(), prefix)) {
        PsiElement declaration = key.getDeclaration().getNavigationElement();
        if (declaration instanceof PsiMethod) {
          if (!constructorBinding && psiMethod.getManager().areElementsEquivalent(declaration, psiMethod)) {
            smartList.add(key);
            return smartList;
          }
        }
        else if ((declaration instanceof PsiParameter) && constructorBinding) {
          PsiManager psiManager = psiMethod.getManager();
          for (PsiElement psiElement : psiMethod.getParameterList().getParameters()) {
            if (psiManager.areElementsEquivalent(declaration, psiElement)) {
              smartList.add(key);
            }
          }
        }
      }
    }
    return smartList;
  }

  @Nullable
  public static String getPrefixIfRelevantPropertyMethod(PsiMethod psiMethod, boolean checkIfBinder) {
    PsiClass containingClass;
    if (!psiMethod.hasModifierProperty("public")
            || psiMethod.hasModifierProperty("static")
            || psiMethod.hasModifierProperty("abstract")
            || (containingClass = psiMethod.getContainingClass()) == null) {
      return null;
    }
    if (checkIfBinder) {
      boolean isConstructor = psiMethod.isConstructor();
      if (!isConstructor && !PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
        return null;
      }
      Module currentModule = ModuleUtilCore.findModuleForPsiElement(psiMethod);
      PsiMethod bindingConstructor = InfraConfigKetPathBeanPropertyResolver.getBindingConstructor(containingClass, currentModule, (MetaConfigKey) null);
      if (isConstructor) {
        if (!psiMethod.getManager().areElementsEquivalent(bindingConstructor, psiMethod)) {
          return null;
        }
      }
      else if (bindingConstructor != null) {
        return null;
      }
    }
    return CachedValuesManager.getCachedValue(containingClass, () -> {
      if (containingClass.isInterface()
              || containingClass.hasModifierProperty("abstract")
              || !InfraUtils.isBeanCandidateClass(containingClass)) {
        return NULL_PREFIX_RESULT;
      }
      ConfigurationProperties configurationProperties = findEnclosingOrUsingConfigurationProperties(containingClass);
      if (configurationProperties == null) {
        return NULL_PREFIX_RESULT;
      }
      String prefix = configurationProperties.getValueOrPrefix();
      if (StringUtil.isEmpty(prefix)) {
        return NULL_PREFIX_RESULT;
      }
      return CachedValueProvider.Result.create(prefix, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  @Nullable
  private static ConfigurationProperties findEnclosingOrUsingConfigurationProperties(PsiClass psiClass) {
    ConfigurationProperties currentClass = ConfigurationProperties.CLASS_META.getJamElement(psiClass);
    if (currentClass != null) {
      return currentClass;
    }
    if (psiClass.getContainingClass() != null) {
      return ConfigurationProperties.CLASS_META.getJamElement(psiClass.getContainingClass());
    }
    SearchScope useScope = psiClass.getUseScope();
    if (!(useScope instanceof GlobalSearchScope searchScope)) {
      return null;
    }
    JamService jamService = JamService.getJamService(psiClass.getProject());
    List<NestedConfigurationProperty> nestedConfigurationProperties =
            jamService.getJamFieldElements(NestedConfigurationProperty.FIELD_META, InfraClassesConstants.NESTED_CONFIGURATION_PROPERTY, searchScope);
    for (NestedConfigurationProperty property : nestedConfigurationProperties) {
      if (property.typeMatches(psiClass)) {
        return property.getEnclosingConfigurationProperties();
      }
    }
    List<ConfigurationProperties.Method> beanConfigurationProperties = jamService.getJamMethodElements(
            ConfigurationProperties.Method.METHOD_META, InfraClassesConstants.CONFIGURATION_PROPERTIES,
            searchScope);
    for (ConfigurationProperties.Method configurationProperties : beanConfigurationProperties) {
      if (configurationProperties.isDefinitionFor(psiClass)) {
        return configurationProperties;
      }
    }
    return null;
  }
}

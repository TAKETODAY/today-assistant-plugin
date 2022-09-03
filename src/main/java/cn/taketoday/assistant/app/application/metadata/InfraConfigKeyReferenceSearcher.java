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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.microservices.jvm.config.ConfigKeyDocumentationProviderBase;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.application.metadata.additional.InfraAdditionalConfigUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.ConfigurationValueSearchParams;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraConfigKeyReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public InfraConfigKeyReferenceSearcher() {
    super(true);
  }

  public void processQuery(ReferencesSearch.SearchParameters queryParameters, Processor<? super PsiReference> consumer) {
    PsiFile psiFile;
    PsiElement elementToSearch = queryParameters.getElementToSearch();
    if (!(elementToSearch instanceof InfraConfigKeyDeclarationPsiElement infraConfigKeyDeclarationPsiElement)) {
      return;
    }
    Module currentModule = elementToSearch.getUserData(ConfigKeyDocumentationProviderBase.CONFIG_KEY_DECLARATION_MODULE);
    Project project = queryParameters.getProject();
    List<MetaConfigKey> metaConfigKeys = findMetaConfigKeys(infraConfigKeyDeclarationPsiElement.getName(), currentModule, project);
    MetaConfigKey metaConfigKey = ContainerUtil.getFirstItem(metaConfigKeys);
    if (metaConfigKey == null) {
      return;
    }
    List<Module> relevantModules = getRelevantModules(project, currentModule);
    Set<String> relaxedKeys = new HashSet<>();
    SearchScope scope = queryParameters.getEffectiveSearchScope();
    PsiManager psiManager = PsiManager.getInstance(project);
    for (Module module : relevantModules) {

      if (InfraLibraryUtil.hasFrameworkLibrary(module)) {
        if (!processAdditionalConfigFiles(module, scope, metaConfigKey, consumer)) {
          return;
        }
        ConfigurationValueSearchParams params = new ConfigurationValueSearchParams(module, metaConfigKey);
        for (VirtualFile file : InfraConfigurationFileService.of().findConfigFiles(module, true)) {
          InfraModelConfigFileContributor contributor = InfraModelConfigFileContributor.getContributor(file);
          if (contributor != null && (psiFile = psiManager.findFile(file)) != null) {
            List<ConfigurationValueResult> values = contributor.findConfigurationValues(psiFile, params);
            boolean isInScope = scope.contains(file);
            for (ConfigurationValueResult value : values) {
              MetaConfigKeyReference<?> metaConfigKeyReference = value.getMetaConfigKeyReference();
              relaxedKeys.add(metaConfigKeyReference.getKeyText());
              if (isInScope && !consumer.process(metaConfigKeyReference)) {
                return;
              }
            }
          }
        }
      }
    }
    for (String relaxedKey : relaxedKeys) {
      queryParameters.getOptimizer().searchWord(relaxedKey, scope, (short) 255, true, queryParameters.getElementToSearch());
    }
  }

  private static List<MetaConfigKey> findMetaConfigKeys(String keyName, Module currentModule, Project project) {
    SmartList<MetaConfigKey> smartList = new SmartList<>();
    if (currentModule != null) {
      ContainerUtil.addIfNotNull(smartList, findMetaConfigKeyInModule(keyName, currentModule));
      return smartList;
    }
    for (Module module : getRelevantModules(project, null)) {
      if (InfraUtils.hasFacet(module) && InfraLibraryUtil.hasFrameworkLibrary(module)) {
        MetaConfigKey configKey = findMetaConfigKeyInModule(keyName, null);
        ContainerUtil.addIfNotNull(smartList, configKey);
      }
    }
    return smartList;
  }

  @Nullable
  private static MetaConfigKey findMetaConfigKeyInModule(String keyName, Module module) {
    for (MetaConfigKey key : InfraApplicationMetaConfigKeyManager.getInstance().getAllMetaConfigKeys(module)) {
      if (keyName.equals(key.getName())) {
        return key;
      }
    }
    return null;
  }

  private static boolean processAdditionalConfigFiles(Module module, SearchScope scope, MetaConfigKey key, Processor<? super PsiReference> consumer) {
//    if (!InfraLibraryUtil.hasConfigurationMetadataAnnotationProcessor(module)) {
//      return true;
//    }
    InfraAdditionalConfigUtils utils = new InfraAdditionalConfigUtils(module);
    return utils.processAdditionalMetadataFiles(file -> {
      JsonObject topValue;
      JsonProperty hints;
      JsonArray array;
      JsonProperty nameProperty;
      JsonStringLiteral nameLiteral;
      MetaConfigKeyReference<?> metaConfigKeyReference;
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile == null || !scope.contains(virtualFile) || (topValue = ObjectUtils.tryCast(file.getTopLevelValue(), JsonObject.class)) == null || (hints = topValue.findProperty(
              InfraMetadataConstant.HINTS)) == null || (array = ObjectUtils.tryCast(hints.getValue(), JsonArray.class)) == null) {
        return true;
      }
      boolean isMapType = key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP);
      for (JsonValue value : array.getValueList()) {
        JsonObject object = ObjectUtils.tryCast(value, JsonObject.class);
        if (object != null && (nameProperty = object.findProperty(InfraMetadataConstant.NAME)) != null && (nameLiteral = ObjectUtils.tryCast(nameProperty.getValue(),
                JsonStringLiteral.class)) != null) {
          String nameValue = nameLiteral.getValue();
          if (isMapType) {
            if (!StringUtil.startsWith(nameValue, key.getName())) {
              continue;
            }
            else {
              metaConfigKeyReference = ContainerUtil.findInstance(nameLiteral.getReferences(), MetaConfigKeyReference.class);
              if (metaConfigKeyReference == null) {
                continue;
              }
              else if (!consumer.process(metaConfigKeyReference)) {
                return false;
              }
              else {
                if (!isMapType) {
                  return true;
                }
              }
            }
          }
          else if (!StringUtil.equals(nameValue, key.getName())) {
            continue;
          }
          else {
            metaConfigKeyReference = ContainerUtil.findInstance(nameLiteral.getReferences(), MetaConfigKeyReference.class);
            if (metaConfigKeyReference == null) {
            }
          }
        }
      }
      return true;
    });
  }

  public static List<Module> getRelevantModules(Project project, @Nullable Module currentModule) {
    if (currentModule == null) {
      return ProjectFacetManager.getInstance(project).getModulesWithFacet(InfraFacet.FACET_TYPE_ID);
    }
    SmartList<Module> smartList = new SmartList<>(currentModule);
    smartList.addAll(ModuleUtilCore.getAllDependentModules(currentModule));
    return smartList;
  }
}

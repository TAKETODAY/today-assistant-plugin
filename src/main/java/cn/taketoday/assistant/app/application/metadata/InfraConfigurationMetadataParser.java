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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.microservices.jvm.config.ConfigKeyDocumentationProviderBase;
import com.intellij.microservices.jvm.config.ConfigKeyPathBeanPropertyResolver;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.RelaxedNames;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.text.CharSequenceReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.app.application.metadata.additional.InfraAdditionalConfigUtils;
import cn.taketoday.lang.Nullable;

final class InfraConfigurationMetadataParser {
  private static final Logger LOG = Logger.getInstance(InfraConfigurationMetadataParser.class);
  @Nullable
  private final Module localModule;
  @Nullable
  private final PsiFile jsonPsiFile;
  @Nullable
  private final File localJsonFile;
  private final String libraryName;
  private final Caching caching;
  private final NotNullLazyValue<Map<String, PsiElement>> additionalConfigTargets;

  private static final Pair<PsiType, MetaConfigKey.AccessType> DUMMY_TYPE = Pair.create(PsiType.NULL, MetaConfigKey.AccessType.NORMAL);

  InfraConfigurationMetadataParser(@Nullable PsiFile jsonPsiFile) {
    this.jsonPsiFile = jsonPsiFile;
    this.localJsonFile = null;
    this.localModule = null;
    this.libraryName = getLibraryOrContainingJarName(jsonPsiFile);
    this.caching = new Caching(jsonPsiFile.getResolveScope());
    this.additionalConfigTargets = NotNullLazyValue.lazy(Collections::emptyMap);
  }

  InfraConfigurationMetadataParser(Module module, @Nullable File localJsonFile) {
    this.localModule = module;
    this.jsonPsiFile = null;
    this.localJsonFile = localJsonFile;
    this.libraryName = this.localModule.getName();
    this.caching = new Caching(module.getModuleRuntimeScope(false));
    this.additionalConfigTargets = NotNullLazyValue.lazy(() -> {
      Map<String, PsiElement> additionalConfigTargets = new HashMap<>();
      InfraAdditionalConfigUtils utils = new InfraAdditionalConfigUtils(this.localModule);
      utils.processAllAdditionalMetadataFiles(psiFile -> {
        collectAdditionalConfigTargets(psiFile, additionalConfigTargets);
        return true;
      });
      return additionalConfigTargets;
    });
  }

  public static void collectAdditionalConfigTargets(PsiFile psiFile, Map<String, PsiElement> additionalConfigTargets) {
    if (psiFile instanceof JsonFile) {
      collectAdditionalConfigTargets((JsonFile) psiFile, additionalConfigTargets);
      return;
    }
    VirtualFile virtualFile = psiFile.getVirtualFile();
    try {
      JsonReader reader = new JsonReader(new CharSequenceReader(VfsUtilCore.loadText(virtualFile)));
      reader.setLenient(true);
      JsonElement parse = JsonParser.parseReader(reader);
      if (parse.isJsonObject()) {
        JsonObject rootObject = parse.getAsJsonObject();
        collectAdditionalConfigTargets(rootObject, psiFile, additionalConfigTargets);
        reader.close();
        return;
      }
      reader.close();
    }
    catch (Throwable ignored) { }
  }

  private static void collectAdditionalConfigTargets(JsonFile jsonFile, Map<String, PsiElement> additionalConfigTargets) {
    JsonProperty propertiesRoot;
    JsonArray propertiesArray;
    JsonProperty nameProperty;
    JsonStringLiteral nameValue;
    com.intellij.json.psi.JsonObject rootObject = ObjectUtils.tryCast(jsonFile.getTopLevelValue(), com.intellij.json.psi.JsonObject.class);
    if (rootObject == null || (propertiesRoot = rootObject.findProperty(
            InfraMetadataConstant.PROPERTIES)) == null || (propertiesArray = ObjectUtils.tryCast(propertiesRoot.getValue(),
            JsonArray.class)) == null) {
      return;
    }
    for (JsonValue value : propertiesArray.getValueList()) {
      com.intellij.json.psi.JsonObject object = ObjectUtils.tryCast(value, com.intellij.json.psi.JsonObject.class);
      if (object != null && (nameProperty = object.findProperty(
              InfraMetadataConstant.NAME)) != null && (nameValue = ObjectUtils.tryCast(nameProperty.getValue(),
              JsonStringLiteral.class)) != null) {
        String key = nameValue.getValue();
        if (StringUtil.isNotEmpty(key)) {
          additionalConfigTargets.put(key, nameProperty);
        }
      }
    }
  }

  private static void collectAdditionalConfigTargets(JsonObject rootObject, PsiFile psiFile, Map<String, PsiElement> additionalConfigTargets) {
    JsonElement propertiesElement = rootObject.get(InfraMetadataConstant.PROPERTIES);
    if (propertiesElement == null) {
      return;
    }
    com.google.gson.JsonArray properties = propertiesElement.getAsJsonArray();
    for (JsonElement element : properties) {
      try {
        JsonObject property = element.getAsJsonObject();
        JsonElement nameProperty = property.get(InfraMetadataConstant.NAME);
        if (nameProperty != null) {
          String nameValue = nameProperty.getAsString();
          if (StringUtil.isNotEmpty(nameValue)) {
            additionalConfigTargets.put(nameValue, psiFile);
          }
        }
      }
      catch (IllegalStateException ignored) { }
    }
  }

  private static String getLibraryOrContainingJarName(PsiFile jsonFile) {
    OrderEntry findLibraryEntry = LibraryUtil.findLibraryEntry(jsonFile.getVirtualFile(), jsonFile.getProject());
    if (findLibraryEntry instanceof LibraryOrderEntry libraryOrderEntry) {
      String libraryName = libraryOrderEntry.getLibraryName();
      if (libraryName != null) {
        return libraryName;
      }
      VirtualFile jarRoot = JarFileSystem.getInstance().getLocalByEntry(jsonFile.getVirtualFile());
      if (jarRoot != null) {
        return jarRoot.getName();
      }
    }
    return "<unknown>";
  }

  boolean processKeys(Module module, Processor<MetaConfigKey> processor) {
    MetaConfigKey.ItemHint valueHint;
    MetaConfigKey.ItemHint keyHint;
    Ref<String> path = Ref.create();
    try {
      JsonReader reader = openReader(path);
      reader.setLenient(true);
      JsonElement parse = JsonParser.parseReader(reader);
      if (parse.isJsonObject()) {
        JsonObject rootObject = parse.getAsJsonObject();
        reader.close();
        JsonElement propertiesElement = rootObject.get(InfraMetadataConstant.PROPERTIES);
        if (propertiesElement == null) {
          return true;
        }
        com.google.gson.JsonArray properties = propertiesElement.getAsJsonArray();
        Map<String, MetaConfigKey.ItemHint> hints = getItemHints(rootObject);
        ConfigKeyPathBeanPropertyResolver resolver = new InfraConfigKetPathBeanPropertyResolver(module);

        for (JsonElement element : properties) {
          JsonObject property = element.getAsJsonObject();
          String configKeyName = getStringLiteral(property, InfraMetadataConstant.NAME);
          if (!StringUtil.isEmptyOrSpaces(configKeyName)) {
            String typeLiteral = getStringLiteral(property, InfraMetadataConstant.TYPE);
            String finalType = Comparing.strEqual(typeLiteral, "java.util.Properties") ? "java.util.Map<java.lang.String,java.lang.String>" : typeLiteral;
            Pair<PsiType, MetaConfigKey.AccessType> typeWithAccess = getPsiTypeToAccessType(module.getProject(), finalType);
            PsiType type = typeWithAccess.getFirst().equals(DUMMY_TYPE.getFirst()) ? null : typeWithAccess.getFirst();
            MetaConfigKey.AccessType accessType = typeWithAccess.getSecond();
            if (accessType == MetaConfigKey.AccessType.MAP) {
              valueHint = hints.get(configKeyName + ".values");
              keyHint = hints.get(configKeyName + ".keys");
            }
            else {
              valueHint = hints.get(configKeyName);
              keyHint = MetaConfigKey.ItemHint.NONE;
            }
            Pair<MetaConfigKey.DeclarationResolveResult, PsiElement> declarationPair = getDeclaration(property, configKeyName, type, module);
            if (declarationPair != null && declarationPair.second != null) {
              declarationPair.second.putUserData(ConfigKeyDocumentationProviderBase.CONFIG_KEY_DECLARATION_MODULE, module);
              MetaConfigKey key = new InfraApplicationMetaConfigKeyImpl(declarationPair.second, declarationPair.first, configKeyName,
                      getDescription(property), getValueAsString(property, InfraMetadataConstant.DEFAULT_VALUE), getDeprecation(property), type,
                      accessType, ObjectUtils.notNull(valueHint, MetaConfigKey.ItemHint.NONE),
                      ObjectUtils.notNull(keyHint, MetaConfigKey.ItemHint.NONE), resolver);
              if (!processor.process(key)) {
                return false;
              }
            }
          }
        }
        return true;
      }

      reader.close();
      return true;
    }
    catch (ProcessCanceledException e) {
      return true;
    }
    catch (Throwable e2) {
      LOG.info("Error parsing Infra metadata JSON from " + path.get(), e2);
      return true;
    }
  }

  private JsonReader openReader(Ref<String> pathRef) throws IOException {
    if (this.jsonPsiFile != null) {
      VirtualFile file = this.jsonPsiFile.getVirtualFile();
      pathRef.set(file.getPath());
      String content = VfsUtilCore.loadText(file);
      return new JsonReader(new CharSequenceReader(content));
    }
    else {
      assert this.localJsonFile != null;
      pathRef.set(this.localJsonFile.getPath());
      return new JsonReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(this.localJsonFile)), StandardCharsets.UTF_8));
    }
  }

  @Nullable
  private static String getValueAsString(JsonObject object, String propertyName) {
    JsonElement propertyElement = object.get(propertyName);
    if (propertyElement == null) {
      return null;
    }
    if (propertyElement.isJsonPrimitive()) {
      return propertyElement.getAsJsonPrimitive().getAsString();
    }
    if (!propertyElement.isJsonArray()) {
      return null;
    }
    com.google.gson.JsonArray array = propertyElement.getAsJsonArray();
    SmartList<String> smartList = new SmartList<>();
    for (JsonElement jsonValue : array) {
      if (jsonValue.isJsonPrimitive()) {
        smartList.add(jsonValue.getAsJsonPrimitive().getAsString());
      }
    }
    return StringUtil.join(smartList, ", ");
  }

  @Nullable
  private Pair<MetaConfigKey.DeclarationResolveResult, PsiElement> getDeclaration(
          JsonObject entry, String configKeyName, PsiType type, Module module) {
    String sourceTypeText = getStringLiteral(entry, InfraMetadataConstant.SOURCE_TYPE);
    if (StringUtil.isEmpty(sourceTypeText)) {
      return getFallbackDeclaration(configKeyName, type, false);
    }
    PsiClass sourceTypeClass = this.caching.myCachedClass.get(sourceTypeText);
    if (sourceTypeClass == null) {
      return getFallbackDeclaration(configKeyName, type, true);
    }
    return Pair.pair(MetaConfigKey.DeclarationResolveResult.PROPERTY,
            new InfraConfigKeyDeclarationPsiElement(this.libraryName, sourceTypeClass,
                    findPropertyNavigationTarget(sourceTypeClass, configKeyName, module), configKeyName, sourceTypeText, type));
  }

  private static PsiElement findPropertyNavigationTarget(PsiClass sourceTypeClass, String configKeyName, Module module) {
    String propertyName = RelaxedNames.dashedPropertyNameToCamelCase(configKeyName);
    PsiMethod constructor = InfraConfigKetPathBeanPropertyResolver.getBindingConstructor(sourceTypeClass, module);
    if (constructor != null) {
      for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
        if (propertyName.equals(parameter.getName())) {
          return parameter;
        }
      }
      return sourceTypeClass;
    }
    PsiMethod setter = PropertyUtilBase.findPropertySetter(sourceTypeClass, propertyName, false, false);
    if (setter != null) {
      return setter;
    }
    PsiMethod getter = PropertyUtilBase.findPropertyGetter(sourceTypeClass, propertyName, false, false, true);
    return getter != null ? getter : sourceTypeClass;
  }

  @Nullable
  private Pair<MetaConfigKey.DeclarationResolveResult, PsiElement> getFallbackDeclaration(
          String configKeyName, PsiType type, boolean unresolvedSourceTypeClass) {
    PsiElement additionalTarget = additionalConfigTargets.getValue().get(configKeyName);
    if (this.jsonPsiFile != null) {
      return Pair.pair(unresolvedSourceTypeClass ? MetaConfigKey.DeclarationResolveResult.JSON_UNRESOLVED_SOURCE_TYPE : MetaConfigKey.DeclarationResolveResult.JSON,
              new InfraConfigKeyDeclarationPsiElement(this.libraryName, this.jsonPsiFile, ObjectUtils.chooseNotNull(additionalTarget, this.jsonPsiFile), configKeyName,
                      configKeyName, type));
    }
    else if (additionalTarget != null) {
      return Pair.pair(MetaConfigKey.DeclarationResolveResult.ADDITIONAL_JSON,
              new InfraConfigKeyDeclarationPsiElement(this.libraryName, additionalTarget, additionalTarget, configKeyName, configKeyName, type));
    }
    else {
      return null;
    }
  }

  private static MetaConfigKey.DescriptionText getDescription(JsonObject property) {
    String descriptionText = getStringLiteral(property, InfraMetadataConstant.DESCRIPTION);
    if (descriptionText == null) {
      return MetaConfigKey.DescriptionText.NONE;
    }
    return new MetaConfigKey.DescriptionText(descriptionText);
  }

  private static MetaConfigKey.Deprecation getDeprecation(JsonObject property) {
    JsonObject deprecationObject = property.getAsJsonObject(InfraMetadataConstant.DEPRECATION);
    if (deprecationObject != null) {
      String reasonText = getStringLiteral(deprecationObject, InfraMetadataConstant.REASON);
      MetaConfigKey.DescriptionText reason = reasonText == null ? MetaConfigKey.DescriptionText.NONE : new MetaConfigKey.DescriptionText(reasonText);
      MetaConfigKey.Deprecation.DeprecationLevel level = MetaConfigKey.Deprecation.DeprecationLevel.WARNING;
      String levelLiteral = getStringLiteral(deprecationObject, InfraMetadataConstant.LEVEL);
      MetaConfigKey.Deprecation.DeprecationLevel parsedLevel = MetaConfigKey.Deprecation.DeprecationLevel.parse(levelLiteral);
      if (parsedLevel != null) {
        level = parsedLevel;
      }
      String replacement = getStringLiteral(deprecationObject, InfraMetadataConstant.REPLACEMENT);
      return new MetaConfigKey.Deprecation(reason, level, replacement);
    }
    JsonPrimitive deprecatedProperty = property.getAsJsonPrimitive(InfraMetadataConstant.DEPRECATED);
    if (deprecatedProperty != null && deprecatedProperty.getAsBoolean()) {
      return MetaConfigKey.Deprecation.DEPRECATED_WITHOUT_REASON;
    }
    return MetaConfigKey.Deprecation.NOT_DEPRECATED;
  }

  private static Map<String, MetaConfigKey.ItemHint> getItemHints(JsonObject rootObject) {
    JsonElement hintsElement = rootObject.get(InfraMetadataConstant.HINTS);
    if (hintsElement == null) {
      return Collections.emptyMap();
    }
    Map<String, MetaConfigKey.ItemHint> itemHintsMap = new HashMap<>();
    for (JsonElement value : hintsElement.getAsJsonArray()) {
      JsonObject entry = value.getAsJsonObject();
      String nameValue = getStringLiteral(entry, InfraMetadataConstant.NAME);
      if (nameValue != null) {
        itemHintsMap.put(nameValue, createItemHint(entry));
      }
    }
    return itemHintsMap;
  }

  private static MetaConfigKey.ItemHint createItemHint(JsonObject entry) {
    List<MetaConfigKey.ValueProvider> providers = getItemHintProviders(entry);
    List<MetaConfigKey.ValueHint> values = getItemHintValues(entry);
    return new MetaConfigKey.ItemHint(providers, values);
  }

  private static List<MetaConfigKey.ValueProvider> getItemHintProviders(JsonObject entry) {
    JsonObject providerObject;
    String name;
    JsonElement providersObject = entry.get(InfraMetadataConstant.PROVIDERS);
    if (providersObject == null) {
      return Collections.emptyList();
    }
    SmartList smartList = new SmartList();
    for (JsonElement arrayValue : providersObject.getAsJsonArray()) {
      if (arrayValue.isJsonObject() && (name = getStringLiteral((providerObject = arrayValue.getAsJsonObject()),
              InfraMetadataConstant.NAME)) != null) {
        Map<String, String> parameters = getItemHintProviderParameters(providerObject);
        smartList.add(new MetaConfigKey.ValueProvider(name, parameters));
      }
    }
    return smartList;
  }

  private static Map<String, String> getItemHintProviderParameters(JsonObject entry) {
    JsonElement parametersProperty = entry.get(InfraMetadataConstant.PARAMETERS);
    if (parametersProperty == null || !parametersProperty.isJsonObject()) {
      return Collections.emptyMap();
    }
    JsonObject parametersObject = parametersProperty.getAsJsonObject();
    Map<String, String> parameters = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> parameterProperty : parametersObject.entrySet()) {
      JsonElement value = parameterProperty.getValue();
      if (value != null && value.isJsonPrimitive()) {
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        parameters.put(parameterProperty.getKey(), primitive.getAsString());
      }
    }
    return parameters;
  }

  private static List<MetaConfigKey.ValueHint> getItemHintValues(JsonObject entry) {
    JsonObject valueObject;
    String value;
    JsonElement valuesObject = entry.get(InfraMetadataConstant.VALUES);
    if (valuesObject == null || !valuesObject.isJsonArray()) {
      return Collections.emptyList();
    }
    SmartList<MetaConfigKey.ValueHint> smartList = new SmartList<>();
    for (JsonElement arrayValue : valuesObject.getAsJsonArray()) {
      if (arrayValue.isJsonObject() && (value = getStringLiteral((valueObject = arrayValue.getAsJsonObject()), "value")) != null) {
        smartList.add(new MetaConfigKey.ValueHint(value, getDescription(valueObject)));
      }
    }
    return smartList;
  }

  @Nullable
  private static String getStringLiteral(JsonObject object, String propertyName) {
    JsonElement property = object.get(propertyName);
    if (property != null) {
      return property.getAsString();
    }
    return null;
  }

  private static Pair<PsiType, MetaConfigKey.AccessType> getPsiTypeToAccessType(Project project, String type) {
    Map<String, Pair<PsiType, MetaConfigKey.AccessType>> myCachedTypes = CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
      Map<String, Pair<PsiType, MetaConfigKey.AccessType>> myFactoryMap = ConcurrentFactoryMap.createMap(key -> {
        if (key == null) {
          return DUMMY_TYPE;
        }
        try {
          PsiType psiType = elementFactory.createTypeFromText(key.replace('$', '.'), null);
          return Pair.create(psiType, MetaConfigKey.AccessType.forPsiType(psiType));
        }
        catch (IncorrectOperationException e) {
          return DUMMY_TYPE;
        }
      });
      return CachedValueProvider.Result.create(myFactoryMap, PsiModificationTracker.MODIFICATION_COUNT);
    });
    return myCachedTypes.get(type);
  }

  private static final class Caching {
    private final Map<String, PsiClass> myCachedClass;

    private Caching(GlobalSearchScope scope) {
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(scope.getProject());
      this.myCachedClass = FactoryMap.create(key -> {
        return javaPsiFacade.findClass(key.replace('$', '.'), scope);
      });
    }
  }
}

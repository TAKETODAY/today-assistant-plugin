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

import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesElementFactory;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.PropertyKeyIndex;
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraLibraryUtil.TodayVersion;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.model.values.InfraPlaceholderReferenceResolver;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * Reference for {@code ${propertyKey}} placeholder expressions in value in application.properties/yaml.
 * <p/>
 * NB: Unresolved is not an error, as can be reference to JNDI and many other environment specific sources.
 *
 * @see ConfigPlaceholderReference#PLACEHOLDER_PREFIX
 * @see ConfigPlaceholderReference#PLACEHOLDER_SUFFIX
 */
public class InfraPlaceholderReference extends PsiReferenceBase.Poly<PsiElement>
        implements HighlightedReference, ConfigPlaceholderReference {

  private static final String RANDOM_KEY_PREFIX = "random.";

  public InfraPlaceholderReference(PsiElement element, TextRange range) {
    super(element, range, false);
  }

  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    String key = getValue();

    if (StringUtil.startsWith(key, RANDOM_KEY_PREFIX)) {
      String after = Objects.requireNonNull(StringUtil.substringAfter(key, RANDOM_KEY_PREFIX));

      for (Random random : Random.values()) {
        String randomValue = random.getValue();
        if (random.isSupportsParameters() && StringUtil.startsWith(after, randomValue) ||
                after.equals(randomValue)) {
          if (InfraLibraryUtil.isBelowVersion(getModule(), random.getMinimumVersion())) {
            continue;
          }

          PsiClass randomClass = findRandomClass();
          return randomClass != null
                 ? PsiElementResolveResult.createResults(randomClass)
                 : PsiElementResolveResult.createResults(getElement());
        }
      }
      return ResolveResult.EMPTY_ARRAY;
    }

    IProperty systemProperty = getSystemProperties().findPropertyByKey(key);
    if (systemProperty != null) {
      return PsiElementResolveResult.createResults(systemProperty.getPsiElement());
    }

    Set<PsiElement> additionalProperties = new HashSet<>();
    for (InfraPlaceholderReferenceResolver placeholderReferenceResolver : InfraPlaceholderReferenceResolver.EP_NAME.getExtensionList()) {
      Pair<List<PsiElement>, List<VirtualFile>> resolveResult = placeholderReferenceResolver.resolve(this);
      additionalProperties.addAll(resolveResult.first);
    }
    if (!additionalProperties.isEmpty()) {
      return PsiElementResolveResult.createResults(additionalProperties.toArray(PsiElement.EMPTY_ARRAY));
    }

    // fallback to key in any .properties file
    GlobalSearchScope contentScope = getModule().getModuleContentScope();
    Collection<Property> properties = PropertyKeyIndex.getInstance().get(key, getElement().getProject(),
            getElement().getResolveScope().uniteWith(contentScope));

    return PsiElementResolveResult.createResults(properties);
  }

  @Nullable
  private PsiClass findRandomClass() {
    return InfraUtils.findLibraryClass(getModule(), getRandomClassName());
  }

  private String getRandomClassName() {
    return InfraClassesConstants.RANDOM_VALUE_PROPERTY_SOURCE_SB2;
  }

  private Module getModule() {
    return ModuleUtilCore.findModuleForPsiElement(getElement());
  }

  private PropertiesFile getSystemProperties() {
    return PropertiesElementFactory.getSystemProperties(myElement.getProject());
  }

  @Override
  public Object[] getVariants() {
    List<LookupElement> variants = new SmartList<>();
    for (InfraPlaceholderReferenceResolver placeholderReferenceResolver : InfraPlaceholderReferenceResolver.EP_NAME.getExtensionList()) {
      variants.addAll(placeholderReferenceResolver.getVariants(this));
    }

    for (IProperty property : getSystemProperties().getProperties()) {
      String key = property.getKey();
      if (key == null)
        continue;
      variants.add(LookupElementBuilder.create(property, key).withIcon(Icons.Today));
    }

    PsiClass randomClass = findRandomClass();
    if (randomClass == null) {
      return ArrayUtil.toObjectArray(variants);
    }

    Module module = getModule();
    for (Random random : Random.values()) {
      if (InfraLibraryUtil.isBelowVersion(module, random.getMinimumVersion())) {
        continue;
      }

      String randomText = random.getValue();
      String insertString = RANDOM_KEY_PREFIX + randomText;

      variants.add(LookupElementBuilder.create(randomClass, insertString).withIcon(Icons.Today));
      if (random.isSupportsParameters()) {
        variants.add(LookupElementBuilder.create(randomClass, insertString)
                .withPresentableText(RANDOM_KEY_PREFIX + randomText + "(value,[max])")
                .withIcon(Icons.Today)
                .withInsertHandler(ParenthesesInsertHandler.WITH_PARAMETERS));
      }
    }

    return ArrayUtil.toObjectArray(variants);
  }

  public static List<VirtualFile> findConfigFiles(PsiReference reference, FileType fileType) {
    Module module = ModuleUtilCore.findModuleForPsiElement(reference.getElement());
    if (module == null)
      return Collections.emptyList();

    if (!InfraLibraryUtil.hasFrameworkLibrary(module))
      return Collections.emptyList();

    PsiFile containingFile = reference.getElement().getContainingFile();
    PsiFile originalFile = null;
    if (containingFile != null) {
      // in completion we need to check where the original file is located
      originalFile = containingFile.getOriginalFile();
    }

    boolean includeTestScope = originalFile != null && ProjectRootsUtil.isInTestSource(originalFile);
    List<VirtualFile> configFiles = InfraConfigurationFileService.of().findConfigFilesWithImports(module, includeTestScope);
    return ContainerUtil.filter(configFiles, configFile -> configFile.getFileType().equals(fileType));
  }

  @Nullable
  public static PsiElement resolveKeyReference(PsiElement keyElement) {
    for (PsiReference psiReference : keyElement.getReferences()) {
      if (psiReference instanceof MetaConfigKeyReference) {
        return psiReference.resolve();
      }
    }
    return null;
  }

  enum Random {
    INT("int", true, TodayVersion.ANY),
    LONG("long", true, TodayVersion.ANY),
    VALUE("value", false, TodayVersion.ANY),
    UUID("uuid", false, TodayVersion.ANY);

    private final String myValue;
    private final boolean mySupportsParameters;
    private final TodayVersion myMinimumVersion;

    Random(String value, boolean supportsParameters, TodayVersion minimumVersion) {
      myValue = value;
      mySupportsParameters = supportsParameters;
      myMinimumVersion = minimumVersion;
    }

    TodayVersion getMinimumVersion() {
      return myMinimumVersion;
    }

    String getValue() {
      return myValue;
    }

    boolean isSupportsParameters() {
      return mySupportsParameters;
    }
  }
}

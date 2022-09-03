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

package cn.taketoday.assistant.model.values;

import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.jam.JamService;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.references.CreatePropertyFix;
import com.intellij.lang.properties.references.PropertyReferenceBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.jam.stereotype.JamPropertySource;
import cn.taketoday.assistant.model.jam.stereotype.PropertySource;
import cn.taketoday.assistant.model.jam.stereotype.PropertySources;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.Prop;
import cn.taketoday.assistant.model.xml.context.PropertyPlaceholder;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class PlaceholderPropertyReference extends PropertyReferenceBase implements LocalQuickFixProvider, HighlightedReference {
  private final PlaceholderInfo myInfo;
  @Nullable
  private final String myDefaultValue;

  private PlaceholderPropertyReference(String key, PsiElement element, TextRange textRange, PlaceholderInfo info, @Nullable String defaultValue, boolean soft) {
    super(key, soft, element, textRange);
    this.myInfo = info;
    this.myDefaultValue = defaultValue;
  }

  @Nullable
  public String getDefaultValue() {
    return this.myDefaultValue;
  }

  public boolean isHighlightedWhenSoft() {
    return true;
  }

  public static PlaceholderPropertyReference create(PsiElement element, TextRange textRange, PlaceholderInfo info) {
    return create(element, textRange, info, false);
  }

  public static PlaceholderPropertyReference create(PsiElement element, TextRange textRange, PlaceholderInfo info, boolean soft) {
    String text = info.text;
    if (text.contains(":")) {
      int offset = textRange.getStartOffset();
      int endOffset = text.indexOf(':');
      String key = text.substring(0, endOffset);
      String defaultValue = text.substring(endOffset + 1);
      return new PlaceholderPropertyReference(key, element, TextRange.from(offset, endOffset), info, defaultValue, soft);
    }
    return new PlaceholderPropertyReference(text, element, textRange, info, null, soft);
  }

  private static Properties getValueProperties(InfraProperty property) {
    Properties props = new Properties();
    String value = property.getValue().getStringValue();
    if (!StringUtil.isEmptyOrSpaces(value)) {
      try {
        props.load(new StringReader(value));
      }
      catch (IOException ignored) { }
    }
    return props;
  }

  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    Set<IProperty> properties = new HashSet<>();
    Processor<PropertiesFile> processor = propertiesFile -> {
      properties.addAll(propertiesFile.findPropertiesByKey(this.myKey));
      return true;
    };
    Set<DomElement> configurerProperties = new HashSet<>();
    CommonInfraModel springModel = InfraModelService.of().getModel(this.myElement);
    List<BeanPointer<?>> placeholders = getPlaceholders(springModel);
    if (placeholders.size() > 0) {
      configurerProperties.addAll(getPlaceholderConfigurerProperties(this.myKey, placeholders));
      processXmlProperties(processor, placeholders);
    }
    Set<PsiElement> additionalProperties = new HashSet<>();
    Set<VirtualFile> processedFiles = new HashSet<>();
    for (InfraPlaceholderReferenceResolver placeholderReferenceResolver : InfraPlaceholderReferenceResolver.EP_NAME.getExtensionList()) {
      Pair<List<PsiElement>, List<VirtualFile>> resolveResult = placeholderReferenceResolver.resolve(this);
      additionalProperties.addAll(resolveResult.first);
      processedFiles.addAll(resolveResult.second);
    }
    for (PropertiesFile propertiesFile2 : getPropertiesFiles(springModel)) {
      if (!processedFiles.contains(propertiesFile2.getVirtualFile())) {
        processor.process(propertiesFile2);
      }
    }
    ResolveResult[] result = new ResolveResult[properties.size() + configurerProperties.size() + additionalProperties.size()];
    if (properties.size() > 0 || configurerProperties.size() > 0 || !additionalProperties.isEmpty()) {
      int i = 0;
      for (IProperty iProperty : properties) {
        int i2 = i;
        i++;
        result[i2] = new PsiElementResolveResult(iProperty instanceof PsiElement ? (PsiElement) iProperty : PomService.convertToPsi((PsiTarget) iProperty));
      }
      for (DomElement configurerProperty : configurerProperties) {
        int i3 = i;
        i++;
        result[i3] = new PsiElementResolveResult(configurerProperty.getXmlElement());
      }
      for (PsiElement additionalProperty : additionalProperties) {
        int i4 = i;
        i++;
        result[i4] = new PsiElementResolveResult(additionalProperty);
      }
    }
    else if (System.getProperties().getProperty(this.myKey) != null) {
      return new ResolveResult[] { new PsiElementResolveResult(getElement()) };
    }
    return result;
  }

  public Collection<PropertiesFile> getPropertiesFiles(CommonInfraModel springModel) {
    return springModel instanceof UserDataHolder ? getCachedPropertiesFiles(springModel) : getFiles(springModel);
  }

  private static Collection<PropertiesFile> getCachedPropertiesFiles(CommonInfraModel springModel) {
    Collection<PropertiesFile> emptyList;
    Module module = springModel.getModule();
    if (module != null && (springModel instanceof UserDataHolder)) {
      emptyList = CachedValuesManager.getManager(module.getProject()).getCachedValue((UserDataHolder) springModel, () -> {
        return CachedValueProvider.Result.create(getFiles(springModel), InfraModificationTrackersManager.from(springModel.getModule().getProject()).getOuterModelsDependencies());
      });
    }
    else {
      emptyList = Collections.emptyList();
    }
    return emptyList;
  }

  private static Collection<PropertiesFile> getFiles(CommonInfraModel springModel) {
    CommonProcessors.CollectProcessor<PropertiesFile> processor = new CommonProcessors.CollectProcessor<>();
    processEmbeddedPropertySources(processor, springModel);
    Module module = springModel.getModule();
    if (module != null) {
      processCommonModel(module.getProject(), processor, springModel);
    }
    return processor.getResults();
  }

  public static void processCommonModel(Project project, Processor<? super PropertiesFile> processor, CommonInfraModel springModel) {
    InfraModelVisitors.visitRelatedModels(springModel, InfraModelVisitorContext.context(processor, (model, params) -> {
      if (model instanceof InfraModel) {
        return processFilesetCustomPropertiesFiles(processor, ((InfraModel) model).getFileSet(), project);
      }
      if (model instanceof LocalAnnotationModel) {
        return processLocalAnnotationModelPropertySources(project, processor, (LocalAnnotationModel) model);
      }
      return true;
    }));
  }

  private static boolean processLocalAnnotationModelPropertySources(Project project, Processor<? super PropertiesFile> processor, LocalAnnotationModel model) {
    Configuration configuration = JamService.getJamService(project).getJamElement(Configuration.JAM_KEY, model.getConfig());
    if (configuration != null) {
      for (PropertySource source : configuration.getPropertySources()) {
        for (PropertiesFile propertiesFile : source.getPropertiesFiles()) {
          if (!processor.process(propertiesFile)) {
            return false;
          }
        }
      }
      return true;
    }
    return true;
  }

  private static boolean processFilesetCustomPropertiesFiles(Processor<? super PropertiesFile> processor, @Nullable InfraFileSet fileSet, Project project) {
    if (fileSet == null) {
      return true;
    }
    Set<VirtualFilePointer> files = fileSet.getPropertiesFiles();
    for (VirtualFilePointer pointer : files) {
      VirtualFile virtualFile = pointer.getFile();
      if (virtualFile != null) {
        PsiFile findFile = PsiManager.getInstance(project).findFile(virtualFile);
        if ((findFile instanceof PropertiesFile propertiesFile) && !processor.process(propertiesFile)) {
          return false;
        }
      }
    }
    return true;
  }

  public static void processEmbeddedPropertySources(Processor<? super PropertiesFile> processor, CommonInfraModel model) {
    List<JamPropertySource> propertySources = getPropertySources(model);
    for (JamPropertySource propertySource : propertySources) {
      for (PropertiesFile file : propertySource.getPropertiesFiles()) {
        processor.process(file);
      }
    }
  }

  private static List<JamPropertySource> getPropertySources(CommonInfraModel model) {
    return ContainerUtil.filter(getPropertySources(model.getModule()), source -> {
      PsiClass psiClass = source.getPsiElement();
      return psiClass.isValid() && InfraUtils.isBeanCandidateClass(psiClass) && InfraModelSearchers.doesBeanExist(model, ModelSearchParameters.byClass(psiClass));
    });
  }

  public static List<JamPropertySource> getPropertySources(@Nullable Module module) {
    if (module == null || DumbService.isDumb(module.getProject())) {
      return Collections.emptyList();
    }
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      SmartList<JamPropertySource> smartList = new SmartList<>();
      JamService jamService = JamService.getJamService(module.getProject());
      smartList.addAll(jamService.getJamClassElements(JamPropertySource.META, AnnotationConstant.PROPERTY_SOURCE, scope));
      for (PropertySources propertySources : jamService.getJamClassElements(PropertySources.META, AnnotationConstant.PROPERTY_SOURCES, scope)) {
        smartList.addAll(propertySources.getPropertySources());
      }
      return CachedValueProvider.Result.create(smartList, InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies());
    });
  }

  public static void processXmlProperties(Processor<? super PropertiesFile> processor, List<? extends BeanPointer<?>> placeholders) {
    for (PropertiesFile resource : cn.taketoday.assistant.model.values.PlaceholderUtils.getInstance().getResources(placeholders)) {
      if (!processor.process(resource)) {
        return;
      }
    }
  }

  private Set<DomElement> getPlaceholderConfigurerProperties(String key, List<BeanPointer<?>> placeholders) {
    Set<DomElement> set = RecursionManager.doPreventingRecursion(
            this, true, () -> getAllPlaceholderConfigurerProperties(placeholders).get(key));
    return set == null ? Collections.emptySet() : set;
  }

  public static Map<String, Set<DomElement>> getAllPlaceholderConfigurerProperties(List<? extends BeanPointer<?>> placeholders) {
    Map<String, Set<DomElement>> all = new HashMap<>();
    for (BeanPointer placeholder : placeholders) {
      CommonInfraBean placeholderBean = placeholder.getBean();
      addProperties(all, placeholderBean);
      if (placeholderBean instanceof PropertyPlaceholder propertyPlaceholder) {
        BeanPointer<?> propertyBeanHolder = propertyPlaceholder.getPropertiesRef().getValue();
        if (propertyBeanHolder != null) {
          addProperties(all, propertyBeanHolder.getBean());
        }
      }
    }
    return all;
  }

  private static void addProperties(Map<String, Set<DomElement>> all, CommonInfraBean bean) {
    InfraPropertyDefinition propertyDefinition = InfraPropertyUtils.findPropertyByName(bean, "properties");
    if (propertyDefinition instanceof InfraProperty) {
      for (Prop prop : ((InfraProperty) propertyDefinition).getProps().getProps()) {
        String propKey = prop.getKey().getStringValue();
        if (!StringUtil.isEmptyOrSpaces(propKey)) {
          if (!all.containsKey(propKey)) {
            all.put(propKey, new HashSet<>());
          }
          all.get(propKey).add(prop);
        }
      }
      for (InfraEntry prop2 : ((InfraProperty) propertyDefinition).getMap().getEntries()) {
        String keyAttr = prop2.getKeyAttr().getStringValue();
        if (!StringUtil.isEmptyOrSpaces(keyAttr)) {
          if (!all.containsKey(keyAttr)) {
            all.put(keyAttr, new HashSet<>());
          }
          all.get(keyAttr).add(prop2);
        }
      }
      for (Object propName : getValueProperties((InfraProperty) propertyDefinition).keySet()) {
        if (propName instanceof String) {
          if (!all.containsKey(propName)) {
            all.put((String) propName, new HashSet<>());
          }
          all.get(propName).add(((InfraProperty) propertyDefinition).getValue());
        }
      }
    }
  }

  public LocalQuickFix[] getQuickFixes() {
    CommonInfraModel model = InfraModelService.of().getModel(this.myElement);
    List<BeanPointer<?>> placeholders = getPlaceholders(model);
    Set<PropertiesFile> resources = cn.taketoday.assistant.model.values.PlaceholderUtils.getInstance().getResources(placeholders);
    return new LocalQuickFix[] { new CreatePropertyFix(getElement(), this.myKey, new ArrayList(resources)) };
  }

  public List<BeanPointer<?>> getPlaceholders(CommonInfraModel model) {
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    for (BeanPointer<?> pointer : getConfigurers(model, myElement.getProject())) {
      if (pointer.isValid()) {
        Pair<String, String> pair = PlaceholderUtils.getInstance().getPlaceholderPrefixAndSuffixInner(pointer);
        if (myInfo.prefixAndSuffix.equals(pair)) {
          smartList.add(pointer);
        }
      }
    }
    return smartList;
  }

  private static Set<BeanPointer<?>> getConfigurers(CommonInfraModel model, Project project) {
    if (model instanceof UserDataHolder) {
      return CachedValuesManager.getManager(project).getCachedValue((UserDataHolder) model, () -> {
        Set<BeanPointer<?>> configurers = InfraModelVisitorUtils.getPlaceholderConfigurers(model);
        return CachedValueProvider.Result.create(configurers, getDependencies(model.getModule(), configurers));
      });
    }
    return InfraModelVisitorUtils.getPlaceholderConfigurers(model);
  }

  private static Object[] getDependencies(@Nullable Module module, Collection<? extends BeanPointer<?>> configurers) {
    Set<Object> dependencies = new LinkedHashSet<>();
    ContainerUtil.addAllNotNull(dependencies,
            configurers.stream()
                    .map(BeanPointer::getContainingFile)
                    .collect(Collectors.toSet())
    );
    ContainerUtil.addAll(dependencies,
            module == null
            ? new Object[] { ModificationTracker.EVER_CHANGED }
            : InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies());
    return ArrayUtil.toObjectArray(dependencies);
  }

  public TextRange getFullTextRange() {
    return this.myInfo.fullTextRange;
  }

  @Nullable
  protected List<PropertiesFile> getPropertiesFiles() {
    return null;
  }

  protected boolean isProperty(PsiElement element) {
    if (super.isProperty(element)) {
      return true;
    }
    for (InfraPlaceholderReferenceResolver placeholderReferenceResolver : InfraPlaceholderReferenceResolver.EP_NAME.getExtensionList()) {
      if (placeholderReferenceResolver.isProperty(element)) {
        return true;
      }
    }
    return false;
  }
}

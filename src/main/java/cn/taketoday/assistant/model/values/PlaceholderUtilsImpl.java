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

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FilteringIterator;
import com.intellij.util.text.PlaceholderTextRanges;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.InfraReferenceUtils;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.context.PropertyPlaceholder;
import cn.taketoday.assistant.model.xml.util.UtilProperties;
import cn.taketoday.lang.Nullable;

public class PlaceholderUtilsImpl extends PlaceholderUtils {
  private static final Pair<String, String> DEFAULT_PLACEHOLDER_PAIR;

  private static final String LOCATION_PROPERTY_NAME = "location";
  private static final String LOCATIONS_PROPERTY_NAME = "locations";
  private static final String PROPERTIES_PROPERTY_NAME = "properties";

  private static final String PROPERTIES_ARRAY_PROPERTY_NAME = "propertiesArray";
  private static final Key<CachedValue<Set<Pair<String, String>>>> PLACEHOLDER_PREFIXES_KEY;
  private static final Key<CachedValue<Pair<String, String>>> PLACEHOLDER_PREFIX_SUFFIX;

  static {
    DEFAULT_PLACEHOLDER_PAIR = Pair.create(PlaceholderUtils.DEFAULT_PLACEHOLDER_PREFIX,
            PlaceholderUtils.DEFAULT_PLACEHOLDER_SUFFIX);
    PLACEHOLDER_PREFIXES_KEY = Key.create("PLACEHOLDER_PREFIX_SUFFIX");
    PLACEHOLDER_PREFIX_SUFFIX = Key.create("PLACEHOLDER_PREFIX_SUFFIX");
  }

  @Override
  public Set<PropertiesFile> getResources(Collection<? extends BeanPointer<?>> configurerBeans) {
    Set<PropertiesFile> resources = new LinkedHashSet<>();
    for (BeanPointer<?> bean : configurerBeans) {
      getResources(bean, resources);
    }
    return resources;
  }

  private boolean getResources(BeanPointer<?> configurerBean, Set<PropertiesFile> resources) {
    List<Pair<String, PsiElement>> locations = getLocations(configurerBean.getBean());
    boolean hasNotResolvedLocations = false;
    for (Pair<String, PsiElement> location : locations) {
      Set<PropertiesFile> propertiesFile = getPropertiesFile(location);
      if (!propertiesFile.isEmpty()) {
        resources.addAll(propertiesFile);
      }
      else {
        hasNotResolvedLocations = true;
      }
    }
    return hasNotResolvedLocations && !locations.isEmpty();
  }

  @Override
  public Set<PropertiesFile> getPropertiesFile(Pair<String, PsiElement> location) {
    PsiElement psiElement = location.second;
    String value = location.first;
    if (psiElement == null || value == null) {
      return Collections.emptySet();
    }
    return new LinkedHashSet(getResourceFiles(psiElement, value, ",", new FilteringIterator.InstanceOf(PsiFile.class)));
  }

  @Override
  public List<Pair<String, PsiElement>> getLocations(CommonInfraBean configurerBean) {
    return CachedValuesManager.getCachedValue(configurerBean.getXmlTag(), () -> {
      BeanPointer<?> propertiesRefBean;
      SmartList<Pair<String, PsiElement>> smartList = new SmartList<>();
      if (configurerBean instanceof PropertyPlaceholder placeholder) {
        GenericAttributeValue<String> location = placeholder.getLocation();
        if (location.getRawText() != null && location.getXmlElement() != null) {
          addIfNotNull(smartList, location.getRawText(), location.getXmlElement());
        }
        BeanPointer<?> propertiesBeanPointer = placeholder.getPropertiesRef().getValue();
        if (propertiesBeanPointer != null) {
          addLocations(smartList, propertiesBeanPointer);
        }
      }
      else {
        InfraPropertyDefinition locationProperty = InfraPropertyUtils.findPropertyByName(configurerBean, LOCATION_PROPERTY_NAME);
        if (locationProperty != null) {
          addIfNotNull(smartList, getPropertyValue(locationProperty));
        }
        processLocationsProperty(smartList, configurerBean);
      }
      InfraPropertyDefinition propertiesProperty = InfraPropertyUtils.findPropertyByName(configurerBean, PROPERTIES_PROPERTY_NAME);
      if (propertiesProperty != null && (propertiesRefBean = propertiesProperty.getRefValue()) != null) {
        addLocations(smartList, propertiesRefBean);
        processLocationsProperty(smartList, propertiesRefBean.getBean());
      }
      InfraPropertyDefinition propertiesArrayProperty = InfraPropertyUtils.findPropertyByName(configurerBean, PROPERTIES_ARRAY_PROPERTY_NAME);
      if (propertiesArrayProperty != null) {
        GenericDomValue<BeanPointer<?>> element = propertiesArrayProperty.getRefElement();
        if (element != null) {
          addLocations(smartList, element.getValue());
        }
        if (propertiesArrayProperty instanceof InfraProperty property) {
          addLocations(smartList, property.getList());
          addLocations(smartList, property.getSet());
          addLocations(smartList, property.getArray());
        }
      }
      return CachedValueProvider.Result.create(smartList, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  @Nullable
  private static Pair<String, PsiElement> getPropertyValue(InfraPropertyDefinition property) {
    GenericDomValue<?> value = getPropertyDomValue(property);
    if (value == null) {
      return null;
    }
    return Pair.create(value.getRawText(), DomUtil.getValueElement(value));
  }

  @Nullable
  private static GenericDomValue<?> getPropertyDomValue(InfraPropertyDefinition property) {
    GenericDomValue<?> valueElement = property.getValueElement();
    if (valueElement == null || valueElement.getRawText() != null) {
      return valueElement;
    }
    return null;
  }

  private static void addIfNotNull(List<Pair<String, PsiElement>> locations, Pair<String, PsiElement> pair) {
    if (pair != null && pair.first != null && pair.second != null) {
      locations.add(pair);
    }
  }

  private static void processLocationsProperty(List<Pair<String, PsiElement>> locations, CommonInfraBean bean) {
    InfraPropertyDefinition locationsProperty = InfraPropertyUtils.findPropertyByName(bean, "locations");
    if (locationsProperty instanceof InfraProperty springProperty) {
      Pair<String, PsiElement> propertyValue = getPropertyValue(locationsProperty);
      if (propertyValue != null) {
        addIfNotNull(locations, propertyValue);
        return;
      }
      addLocationsFromCollection(locations, springProperty.getList());
      addLocationsFromCollection(locations, springProperty.getSet());
      addLocationsFromCollection(locations, springProperty.getArray());
    }
  }

  private static void addLocationsFromCollection(List<Pair<String, PsiElement>> locations, ListOrSet list) {
    for (InfraValue value : list.getValues()) {
      for (String s : splitLocationString(value.getRawText())) {
        addIfNotNull(locations, s, value.getXmlElement());
      }
    }
  }

  private static void addLocations(List<Pair<String, PsiElement>> locations, ListOrSet listOrSet) {
    for (InfraRef value : listOrSet.getRefs()) {
      addLocations(locations, value.getBean().getValue());
      addLocations(locations, value.getLocal().getValue());
    }
  }

  private static void addLocations(List<Pair<String, PsiElement>> locations, @Nullable BeanPointer<?> beanPointer) {
    GenericDomValue<String> location;
    if (beanPointer != null && (location = getLocationDomElementValue(beanPointer.getBean())) != null) {
      String locationString = location.getRawText();
      for (String s : splitLocationString(locationString)) {
        addIfNotNull(locations, s.trim(), DomUtil.getValueElement(location));
      }
    }
  }

  private static void addIfNotNull(List<Pair<String, PsiElement>> locations, @Nullable String str, @Nullable XmlElement valueElement) {
    if (str != null && valueElement != null) {
      locations.add(Pair.create(str, valueElement));
    }
  }

  private static Set<String> splitLocationString(@Nullable String location) {
    Set<String> locations = new LinkedHashSet<>();
    if (!StringUtil.isEmptyOrSpaces(location)) {
      for (String s : StringUtil.split(location, ",")) {
        if (!StringUtil.isEmptyOrSpaces(s)) {
          locations.add(s.trim());
        }
      }
    }
    return locations;
  }

  @Nullable
  private static GenericDomValue<String> getLocationDomElementValue(CommonInfraBean springBean) {
    PsiClass psiClass;
    InfraPropertyDefinition location;
    if (springBean instanceof UtilProperties utilProperties) {
      return utilProperties.getLocation();
    }
    else if ((springBean instanceof InfraBean) && (psiClass = PsiTypesUtil.getPsiClass(springBean.getBeanType())) != null && InfraConstant.PROPERTIES_FACTORY_BEAN.equals(
            psiClass.getQualifiedName()) && (location = InfraPropertyUtils.findPropertyByName(springBean, LOCATION_PROPERTY_NAME)) != null) {
      return (GenericDomValue<String>) location.getValueElement();
    }
    else {
      return null;
    }
  }

  @Override
  public boolean containsDefaultPlaceholderDefinitions(GenericDomValue genericDomValue) {
    String stringValue = genericDomValue.getRawText();
    if (stringValue != null && !StringUtil.isEmptyOrSpaces(stringValue)) {
      return isPrefixAndSuffixDefinedCorrectly(stringValue, DEFAULT_PLACEHOLDER_PAIR);
    }
    return false;
  }

  @Override
  public boolean isRawTextPlaceholder(GenericDomValue genericDomValue) {
    return isPlaceholder(genericDomValue, genericDomValue.getRawText());
  }

  @Override
  public boolean isPlaceholder(GenericDomValue genericDomValue) {
    return isPlaceholder(genericDomValue, genericDomValue.getStringValue());
  }

  @Override
  public boolean isPlaceholder(GenericDomValue genericDomValue, String stringValue) {
    if (stringValue != null && !StringUtil.isEmptyOrSpaces(stringValue)) {
      if (isPrefixAndSuffixDefinedCorrectly(stringValue, DEFAULT_PLACEHOLDER_PAIR)) {
        return true;
      }
      if (DomUtil.hasXml(genericDomValue)) {
        XmlElement element = genericDomValue.getXmlElement();
        Set<Pair<String, String>> prefixes = getPlaceholderPrefixes(element);
        if (prefixes == null) {
          return false;
        }
        for (Pair<String, String> pair : prefixes) {
          if (isPrefixAndSuffixDefinedCorrectly(stringValue, pair)) {
            return true;
          }
        }
        return false;
      }
      return false;
    }
    return false;
  }

  @Override
  public boolean isPlaceholder(String stringValue, List<BeanPointer<?>> configurers) {
    for (BeanPointer<?> configurer : configurers) {
      Object mo448getSpringBean = configurer.getBean();
      if ((mo448getSpringBean instanceof DomInfraBean) && isPrefixAndSuffixDefinedCorrectly(stringValue, getPlaceholderPrefixAndSuffix((DomInfraBean) mo448getSpringBean))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isDefaultPlaceholder(@Nullable String stringValue) {
    return stringValue != null && isPrefixAndSuffixDefinedCorrectly(stringValue, DEFAULT_PLACEHOLDER_PAIR);
  }

  private static boolean isPrefixAndSuffixDefinedCorrectly(String stringValue, Pair<String, String> prefixAndSuffix) {
    int prefixPos = stringValue.indexOf(prefixAndSuffix.first);
    return prefixPos >= 0 && prefixPos < stringValue.indexOf(prefixAndSuffix.second);
  }

  @Override
  public Pair<String, String> getPlaceholderPrefixAndSuffix(DomInfraBean placeholderBean) {
    return CachedValuesManager.getManager(placeholderBean.getPsiManager().getProject())
            .getCachedValue(placeholderBean, PLACEHOLDER_PREFIX_SUFFIX,
                    () -> new CachedValueProvider.Result<>(getPlaceholderPrefixAndSuffixInner(placeholderBean), placeholderBean.getXmlElement()), false);
  }

  @Override
  public Pair<String, String> getPlaceholderPrefixAndSuffixInner(@Nullable BeanPointer<?> pointer) {
    if (pointer != null) {
      Object mo448getSpringBean = pointer.getBean();
      if (mo448getSpringBean instanceof DomInfraBean) {
        return getPlaceholderPrefixAndSuffixInner((DomInfraBean) mo448getSpringBean);
      }
    }
    return DEFAULT_PLACEHOLDER_PAIR;
  }

  @Override
  public Pair<String, String> getPlaceholderPrefixAndSuffixInner(DomInfraBean placeholderBean) {
    String prefix = getPlaceholderConfigProperty(placeholderBean, PlaceholderUtils.PLACEHOLDER_PREFIX_PROPERTY_NAME,
            PlaceholderUtils.DEFAULT_PLACEHOLDER_PREFIX);
    String suffix = getPlaceholderConfigProperty(placeholderBean, PlaceholderUtils.PLACEHOLDER_SUFFIX_PROPERTY_NAME,
            PlaceholderUtils.DEFAULT_PLACEHOLDER_SUFFIX);
    return Pair.create(prefix, suffix);
  }

  private static String getPlaceholderConfigProperty(DomInfraBean placeholderBean, String propertyName, String defaultValue) {
    InfraPropertyDefinition userDefinedPrefixProperty = InfraPropertyUtils.findPropertyByName(placeholderBean, propertyName);
    if (userDefinedPrefixProperty != null && userDefinedPrefixProperty.getValueElement() != null) {
      String value = userDefinedPrefixProperty.getValueElement().getRawText();
      if (!StringUtil.isEmptyOrSpaces(value)) {
        return value;
      }
    }
    return defaultValue;
  }

  @Override
  public PsiReference[] createPlaceholderPropertiesReferences(GenericDomValue genericDomValue) {
    Map<TextRange, PlaceholderInfo> textRanges = getTextRanges(genericDomValue);
    if (textRanges == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    return createPlaceholderPropertiesReferences(textRanges, DomUtil.getValueElement(genericDomValue), true);
  }

  @Override
  public PsiReference[] createPlaceholderPropertiesReferences(PsiElement psiElement) {
    return createPlaceholderPropertiesReferences(getTextRangesWithNested(psiElement), psiElement, false);
  }

  private static PsiReference[] createPlaceholderPropertiesReferences(Map<TextRange, PlaceholderInfo> textRanges, @Nullable PsiElement valueElement, boolean soft) {
    if (valueElement == null || textRanges.isEmpty()) {
      return PsiReference.EMPTY_ARRAY;
    }
    SmartList<PsiReference> smartList = new SmartList<>();
    TextRange[] ranges = textRanges.keySet().toArray(TextRange.EMPTY_ARRAY);
    for (TextRange textRange : ranges) {
      if (!hasNestedPlaceholders(textRange, ranges)) {
        PlaceholderInfo info = textRanges.get(textRange);
        smartList.add(PlaceholderPropertyReference.create(valueElement, textRange, info, soft));
      }
    }
    return smartList.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static boolean hasNestedPlaceholders(TextRange textRange, TextRange... ranges) {
    for (TextRange range : ranges) {
      if (!range.equals(textRange) && textRange.contains(range)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private Map<TextRange, PlaceholderInfo> getTextRanges(GenericDomValue<?> domValue) {
    XmlElement valueElement;
    Map<TextRange, PlaceholderInfo> textRanges = new HashMap<>();
    Set<Pair<String, String>> prefixes = getPlaceholderPrefixes(domValue.getXmlElement());
    if (prefixes == null || (valueElement = DomUtil.getValueElement(domValue)) == null) {
      return null;
    }
    String text = valueElement.getText();
    for (Pair<String, String> prefixAndSuffix : prefixes) {
      String prefix = prefixAndSuffix.first;
      String suffix = prefixAndSuffix.second;
      for (TextRange fullTextRange : PlaceholderTextRanges.getPlaceholderRanges(text, prefix, suffix, true)) {
        TextRange placeholderTextRange = new TextRange(fullTextRange.getStartOffset() + prefix.length(), fullTextRange.getEndOffset() - suffix.length());
        String placeholderText = placeholderTextRange.substring(text);
        textRanges.put(placeholderTextRange, new PlaceholderInfo(placeholderText, prefixAndSuffix, text, fullTextRange));
      }
    }
    return textRanges;
  }

  private Map<TextRange, PlaceholderInfo> getTextRangesWithNested(PsiElement element) {
    Map<TextRange, PlaceholderInfo> textRanges = new HashMap<>();
    Set<Pair<String, String>> prefixes = getPlaceholderPrefixes(element);
    if (prefixes == null) {
      return Collections.emptyMap();
    }
    for (Pair<String, String> prefixAndSuffix : prefixes) {
      String text = element.getText();
      String prefix = prefixAndSuffix.first;
      String suffix = prefixAndSuffix.second;
      for (TextRange textRange : PlaceholderTextRanges.getPlaceholderRanges(text, prefix, suffix)) {
        String placeholderText = textRange.substring(text);
        textRanges.put(textRange, new PlaceholderInfo(placeholderText, prefixAndSuffix, text, textRange));
      }
    }
    return textRanges;
  }

  private Collection<String> getExpandedVariants(GenericDomValue<?> value) {
    String stringValue = value.getRawText();
    Map<TextRange, PlaceholderInfo> textRanges = this.getTextRanges(value);
    if (textRanges != null && !textRanges.isEmpty()) {
      TextRange[] ranges = sortTextRanges(textRanges);
      XmlElement valueElement = DomUtil.getValueElement(value);

      assert valueElement != null;

      Set<String> result = new HashSet<>();
      ResolveResult[][] variants = new ResolveResult[ranges.length][];

      for (int i = 0; i < ranges.length; ++i) {
        TextRange range = ranges[i];
        PlaceholderInfo info = textRanges.get(range);
        PlaceholderPropertyReference reference = PlaceholderPropertyReference.create(valueElement, range, info, true);
        ResolveResult[] results = reference.multiResolve(false);
        variants[i] = results;
      }

      int[] indices = new int[ranges.length];
      int offsetBase = -ElementManipulators.getValueTextRange(valueElement).getStartOffset();

      boolean quit;
      label62:
      do {
        int offset = offsetBase;
        StringBuilder sb = new StringBuilder(stringValue);

        for (int i = 0; i < indices.length; ++i) {
          PlaceholderInfo info = textRanges.get(ranges[i]);
          if (variants[i].length != 0) {
            ResolveResult resolveResult = variants[i][indices[i]];
            PsiElement element = resolveResult.getElement();
            if (element instanceof IProperty property) {
              String replacement = property.getValue();
              if (replacement != null) {
                sb.replace(offset + info.fullTextRange.getStartOffset(), offset + info.fullTextRange.getEndOffset(), replacement);
                offset += replacement.length() - info.fullTextRange.getLength();
              }
            }
          }
        }

        result.add(sb.toString());
        quit = true;

        for (int idx = 0; idx < indices.length; ++idx) {
          if (indices[idx] < variants[idx].length - 1) {
            quit = false;
            int var10002 = indices[idx]++;
            int i = 0;

            while (true) {
              if (i >= idx) {
                continue label62;
              }

              indices[i] = 0;
              ++i;
            }
          }
        }
      }
      while (!quit);

      return result;
    }
    else {
      return ContainerUtil.createMaybeSingletonList(stringValue);
    }
  }

  @Override
  @Nullable
  public String resolvePlaceholders(GenericDomValue genericDomValue) {
    String result = genericDomValue.getRawText();
    if (StringUtil.isEmptyOrSpaces(result)) {
      return result;
    }
    XmlElement valueElement = DomUtil.getValueElement(genericDomValue);
    assert valueElement != null;

    Map<TextRange, PlaceholderInfo> textRanges = getTextRanges(genericDomValue);
    if (textRanges == null) {
      return result;
    }
    TextRange[] ranges = sortTextRanges(textRanges);
    Map<String, String> changes = new HashMap<>();
    for (TextRange range : ranges) {
      PlaceholderInfo placeholderInfo = textRanges.get(range);
      String propertyValue = getPropertyValue(valueElement, range, placeholderInfo);
      if (propertyValue != null) {
        String strToReplace = placeholderInfo.fullTextRange.substring(placeholderInfo.myElementText);
        if (!StringUtil.isEmptyOrSpaces(strToReplace)) {
          changes.put(strToReplace, propertyValue);
        }
      }
    }
    if (textRanges.size() == changes.size()) {
      for (Map.Entry<String, String> entry : changes.entrySet()) {
        result = result.replace(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  @Nullable
  private static String getPropertyValue(XmlElement valueElement, TextRange range, PlaceholderInfo info) {
    return RecursionManager.doPreventingRecursion(valueElement, true, () -> {
      PlaceholderPropertyReference reference = PlaceholderPropertyReference.create(valueElement, range, info, true);
      ResolveResult[] results = reference.multiResolve(false);
      if (results.length > 0) {
        PsiElement element = results[0].getElement();
        if (element instanceof IProperty property) {
          return property.getValue();
        }
      }
      return reference.getDefaultValue();
    });
  }

  private static TextRange[] sortTextRanges(Map<TextRange, PlaceholderInfo> textRanges) {
    TextRange[] ranges = textRanges.keySet().toArray(TextRange.EMPTY_ARRAY);
    Arrays.sort(ranges, Comparator.comparingInt(TextRange::getStartOffset));
    return ranges;
  }

  @Nullable
  private Set<Pair<String, String>> getPlaceholderPrefixes(PsiElement context) {
    if (context == null) {
      return null;
    }
    else {
      Module module = ModuleUtilCore.findModuleForPsiElement(context);
      if (module == null) {
        return Collections.singleton(DEFAULT_PLACEHOLDER_PAIR);
      }
      else if (!hasCustomPlaceholderPrefixCandidates(module)) {
        return Collections.singleton(DEFAULT_PLACEHOLDER_PAIR);
      }
      else {
        PsiFile contextContainingFile = context.getContainingFile();
        return CachedValuesManager.getCachedValue(contextContainingFile, () -> {
          Set<Pair<String, String>> prefixes = new HashSet<>();
          Set<PsiFile> processed = new HashSet<>();
          if (contextContainingFile instanceof XmlFile) {
            processed.add(contextContainingFile);
            this.processFile((XmlFile) contextContainingFile, prefixes);
          }

          ModuleUtilCore.visitMeAndDependentModules(module, (module1) -> {
            InfraFacet facet = InfraFacet.from(module1);
            if (facet != null) {
              Set<InfraFileSet> sets = facet.getFileSets();
              InfraFileSet[] var6 = sets.toArray(new InfraFileSet[0]);

              for (InfraFileSet set : var6) {
                Set<VirtualFilePointer> files = set.getXmlFiles();

                for (VirtualFilePointer pointer : files) {
                  VirtualFile file = pointer.getFile();
                  if (file != null) {
                    PsiFile psiFile = PsiManager.getInstance(module1.getProject()).findFile(file);
                    if (psiFile instanceof XmlFile && processed.add(psiFile)) {
                      processFile((XmlFile) psiFile, prefixes);
                    }
                  }
                }
              }
            }

            return true;
          });
          return CachedValueProvider.Result.create(prefixes, PsiModificationTracker.MODIFICATION_COUNT);
        });
      }
    }
  }

  private static boolean hasCustomPlaceholderPrefixCandidates(Module module) {

    Project project = module.getProject();
    if (DumbService.isDumb(project)) {
      return false;
    }
    else {
      GlobalSearchScope moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
      Collection<VirtualFile> springXmlFiles = DomService.getInstance().getDomFileCandidates(Beans.class, moduleScope);
      return !CacheManager.getInstance(project)
              .processFilesWithWord(new CommonProcessors.FindFirstProcessor<>(),
                      "placeholderPrefix", UsageSearchContext.ANY, GlobalSearchScope.filesScope(project, springXmlFiles), true);
    }
  }

  private void processFile(XmlFile psiFile, Set<Pair<String, String>> prefixes) {
    Set<Pair<String, String>> values = CachedValuesManager.getManager(psiFile.getProject())
            .getCachedValue(psiFile, PLACEHOLDER_PREFIXES_KEY,
                    () -> new CachedValueProvider.Result<>(this.getPlaceholderPrefixes(psiFile), psiFile), false);
    if (values != null) {
      prefixes.addAll(values);
    }
  }

  private Set<Pair<String, String>> getPlaceholderPrefixes(XmlFile configFile) {

    Module module = ModuleUtilCore.findModuleForFile(configFile);
    Set var10000;
    if (module == null) {
      var10000 = Collections.emptySet();
      return var10000;
    }
    else {
      LocalXmlModel springModel = LocalModelFactory.of().getOrCreateLocalXmlModel(configFile, module, Collections.emptySet());
      if (springModel == null) {
        return Collections.emptySet();
      }
      else {
        List<BeanPointer<?>> configurers = springModel.getPlaceholderConfigurerBeans();
        if (configurers.isEmpty()) {

          return Collections.singleton(DEFAULT_PLACEHOLDER_PAIR);
        }
        else {
          Set<Pair<String, String>> set = new HashSet();

          for (BeanPointer<?> beanPointer : configurers) {
            BeanPointer<?> configurer = beanPointer;
            if (configFile.equals(configurer.getContainingFile())) {
              CommonInfraBean springBean = configurer.getBean();
              if (springBean instanceof DomInfraBean) {
                set.add(this.getPlaceholderPrefixAndSuffixInner((DomInfraBean) springBean));
              }
            }
          }

          return set;
        }
      }
    }
  }

  public Collection<String> getValueVariants(InfraValueHolderDefinition property) {

    GenericDomValue<?> value = InfraPropertyUtils.getPropertyDomValue(property);
    if (value == null) {
      return Collections.emptyList();
    }
    else {
      return getValueVariants(value);
    }
  }

  public Collection<String> getValueVariants(GenericDomValue value) {
    String stringValue = value.getStringValue();
    if (StringUtil.isEmpty(stringValue)) {
      return Collections.emptyList();
    }
    else {
      return getExpandedVariants(value);
    }
  }

  private static Collection<PropertiesFile> getResourceFiles(PsiElement element, String s, String delimiter, Condition<PsiFileSystemItem> filter) {
    List<PsiReference> references = new ArrayList<>();
    int startInElement = ElementManipulators.getOffsetInElement(element);
    ResourcesUtil resourcesUtil = ResourcesUtil.of();
    InfraReferenceUtils.processSeparatedString(s, delimiter, (s1, offset) -> {
      InfraResourcesBuilder builder = InfraResourcesBuilder.create(element, s1).offset(offset + startInElement);
      ContainerUtil.addAll(references, resourcesUtil.getReferences(builder));
      return true;
    });
    PsiReference[] psiReferences = references.toArray(PsiReference.EMPTY_ARRAY);
    Collection<PsiFile> files = resourcesUtil.getResourceItems(psiReferences, filter);
    return ContainerUtil.mapNotNull(files, PropertiesImplUtil::getPropertiesFile);
  }

}

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

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.references.PropertiesCompletionContributor;
import com.intellij.lang.properties.references.PropertiesPsiCompletionUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;
import one.util.streamex.StreamEx;

public class PlaceholderPropertyCompletionContributor extends CompletionContributor {

  public PlaceholderPropertyCompletionContributor() {
    extend(null, PlatformPatterns.psiElement(), new CompletionProvider<>() {

      protected void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
        doAdd(parameters, result);
      }
    });
  }

  private static void doAdd(CompletionParameters parameters, CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    if (!InfraUtils.hasFacets(position.getProject())) {
      return;
    }
    Optional<PsiLanguageInjectionHost> injectionHostOpt = StreamEx.iterate(position.getParent(), Objects::nonNull, PsiElement::getParent).limit(3L).select(PsiLanguageInjectionHost.class).findFirst();
    Optional<PsiReference[]> map = injectionHostOpt.map(host -> ArrayUtil.mergeArrays(position.getReferences(), host.getReferences()));
    PsiReference[] references = map.orElseGet(position::getReferences);
    PlaceholderPropertyReference propertyReference = ContainerUtil.findInstance(references,
            PlaceholderPropertyReference.class);
    if (propertyReference != null) {
      int startOffset = parameters.getOffset();
      PsiElement element = propertyReference.getElement();
      int offsetInElement = startOffset - element.getTextRange().getStartOffset();
      TextRange range = propertyReference.getRangeInElement();
      if (offsetInElement >= range.getStartOffset()) {
        String prefix = element.getText().substring(range.getStartOffset(), offsetInElement);
        LookupElement[] variants = getVariants(propertyReference);
        result.withPrefixMatcher(prefix).addAllElements(Arrays.asList(variants));
        if (variants.length != 0) {
          result.stopHere();
        }
      }
    }
  }

  private static LookupElement[] getVariants(PlaceholderPropertyReference propertyReference) {
    Set<Object> variants = new HashSet<>();
    Processor<PropertiesFile> processor = propertiesFile -> {
      PropertiesPsiCompletionUtil.addVariantsFromFile(propertyReference, propertiesFile, variants);
      return true;
    };
    CommonInfraModel infraModel = InfraModelService.of().getModel(propertyReference.getElement());
    List<BeanPointer<?>> placeholders = propertyReference.getPlaceholders(infraModel);
    PlaceholderPropertyReference.processXmlProperties(processor, placeholders);
    PlaceholderPropertyReference.processEmbeddedPropertySources(processor, infraModel);
    for (String key : PlaceholderPropertyReference.getAllPlaceholderConfigurerProperties(placeholders).keySet()) {
      if (!StringUtil.isEmptyOrSpaces(key)) {
        variants.add(key);
      }
    }
    PlaceholderPropertyReference.processCommonModel(propertyReference.getElement().getProject(), processor, infraModel);
    LookupElement[] lookupElements = PropertiesCompletionContributor.getVariants(variants);
    SmartList<LookupElement> smartList = new SmartList<>();
    for (InfraPlaceholderReferenceResolver resolver : InfraPlaceholderReferenceResolver.array()) {
      smartList.addAll(resolver.getVariants(propertyReference));
    }
    Set<PsiElement> overrideVariants = ContainerUtil.map2Set(smartList, LookupElement::getPsiElement);
    List<LookupElement> filteredElements = ContainerUtil.filter(lookupElements, element -> {
      return !overrideVariants.contains(element.getPsiElement());
    });
    return ContainerUtil.concat(filteredElements, smartList).toArray(LookupElement.EMPTY_ARRAY);
  }
}

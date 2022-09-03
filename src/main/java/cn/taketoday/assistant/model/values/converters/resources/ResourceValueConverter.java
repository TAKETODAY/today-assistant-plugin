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
package cn.taketoday.assistant.model.values.converters.resources;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.impl.ConvertContextFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.utils.InfraReferenceUtils;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

/**
 * Use {@link InfraResourceTypeProvider} to filter specific files (e.g. by extension, XSD).
 */
public class ResourceValueConverter extends Converter<Set<PsiFileSystemItem>> implements CustomReferenceConverter {

  @Override
  public Set<PsiFileSystemItem> fromString(@Nullable String s, ConvertContext context) {
    GenericDomValue domValue = (GenericDomValue) context.getInvocationElement();
    if (StringUtil.isEmpty(s)) {
      return Collections.emptySet();
    }

    return addResourceFilesFrom(domValue, new LinkedHashSet<>(), getFilter(), this);
  }

  private static <V extends PsiFileSystemItem> Set<V> addResourceFilesFrom(GenericDomValue element,
          Set<V> result,
          Condition<PsiFileSystemItem> filter,
          Converter converter) {
    if (converter instanceof CustomReferenceConverter) {
      @SuppressWarnings("unchecked") PsiReference[] references =
              ((CustomReferenceConverter) converter)
                      .createReferences(element, element.getXmlElement(), ConvertContextFactory.createConvertContext(element));
      result.addAll(ResourcesUtil.of().getResourceItems(references, filter));
      return result;
    }
    return result;
  }

  @Override
  public String toString(@Nullable Set<PsiFileSystemItem> o, ConvertContext context) {
    return null;
  }

  @Override
  public PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context) {
    int startInElement = ElementManipulators.getOffsetInElement(element);
    Condition<PsiFileSystemItem> filterCondition = getFilter(genericDomValue);
    Function<PsiFile, Collection<PsiFileSystemItem>> customDefaultPathEvaluator = getCustomDefaultPathEvaluator(element);

    List<PsiReference> result = new SmartList<>();
    InfraReferenceUtils.processSeparatedString(genericDomValue.getStringValue(), ",", (s, offset) -> {
      InfraResourcesBuilder builder = InfraResourcesBuilder.create(element, s).
              fromRoot(true).
              offset(offset + startInElement).
              filter(filterCondition).
              endingSlashNotAllowed(isEndingSlashNotAllowed()).
              customDefaultPathEvaluator(customDefaultPathEvaluator);
      ContainerUtil.addAll(result, ResourcesUtil.of().getReferences(builder));
      return true;
    });
    return result.isEmpty() ? PsiReference.EMPTY_ARRAY : result.toArray(PsiReference.EMPTY_ARRAY);
  }

  protected boolean isEndingSlashNotAllowed() {
    return false;
  }

  protected Condition<PsiFileSystemItem> getFilter() {
    return Conditions.alwaysTrue();
  }

  @Nullable
  protected Function<PsiFile, Collection<PsiFileSystemItem>> getCustomDefaultPathEvaluator(@SuppressWarnings("UnusedParameters") PsiElement element) {
    return null;
  }

  protected Condition<PsiFileSystemItem> getFilter(GenericDomValue genericDomValue) {
    for (InfraResourceTypeProvider provider : InfraResourceTypeProvider.EP_NAME.getExtensionList()) {
      Condition<PsiFileSystemItem> filter = provider.getResourceFilter(genericDomValue);
      if (filter != null)
        return filter;
    }
    return getFilter();
  }

  public static class ResourceValueConverterCondition implements Condition<Pair<PsiType, GenericDomValue>> {

    @Override
    public boolean value(Pair<PsiType, GenericDomValue> pair) {
      PsiType psiType = pair.getFirst();
      if (psiType instanceof PsiArrayType) {
        psiType = ((PsiArrayType) psiType).getComponentType();
      }
      if (!(psiType instanceof PsiClassType)) {
        return false;
      }

      String psiTypeText = psiType.getCanonicalText();
      if (CommonClassNames.JAVA_LANG_STRING.equals(psiTypeText)) {
        return false;
      }

      return InfraConstant.IO_RESOURCE.equals(psiTypeText) ||
              CommonClassNames.JAVA_IO_FILE.equals(psiTypeText) ||
              InheritanceUtil.isInheritor(psiType, "java.io.InputStream");
    }
  }
}

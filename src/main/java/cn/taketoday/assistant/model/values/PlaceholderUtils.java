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

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public abstract class PlaceholderUtils {

  public static final String PLACEHOLDER_CONFIGURER_CLASS = "cn.taketoday.beans.factory.config.PlaceholderConfigurerSupport";

  public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

  public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

  public static final String PLACEHOLDER_PREFIX_PROPERTY_NAME = "placeholderPrefix";

  public static final String PLACEHOLDER_SUFFIX_PROPERTY_NAME = "placeholderSuffix";

  public static PlaceholderUtils getInstance() {
    return ApplicationManager.getApplication().getService(PlaceholderUtils.class);
  }

  public abstract Set<PropertiesFile> getResources(Collection<? extends BeanPointer<?>> configurerBeans);

  public abstract Set<PropertiesFile> getPropertiesFile(Pair<String, PsiElement> location);

  public abstract List<Pair<String, PsiElement>> getLocations(CommonInfraBean configurerBean);

  public abstract boolean containsDefaultPlaceholderDefinitions(GenericDomValue genericDomValue);

  public abstract boolean isRawTextPlaceholder(GenericDomValue genericDomValue);

  public abstract boolean isPlaceholder(GenericDomValue genericDomValue);

  public abstract boolean isPlaceholder(GenericDomValue genericDomValue, String stringValue);

  public abstract boolean isPlaceholder(String stringValue, List<BeanPointer<?>> configurers);

  public abstract boolean isDefaultPlaceholder(@Nullable String stringValue);

  public abstract Pair<String, String> getPlaceholderPrefixAndSuffix(DomInfraBean placeholderBean);

  public abstract Pair<String, String> getPlaceholderPrefixAndSuffixInner(@Nullable BeanPointer<?> pointer);

  public abstract Pair<String, String> getPlaceholderPrefixAndSuffixInner(DomInfraBean placeholderBean);

  public abstract PsiReference[] createPlaceholderPropertiesReferences(GenericDomValue genericDomValue);

  public abstract PsiReference[] createPlaceholderPropertiesReferences(PsiElement psiElement);

  @Nullable
  public abstract String resolvePlaceholders(GenericDomValue genericDomValue);

  public abstract Collection<String> getValueVariants(InfraValueHolderDefinition property);

  public abstract Collection<String> getValueVariants(GenericDomValue value);
}

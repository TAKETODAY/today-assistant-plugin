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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.model.config.jam.ResourceItemsConverter;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.lang.Nullable;

public class ConfigurationProperties extends JamBaseElement<PsiModifierListOwner> {

  private static final JamStringAttributeMeta.Single<String> VALUE_META = JamAttributeMeta.singleString("value");
  private static final JamStringAttributeMeta.Single<String> PREFIX_META = JamAttributeMeta.singleString("prefix");
  private static final JamBooleanAttributeMeta IGNORE_INVALID_FIELDS_META = JamAttributeMeta.singleBoolean("ignoreInvalidFields", false);
  private static final JamBooleanAttributeMeta IGNORE_NESTED_PROPERTIES_META = JamAttributeMeta.singleBoolean("ignoreNestedProperties", false);
  private static final JamBooleanAttributeMeta IGNORE_UNKNOWN_FIELDS_META = JamAttributeMeta.singleBoolean("ignoreUnknownFields", true);
  private static final JamBooleanAttributeMeta EXCEPTION_IF_INVALID_META = JamAttributeMeta.singleBoolean("exceptionIfInvalid", true);
  private static final JamStringAttributeMeta.Collection<Collection<PsiFileSystemItem>> LOCATIONS_META =
          JamAttributeMeta.collectionString("locations", new ResourceItemsConverter());
  private static final JamBooleanAttributeMeta MERGE_META = JamAttributeMeta.singleBoolean("merge", true);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraClassesConstants.CONFIGURATION_PROPERTIES)
          .addAttribute(VALUE_META)
          .addAttribute(PREFIX_META)
          .addAttribute(IGNORE_INVALID_FIELDS_META)
          .addAttribute(IGNORE_NESTED_PROPERTIES_META)
          .addAttribute(IGNORE_UNKNOWN_FIELDS_META)
          .addAttribute(EXCEPTION_IF_INVALID_META)
          .addAttribute(LOCATIONS_META)
          .addAttribute(MERGE_META);

  public static final JamClassMeta<ConfigurationProperties> CLASS_META = new JamClassMeta<>(ConfigurationProperties.class)
          .addAnnotation(ANNOTATION_META);

  public ConfigurationProperties(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }

  @Nullable
  public String getValueOrPrefix() {
    return getValueOrPrefixAttribute().getStringValue();
  }

  public JamStringAttributeElement<String> getValueOrPrefixAttribute() {
    JamStringAttributeElement<String> valueAttribute = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META);
    if (valueAttribute.getPsiElement() != null) {
      return valueAttribute;
    }
    return ANNOTATION_META.getAttribute(getPsiElement(), PREFIX_META);
  }

  public boolean isIgnoreInvalidFields() {
    return ANNOTATION_META.getAttribute(getPsiElement(), IGNORE_INVALID_FIELDS_META).getValue();
  }

  public boolean isIgnoreNestedProperties() {
    return ANNOTATION_META.getAttribute(getPsiElement(), IGNORE_NESTED_PROPERTIES_META).getValue();
  }

  public boolean isIgnoreUnknownFields() {
    return ANNOTATION_META.getAttribute(getPsiElement(), IGNORE_UNKNOWN_FIELDS_META).getValue();
  }

  public boolean isExceptionIfInvalid() {
    return ANNOTATION_META.getAttribute(getPsiElement(), EXCEPTION_IF_INVALID_META).getValue();
  }

  public Collection<PsiFileSystemItem> getLocations() {
    List<JamStringAttributeElement<Collection<PsiFileSystemItem>>> attributeElements = ANNOTATION_META.getAttribute(getPsiElement(), LOCATIONS_META);
    SmartList<PsiFileSystemItem> smartList = new SmartList<>();
    for (JamStringAttributeElement<Collection<PsiFileSystemItem>> element : attributeElements) {
      ContainerUtil.addAllNotNull(smartList, element.getValue());
    }
    return smartList;
  }

  public boolean isMerge() {
    return ANNOTATION_META.getAttribute(getPsiElement(), MERGE_META).getValue();
  }

  public static class Method extends ConfigurationProperties {
    public static final JamMethodMeta<Method> METHOD_META = new JamMethodMeta<>(Method.class).addAnnotation(ANNOTATION_META);

    public Method(PsiElementRef<?> ref) {
      super(ref);
    }

    public boolean isDefinitionFor(PsiClass psiClass) {
      PsiMethod psiMethod = (PsiMethod) getPsiElement();
      PsiClass returnPsiClass = PsiTypesUtil.getPsiClass(psiMethod.getReturnType());
      return psiClass.isEquivalentTo(returnPsiClass) && JamService.getJamService(psiMethod.getProject()).getJamElement(ContextJavaBean.BEAN_JAM_KEY, psiMethod) != null;
    }
  }
}

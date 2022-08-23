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

package cn.taketoday.assistant.beans.stereotype;

import com.intellij.jam.JamCommonModelElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;
import com.intellij.spring.model.jam.testContexts.converters.ApplicationContextReferenceConverter;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;

public class ImportResource extends JamCommonModelElement<PsiClass> implements com.intellij.spring.model.jam.stereotype.ImportResource {
  private static final SemKey<ImportResource> JAM_KEY = IMPORT_RESOURCE_JAM_KEY.subKey("ImportResource");
  public static final JamClassMeta<ImportResource> META = new JamClassMeta<>(null, ImportResource.class, JAM_KEY);
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.CONTEXT_IMPORT_RESOURCE);
  private static final JamStringAttributeMeta.Collection<List<XmlFile>> VALUE_ATTR_META = new JamStringAttributeMeta.Collection<>("value", new ApplicationContextReferenceConverter());
  private static final JamStringAttributeMeta.Collection<List<XmlFile>> LOCATION_ATTR_META = new JamStringAttributeMeta.Collection<>("locations", new ApplicationContextReferenceConverter());

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(LOCATION_ATTR_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
  }

  public ImportResource(PsiClass psiElement) {
    super(PsiElementRef.real(psiElement));
  }

  @Override
  public List<XmlFile> getImportedResources(Module... contexts) {
    SmartList smartList = new SmartList();
    Processor<Pair<List<XmlFile>, ? extends PsiElement>> collect = pair -> {
      smartList.addAll(pair.first);
      return true;
    };
    processImportedResources(collect, contexts);
    return smartList;
  }

  public boolean processImportedResources(Processor<Pair<List<XmlFile>, ? extends PsiElement>> processor, Module... contexts) {
    PsiAnnotation annotation = ANNO_META.getAnnotation(getPsiElement());
    if (annotation != null) {
      return addFiles(processor, getValueAttrElements(), annotation, contexts) && addFiles(processor, getLocationsAttrElements(), annotation, contexts);
    }
    return true;
  }

  protected List<JamStringAttributeElement<List<XmlFile>>> getValueAttrElements() {
    return (List<JamStringAttributeElement<List<XmlFile>>>) ANNO_META.getAttribute(getPsiElement(), VALUE_ATTR_META);
  }

  protected List<JamStringAttributeElement<List<XmlFile>>> getLocationsAttrElements() {
    return ANNO_META.getAttribute(getPsiElement(), LOCATION_ATTR_META);
  }

  public List<JamStringAttributeElement<List<XmlFile>>> getLocationElements() {
    return ContainerUtil.concat(getValueAttrElements(), getLocationsAttrElements());
  }

  protected boolean addFiles(Processor<Pair<List<XmlFile>, ? extends PsiElement>> processor, List<JamStringAttributeElement<List<XmlFile>>> valueAttributeElements,
          PsiElement annotationElement, Module[] contexts) {
    boolean useAnnotationAsElement = valueAttributeElements.size() == 1;
    for (JamStringAttributeElement<List<XmlFile>> element : valueAttributeElements) {
      List<XmlFile> value = element.getValue();
      if (!(getPsiElement() instanceof PsiCompiledElement)) {
        if (value != null) {
          if (!processor.process(Pair.create(value, useAnnotationAsElement ? annotationElement : element.getPsiElement()))) {
            return false;
          }
        }
      }
      else {
        String stringValue = element.getStringValue();
        if (StringUtil.isNotEmpty(stringValue)) {
          List<XmlFile> xmlContexts = ApplicationContextReferenceConverter.getApplicationContexts(stringValue, getPsiElement(), contexts);
          if (!processor.process(Pair.create(xmlContexts, useAnnotationAsElement ? annotationElement : element.getPsiElement()))) {
            return false;
          }
        }
      }
    }
    return true;
  }
}

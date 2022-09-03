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
package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.jam.testContexts.converters.ApplicationContextReferenceConverter;
import cn.taketoday.assistant.model.jam.testContexts.profiles.InfraActiveProfile;
import cn.taketoday.lang.Nullable;

public class InfraContextConfiguration implements ContextConfiguration {
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = CONTEXT_CONFIGURATION_JAM_ANNOTATION_KEY
          .subKey("InfraContextConfiguration");

  private static final SemKey<InfraContextConfiguration> JAM_KEY =
          ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY.subKey("InfraContextConfiguration");
  public static final JamClassMeta<InfraContextConfiguration> META = new JamClassMeta<>(null, InfraContextConfiguration.class, JAM_KEY);

  public static final JamClassAttributeMeta.Single LOADER_ATTR_META = JamAttributeMeta.singleClass(LOADER_ATTR_NAME);

  public static final JamStringAttributeMeta.Collection<List<XmlFile>> LOCATION_ATTR_META =
          JamAttributeMeta.collectionString(LOCATIONS_ATTR_NAME, new ApplicationContextReferenceConverter());

  public static final JamStringAttributeMeta.Collection<List<XmlFile>> VALUE_ATTR_META =
          JamAttributeMeta.collectionString(VALUE_ATTR_NAME, new ApplicationContextReferenceConverter());

  protected static final JamClassAttributeMeta.Collection CLASSES_ATTR_META =
          new JamClassAttributeMeta.Collection(CLASSES_ATTR_NAME);

  protected static final JamClassAttributeMeta.Collection INITIALIZERS_ATTR_META =
          new JamClassAttributeMeta.Collection("initializers");

  public static final JamAnnotationArchetype ARCHETYPE =
          new JamAnnotationArchetype()
                  .addAttribute(LOCATION_ATTR_META)
                  .addAttribute(VALUE_ATTR_META)
                  .addAttribute(CLASSES_ATTR_META)
                  .addAttribute(INITIALIZERS_ATTR_META);

  public static final JamAnnotationMeta ANNO_META =
          new JamAnnotationMeta(AnnotationConstant.CONTEXT_CONFIGURATION, ARCHETYPE, JAM_ANNO_META_KEY);

  static {
    META.addAnnotation(ANNO_META);
  }

  @Nullable
  private final PsiAnchor myPsiClassAnchor;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  @SuppressWarnings("unused")
  public InfraContextConfiguration(PsiClass psiClass) {
    myPsiClassAnchor = PsiAnchor.create(psiClass);
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  @SuppressWarnings("unused")
  public InfraContextConfiguration(PsiAnnotation annotation) {
    PsiClass psiClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true);
    myPsiClassAnchor = psiClass != null ? PsiAnchor.create(psiClass) : null;
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  public List<JamStringAttributeElement<List<XmlFile>>> getLocationsAttributeElement() {
    return LOCATION_ATTR_META.getJam(myPsiAnnotation);
  }

  public List<JamStringAttributeElement<List<XmlFile>>> getValueAttributeElement() {
    return VALUE_ATTR_META.getJam(myPsiAnnotation);
  }

  public boolean isInheritLocations() {
    PsiAnnotation annotation = getAnnotation();
    Boolean value = annotation == null ? null :
                    JamCommonUtil.getObjectValue(annotation.findAttributeValue("inheritLocations"), Boolean.class);

    return value == null || value;
  }

  @Override
  @Nullable
  public PsiClass getPsiElement() {
    return myPsiClassAnchor != null ? (PsiClass) myPsiClassAnchor.retrieveOrThrow() : null;
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Override

  public Set<XmlFile> getLocations(PsiClass... contexts) {
    Set<XmlFile> locations = new LinkedHashSet<>();

    addFiles(contexts, locations, getValueAttributeElement());
    addFiles(contexts, locations, getLocationsAttributeElement());

    return locations;
  }

  private void addFiles(PsiClass[] contexts,
          Set<XmlFile> locations,
          List<JamStringAttributeElement<List<XmlFile>>> elements) {
    for (JamStringAttributeElement<List<XmlFile>> stringAttributeElement : elements) {
      for (PsiClass context : contexts) {
        if (context.equals(getPsiElement())) {
          List<XmlFile> xmlFiles = stringAttributeElement.getValue();
          if (xmlFiles != null) {
            locations.addAll(xmlFiles);
          }
        }
        else {
          String value = stringAttributeElement.getStringValue();
          if (value != null) {
            locations.addAll(ApplicationContextReferenceConverter.getApplicationContexts(value, context));
          }
        }
      }
    }
  }

  @Override

  public List<PsiClass> getConfigurationClasses() {
    List<PsiClass> imported = new SmartList<>();
    for (JamClassAttributeElement jamClassAttributeElement : CLASSES_ATTR_META.getJam(myPsiAnnotation)) {
      ContainerUtil.addIfNotNull(imported, jamClassAttributeElement.getValue());
    }
    return imported;
  }

  public Set<String> getActiveProfiles() {
    PsiClass psiClass = getPsiElement();
    if (psiClass == null)
      return Collections.emptySet();

    InfraActiveProfile profiles = getActiveProfiles(psiClass);
    return profiles != null ? profiles.getActiveProfiles() : Collections.emptySet();
  }

  @Nullable
  public static InfraActiveProfile getActiveProfiles(PsiClass psiClass) {
    return JamService.getJamService(psiClass.getProject()).getJamElement(InfraActiveProfile.ACTIVE_PROFILE_JAM_KEY, psiClass);
  }

  @Override
  public boolean hasLocationsAttribute() {
    return !getLocationsAttributeElement().isEmpty();
  }

  @Override
  public boolean hasValueAttribute() {
    return !getValueAttributeElement().isEmpty();
  }

  @Nullable
  @Override
  public PsiClass getLoaderClass() {
    return LOADER_ATTR_META.getJam(myPsiAnnotation).getValue();
  }

  @Override
  public List<JamStringAttributeElement<List<XmlFile>>> getLocationElements() {
    return ContainerUtil.concat(
            getValueAttributeElement(),
            getLocationsAttributeElement()
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    InfraContextConfiguration that = (InfraContextConfiguration) o;
    return Objects.equals(myPsiClassAnchor, that.myPsiClassAnchor) &&
            Objects.equals(myPsiAnnotation, that.myPsiAnnotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myPsiClassAnchor, myPsiAnnotation);
  }
}

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
package cn.taketoday.assistant.model.jam.testContexts.propertySources;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.jam.converters.PropertiesFileConverter;
import cn.taketoday.assistant.model.jam.stereotype.PropertySource;
import cn.taketoday.lang.Nullable;

public class TestPropertySource extends JamBaseElement<PsiClass> implements PropertySource {

  protected static SemKey<TestPropertySource>
          SPRING_TEST_PROPERTY_SOURCE_JAM_KEY = PROPERTY_SOURCE_JAM_KEY.subKey("SpringTestPropertySource");

  private static final JamStringAttributeMeta.Collection<Set<PropertiesFile>> VALUE_ATTR_META =
          JamAttributeMeta.collectionString("value", new PropertiesFileConverter());

  private static final JamStringAttributeMeta.Collection<Set<PropertiesFile>> LOCATIONS_ATTR_META =
          JamAttributeMeta.collectionString("locations", new PropertiesFileConverter());

  static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.TEST_PROPERTY_SOURCE);

  public static final JamClassMeta<TestPropertySource> META = new JamClassMeta<>(null, TestPropertySource.class,
          SPRING_TEST_PROPERTY_SOURCE_JAM_KEY);

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
    ANNO_META.addAttribute(LOCATIONS_ATTR_META);
  }

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public TestPropertySource(PsiClass psiClass) {
    super(PsiElementRef.real(psiClass));
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  public TestPropertySource(PsiAnnotation annotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true))));
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return ANNO_META.getAnnotation(getPsiElement());
  }

  @Override
  public List<JamStringAttributeElement<Set<PropertiesFile>>> getLocationElements() {
    return ContainerUtil.concat(
            getLocationsAttr(),
            getValueAttr()
    );
  }

  @Override
  public boolean isIgnoreResourceNotFound() {
    // @TestPropertySource do not support ignoreResourceNotFound, resources must be available
    return false;
  }

  @Override
  public Set<PropertiesFile> getPropertiesFiles() {
    Set<PropertiesFile> propertiesFiles = new LinkedHashSet<>();
    if (getValueAttr().isEmpty() && getLocationsAttr().isEmpty()) {
      PropertiesFile propertiesFile = getDefaultPropertiesFile();
      if (propertiesFile != null)
        return Collections.singleton(propertiesFile);
    }
    else {
      addPropertiesFiles(propertiesFiles, getValueAttr());
      addPropertiesFiles(propertiesFiles, getLocationsAttr());
    }
    return propertiesFiles;
  }

  @Override
  public boolean isPsiValid() {
    return isValid();
  }

  protected List<JamStringAttributeElement<Set<PropertiesFile>>> getLocationsAttr() {
    return LOCATIONS_ATTR_META.getJam(myPsiAnnotation);
  }

  protected List<JamStringAttributeElement<Set<PropertiesFile>>> getValueAttr() {
    return VALUE_ATTR_META.getJam(myPsiAnnotation);
  }

  protected void addPropertiesFiles(Set<PropertiesFile> propertiesFiles,
          List<JamStringAttributeElement<Set<PropertiesFile>>> attributeValues) {
    for (JamStringAttributeElement<Set<PropertiesFile>> attributeElement : attributeValues) {
      final Set<PropertiesFile> value = attributeElement.getValue();
      if (value != null)
        propertiesFiles.addAll(value);
    }
  }

  @Nullable
  private PropertiesFile getDefaultPropertiesFile() {
    final String propertiesFileName = getDefaultPropertiesFileName();
    final PsiDirectory containingDirectory = getPsiElement().getContainingFile().getContainingDirectory();
    if (containingDirectory != null) {
      PsiFile file = containingDirectory.findFile(propertiesFileName);
      if (file instanceof PropertiesFile)
        return (PropertiesFile) file;

      final PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(containingDirectory);
      if (psiPackage != null) {
        for (PsiDirectory psiDirectory : psiPackage.getDirectories()) {
          file = psiDirectory.findFile(propertiesFileName);
          if (file instanceof PropertiesFile)
            return (PropertiesFile) file;
        }
      }
    }

    return null;
  }

  private String getDefaultPropertiesFileName() {
    return getPsiElement().getName() + ".properties";
  }
}

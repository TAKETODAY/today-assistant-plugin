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

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;

import java.util.List;
import java.util.Set;

import cn.taketoday.lang.Nullable;

public interface ContextConfiguration extends JamElement {
  String LOCATIONS_ATTR_NAME = "locations";
  String VALUE_ATTR_NAME = "value";
  String CLASSES_ATTR_NAME = "classes";
  String LOADER_ATTR_NAME = "loader";
  String[] XML_FILES_ATTRS = { LOCATIONS_ATTR_NAME, VALUE_ATTR_NAME };

  SemKey<JamAnnotationMeta> CONTEXT_CONFIGURATION_JAM_ANNOTATION_KEY = JamService.ANNO_META_KEY.subKey("ContextConfiguration");
  SemKey<ContextConfiguration> CONTEXT_CONFIGURATION_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("ContextConfiguration");
  SemKey<JamMemberMeta> CONTEXT_CONFIGURATION_META_KEY = JamService.getMetaKey(CONTEXT_CONFIGURATION_JAM_KEY);

  Set<XmlFile> getLocations(PsiClass... contexts);

  List<PsiClass> getConfigurationClasses();

  @Nullable
  PsiClass getLoaderClass();

  boolean hasLocationsAttribute();

  boolean hasValueAttribute();

  PsiClass getPsiElement();

  @Nullable
  PsiAnnotation getAnnotation();

  List<JamStringAttributeElement<List<XmlFile>>> getLocationElements();
}

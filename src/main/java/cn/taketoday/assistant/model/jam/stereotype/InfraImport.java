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

package cn.taketoday.assistant.model.jam.stereotype;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;

import java.util.List;

import cn.taketoday.lang.Nullable;

public interface InfraImport extends CommonModelElement, JamElement {
  SemKey<JamAnnotationMeta> IMPORT_JAM_ANNOTATION_KEY = JamService.ANNO_META_KEY.subKey("SpringImport");
  SemKey<InfraImport> IMPORT_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("SpringImport");
  SemKey<JamMemberMeta> IMPORT_META_KEY = JamService.getMetaKey(IMPORT_JAM_KEY);
  String VALUE_ATTR_NAME = "value";

  List<PsiClass> getImportedClasses();

  PsiClass getPsiElement();

  @Nullable
  PsiAnnotation getAnnotation();
}

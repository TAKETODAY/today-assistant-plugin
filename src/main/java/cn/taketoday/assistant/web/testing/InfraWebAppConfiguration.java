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

package cn.taketoday.assistant.web.testing;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.model.jam.testContexts.converters.ResourcePathReferenceConverter;
import cn.taketoday.assistant.web.InfraWebConstant;
import cn.taketoday.lang.Nullable;

public class InfraWebAppConfiguration implements WebAppConfiguration {
  private static final SemKey<InfraWebAppConfiguration> JAM_KEY = WebAppConfiguration.WEB_APP_CONFIGURATION_JAM_KEY.subKey("WebAppConfiguration");
  public static final JamClassMeta<InfraWebAppConfiguration> META = new JamClassMeta<>(null, InfraWebAppConfiguration.class, JAM_KEY);
  public static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(InfraWebConstant.WEB_APP_CONFIGURATION);
  public static final JamStringAttributeMeta.Collection<List<PsiFileSystemItem>> VALUE_ATTR_META
          = JamAttributeMeta.collectionString("value", new ResourcePathReferenceConverter<>() {
    @Override
    protected Function<ResolveResult, PsiFileSystemItem> getMapper() {
      return result -> {
        PsiElement element = result.getElement();
        if (element instanceof XmlFile xmlFile) {
          return xmlFile;
        }
        return null;
      };
    }
  });
  private final PsiClass myPsiClass;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
  }

  public InfraWebAppConfiguration(PsiClass psiClass) {
    this.myPsiClass = psiClass;
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  public InfraWebAppConfiguration(PsiAnnotation annotation) {
    this.myPsiClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true);
    this.myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Override
  public List<PsiFileSystemItem> getRootDirectoryResourcePaths() {
    SmartList<PsiFileSystemItem> smartList = new SmartList<>();
    for (JamStringAttributeElement<List<PsiFileSystemItem>> element : getValueAttributeElement()) {
      List<PsiFileSystemItem> value = element.getValue();
      if (value != null) {
        ContainerUtil.addAllNotNull(smartList, value);
      }
    }
    return smartList;
  }

  public List<JamStringAttributeElement<List<PsiFileSystemItem>>> getValueAttributeElement() {
    return VALUE_ATTR_META.getJam(this.myPsiAnnotation);
  }

  @Override
  @Nullable
  public PsiClass getPsiElement() {
    return this.myPsiClass;
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InfraWebAppConfiguration that = (InfraWebAppConfiguration) o;
    return Objects.equals(this.myPsiClass, that.myPsiClass) && Objects.equals(this.myPsiAnnotation, that.myPsiAnnotation);
  }

  public int hashCode() {
    return Objects.hash(this.myPsiClass, this.myPsiAnnotation);
  }
}

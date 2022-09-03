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

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.ref.AnnotationChildLink;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.lang.Nullable;

public class CustomInfraImport extends CommonModelElement.PsiBase implements InfraImport {
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = IMPORT_JAM_ANNOTATION_KEY.subKey("CustomSpringImport");
  public static final SemKey<CustomInfraImport> JAM_KEY = IMPORT_JAM_KEY.subKey("CustomSpringImport");
  public static final SemKey<JamMemberMeta<PsiClass, CustomInfraImport>> META_KEY = IMPORT_META_KEY.subKey("CustomSpringImport");
  protected final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final AnnotationChildLink myAnnotationChildLink;
  private final PsiAnchor myPsiClassAnchor;
  private final NullableLazyValue<InfraContextImport> myDefiningMetaAnnotation;

  public CustomInfraImport(String anno, PsiClass psiClassAnchor) {
    this.myDefiningMetaAnnotation = new NullableLazyValue<>() {
      @Nullable
      public InfraContextImport compute() {
        PsiClass annotationType;
        PsiAnnotation definingMetaAnnotation = AliasForUtils.findDefiningMetaAnnotation(CustomInfraImport.this.getPsiElement(),
                CustomInfraImport.this.myAnnotationChildLink.getAnnotationQualifiedName(), AnnotationConstant.CONTEXT_IMPORT);
        if (definingMetaAnnotation != null && (annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true)) != null) {
          return InfraContextImport.META.getJamElement(annotationType);
        }
        return null;
      }
    };
    this.myAnnotationChildLink = new AnnotationChildLink(anno);
    this.myPsiClassAnchor = PsiAnchor.create(psiClassAnchor);
    this.myPsiAnnotation = this.myAnnotationChildLink.createChildRef(getPsiElement());
  }

  @Override
  public PsiClass getPsiElement() {
    return (PsiClass) this.myPsiClassAnchor.retrieve();
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  @Override
  public List<PsiClass> getImportedClasses() {
    InfraAliasFor aliasFor = getAliasAttribute("value");
    if (aliasFor != null) {
      return getAliasedPsiClasses(aliasFor);
    }
    InfraContextImport definingContextConfiguration = getDefiningContextConfiguration();
    if (definingContextConfiguration != null) {
      return definingContextConfiguration.getImportedClasses();
    }
    return Collections.emptyList();
  }

  private List<PsiClass> getAliasedPsiClasses(InfraAliasFor aliasFor) {
    SmartList smartList = new SmartList();
    JamClassAttributeMeta.Collection collection = new JamClassAttributeMeta.Collection(aliasFor.getMethodName());
    for (JamClassAttributeElement classAttributeElement : collection.getJam(this.myPsiAnnotation)) {
      ContainerUtil.addIfNotNull(smartList, classAttributeElement.getValue());
    }
    return smartList;
  }

  private InfraContextImport getDefiningContextConfiguration() {
    return this.myDefiningMetaAnnotation.getValue();
  }

  private InfraAliasFor getAliasAttribute(String attrName) {
    return AliasForUtils.findAliasFor(getPsiElement(), this.myAnnotationChildLink.getAnnotationQualifiedName(), AnnotationConstant.CONTEXT_CONFIGURATION, attrName);
  }
}

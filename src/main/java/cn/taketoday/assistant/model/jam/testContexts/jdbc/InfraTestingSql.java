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
package cn.taketoday.assistant.model.jam.testContexts.jdbc;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationAttributeMeta;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.jam.testContexts.converters.PsiFileResourcePathReferenceConverter;
import cn.taketoday.lang.Nullable;

import static com.intellij.jam.reflect.JamAttributeMeta.collectionString;
import static com.intellij.jam.reflect.JamAttributeMeta.singleAnno;

public class InfraTestingSql implements TestingSql {
  private static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = JAM_ANNOTATION_KEY.subKey("InfraTestingSql");
  public static final SemKey<InfraTestingSql> REPEATABLE_ANNO_JAM_KEY = TestingSql.JAM_KEY.subKey("InfraTestingSql");

  public static final JamClassMeta<InfraTestingSql> CLASS_META = new JamClassMeta<>(null, InfraTestingSql.class, REPEATABLE_ANNO_JAM_KEY);
  public static final JamMethodMeta<InfraTestingSql> METHOD_META = new JamMethodMeta<>(null, InfraTestingSql.class, REPEATABLE_ANNO_JAM_KEY);

  private static final JamAnnotationAttributeMeta.Single<InfraTestingSqlConfig> SQL_CONFIG_ATTR =
          singleAnno(CONFIG_ATTR_NAME, InfraTestingSqlConfig.ANNO_META, InfraTestingSqlConfig.class);

  private static final JamStringAttributeMeta.Collection<List<PsiFile>> SCRIPTS_ATTR_META =
          collectionString(SCRIPTS_ATTR_NAME, new PsiFileResourcePathReferenceConverter());

  private static final JamStringAttributeMeta.Collection<List<PsiFile>> VALUE_ATTR_META =
          collectionString(VALUE_ATTR_NAME, new PsiFileResourcePathReferenceConverter());

  private static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype()
          .addAttribute(SCRIPTS_ATTR_META)
          .addAttribute(VALUE_ATTR_META)
          .addAttribute(SQL_CONFIG_ATTR);

  public static final JamAnnotationMeta ANNO_META =
          new JamAnnotationMeta(AnnotationConstant.TEST_SQL, ARCHETYPE, JAM_ANNO_META_KEY);

  static {
    CLASS_META.addAnnotation(ANNO_META);
    METHOD_META.addAnnotation(ANNO_META);
  }

  private final PsiMember myPsiMember;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  @SuppressWarnings("unused")
  public InfraTestingSql(PsiMember psiMember) {
    myPsiMember = psiMember;
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiMember);
  }

  @SuppressWarnings("unused")
  public InfraTestingSql(PsiAnnotation annotation) {
    myPsiMember = PsiTreeUtil.getParentOfType(annotation, PsiMember.class, true);
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Nullable
  public InfraTestingSqlConfig getSqlConfig() {
    PsiAnnotation element = myPsiAnnotation.getPsiElement();
    if (element != null) {
      PsiAnnotationMemberValue config = element.findDeclaredAttributeValue(CONFIG_ATTR_NAME);
      if (config instanceof PsiAnnotation) {
        return SQL_CONFIG_ATTR.getNestedJam(PsiElementRef.real((PsiAnnotation) config));
      }
    }
    return null;
  }

  @Nullable
  public PsiMember getPsiElement() {
    return myPsiMember;
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Override
  public Set<PsiFile> getScripts() {
    Set<PsiFile> locations = new LinkedHashSet<>();
    addFiles(locations, VALUE_ATTR_META.getJam(myPsiAnnotation));
    addFiles(locations, SCRIPTS_ATTR_META.getJam(myPsiAnnotation));
    return locations;
  }

  @Override
  public List<JamStringAttributeElement<List<PsiFile>>> getScriptElements() {
    return ContainerUtil.concat(
            VALUE_ATTR_META.getJam(myPsiAnnotation),
            SCRIPTS_ATTR_META.getJam(myPsiAnnotation)
    );
  }

  private static void addFiles(Set<PsiFile> locations,
          List<JamStringAttributeElement<List<PsiFile>>> jam) {
    for (JamStringAttributeElement<List<PsiFile>> element : jam) {
      List<PsiFile> value = element.getValue();
      if (value != null) {
        ContainerUtil.addAllNotNull(locations, value);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    InfraTestingSql sql = (InfraTestingSql) o;
    return Objects.equals(myPsiMember, sql.myPsiMember) && Objects.equals(myPsiAnnotation, sql.myPsiAnnotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myPsiMember, myPsiAnnotation);
  }
}

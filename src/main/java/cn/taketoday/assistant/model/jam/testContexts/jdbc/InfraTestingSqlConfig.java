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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Objects;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.converters.InfraBeanReferenceJamConverter;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraConstant.JAVAX_SQL_DATA_SOURCE;

public class InfraTestingSqlConfig extends JamBaseElement<PsiClass> {
  public static final JamClassMeta<InfraTestingSqlConfig> META = new JamClassMeta<>(InfraTestingSqlConfig.class);
  public static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.TEST_SQL_CONFIG);

  private static final JamStringAttributeMeta.Single<BeanPointer<?>> TRANSACTION_MANAGER_ATTR_META =
          JamAttributeMeta
                  .singleString("transactionManager", new InfraBeanReferenceJamConverter(AnnotationConstant.PLATFORM_TRANSACTION_MANAGER));

  private static final JamStringAttributeMeta.Single<BeanPointer<?>> DATASOURCE_ATTR_META =
          JamAttributeMeta.singleString("dataSource", new InfraBeanReferenceJamConverter(JAVAX_SQL_DATA_SOURCE));

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(TRANSACTION_MANAGER_ATTR_META).addAttribute(DATASOURCE_ATTR_META);
  }

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  @SuppressWarnings("unused")
  public InfraTestingSqlConfig(PsiClass psiClass) {
    super(PsiElementRef.real(psiClass));
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  @SuppressWarnings("unused")
  public InfraTestingSqlConfig(PsiAnnotation annotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true))));
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  public JamStringAttributeElement<BeanPointer<?>> getTransactionManagerElement() {
    return TRANSACTION_MANAGER_ATTR_META.getJam(myPsiAnnotation);
  }

  public JamStringAttributeElement<BeanPointer<?>> getDatasourceAttrElement() {
    return DATASOURCE_ATTR_META.getJam(myPsiAnnotation);
  }
}

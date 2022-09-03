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
package cn.taketoday.assistant.model.jam.qualifiers;

import com.intellij.codeInsight.AnnotationTargetUtil;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.util.SmartList;
import com.intellij.util.xml.NameValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import cn.taketoday.assistant.model.InfraInheritableQualifier;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.QualifierAttribute;
import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.JavaeeConstant.JAKARTA_NAMED;
import static cn.taketoday.assistant.JavaeeConstant.JAVAX_NAMED;
import static cn.taketoday.assistant.PresentationConstant.QUALIFIER;

@Presentation(typeName = QUALIFIER)
public class InfraJamQualifier extends CommonModelElement.PsiBase implements JamElement, InfraInheritableQualifier {
  private final PsiAnnotation myAnno;
  protected final PsiModifierListOwner myModifierListOwner;
  private final Project myProject;

  public InfraJamQualifier(PsiAnnotation anno, @Nullable PsiModifierListOwner modifierListOwner) {
    myAnno = anno;
    myModifierListOwner = modifierListOwner;
    myProject = anno.getProject();
  }

  public PsiAnnotation getAnnotation() {
    return myAnno;
  }

  @Nullable
  @NameValue
  public String getQualifiedName() {
    return JamCommonUtil.getObjectValue(myAnno.findAttributeValue(null), String.class);
  }

  @Override

  public PsiModifierListOwner getPsiElement() {
    return myModifierListOwner == null ? getType() : myModifierListOwner;
  }

  @Nullable
  private PsiClass getType() {
    String annoQualifiedName = myAnno.getQualifiedName();

    return annoQualifiedName == null ? null : JavaPsiFacade.getInstance(myProject).findClass(annoQualifiedName, myAnno.getResolveScope());
  }

  @Override
  public PsiClass getQualifierType() {
    return getType();
  }

  @Override
  public String getQualifierValue() {
    return getQualifiedName();
  }

  @Override

  public List<? extends QualifierAttribute> getQualifierAttributes() {
    PsiNameValuePair[] attributes = myAnno.getParameterList().getAttributes();
    List<QualifierAttribute> list = new SmartList<>();
    for (PsiNameValuePair pair : attributes) {
      String name = pair.getName();
      if (name == null || "value".equals(name)) {
        continue;
      }
      list.add(new QualifierAttribute() {
        @Override
        public String getAttributeKey() {
          return name;
        }

        @Override
        public Object getAttributeValue() {
          return JamCommonUtil.getObjectValue(pair.getValue(), Object.class);
        }
      });
    }
    return list;
  }

  public static Collection<InfraQualifier> findSpringJamQualifiers(Module module, PsiModifierListOwner element) {
    Collection<InfraQualifier> jamQualifiers = new HashSet<>();
    List<PsiClass> annotationTypeClasses = JamAnnotationTypeUtil.getQualifierAnnotationTypesWithChildren(module);

    for (PsiClass annotationTypeClass : annotationTypeClasses) {
      if (isElementTypeAccepted(annotationTypeClass, element)) {
        String qname = annotationTypeClass.getQualifiedName();
        if (qname != null && !isNamedAnnotation(qname)) {
          PsiAnnotation annotation = AnnotationUtil.findAnnotation(element, true, qname);
          if (annotation != null) {
            jamQualifiers.add(new InfraJamQualifier(annotation, element));
          }
        }
      }
    }
    // @Named can be used with custom qualifier annotations and @Named is annotated with javax.inject.@Qualifier,
    // @Named should be used as qualifier if there are NO other qualifier annotations
    // @Named @MyQualifier public class Foo{}
    PsiAnnotation namedAnnotation = AnnotationUtil.findAnnotation(element, true, JAVAX_NAMED, JAKARTA_NAMED);
    if (namedAnnotation != null) {
      jamQualifiers.add(new InfraJamQualifier(namedAnnotation, element));
    }

    return jamQualifiers;
  }

  private static boolean isNamedAnnotation(String qname) {
    return qname.equals(JAVAX_NAMED) || qname.equals(JAKARTA_NAMED);
  }

  private static boolean isElementTypeAccepted(PsiClass annotationTypeClass, PsiModifierListOwner element) {
    if (element instanceof PsiClass) {
      return AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.TYPE) != null;
    }
    if (element instanceof PsiMethod) {
      return AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.METHOD) != null;
    }
    if (element instanceof PsiField) {
      return AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.FIELD) != null;
    }
    if (element instanceof PsiParameter) {
      return AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.PARAMETER) != null;
    }

    return false;
  }
}

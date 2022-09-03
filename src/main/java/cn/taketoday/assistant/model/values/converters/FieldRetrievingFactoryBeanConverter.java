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
package cn.taketoday.assistant.model.values.converters;

import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public abstract class FieldRetrievingFactoryBeanConverter extends Converter<String> implements CustomReferenceConverter<String> {

  protected final boolean mySoft;

  protected FieldRetrievingFactoryBeanConverter() {
    this(true);
  }

  protected FieldRetrievingFactoryBeanConverter(boolean soft) {
    mySoft = soft;
  }

  protected boolean requireFieldReference() {
    return false;
  }

  @Override
  public PsiReference[] createReferences(GenericDomValue<String> genericDomValue,
          PsiElement element,
          ConvertContext context) {
    return createReferences(genericDomValue, element);
  }

  @Override
  public String fromString(@Nullable String s, ConvertContext context) {
    return s;
  }

  @Override
  public String toString(@Nullable String s, ConvertContext context) {
    return s;
  }

  public PsiReference[] createReferences(GenericDomValue<String> genericDomValue, PsiElement element) {
    String stringValue = genericDomValue.getStringValue();
    if (stringValue == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    List<PsiReference> collectedReferences = new ArrayList<>();

    JavaClassReferenceProvider provider = new JavaClassReferenceProvider();
    provider.setSoft(mySoft);
    provider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.TRUE);
    PsiReference[] javaClassReferences = provider.getReferencesByElement(element);

    PsiClass psiClass = null;
    for (PsiReference reference : javaClassReferences) {
      PsiElement psiElement = reference.resolve();
      if (psiElement == null)
        break;

      collectedReferences.add(reference);
      if (psiElement instanceof PsiClass) {
        psiClass = (PsiClass) psiElement;
      }
    }

    if (psiClass == null ||
            !requireFieldReference() && psiClass.getQualifiedName() != null && stringValue.endsWith(psiClass.getQualifiedName())) {
      return javaClassReferences;
    }

    collectedReferences.add(createFieldReference(psiClass, element, stringValue, genericDomValue));

    return collectedReferences.toArray(PsiReference.EMPTY_ARRAY);
  }

  private PsiReference createFieldReference(PsiClass psiClass,
          PsiElement element,
          String stringValue,
          GenericDomValue<String> genericDomValue) {
    String className = psiClass.getName();
    assert className != null;
    int fieldNameIdx = stringValue.lastIndexOf(className) + className.length();
    String fieldName = stringValue.substring(Math.min(stringValue.length(), fieldNameIdx + 1)).trim();

    TextRange textRange;
    if (fieldName.isEmpty()) {
      textRange = TextRange.from(element.getText().indexOf(className) + className.length() + 1, 0);
    }
    else {
      textRange = TextRange.from(Math.max(0, element.getText().lastIndexOf(fieldName)), fieldName.length());
    }

    return new FieldReference(element, textRange, fieldName, psiClass, genericDomValue);
  }

  protected class FieldReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {

    private final String myFieldName;
    private final PsiClass myPsiClass;
    private final GenericDomValue<String> myGenericDomValue;

    protected FieldReference(PsiElement element,
            TextRange textRange,
            String fieldName,
            PsiClass psiClass,
            GenericDomValue<String> genericDomValue) {
      super(element, textRange, FieldRetrievingFactoryBeanConverter.this.mySoft);
      myFieldName = fieldName;
      myPsiClass = psiClass;
      myGenericDomValue = genericDomValue;
    }

    @Override
    public PsiElement resolve() {
      if (myFieldName.length() != 0) {
        PsiField[] psiFields = myPsiClass.getAllFields();
        for (PsiField psiField : psiFields) {
          if (psiField.hasModifierProperty(PsiModifier.PUBLIC) &&
                  psiField.hasModifierProperty(PsiModifier.STATIC) &&
                  myFieldName.equals(psiField.getName())) {
            return psiField;
          }
        }
      }
      return null;
    }

    @Override
    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
      if (element instanceof final PsiField field) {
        PsiClass containingClass = field.getContainingClass();
        if (containingClass != null) {
          myGenericDomValue.setStringValue(containingClass.getQualifiedName() + "." + field.getName());
        }
      }
      return getElement();
    }

    @Override
    public Object[] getVariants() {
      List<LookupElement> staticFields = new ArrayList<>();
      PsiField[] psiFields = myPsiClass.getFields();
      for (PsiField psiField : psiFields) {
        if (psiField.hasModifierProperty(PsiModifier.PUBLIC) && psiField.hasModifierProperty(PsiModifier.STATIC)) {
          staticFields.add(JavaLookupElementBuilder.forField(psiField, psiField.getName(), myPsiClass)
                  .withTypeText(psiField.getType().getPresentableText()));
        }
      }
      return ArrayUtil.toObjectArray(staticFields);
    }

    @Override
    public String getUnresolvedMessagePattern() {
      String fieldName = getValue();
      if (fieldName.isEmpty() || ".".equals(fieldName)) {
        return InfraBundle.message("FieldRetrievingFactoryBeanConverter.field.name.expected");
      }
      return InfraBundle.message("FieldRetrievingFactoryBeanConverter.cannot.resolve.field", fieldName);
    }
  }

  /**
   * Highlights missing field reference.
   */
  public static class FieldReferenceRequired extends FieldRetrievingFactoryBeanConverter {

    @Override
    protected boolean requireFieldReference() {
      return true;
    }
  }
}

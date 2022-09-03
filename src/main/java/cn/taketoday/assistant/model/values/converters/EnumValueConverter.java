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

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.WrappingConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import cn.taketoday.assistant.model.values.PropertyValueConverter;
import cn.taketoday.lang.Nullable;

public class EnumValueConverter extends Converter<PsiField> implements CustomReferenceConverter {

  public static PsiReference[] createReferences(PsiType type, GenericDomValue genericDomValue, PsiElement element) {
    PsiClass psiClass;
    String stringValue = genericDomValue.getStringValue();
    if (!(type instanceof PsiClassType) || (psiClass = ((PsiClassType) type).resolve()) == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    return new PsiReference[] { createReference(psiClass, element, stringValue) };
  }

  private static PsiReference createReference(final PsiClass psiClass, final PsiElement element, final String stringValue) {
    return new PsiReferenceBase<>(element, true) {

      public PsiElement resolve() {
        PsiField psiField = psiClass.findFieldByName(stringValue, false);
        if (psiField == null && !psiClass.isEnum()) {
          return element;
        }
        return psiField;
      }

      public Object[] getVariants() {
        return EnumValueConverter.getFields(psiClass);
      }
    };
  }

  public PsiField fromString(@Nullable String s, ConvertContext context) {
    return null;
  }

  public String toString(@Nullable PsiField s, ConvertContext context) {
    return null;
  }

  public PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context) {
    Converter converter = genericDomValue.getConverter();
    while (true) {
      Converter propertyValueConverter = converter;
      if (propertyValueConverter instanceof WrappingConverter wrappingConverter) {
        if (propertyValueConverter instanceof PropertyValueConverter propertyValueConverter1) {
          List<PsiType> types = propertyValueConverter1.getValueTypes(genericDomValue);
          for (PsiType type : types) {
            PsiReference[] psiReferences = createReferences(type, genericDomValue, element);
            if (psiReferences.length > 0) {
              return psiReferences;
            }
          }
          continue;
        }
        converter = wrappingConverter.getConverter(genericDomValue);
      }
      else {
        return PsiReference.EMPTY_ARRAY;
      }
    }
  }

  public static class TypeCondition implements Condition<Pair<PsiType, GenericDomValue>> {
    private final List<String> EXCLUDE_CLASSES = Arrays.asList(Boolean.class.getName(), Locale.class.getName());

    public boolean value(Pair<PsiType, GenericDomValue> pair) {
      PsiClass psiClass;
      PsiType psiClassType = pair.getFirst();
      if ((psiClassType instanceof PsiClassType psiClassType1)
              && !this.EXCLUDE_CLASSES.contains(psiClassType.getCanonicalText()) && (psiClass = psiClassType1.resolve()) != null) {
        if (psiClass.isEnum()) {
          return true;
        }
        for (PsiField psiField : psiClass.getFields()) {
          if (psiField.hasModifierProperty("static") && psiField.hasModifierProperty("public") && psiField.getType().equals(psiClassType)) {
            return true;
          }
        }
        return false;
      }
      return false;
    }
  }

  private static PsiField[] getFields(PsiClass psiClass) {
    PsiClass typeClass;
    ArrayList<PsiField> fields = new ArrayList<>();
    PsiField[] psiFields = psiClass.getFields();
    for (PsiField psiField : psiFields) {
      if (psiField.hasModifierProperty("static") && psiField.hasModifierProperty("public")) {
        PsiType type = psiField.getType();
        if ((type instanceof PsiClassType psiClassType) && (typeClass = psiClassType.resolve()) != null && typeClass.equals(psiClass)) {
          fields.add(psiField);
        }
      }
    }
    return fields.toArray(PsiField.EMPTY_ARRAY);
  }
}

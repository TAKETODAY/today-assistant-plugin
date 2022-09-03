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
package cn.taketoday.assistant.model.converters;

import com.intellij.util.xml.Converter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.PsiClassConverter;
import com.intellij.util.xml.WrappingConverter;

import cn.taketoday.assistant.model.xml.context.Filter;
import cn.taketoday.assistant.model.xml.context.Type;

public class InfraContextFilterExpressionConverter extends WrappingConverter {
  private static final PsiClassConverter myClassConverter = new PsiClassConverter();
  private static final PsiClassConverter myAnnotationTypeClassConverter = new PsiClassConverter.AnnotationType();

  @Override
  public Converter getConverter(GenericDomValue domElement) {
    Filter filter = domElement.getParentOfType(Filter.class, true);
    if (filter != null) {
      Type type = filter.getType().getValue();

      if (type != null) {
        if (type == Type.ANNOTATION) {
          return myAnnotationTypeClassConverter;
        }
        else if (type == Type.ASSIGNABLE) {
          return myClassConverter;
        }
      }
    }
    return null;
  }
}

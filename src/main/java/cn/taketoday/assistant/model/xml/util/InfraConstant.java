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

// Generated on Thu Nov 09 17:15:14 MSK 2006
// DTD/Schema  :    http://www.springframework.org/schema/util

package cn.taketoday.assistant.model.xml.util;

import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Referencing;
import com.intellij.util.xml.Required;

import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.values.converters.FieldRetrievingFactoryBeanConverter;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.BeanTypeProvider;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.PresentationConstant.INFRA_CONSTANT;

@Presentation(typeName = INFRA_CONSTANT)
@BeanType(provider = InfraConstant.InfraConstantBeanTypeProvider.class)
public interface InfraConstant extends InfraUtilElement, DomInfraBean {

  class InfraConstantBeanTypeProvider implements BeanTypeProvider<InfraConstant> {

    @Override
    public String[] getBeanTypeCandidates() {
      return ArrayUtilRt.EMPTY_STRING_ARRAY;
    }

    @Nullable
    @Override
    public String getBeanType(InfraConstant constant) {
      final PsiClass psiClass = getStaticFieldType(constant);
      return psiClass != null ? StringUtil.notNullize(psiClass.getQualifiedName()) : "";
    }

    @Nullable
    private static PsiClass getStaticFieldType(InfraConstant constant) {
      final String staticField = constant.getStaticField().getRawText();
      if (StringUtil.isEmptyOrSpaces(staticField) || PlaceholderUtils.getInstance().isDefaultPlaceholder(staticField))
        return null;
      int lastPoint = staticField.indexOf('$');
      if (lastPoint == -1)
        lastPoint = staticField.indexOf('.');

      if (lastPoint >= 0) {
        String className = staticField.substring(0, lastPoint);
        if (StringUtil.isEmptyOrSpaces(className))
          return null;
        String fieldName = staticField.substring(lastPoint + 1);
        if (StringUtil.isEmptyOrSpaces(fieldName))
          return null;

        Project project = constant.getPsiManager().getProject();
        PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
        if (aClass != null) {
          PsiField fieldByName = aClass.findFieldByName(fieldName, true);
          if (fieldByName != null) {
            PsiType type = fieldByName.getType();
            if (type instanceof PsiClassType)
              return ((PsiClassType) type).resolve();
          }
          else {
            for (PsiMethod method : aClass.findMethodsByName(fieldName, true)) {
              PsiType returnType = method.getReturnType();
              if (returnType instanceof PsiClassType)
                return ((PsiClassType) returnType).resolve();
            }
          }
        }
      }
      return null;
    }
  }

  /**
   * Returns the value of the static-field child.
   *
   * @return the value of the static-field child.
   */

  @Required
  @Referencing(value = FieldRetrievingFactoryBeanConverter.FieldReferenceRequired.class)
  GenericAttributeValue<String> getStaticField();
}

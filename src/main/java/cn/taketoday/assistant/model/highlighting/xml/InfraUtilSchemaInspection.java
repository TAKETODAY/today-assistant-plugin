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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.utils.PsiTypeUtil;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.util.UtilList;
import cn.taketoday.assistant.model.xml.util.UtilMap;
import cn.taketoday.assistant.model.xml.util.UtilSet;

public final class InfraUtilSchemaInspection extends InfraInjectionValueTypeInspection {
  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    if (!InfraDomUtils.hasNamespace(domFileElement, InfraConstant.UTIL_NAMESPACE_KEY)) {
      return;
    }
    Beans beans = domFileElement.getRootElement();
    for (UtilSet springSet : DomUtil.getDefinedChildrenOfType(beans, UtilSet.class, true, false)) {
      checkSetBean(springSet, holder);
      checkElementsHolder(springSet, holder);
    }
    for (UtilList list : DomUtil.getDefinedChildrenOfType(beans, UtilList.class, true, false)) {
      checkListBean(list, holder);
      checkElementsHolder(list, holder);
    }
    for (UtilMap map : DomUtil.getDefinedChildrenOfType(beans, UtilMap.class, true, false)) {
      checkMapBean(map, holder);
    }
  }

  private void checkElementsHolder(ListOrSet springSet, DomElementAnnotationHolder holder) {
    checkSpringPropertyCollection(springSet, holder);
  }

  private static void checkMapBean(UtilMap map, DomElementAnnotationHolder holder) {
    checkProperClass(map.getMapClass(), Map.class, holder);
  }

  private static void checkListBean(UtilList list, DomElementAnnotationHolder holder) {
    checkProperClass(list.getListClass(), List.class, holder);
  }

  private static void checkSetBean(UtilSet set, DomElementAnnotationHolder holder) {
    checkProperClass(set.getSetClass(), Set.class, holder);
  }

  private static void checkProperClass(GenericAttributeValue<PsiClass> attrClass, Class<?> aClass, DomElementAnnotationHolder holder) {
    PsiClass psiClass;
    if (DomUtil.hasXml(attrClass) && (psiClass = attrClass.getValue()) != null && !isAssignable(psiClass, aClass)) {
      holder.createProblem(attrClass, InfraBundle.message("util.required.class.message", aClass.getName()));
    }
  }

  private static boolean isAssignable(PsiClass psiClass, Class<?> fromClass) {
    Project project = psiClass.getProject();
    PsiType fromType = PsiTypeUtil.getInstance(project).findType(fromClass);
    PsiClassType classType = PsiTypesUtil.getClassType(psiClass);
    return fromType != null && fromType.isAssignableFrom(classType);
  }
}

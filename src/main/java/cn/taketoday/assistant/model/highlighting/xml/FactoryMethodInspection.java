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

import com.intellij.psi.PsiMethod;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public final class FactoryMethodInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean springBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel model) {
    PsiMethod factoryMethod;
    GenericAttributeValue<PsiMethod> factoryMethodAttribute = springBean.getFactoryMethod();
    if (DomUtil.hasXml(factoryMethodAttribute) && (factoryMethod = factoryMethodAttribute.getValue()) != null) {
      boolean isStatic = factoryMethod.hasModifierProperty("static");
      BeanPointer<?> factoryBean = springBean.getFactoryBean().getValue();
      if (!isStatic && factoryBean == null) {
        holder.createProblem(factoryMethodAttribute, InfraBundle.message("method.must.be.static", factoryMethod.getName()));
      }
      else if (isStatic && factoryBean != null) {
        holder.createProblem(factoryMethodAttribute, InfraBundle.message("method.must.not.be.static", factoryMethod.getName()));
      }
      if (!FactoryBeansManager.hasFactoryReturnType(factoryMethod)) {
        holder.createProblem(factoryMethodAttribute, InfraBundle.message("method.cannot.instantiate.bean", factoryMethod.getName()));
      }
    }
  }
}

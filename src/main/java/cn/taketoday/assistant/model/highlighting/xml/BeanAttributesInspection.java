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

import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.highlighting.dom.InfraDomInspectionUtils;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.lang.Nullable;

public final class BeanAttributesInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel infraModel) {
    InfraDomInspectionUtils utils = new InfraDomInspectionUtils(holder);
    if (!utils.onlyOneOf(infraBean, infraBean.getClazz(), infraBean.getFactoryBean())) {
      utils.ifExistsOtherRequired(infraBean, infraBean.getFactoryBean(), infraBean.getFactoryMethod());
    }
    utils.attributeWithDefaultSuperfluous(infraBean.getAbstract(), Boolean.FALSE);
    utils.attributeWithDefaultSuperfluous(infraBean.getSingleton(), Boolean.TRUE);
    utils.attributeWithDefaultSuperfluous(infraBean.getScope(), BeanScope.SINGLETON_SCOPE);
    if (infraBean.getParent() instanceof InfraValueHolder) {
      utils.attributeSuperfluous(infraBean.getId());
      utils.attributeSuperfluous(infraBean.getName());
      utils.attributeSuperfluous(infraBean.getScope());
    }
  }
}

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

package cn.taketoday.assistant.model.xml;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.util.xml.DomReflectionUtil;
import com.intellij.util.xml.DomUtil;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.pom.InfraBeanPomTargetUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;

public class DomInfraBeanRenameVetoCondition implements Condition<PsiElement> {
  public boolean value(PsiElement element) {
    CommonInfraBean bean = InfraBeanPomTargetUtils.getBean(element);
    if (!(bean instanceof DomInfraBean)) {
      return false;
    }
    if (bean instanceof InfraBean) {
      return !DomUtil.hasXml(((DomInfraBean) bean).getId()) && !DomUtil.hasXml(((InfraBean) bean).getName());
    }
    BeanName beanName = DomReflectionUtil.findAnnotationDFS(bean.getClass(), BeanName.class);
    return beanName != null && beanName.displayOnly();
  }
}

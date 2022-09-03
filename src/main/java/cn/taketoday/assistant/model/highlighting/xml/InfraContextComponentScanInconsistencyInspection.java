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

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.List;

import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.context.BeansPackagesScanBean;
import cn.taketoday.assistant.model.xml.context.Filter;
import cn.taketoday.assistant.model.xml.context.Type;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InfraContextComponentScanInconsistencyInspection extends InfraBeanInspectionBase {

  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    List<BeansPackagesScanBean> packagesScanBeans = DomUtil.getDefinedChildrenOfType(domFileElement.getRootElement(), BeansPackagesScanBean.class, true, false);
    for (BeansPackagesScanBean componentScan : packagesScanBeans) {
      checkComponentScan(componentScan, holder);
    }
  }

  private static void checkComponentScan(BeansPackagesScanBean componentScan, DomElementAnnotationHolder holder) {
    for (Filter filter : componentScan.getExcludeFilters()) {
      checkFilterType(filter, holder);
    }
    for (Filter filter2 : componentScan.getIncludeFilters()) {
      checkFilterType(filter2, holder);
    }
  }

  private static void checkFilterType(Filter filter, DomElementAnnotationHolder holder) {
    Type type = filter.getType().getValue();
    if (type == Type.ANNOTATION) {
      Object value = filter.getExpression().getValue();
      if ((value instanceof PsiClass psiClass) && !psiClass.isAnnotationType()) {
        holder.createProblem(filter.getExpression(), message("ContextComponentScanInconsistencyInspection.annotation.is.expected.here"));
      }
    }
  }
}

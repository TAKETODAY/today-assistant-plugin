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
package cn.taketoday.assistant.model.highlighting.dom;

import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsInspection;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraModelVisitor;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class InfraBeanInspectionBase extends DomElementsInspection<Beans> {

  public InfraBeanInspectionBase() {
    super(Beans.class);
  }

  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    XmlFile xmlFile = domFileElement.getFile();
    Beans beans = domFileElement.getRootElement();
    CommonInfraModel model = InfraManager.from(xmlFile.getProject()).getInfraModelByFile(xmlFile);
    InfraModelVisitor visitor = createVisitor(holder, beans, model);
    InfraModelVisitor.visitBeans(visitor, beans);
  }

  protected InfraModelVisitor createVisitor(DomElementAnnotationHolder holder, Beans beans, @Nullable CommonInfraModel model) {
    return new InfraModelVisitor() {

      @Override
      protected boolean visitBean(CommonInfraBean bean) {
        if (bean instanceof InfraBean) {
          checkBean((InfraBean) bean, beans, holder, model);
        }
        return true;
      }
    };
  }

  protected void checkBean(InfraBean springBean,
          Beans beans,
          DomElementAnnotationHolder holder,
          @Nullable CommonInfraModel springModel) {
  }
}

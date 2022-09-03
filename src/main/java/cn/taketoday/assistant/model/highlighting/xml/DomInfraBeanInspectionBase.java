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

import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsInspection;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;

public abstract class DomInfraBeanInspectionBase extends DomElementsInspection<Beans> {
  public DomInfraBeanInspectionBase() {
    super(Beans.class);
  }

  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    PsiFile file = domFileElement.getFile();
    Beans beans = domFileElement.getRootElement();
    InfraModel model = InfraManager.from(file.getProject()).getInfraModelByFile(file);
    Consumer<DomElement> consumer = new Consumer<>() {
      public void consume(DomElement element) {
        if (element instanceof DomInfraBean) {
          checkBean((DomInfraBean) element, beans, holder, model);
        }
        else if (!(element instanceof GenericDomValue) && DomUtil.hasXml(element)) {
          checkChildren(element, this);
        }
      }
    };
    consumer.consume(domFileElement.getRootElement());
  }

  protected void checkBean(DomInfraBean infraBean, Beans beans,
          DomElementAnnotationHolder holder, InfraModel infraModel) {
  }
}

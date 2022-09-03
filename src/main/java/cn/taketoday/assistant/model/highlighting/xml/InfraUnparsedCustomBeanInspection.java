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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraModelVisitor;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.custom.ParseCustomBeanIntention;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InfraUnparsedCustomBeanInspection extends InfraBeanInspectionBase {

  @Override
  protected InfraModelVisitor createVisitor(DomElementAnnotationHolder holder, Beans beans, @Nullable CommonInfraModel model) {
    return new InfraModelVisitor() {
      @Override
      public boolean visitBean(CommonInfraBean bean) {
        if (bean instanceof CustomBeanWrapper wrapper) {
          if (wrapper.isParsed()) {
            return true;
          }
          if (ParseCustomBeanIntention.isUnregisteredNamespace(wrapper)) {
            holder.createProblem(wrapper, message("unparsed.custom.bean.message"));
            return true;
          }
          AnAction reportProblemAction = ActionManager.getInstance().getAction("ReportProblem");
          String reportProblemActionText = reportProblemAction.getTemplatePresentation().getText();
          holder.createProblem(wrapper, message("UnparsedCustomBeanInspection.unsupported.tag", wrapper.getXmlElementName(), wrapper.getXmlElementNamespace(), reportProblemActionText)
          );
          return true;
        }
        return true;
      }
    };
  }
}

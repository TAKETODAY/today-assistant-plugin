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

package cn.taketoday.assistant.model.actions;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.actions.generate.InfraBeanGenerateProvider;
import cn.taketoday.assistant.model.actions.generate.InfraConstructorDependenciesGenerateProvider;
import cn.taketoday.assistant.model.actions.generate.InfraPropertiesGenerateProvider;
import cn.taketoday.assistant.model.actions.generate.InfraSetterDependenciesGenerateProvider;

import static cn.taketoday.assistant.InfraBundle.message;

public class GenerateDomElementActionGroup extends DefaultActionGroup {

  public GenerateDomElementActionGroup() {
    add(createGenerateBeanAction());
    add(new GenerateDomElementAction(new InfraBeanGenerateProvider(message("bean.instantiation.by.factory"), "spring-bean-with-factory-bean"), Icons.SpringBean));
    add(new GenerateDomElementAction(new InfraBeanGenerateProvider(message("bean.instantiation.using.factory.method"), "spring-bean-with-factory-method"), Icons.SpringBean));
    add(Separator.getInstance());
    add(new GenerateBeanBodyAction(new InfraPropertiesGenerateProvider()) {
      protected boolean startInWriteAction() {
        return false;
      }
    });
    add(new GenerateBeanBodyAction(new InfraSetterDependenciesGenerateProvider(), Icons.SpringBean) {
      protected boolean startInWriteAction() {
        return false;
      }
    });
    add(new GenerateBeanBodyAction(new InfraConstructorDependenciesGenerateProvider(), Icons.SpringBean) {
      protected boolean startInWriteAction() {
        return false;
      }
    });
    add(Separator.getInstance());
  }

  public static GenerateDomElementAction createGenerateBeanAction() {
    return new GenerateDomElementAction(new InfraBeanGenerateProvider(message("infra.bean"), "spring-bean"), Icons.SpringBean);
  }
}

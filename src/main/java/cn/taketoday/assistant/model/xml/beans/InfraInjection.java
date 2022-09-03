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

package cn.taketoday.assistant.model.xml.beans;

import com.intellij.util.xml.SubTag;

import cn.taketoday.assistant.model.xml.util.InfraConstant;

/**
 * @author Dmitry Avdeev
 */
public interface InfraInjection extends InfraValueHolder, InfraValueHolderDefinition, Description {

  /**
   * Returns the value of the meta child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:meta documentation</h3>
   * 	Arbitrary metadata attached to a bean definition.
   * <p/>
   * </pre>
   *
   * @return the value of the meta child.
   */

  Meta getMeta();

  @SubTag("constant")
  InfraConstant getConstant();
}

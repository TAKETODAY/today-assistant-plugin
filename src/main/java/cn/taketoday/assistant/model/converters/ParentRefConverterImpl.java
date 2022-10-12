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

package cn.taketoday.assistant.model.converters;

import com.intellij.util.xml.ConvertContext;

import java.util.Collection;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.XmlInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

public class ParentRefConverterImpl extends ParentRefConverter {

  @Override
  public BeanPointer<?> fromString(@Nullable String s, ConvertContext context) {
    if (s == null) {
      return null;
    }
    CommonInfraModel model = getInfraModel(context);
    if (!(model instanceof XmlInfraModel)) {
      return null;
    }
    for (CommonInfraModel commonModel : ((XmlInfraModel) model).getDependencies()) {
      BeanPointer<?> bean = InfraModelSearchers.findBean(commonModel, s);
      if (bean != null) {
        return bean;
      }
    }
    return null;
  }

  @Override
  public Collection<BeanPointer<?>> getVariants(ConvertContext context) {
    return getVariants(context, true, false, getRequiredClasses(context), getInfraModel(context));
  }
}

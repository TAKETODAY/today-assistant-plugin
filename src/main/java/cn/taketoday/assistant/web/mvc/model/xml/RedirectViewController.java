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

package cn.taketoday.assistant.web.mvc.model.xml;

import com.intellij.ide.presentation.Presentation;
import com.intellij.javaee.web.model.xml.HttpStatusCode;
import com.intellij.javaee.web.model.xml.converters.HttpStatusCodeConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;

import cn.taketoday.assistant.web.mvc.presentation.WebMvcPresentationConstant;

@Presentation(typeName = WebMvcPresentationConstant.REDIRECT_VIEW_CONTROLLER)
public interface RedirectViewController extends ViewControllerBase {
  @Required
  GenericAttributeValue<String> getRedirectUrl();

  @Convert(RedirectOnlyHttpStatusCodeConverter.class)
  GenericAttributeValue<HttpStatusCode> getStatusCode();

  GenericAttributeValue<Boolean> getContextRelative();

  GenericAttributeValue<Boolean> getKeepQueryParams();

  class RedirectOnlyHttpStatusCodeConverter extends HttpStatusCodeConverter {
    public RedirectOnlyHttpStatusCodeConverter() {
      super(HttpStatusCode.Series.REDIRECTION.allStatusCodes(), false);
    }
  }
}
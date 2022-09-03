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

package cn.taketoday.assistant.web.mvc.config.anno;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.javaee.web.CommonParamValue;
import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.CommonServletMapping;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.MutableGenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;

import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/29 15:35
 */
public class PsiBasedServlet extends CommonModelElement.PsiBase implements CommonServlet, CommonServletMapping<CommonServlet> {
  private final String myServletName;
  private final PsiClass myServletClass;
  private final PsiElement myServletDefinitionElement;
  private final PsiElement myMappingDefinitionElement;
  private final List<GenericValue<String>> myUrlPatterns;

  public PsiBasedServlet(String servletName, PsiClass servletClass, PsiElement servletDefinitionElement, PsiElement mappingDefinitionElement,
          String... mappings) {

    this.myServletName = servletName;
    this.myServletClass = servletClass;
    this.myServletDefinitionElement = servletDefinitionElement;
    this.myMappingDefinitionElement = mappingDefinitionElement;
    this.myUrlPatterns = ContainerUtil.map(mappings, ReadOnlyGenericValue::getInstance);
  }

  public PsiElement getPsiElement() {
    return this.myServletDefinitionElement;
  }

  @Nullable
  public PsiBasedServlet getServlet() {
    return this;
  }

  @Nullable
  public PsiElement getMappingElement() {
    return this.myMappingDefinitionElement;
  }

  @Nullable
  public PsiClass getPsiClass() {
    return this.myServletClass;
  }

  public MutableGenericValue<String> getServletName() {
    return new MutableGenericValue<String>() {
      public void setStringValue(String value) {
        throw new UnsupportedOperationException();
      }

      public void setValue(String value) {
        throw new UnsupportedOperationException();
      }

      @Nullable
      public String getStringValue() {
        return PsiBasedServlet.this.myServletName;
      }

      @Nullable
      public String getValue() {
        return this.getStringValue();
      }
    };
  }

  public List<? extends GenericValue<String>> getUrlPatterns() {
    return this.myUrlPatterns;
  }

  public List<? extends CommonParamValue> getInitParams() {
    return Collections.emptyList();
  }

  public CommonParamValue addInitParam() {
    throw new UnsupportedOperationException();
  }
}

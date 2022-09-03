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

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.xml.CanonicalPsiTypeConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.PsiClassConverter;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;
import com.intellij.util.xml.impl.ExtendsClassChecker;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;

public final class InfraXmlModelInspection extends BasicDomElementsInspection<Beans> {

  public InfraXmlModelInspection() {
    super(Beans.class);
  }

  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    int oldSize = holder.getSize();
    super.checkDomElement(element, holder, helper);
    if (oldSize != holder.getSize() || !(element instanceof GenericDomValue)) {
      return;
    }
    if (element.getAnnotation(Convert.class) != null) {
      Converter converter = ((GenericDomValue) element).getConverter();
      if ((converter instanceof PsiClassConverter) || (converter instanceof CanonicalPsiTypeConverter)) {
        return;
      }
    }
    ExtendsClassChecker.checkExtendsClassInReferences((GenericDomValue) element, holder);
  }

  private static boolean hasInjections(GenericDomValue value) {
    if (StringUtil.isEmptyOrSpaces(value.getRawText())) {
      return false;
    }
    XmlElement element = value.getXmlElement();
    if (element instanceof XmlAttribute xmlAttribute) {
      element = xmlAttribute.getValueElement();
    }
    return element != null && InjectedLanguageManager.getInstance(element.getProject()).getInjectedPsiFiles(element) != null;
  }

  protected boolean shouldCheckResolveProblems(GenericDomValue value) {
    return !BeanScope.class.equals(DomUtil.getGenericValueParameter(value.getDomElementType())) && super.shouldCheckResolveProblems(value) && isInActiveProfile(
            value) && !PlaceholderUtils.getInstance().isPlaceholder(value) && !hasInjections(value);
  }

  private static boolean isInActiveProfile(DomElement value) {
    Beans beans = DomUtil.getParentOfType(value, Beans.class, true);
    if (beans != null) {
      CommonInfraModel model = InfraModelService.of().getModel(value.getXmlElement());
      return ProfileUtils.isActiveProfile(beans, model.getActiveProfiles());
    }
    return true;
  }
}

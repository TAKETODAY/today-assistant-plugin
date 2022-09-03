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
package cn.taketoday.assistant.model.values;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.WrappingConverter;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.xml.beans.TypeHolder;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;

/**
 * @author Yann C&eacute;bron
 */
public abstract class PropertyValueConverter extends WrappingConverter {

  @Override
  public Converter getConverter(final GenericDomValue domElement) {
    final List<Converter> converters = getConverters(domElement);
    return converters.isEmpty() ? null : converters.get(0);
  }

  @Override

  public List<Converter> getConverters(final GenericDomValue element) {
    XmlElement xmlElement = element.getXmlElement();
    if (xmlElement instanceof XmlAttribute) {
      PsiLanguageInjectionHost host = (PsiLanguageInjectionHost) ((XmlAttribute) xmlElement).getValueElement();
      if (host == null || InjectedLanguageManager.getInstance(host.getProject()).getInjectedPsiFiles(host) != null) {
        return Collections.emptyList();
      }
    }
    final GenericDomValueConvertersRegistry registry = InfraValueConvertersRegistry.of();

    final List<PsiType> types = getValueTypes(element);
    if (types.isEmpty()) {
      final Converter converter = registry.getConverter(element, null);
      return ContainerUtil.createMaybeSingletonList(converter);
    }

    final List<Converter> converters = new SmartList<>();
    for (PsiType type : types) {
      final Converter converter = registry.getConverter(element, type instanceof PsiEllipsisType ? ((PsiEllipsisType) type).getComponentType() : type);
      if (converter != null) {
        converters.add(converter);
      }
      else {
        return Collections.emptyList();
      }
    }
    return converters;
  }

  public List<PsiType> getValueTypes(final GenericDomValue element) {
    if (element instanceof TypeHolder) {
      final List<PsiType> psiTypes = TypeHolderUtil.getRequiredTypes(((TypeHolder) element));
      if (!psiTypes.isEmpty()) {
        return psiTypes;
      }
    }

    final DomElement parent = element.getParent();
    return parent instanceof TypeHolder ? TypeHolderUtil.getRequiredTypes(((TypeHolder) parent)) : Collections.emptyList();
  }
}

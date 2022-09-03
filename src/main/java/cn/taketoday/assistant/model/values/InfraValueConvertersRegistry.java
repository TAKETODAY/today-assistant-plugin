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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;

import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public abstract class InfraValueConvertersRegistry extends GenericDomValueConvertersRegistry {

  public static GenericDomValueConvertersRegistry of() {
    return ApplicationManager.getApplication().getService(InfraValueConvertersRegistry.class);
  }

  @SuppressWarnings("AbstractMethodCallInConstructor")
  protected InfraValueConvertersRegistry() {
    registerBuiltinValueConverters();
  }

  @Override
  @Nullable
  protected Converter<?> getCustomConverter(Pair<PsiType, GenericDomValue> pair) {
    for (InfraValueConvertersProvider provider : InfraValueConvertersProvider.EP.getExtensionList()) {
      Condition<Pair<PsiType, GenericDomValue>> condition = provider.getCondition();
      if (condition.value(pair)) {
        return provider.getConverter();
      }
    }
    return null;
  }

  protected abstract void registerBuiltinValueConverters();
}

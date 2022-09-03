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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.xml.beans.InfraEntry;

/**
 * @author Dmitry Avdeev
 */
public class EntryKeyConverter extends PropertyValueConverter {

  @Override

  public List<PsiType> getValueTypes(GenericDomValue domValue) {
    InfraEntry entry = (InfraEntry) domValue.getParent();
    assert entry != null;
    PsiClass psiClass = entry.getRequiredKeyClass();
    if (psiClass == null) {
      return Collections.emptyList();
    }

    PsiClassType psiClassType = PsiTypesUtil.getClassType(psiClass);
    return Collections.singletonList(psiClassType);
  }
}

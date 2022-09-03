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

package cn.taketoday.assistant.model.converters.fixes.bean;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiClassType;
import com.intellij.util.xml.ConvertContext;

import java.util.List;

import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

interface BeanResolveQuickFixProvider {

  List<LocalQuickFix> getQuickFixes(ConvertContext convertContext,
          Beans beans, @Nullable String str, List<PsiClassType> list);
}

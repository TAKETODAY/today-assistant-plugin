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

package cn.taketoday.assistant.beans;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.spring.model.jam.testContexts.SpringTestingAnnotationsProvider;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:08
 */
public class TestingAnnotationsProvider implements SpringTestingAnnotationsProvider {

  @Override
  public Collection<PsiClass> getTestingAnnotations(Module module) {
    LinkedHashSet<PsiClass> ret = new LinkedHashSet<>();
    ret.addAll(MetaAnnotationUtil.getAnnotationTypesWithChildren(module, "cn.taketoday.test.context.BootstrapWith", true));
    ret.addAll(MetaAnnotationUtil.getAnnotationTypesWithChildren(module, "cn.taketoday.test.context.ContextConfiguration", true));
    return ret;
  }

}

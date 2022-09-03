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
package cn.taketoday.assistant.model.jam.javaConfig;

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.util.ArrayUtilRt;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;

@Deprecated
public class JavaConfigJavaBean extends InfraJavaBean {
  private static final JamStringAttributeMeta.Collection<String> ALIASES_META = JamAttributeMeta.collectionString("aliases");

  public static final JamAnnotationMeta META =
          new JamAnnotationMeta(AnnotationConstant.COMPONENT).addAttribute(ALIASES_META);

  public JavaConfigJavaBean(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  public PsiAnnotation getPsiAnnotation() {
    return META.getAnnotation(getPsiElement());
  }

  @Override
  public String[] getAliases() {
    List<String> aliases = getStringNames(META.getAttribute(getPsiElement(), ALIASES_META));

    return ArrayUtilRt.toStringArray(aliases);
  }
}
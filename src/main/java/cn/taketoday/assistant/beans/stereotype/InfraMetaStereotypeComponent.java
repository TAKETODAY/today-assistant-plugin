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

package cn.taketoday.assistant.beans.stereotype;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 14:47
 */
public class InfraMetaStereotypeComponent extends InfraStereotypeElement {
  private static final NullableFunction<PsiClass, String> PSI_CLASS_FQN = PsiClass::getQualifiedName;

  @Nullable
  private final String myAnno;

  public InfraMetaStereotypeComponent(@Nullable String anno, PsiClass psiClass) {
    super(anno, PsiElementRef.real(psiClass));
    myAnno = anno;
  }

  /**
   * Defining annotation class.
   */
  @Nullable
  public String getDefiningAnnotation() {
    return myAnno;
  }

  protected static Collection<String> getAnnotations(@Nullable Module module, String annotation) {
    if (module == null || module.isDisposed()) {
      return Collections.singleton(annotation);
    }

    Collection<PsiClass> classes = JamAnnotationTypeUtil.getAnnotationTypesWithChildrenIncludingTests(module, annotation);
    return ContainerUtil.mapNotNull(classes, PSI_CLASS_FQN);
  }

}

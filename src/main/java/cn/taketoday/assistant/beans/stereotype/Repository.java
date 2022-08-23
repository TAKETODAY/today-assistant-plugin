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

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.util.Function;

import java.util.Collection;

import cn.taketoday.assistant.AnnotationConstant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:58
 */
@Presentation(typeName = "@Repository")
public class Repository extends InfraMetaStereotypeComponent {
  public static final SemKey<JamMemberMeta<PsiClass, Repository>> META_KEY;
  public static final SemKey<Repository> JAM_KEY;
  public static final JamClassMeta<Repository> META;
  private static final Function<Module, Collection<String>> ANNOTATIONS;

  public Repository(PsiClass psiClass) {
    this(AnnotationConstant.REPOSITORY, psiClass);
  }

  public Repository(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public Repository(Pair<String, PsiClass> pair) {
    super(pair.getFirst(), pair.getSecond());
  }

  static {
    META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("RepositoryMeta");
    JAM_KEY = JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("Repository");
    META = new JamClassMeta<>(null, Repository.class, JAM_KEY);
    addPomTargetProducer(META);
    ANNOTATIONS = (module) -> {
      return getAnnotations(module, AnnotationConstant.REPOSITORY);
    };
  }

  public static Function<Module, Collection<String>> getRepositoryAnnotations() {
    return ANNOTATIONS;
  }

}

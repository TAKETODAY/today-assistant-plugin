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

package cn.taketoday.assistant.model.config.autoconfigure.jam.el;

import com.intellij.javaee.el.providers.ELContextProvider;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.spring.el.SpringELLanguage;
import com.intellij.spring.el.contextProviders.SpringElContextProvider;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;

public class InfraAutoconfigureELInjector implements MultiHostInjector {

  private final Project myProject;
  private static final ElementPattern CONDITIONAL_ON_EXPRESSION = PsiJavaPatterns.literalExpression()
          .annotationParam(InfraConfigConstant.CONDITIONAL_ON_EXPRESSION);

  public InfraAutoconfigureELInjector(Project project) {
    this.myProject = project;
  }

  @Override
  public void getLanguagesToInject(MultiHostRegistrar registrar, PsiElement context) {
    if (InfraLibraryUtil.hasFrameworkLibrary(this.myProject)
            && CONDITIONAL_ON_EXPRESSION.accepts(context)) {
      registrar.startInjecting(SpringELLanguage.INSTANCE)
              .addPlace("", "", (PsiLanguageInjectionHost) context, ElementManipulators.getValueTextRange(context))
              .doneInjecting();
      context.putUserData(ELContextProvider.ourContextProviderKey, new SpringElContextProvider(context));
    }
  }

  @Override
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(PsiLiteralExpression.class);
  }
}

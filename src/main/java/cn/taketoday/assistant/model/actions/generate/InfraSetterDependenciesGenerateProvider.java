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

package cn.taketoday.assistant.model.actions.generate;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementNavigationProvider;
import com.intellij.util.xml.actions.generate.AbstractDomGenerateProvider;

import java.util.List;

import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraInjection;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraSetterDependenciesGenerateProvider extends AbstractDomGenerateProvider<InfraProperty> {

  public InfraSetterDependenciesGenerateProvider() {
    this(message("generate.setter.dependencies"));
  }

  public InfraSetterDependenciesGenerateProvider(String description) {
    super(description, description, InfraProperty.class);
  }

  protected DomElement getParentDomElement(Project project, Editor editor, PsiFile file) {
    return BeanCoreUtils.getBeanForCurrentCaretPosition(editor, file);
  }

  public InfraProperty generate(@Nullable DomElement parent, Editor editor) {
    if (!(parent instanceof InfraBean)) {
      return null;
    }
    List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> list = GenerateBeanDependenciesUtil.generateDependenciesFor((InfraBean) parent, true);
    InfraGenerateTemplatesHolder merged = new InfraGenerateTemplatesHolder(editor.getProject());
    for (Pair<InfraInjection, InfraGenerateTemplatesHolder> pair : list) {
      merged.addAll(pair.getSecond());
    }
    merged.runTemplates();
    if (list.size() == 0) {
      return null;
    }
    return (InfraProperty) list.get(0).getFirst();
  }

  protected void doNavigate(DomElementNavigationProvider navigateProvider, DomElement element) {
  }

  public boolean isAvailableForElement(DomElement contextElement) {
    return (contextElement instanceof InfraBean) && GenerateBeanDependenciesUtil.acceptBean((InfraBean) contextElement, true);
  }
}

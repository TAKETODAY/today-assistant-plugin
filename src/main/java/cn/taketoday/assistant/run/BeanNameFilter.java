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


package cn.taketoday.assistant.run;

import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.NullableFunction;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.gutter.BeansPsiElementCellRenderer;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

class BeanNameFilter extends MultipleOccurrencesFilter {
  private static final String[] DETECTION_MESSAGES = {
          "Error creating bean with name '", "containing bean '",
          "Creating shared instance of singleton bean '",
          "Creating MapperFactoryBean with name '",
          "@EventListener methods processed on bean '"
  };
  private static final NullableFunction<BeanPointer<?>, NavigatablePsiElement> MAPPER = (pointer) -> {
    PsiElement element = pointer.isValid() ? pointer.getPsiElement() : null;
    return element instanceof NavigatablePsiElement ? (NavigatablePsiElement) element : null;
  };

  @Nullable
  protected Filter.Result findNextOccurrence(int startOffset, String line, int entireLength) {
    int index = -1;
    int messageLength = -1;
    int beanNameEnd = DETECTION_MESSAGES.length;
    for (int i = 0; i < beanNameEnd; ++i) {
      String message = DETECTION_MESSAGES[i];
      index = StringUtil.indexOf(line, message, startOffset);
      if (index != -1) {
        messageLength = message.length();
        break;
      }
    }

    if (index == -1) {
      return null;
    }
    else {
      int beanNameStart = index + messageLength;
      beanNameEnd = StringUtil.indexOf(line, '\'', beanNameStart);
      if (beanNameEnd == -1) {
        return null;
      }
      else {
        String beanName = line.substring(beanNameStart, beanNameEnd);
        int textStartOffset = entireLength - line.length() + beanNameStart;
        return new Filter.Result(textStartOffset, textStartOffset + beanName.length(), (project) -> {
          showResult(editor -> {
            List<BeanPointer<?>> beans = findBeanCandidates(project, beanName);
            if (!beans.isEmpty()) {
              showBeanTargets(editor, beans);
            }
            else {
              PsiClass byBeanName = JavaPsiFacade.getInstance(project)
                      .findClass(beanName, GlobalSearchScope.allScope(project));
              if (byBeanName != null) {
                PsiNavigateUtil.navigate(byBeanName);
              }
              else {
                HintManager.getInstance().showErrorHint(editor, InfraBundle.message("model.bean.not.found.error.message", beanName));
              }
            }
          });
        });
      }
    }
  }

  private static List<BeanPointer<?>> findBeanCandidates(Project project, String beanName) {
    List<BeanPointer<?>> beans = new SmartList<>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      Set<InfraModel> models = InfraManager.from(project).getAllModelsWithoutDependencies(module);

      for (InfraModel model : models) {
        BeanPointer<?> byName = InfraModelSearchers.findBean(model, beanName);
        ContainerUtil.addIfNotNull(beans, byName);
      }
    }

    return beans;
  }

  private static void showBeanTargets(Editor editor, Collection<? extends BeanPointer<?>> beans) {
    Set<NavigatablePsiElement> targets = ContainerUtil.map2SetNotNull(beans, MAPPER);
    PsiElementListNavigator.openTargets(editor, targets.toArray(NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY),
            InfraBundle.message("bean.class.navigate.choose.class.title"),
            InfraBundle.message("bean.show.beans.candidates.title"),
            new BeansPsiElementCellRenderer()
    );
  }
}

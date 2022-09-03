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

package cn.taketoday.assistant.code;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.DomGotoRelatedItem;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.util.NotNullFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.gutter.BeansPsiElementCellRenderer;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.DomInfraBean;

final class NavigationGutterIconBuilderUtil {

  static final NotNullFunction<BeanPointer<?>, Collection<? extends PsiElement>> BEAN_POINTER_CONVERTOR
          = pointer -> !pointer.isValid() ? Collections.emptySet() : Collections.singleton(pointer.getBean().getIdentifyingPsiElement());

  static final NotNullFunction<CommonModelElement, Collection<? extends PsiElement>> COMMON_MODEL_ELEMENT_CONVERTOR
          = modelElement -> Collections.singleton(modelElement.getIdentifyingPsiElement());

  static final NotNullFunction<BeanPointer<?>, Collection<? extends GotoRelatedItem>> AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
          = pointer -> {
    CommonInfraBean bean = pointer.getBean();
    if (bean instanceof DomInfraBean) {
      return Collections.singletonList(new DomGotoRelatedItem((DomInfraBean) bean,
              InfraBundle.message("autowired.dependencies.goto.related.item.group.name")));
    }
    else {
      PsiElement element = bean.getIdentifyingPsiElement();
      return element != null ? Collections.singletonList(
              new GotoRelatedItem(element, InfraBundle.message("autowired.dependencies.goto.related.item.group.name"))) : Collections.emptyList();
    }
  };

  static final NotNullFunction<CommonModelElement, Collection<? extends GotoRelatedItem>> COMMON_MODEL_ELEMENT_GOTO_PROVIDER = (modelElement) -> {
    if (modelElement instanceof DomInfraBean) {
      return Collections.singletonList(new DomGotoRelatedItem((DomInfraBean) modelElement));
    }
    else {
      PsiElement element = modelElement.getIdentifyingPsiElement();
      return element != null ? Collections.singletonList(new GotoRelatedItem(element, "Today") {
        public Icon getCustomIcon() {
          return element instanceof PsiAnnotation ? Icons.Today : null;
        }
      }) : Collections.emptyList();
    }
  };

  static void addAutowiredDependenciesIcon(
          Collection<? extends BeanPointer<?>> collection,
          Collection<? super RelatedItemLineMarkerInfo<?>> holder, PsiElement identifier) {

    addAutowiredDependenciesIcon(collection, holder, identifier, InfraBundle.message("navigate.to.autowired.dependencies"));
  }

  static void addAutowiredDependenciesIcon(
          Collection<? extends BeanPointer<?>> collection,
          Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement identifier, String tooltipText) {

    ArrayList<BeanPointer<?>> sorted = new ArrayList<>(collection);
    sorted.sort(BeanPointer.DISPLAY_COMPARATOR);
    GutterIconBuilder<BeanPointer<?>> builder = GutterIconBuilder.create(
            Icons.Gutter.ShowAutowiredDependencies,
            BEAN_POINTER_CONVERTOR,
            AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
    );
    builder.setPopupTitle(InfraBundle.message("bean.class.navigate.choose.class.title"))
            .setCellRenderer(BeansPsiElementCellRenderer::new)
            .setTooltipText(tooltipText)
            .setTargets(sorted);
    result.add(builder.createRelatedMergeableLineMarkerInfo(identifier));
  }
}

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
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.gutter.SpringBeansPsiElementCellRenderer;
import com.intellij.spring.gutter.groups.SpringGutterIconBuilder;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.xml.DomSpringBean;
import com.intellij.util.NotNullFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;

final class NavigationGutterIconBuilderUtil {
  static final NotNullFunction<SpringBeanPointer<?>, Collection<? extends PsiElement>> BEAN_POINTER_CONVERTOR
          = (pointer) -> !pointer.isValid() ? Collections.emptySet() : Collections.singleton(pointer.getSpringBean().getIdentifyingPsiElement());

  static final NotNullFunction<CommonModelElement, Collection<? extends PsiElement>> COMMON_MODEL_ELEMENT_CONVERTOR
          = (modelElement) -> Collections.singleton(modelElement.getIdentifyingPsiElement());

  static final NotNullFunction<SpringBeanPointer<?>, Collection<? extends GotoRelatedItem>> AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
          = (pointer) -> {
    CommonSpringBean bean = pointer.getSpringBean();
    if (bean instanceof DomSpringBean) {
      return Collections.singletonList(new DomGotoRelatedItem((DomSpringBean) bean, SpringBundle.message("autowired.dependencies.goto.related.item.group.name")));
    }
    else {
      PsiElement element = bean.getIdentifyingPsiElement();
      return element != null ? Collections.singletonList(
              new GotoRelatedItem(element, SpringBundle.message("autowired.dependencies.goto.related.item.group.name"))) : Collections.emptyList();
    }
  };

  static final NotNullFunction<CommonModelElement, Collection<? extends GotoRelatedItem>> COMMON_MODEL_ELEMENT_GOTO_PROVIDER = (modelElement) -> {
    if (modelElement instanceof DomSpringBean) {
      return Collections.singletonList(new DomGotoRelatedItem((DomSpringBean) modelElement));
    }
    else {
      final PsiElement element = modelElement.getIdentifyingPsiElement();
      return element != null ? Collections.singletonList(new GotoRelatedItem(element, "Today") {
        public Icon getCustomIcon() {
          return element instanceof PsiAnnotation ? Icons.Today : null;
        }
      }) : Collections.emptyList();
    }
  };

  static void addAutowiredBeansGutterIcon(
          Collection<? extends SpringBeanPointer<?>> collection,
          Collection<? super RelatedItemLineMarkerInfo<?>> holder, PsiElement identifier) {

    addAutowiredBeansGutterIcon(collection, holder, identifier, SpringBundle.message("navigate.to.autowired.dependencies"));
  }

  static void addAutowiredBeansGutterIcon(
          Collection<? extends SpringBeanPointer<?>> collection,
          Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement identifier, String tooltipText) {

    List<SpringBeanPointer<?>> sorted = new ArrayList<>(collection);
    sorted.sort(SpringBeanPointer.DISPLAY_COMPARATOR);
    SpringGutterIconBuilder<SpringBeanPointer<?>> builder = SpringGutterIconBuilder.createBuilder(
            SpringApiIcons.Gutter.ShowAutowiredDependencies,
            BEAN_POINTER_CONVERTOR,
            AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
    );
    builder.setPopupTitle(SpringBundle.message("spring.bean.class.navigate.choose.class.title"))
            .setCellRenderer(SpringBeansPsiElementCellRenderer::new)
            .setTooltipText(tooltipText)
            .setTargets(sorted);
    result.add(builder.createSpringRelatedMergeableLineMarkerInfo(identifier));
  }
}

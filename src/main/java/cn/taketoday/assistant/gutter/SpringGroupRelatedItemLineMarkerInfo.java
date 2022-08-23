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

package cn.taketoday.assistant.gutter;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.psi.PsiElement;
import com.intellij.spring.gutter.groups.SpringGroupLineMarker;
import com.intellij.util.ConstantFunction;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import static cn.taketoday.assistant.gutter.GutterIconBuilder.DEFAULT_GUTTER_ICON_ALIGNMENT;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 11:23
 */
public class SpringGroupRelatedItemLineMarkerInfo extends RelatedItemLineMarkerInfo<PsiElement> implements SpringGroupLineMarker {

  SpringGroupRelatedItemLineMarkerInfo(PsiElement element,
          NavigationGutterIconRenderer renderer,
          @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
          NotNullFactory<? extends Collection<GotoRelatedItem>> targets) {
    super(element, element.getTextRange(), renderer.getIcon(),
            renderer.getTooltipText() == null ? null : new ConstantFunction<>(renderer.getTooltipText()),
            navHandler, DEFAULT_GUTTER_ICON_ALIGNMENT,
            targets);
  }

  @Override
  public boolean canMergeWith(MergeableLineMarkerInfo<?> info) {
    return info instanceof SpringGroupLineMarker;
  }

  @Override
  public Icon getCommonIcon(List<? extends MergeableLineMarkerInfo<?>> infos) {
    return getSpringActionGroupIcon();
  }

  @Override
  public GutterIconRenderer.Alignment getCommonIconAlignment(List<? extends MergeableLineMarkerInfo<?>> infos) {
    return DEFAULT_GUTTER_ICON_ALIGNMENT;
  }

  @Override
  public String getElementPresentation(PsiElement element) {
    String tooltip = getLineMarkerTooltip();
    return tooltip != null ? tooltip : super.getElementPresentation(element);
  }
}


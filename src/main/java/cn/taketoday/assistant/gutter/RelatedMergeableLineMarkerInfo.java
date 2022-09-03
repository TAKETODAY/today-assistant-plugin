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
import com.intellij.icons.AllIcons;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ConstantFunction;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.gutter.GutterIconBuilder.DEFAULT_GUTTER_ICON_ALIGNMENT;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 11:24
 */
public class RelatedMergeableLineMarkerInfo extends RelatedItemLineMarkerInfo<PsiElement> {
  @Nullable
  private String elementPresentation;

  private RelatedMergeableLineMarkerInfo(PsiElement element,
          Icon icon,
          String tooltip,
          @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
          NotNullFactory<? extends Collection<GotoRelatedItem>> targets) {
    super(element, element.getTextRange(), icon,
            new ConstantFunction<>(tooltip),
            navHandler, DEFAULT_GUTTER_ICON_ALIGNMENT, targets);
  }

  RelatedMergeableLineMarkerInfo(PsiElement element,
          NavigationGutterIconRenderer renderer,
          @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
          NotNullFactory<? extends Collection<GotoRelatedItem>> targets) {
    super(element, element.getTextRange(), renderer.getIcon(),
            renderer.getTooltipText() == null ? null : new ConstantFunction<>(renderer.getTooltipText()),
            navHandler, DEFAULT_GUTTER_ICON_ALIGNMENT,
            targets);
  }

  RelatedMergeableLineMarkerInfo(PsiElement element,
          Icon icon,
          String tooltip,
          @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
          NotNullFactory<? extends Collection<GotoRelatedItem>> targets,
          @Nullable String elementPresentation) {
    this(element, icon, tooltip, navHandler, targets);
    this.elementPresentation = elementPresentation;
  }

  @Override
  public GutterIconRenderer.Alignment getCommonIconAlignment(List<? extends MergeableLineMarkerInfo<?>> infos) {
    return DEFAULT_GUTTER_ICON_ALIGNMENT;
  }

  @Override
  public Icon getCommonIcon(List<? extends MergeableLineMarkerInfo<?>> infos) {
    Icon itemIcon = super.getCommonIcon(infos);
    LayeredIcon icon = JBUIScale.scaleIcon(new LayeredIcon(2));
    icon.setIcon(itemIcon, 0, 0, 0);
    icon.setIcon(AllIcons.General.DropdownGutter, 1, 0, 0);
    return icon;
  }

  @Override
  public String getElementPresentation(PsiElement element) {
    return elementPresentation != null ? elementPresentation : super.getElementPresentation(element);
  }
}


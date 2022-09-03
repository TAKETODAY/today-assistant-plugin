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
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.util.ConstantFunction;
import com.intellij.util.NotNullFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 11:21
 */
public class GutterIconBuilder<T> extends NavigationGutterIconBuilder<T> {

  public static final GutterIconRenderer.Alignment DEFAULT_GUTTER_ICON_ALIGNMENT = GutterIconRenderer.Alignment.LEFT;

  protected static final NotNullFunction<PsiElement, Collection<? extends GotoRelatedItem>> GOTO_RELATED_ITEM_PROVIDER =
          psiElement -> List.of(new GotoRelatedItem(psiElement, InfraBundle.message("infra")));

  private GutterIconBuilder(Icon icon,
          NotNullFunction<? super T, ? extends Collection<? extends PsiElement>> converter,
          @Nullable NotNullFunction<? super T, ? extends Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    super(icon, converter, gotoRelatedItemProvider);
  }

  public RelatedItemLineMarkerInfo<PsiElement> createGroupLineMarkerInfo(PsiElement element) {
    NavigationGutterIconRenderer renderer = createGutterIconRenderer(element.getProject());
    return new InfraGroupRelatedItemLineMarkerInfo(element, renderer, renderer, this::computeGotoTargets);
  }

  public RelatedItemLineMarkerInfo<PsiElement> createRelatedMergeableLineMarkerInfo(PsiElement element) {
    NavigationGutterIconRenderer renderer = createGutterIconRenderer(element.getProject());
    return new RelatedMergeableLineMarkerInfo(element, renderer, renderer, this::computeGotoTargets);
  }

  public static GutterIconBuilder<PsiElement> create(Icon icon) {
    return new GutterIconBuilder<>(icon, DEFAULT_PSI_CONVERTOR, GOTO_RELATED_ITEM_PROVIDER);
  }

  public static <T> GutterIconBuilder<T> create(Icon icon,
          NotNullFunction<? super T, ? extends Collection<? extends PsiElement>> converter,
          @Nullable NotNullFunction<? super T, ? extends Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    return new GutterIconBuilder<>(icon, converter, gotoRelatedItemProvider);
  }

  public static final class CustomNavigationHandlerBuilder<T> extends GutterIconBuilder<T> {

    private final GutterIconNavigationHandler<PsiElement> myNavigationHandler;

    @Nullable
    private String myElementPresentation;

    private Collection<GotoRelatedItem> myAdditionalGotoRelatedItems = Collections.emptyList();

    private CustomNavigationHandlerBuilder(Icon icon,
            String tooltipText,
            GutterIconNavigationHandler<PsiElement> navigationHandler,
            NotNullFunction<T, Collection<? extends PsiElement>> converter,
            @Nullable NotNullFunction<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
      super(icon, converter, gotoRelatedItemProvider);
      myTooltipText = tooltipText;
      myNavigationHandler = navigationHandler;
    }

    /**
     * Creates builder for custom navigation popup (EXPERIMENTAL API).
     *
     * @param icon Gutter icon.
     * @param tooltipText Gutter icon tooltip.
     * @param navigationHandler Provides custom navigation popup.
     * @param gotoRelatedItemProvider (Optional) For supporting "Goto Related Symbols".
     * @param <T> Type.
     * @return Builder instance.
     */
    public static <T> CustomNavigationHandlerBuilder<T> createBuilder(Icon icon,
            String tooltipText,
            GutterIconNavigationHandler<PsiElement> navigationHandler,
            @Nullable NotNullFunction<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
      return new CustomNavigationHandlerBuilder<>(icon, tooltipText, navigationHandler,
              new ConstantFunction<>(Collections.emptyList()),
              gotoRelatedItemProvider);
    }

    public CustomNavigationHandlerBuilder<T> withElementPresentation(String elementPresentation) {
      myElementPresentation = elementPresentation;
      return this;
    }

    public CustomNavigationHandlerBuilder<T> withAdditionalGotoRelatedItems(GotoRelatedItem... items) {
      myAdditionalGotoRelatedItems = List.of(items);
      return this;
    }

    public CustomNavigationHandlerBuilder<T> withAdditionalGotoRelatedItems(Collection<GotoRelatedItem> items) {
      myAdditionalGotoRelatedItems = items;
      return this;
    }

    @Override
    public RelatedItemLineMarkerInfo<PsiElement> createRelatedMergeableLineMarkerInfo(PsiElement element) {
      return new RelatedMergeableLineMarkerInfo(element, myIcon, myTooltipText, myNavigationHandler,
              () -> {
                List<GotoRelatedItem> result = new ArrayList<>(computeGotoTargets());
                result.addAll(myAdditionalGotoRelatedItems);
                return result;
              },
              myElementPresentation);
    }
  }
}


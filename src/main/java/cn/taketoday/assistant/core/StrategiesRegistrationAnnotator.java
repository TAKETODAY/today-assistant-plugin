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

package cn.taketoday.assistant.core;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.lang.properties.IProperty;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.spring.gutter.groups.SpringGutterIconBuilder;
import com.intellij.spring.spi.SpringSpiIconService;
import com.intellij.spring.spi.SpringSpiManager;
import com.intellij.util.NotNullFunction;
import com.intellij.util.PairProcessor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.TodayLibraryUtil;
import cn.taketoday.assistant.service.IconService;
import cn.taketoday.assistant.util.CommonUtils;

public final class StrategiesRegistrationAnnotator extends RelatedItemLineMarkerProvider {
  private static final NotNullFunction<IProperty, Collection<? extends PsiElement>> PROPERTY_CONVERTER = dom -> {
    return Collections.singleton(dom.getPsiElement());
  };
  private static final NotNullFunction<IProperty, Collection<? extends GotoRelatedItem>> PROPERTY_RELATED_CONVERTER = dom -> {
    return GotoRelatedItem.createItems(Collections.singleton(dom.getPsiElement()), TodayStrategiesFileType.STRATEGIES_FILE_NAME);
  };

  @Override
  public String getId() {
    return "StrategiesRegistrationAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("StrategiesRegistrationAnnotator.today.strategies.registration");
  }

  
  public Icon getIcon() {
    return IconService.getInstance().getFileIcon();
  }

  public void collectNavigationMarkers(List<? extends PsiElement> elements, Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {
    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement == null || !TodayLibraryUtil.hasLibrary(psiElement.getProject())) {
      return;
    }
    super.collectNavigationMarkers(elements, result, forNavigation);
  }

  protected void collectNavigationMarkers(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UClass uClass;
    PsiElement nameIdentifier;
    Module module;
    if (!TodayLibraryUtil.hasLibrary(element.getProject())
            || (uClass = UastContextKt.toUElement(element, UClass.class)) == null
            || (nameIdentifier = UElementKt.getSourcePsiElement(uClass.getUastAnchor())) == null) {
      return;
    }
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (!CommonUtils.isBeanCandidateClass(psiClass) || (module = ModuleUtilCore.findModuleForPsiElement(psiClass)) == null) {
      return;
    }
    PsiManager psiManager = PsiManager.getInstance(module.getProject());
    PairProcessor<IProperty, PsiClass> findFirstProcessor = (property, aClass) -> {
      return !psiManager.areElementsEquivalent(psiClass, aClass);
    };
    String clazzName = uClass.getQualifiedName();
    boolean foundEntry = SpringSpiManager.getInstance(module).processClassesListValues(false, clazzName, findFirstProcessor);
    if (!foundEntry) {
      SpringGutterIconBuilder<IProperty> builder = SpringGutterIconBuilder.createBuilder(SpringSpiIconService.getInstance().getGutterIcon(), PROPERTY_CONVERTER, PROPERTY_RELATED_CONVERTER);
      builder.setTargets((IProperty) NotNullLazyValue.lazy(() -> {
                SmartList<IProperty> smartList = new SmartList<>();
                PairProcessor<IProperty, PsiClass> processor = (property2, aClass2) -> {
                  if (psiManager.areElementsEquivalent(psiClass, aClass2)) {
                    smartList.add(property2);
                    return true;
                  }
                  return true;
                };
                SpringSpiManager.getInstance(module).processClassesListValues(false, clazzName, processor);
                return smartList;
              })).setPopupTitle(InfraBundle.message("StrategiesRegistrationAnnotator.choose.registration"))
              .setTooltipText(InfraBundle.message("StrategiesRegistrationAnnotator.tooltip"));
      result.add(builder.createSpringRelatedMergeableLineMarkerInfo(nameIdentifier));
    }
  }
}

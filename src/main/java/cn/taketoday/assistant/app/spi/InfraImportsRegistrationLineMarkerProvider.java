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

package cn.taketoday.assistant.app.spi;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.util.NotNullFunction;
import com.intellij.util.PairProcessor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.service.IconService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.NonNull;
import kotlin.collections.SetsKt;

import static cn.taketoday.assistant.InfraAppBundle.message;

public final class InfraImportsRegistrationLineMarkerProvider extends RelatedItemLineMarkerProvider {

  private static final NotNullFunction<PsiElement, Collection<PsiElement>> CONVERTER = new NotNullFunction<>() {
    @NonNull
    @Override
    public Collection<PsiElement> fun(PsiElement dom) {
      return SetsKt.setOf(dom);
    }
  };

  private static final NotNullFunction<PsiElement, Collection<GotoRelatedItem>> RELATED_ITEM_PROVIDER =
          new NotNullFunction<>() {
            @NonNull
            @Override
            public Collection<GotoRelatedItem> fun(PsiElement dom) {
              return GotoRelatedItem.createItems(SetsKt.setOf(dom), message("infra.imports.file.type.name"));
            }
          };

  public String getId() {
    return "InfraImportsRegistrationLineMarkerProvider";
  }

  public String getName() {
    return message("infra.imports.registration");
  }

  public Icon getIcon() {
    IconService springSpiIconService = IconService.getInstance();
    return springSpiIconService.getFileIcon();
  }

  public void collectNavigationMarkers(List<? extends PsiElement> list, Collection<? super RelatedItemLineMarkerInfo<?>> collection, boolean forNavigation) {
    PsiElement psiElement = ContainerUtil.getFirstItem(list);
    if (psiElement == null || !InfraLibraryUtil.hasFrameworkLibrary(psiElement.getProject())) {
      return;
    }
    super.collectNavigationMarkers(list, collection, forNavigation);
  }

  protected void collectNavigationMarkers(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> collection) {
    UClass uElement;
    PsiElement nameIdentifier;
    if (!InfraLibraryUtil.hasLibrary(element.getProject())
            || (uElement = UastContextKt.toUElement(element, UClass.class)) == null
            || (nameIdentifier = UElementKt.getSourcePsiElement(uElement.getUastAnchor())) == null) {
      return;
    }
    PsiClass psiElement = UElementKt.getAsJavaPsiElement(uElement, PsiClass.class);
    if (!InfraUtils.isBeanCandidateClass(psiElement)) {
      return;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) {
      return;
    }
    PsiManager psiManager = PsiManager.getInstance(module.getProject());

    var findFirstProcessor = new PairProcessor<PsiElement, PsiClass>() {
      public boolean process(PsiElement s, PsiClass aClass) {
        return !psiManager.areElementsEquivalent(psiElement, aClass);
      }
    };

    String clazzName = uElement.getQualifiedName();
    boolean foundEntry = InfraImportsManager.getInstance(module).processValues(false, clazzName, findFirstProcessor);
    if (!foundEntry) {
      IconService iconService = IconService.getInstance();
      var builder = GutterIconBuilder.create(
              iconService.getGutterIcon(), CONVERTER, RELATED_ITEM_PROVIDER);
      builder.setTargets(NotNullLazyValue.lazy(
                      (Supplier<Collection<PsiElement>>) () -> {
                        var mappedConfigKeys = new SmartList<PsiElement>();

                        var processor = new PairProcessor<PsiElement, PsiClass>() {
                          public boolean process(PsiElement psiElement1, PsiClass aClass) {
                            if (psiManager.areElementsEquivalent(psiElement1, aClass)) {
                              mappedConfigKeys.add(psiElement1);
                              return true;
                            }
                            return true;
                          }
                        };
                        InfraImportsManager.getInstance(module).processValues(false, clazzName, processor);
                        return mappedConfigKeys;
                      }

              ))
              .setPopupTitle(message("infra.imports.registration.title"))
              .setTooltipText(message("infra.imports.registration.tooltip"));

      collection.add(builder.createRelatedMergeableLineMarkerInfo(nameIdentifier));
    }
  }
}

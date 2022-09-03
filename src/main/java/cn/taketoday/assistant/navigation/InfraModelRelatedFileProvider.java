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

package cn.taketoday.assistant.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.jam.testContexts.InfraTestContextUtil;
import cn.taketoday.assistant.util.InfraUtils;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraModelRelatedFileProvider extends GotoRelatedProvider {

  public List<? extends GotoRelatedItem> getItems(PsiElement psiElement) {
    String message;
    PsiFile containingFile = psiElement.getContainingFile();
    if ((containingFile instanceof XmlFile xmlFile) && !InfraDomUtils.isInfraXml(xmlFile)) {
      return Collections.emptyList();
    }
    else if (!(containingFile instanceof PsiClassOwner)) {
      return Collections.emptyList();
    }
    else if (!InfraLibraryUtil.hasLibrary(psiElement.getProject())) {
      return Collections.emptyList();
    }
    else {
      List<GotoRelatedItem> items = new ArrayList<>();
      for (InfraModel model : InfraManager.from(psiElement.getProject()).getInfraModelsByFile(containingFile)) {
        InfraFileSet set = model.getFileSet();
        if (set != null) {
          message = message("model.goto.related.item.group.context", set.getName());
        }
        else {
          message = message("model.goto.related.item.group.application.context");
        }
        String groupName = message;
        addItems(items, containingFile, model, groupName);
      }
      PsiClass type = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
      if (InfraUtils.isBeanCandidateClass(type)
              && InfraTestContextUtil.of().isTestContextConfigurationClass(type)) {
        addItems(items, containingFile, InfraTestContextUtil.of().getTestingModel(type),
                message("model.goto.related.item.group.test.context"));
      }
      return items;
    }
  }

  private static void addItems(List<GotoRelatedItem> items, PsiFile containingFile, CommonInfraModel model, String groupName) {
    for (PsiFile file : InfraModelVisitorUtils.getConfigFiles(model)) {
      if (!file.equals(containingFile)) {
        items.add(new GotoRelatedItem(file, groupName));
      }
    }
  }
}

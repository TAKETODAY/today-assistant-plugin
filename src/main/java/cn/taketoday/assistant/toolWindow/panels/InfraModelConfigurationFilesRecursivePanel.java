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

package cn.taketoday.assistant.toolWindow.panels;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.profiles.ChangeActiveProfilesAction;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.Icons.SpringJavaConfig;
import static cn.taketoday.assistant.InfraBundle.message;

public abstract class InfraModelConfigurationFilesRecursivePanel extends FinderRecursivePanel<SmartPsiElementPointer<NavigatablePsiElement>> {

  private final Module module;

  @Nullable
  protected abstract InfraModel findModel();

  public InfraModelConfigurationFilesRecursivePanel(FinderRecursivePanel panel, Module module) {
    super(panel);
    this.module = module;
    setNonBlockingLoad(true);
  }

  protected List<SmartPsiElementPointer<NavigatablePsiElement>> getListItems() {
    InfraModel model = findModel();
    if (model == null) {
      return Collections.emptyList();
    }
    Set<PsiFile> configFiles = new LinkedHashSet<>();
    InfraModelVisitors.visitRelatedModels(model, InfraModelVisitorContext.context(p -> true, (commonInfraModel, p2) -> {
      if (commonInfraModel instanceof LocalXmlModel) {
        configFiles.add(((LocalXmlModel) commonInfraModel).getConfig());
      }
      if (commonInfraModel instanceof LocalAnnotationModel) {
        configFiles.add(((LocalAnnotationModel) commonInfraModel).getConfig().getContainingFile());
        return true;
      }
      return true;
    }), false);
    Set<NavigatablePsiElement> listItems = new HashSet<>(ContainerUtil.findAll(configFiles, XmlFile.class));
    for (PsiFile psiFile : configFiles) {
      if (psiFile instanceof PsiClassOwner psiClassOwner) {
        for (PsiClass topClass : psiClassOwner.getClasses()) {
          listItems.add(topClass);
          processInnerConfigurationClasses(listItems, topClass);
        }
      }
    }
    SmartPointerManager instance = SmartPointerManager.getInstance(getProject());
    List<SmartPsiElementPointer<NavigatablePsiElement>> pointers = new ArrayList<>(listItems.size());
    for (NavigatablePsiElement item : listItems) {
      pointers.add(instance.createSmartPsiElementPointer(item));
    }
    pointers.sort((o1, o2) -> StringUtil.naturalCompare(getItemText(o1), getItemText(o2)));
    return pointers;
  }

  public Module getModule() {
    return module;
  }

  private static void processInnerConfigurationClasses(Collection<NavigatablePsiElement> listItems, PsiClass psiClass) {
    PsiClass[] innerClasses;
    for (PsiClass innerClass : psiClass.getInnerClasses()) {
      if (InfraUtils.isConfigurationOrMeta(innerClass)) {
        listItems.add(innerClass);
      }
      processInnerConfigurationClasses(listItems, innerClass);
    }
  }

  @Nullable
  private static NavigatablePsiElement getNavigatable(@Nullable SmartPsiElementPointer<NavigatablePsiElement> pointer) {
    if (pointer != null) {
      return pointer.getElement();
    }
    return null;
  }

  public boolean hasChildren(SmartPsiElementPointer<NavigatablePsiElement> pointer) {
    return true;
  }

  @Nullable
  public Object getData(String dataId) {
    if (CommonDataKeys.PSI_FILE.is(dataId)) {
      NavigatablePsiElement navigatablePsiElement = getNavigatable(getSelectedValue());
      if (navigatablePsiElement == null) {
        return null;
      }
      return navigatablePsiElement.getContainingFile();
    }
    else if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      return getNavigatable(getSelectedValue());
    }
    else {
      return super.getData(dataId);
    }
  }

  protected AnAction[] getCustomListActions() {
    return new AnAction[] { ActionManager.getInstance().getAction(ChangeActiveProfilesAction.ACTION_ID) };
  }

  public String getItemText(SmartPsiElementPointer<NavigatablePsiElement> pointer) {
    NavigatablePsiElement navigatablePsiElement = getNavigatable(pointer);
    if (navigatablePsiElement == null) {
      return message("bean.pointer.invalid");
    }
    ItemPresentation presentation = navigatablePsiElement.getPresentation();
    String itemText = presentation != null ? presentation.getPresentableText() : navigatablePsiElement.getName();
    return StringUtil.notNullize(itemText, message("bean.pointer.unknown"));
  }

  @Nullable
  public Icon getItemIcon(SmartPsiElementPointer<NavigatablePsiElement> pointer) {
    NavigatablePsiElement navigatable = getNavigatable(pointer);
    if (navigatable == null) {
      return null;
    }
    if ((navigatable instanceof PsiClass psiClass) && InfraUtils.isConfiguration(psiClass)) {
      return SpringJavaConfig;
    }
    return navigatable.getIcon(0);
  }

  @Nullable
  public VirtualFile getContainingFile(SmartPsiElementPointer<NavigatablePsiElement> pointer) {
    return pointer.getVirtualFile();
  }

  @Nullable
  public JComponent createRightComponent(SmartPsiElementPointer<NavigatablePsiElement> pointer) {
    NavigatablePsiElement navigatablePsiElement = getNavigatable(pointer);
    if (navigatablePsiElement == null) {
      return null;
    }
    NullableFactory<CommonInfraModel> factory = getLocalInfraModel(navigatablePsiElement);
    return new InfraBeanPointerFinderRecursivePanel(this, factory);
  }

  private NullableFactory<CommonInfraModel> getLocalInfraModel(NavigatablePsiElement navigatablePsiElement) {
    return () -> {
      if (!navigatablePsiElement.isValid()) {
        return null;
      }
      else if (navigatablePsiElement instanceof XmlFile) {
        return LocalModelFactory.of().getOrCreateLocalXmlModel((XmlFile) navigatablePsiElement, module, Collections.emptySet());
      }
      else {
        return navigatablePsiElement instanceof PsiClass ? LocalModelFactory.of()
                .getOrCreateLocalAnnotationModel((PsiClass) navigatablePsiElement, this.module, Collections.emptySet()) : null;
      }
    };
  }
}

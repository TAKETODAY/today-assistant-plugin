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

package cn.taketoday.assistant.web.mvc.toolwindow;

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.CommonProcessors;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.lang.Nullable;

public class WebMvcRequestMappingsPanel extends FinderRecursivePanel<UrlMappingElement> {
  private final Set<Module> myModules;
  @Nullable
  private BeanPointer<?> myControllerBeanPointer;

  public WebMvcRequestMappingsPanel(Project project, @Nullable String groupId) {
    super(project, groupId);
    Module[] modules;
    this.myModules = new HashSet();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (isRelevantModule(module)) {
        this.myModules.add(module);
      }
    }
  }

  public WebMvcRequestMappingsPanel(FinderRecursivePanel parent, Module module) {
    super(parent);
    this.myModules = getRelevantModules(module);
  }

  public WebMvcRequestMappingsPanel(FinderRecursivePanel<BeanPointer<?>> parent, BeanPointer<?> controllerBeanPointer) {
    super(parent);
    PsiFile myControllerFile = controllerBeanPointer.getContainingFile();
    Module module = ModuleUtilCore.findModuleForPsiElement(myControllerFile);
    this.myModules = getRelevantModules(module);
    this.myControllerBeanPointer = controllerBeanPointer;
  }

  private static Set<Module> getRelevantModules(Module module) {
    Set<Module> modules = new HashSet<>();
    ModuleUtilCore.visitMeAndDependentModules(module, module1 -> {
      if (isRelevantModule(module1)) {
        modules.add(module1);
        return true;
      }
      return true;
    });
    return modules;
  }

  private static boolean isRelevantModule(Module module) {
    return InfraUtils.hasFacet(module);
  }

  protected List<UrlMappingElement> getListItems() {
    List<UrlMappingElement> items = new ArrayList<>();
    Set<RequestMethod> requestMethods = WebMvcViewSettings.getInstance(getProject()).getRequestMethods();
    CommonProcessors.CollectProcessor collectProcessor = new CommonProcessors.CollectProcessor(items);
    for (Module module : this.myModules) {
      if (!WebMvcViewUtils.processUrls(module, this.myControllerBeanPointer, requestMethods, collectProcessor)) {
        break;
      }
    }
    items.sort((item, item2) -> {
      return item.getPresentation().compareToIgnoreCase(item2.getPresentation());
    });
    return items;
  }

  public void doCustomizeCellRenderer(SimpleColoredComponent comp, JList list, UrlMappingElement value, int index, boolean selected, boolean hasFocus) {
    String containingFileName;
    comp.clear();
    comp.setIcon(getItemIcon(value));
    comp.append(UrlMappingElement.getPathPresentation(value), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    if (this.myControllerBeanPointer == null && (containingFileName = UrlMappingElement.getContainingFileName(value)) != null) {
      comp.append(" (" + containingFileName + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    String requestMethodPresentation = UrlMappingElement.getRequestMethodPresentation(value);
    if (requestMethodPresentation != null) {
      comp.append(" " + requestMethodPresentation, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
    }
  }

  public String getItemText(UrlMappingElement item) {
    return UrlMappingElement.getPathPresentation(item) + " " +
            StringUtil.notNullize(UrlMappingElement.getContainingFileName(item)) + " " +
            StringUtil.notNullize(UrlMappingElement.getRequestMethodPresentation(item));
  }

  @Nullable
  public Icon getItemIcon(UrlMappingElement item) {
    return Icons.RequestMapping;
  }

  public boolean hasChildren(UrlMappingElement item) {
    return false;
  }

  @Nullable
  public VirtualFile getContainingFile(UrlMappingElement item) {
    PsiElement psiElement = item.getNavigationTarget();
    if (psiElement == null) {
      return null;
    }
    return psiElement.getContainingFile().getVirtualFile();
  }

  protected boolean performEditAction() {
    PsiElement element;
    UrlMappingElement value = getSelectedValue();
    if (value != null && (element = value.getNavigationTarget()) != null) {
      NavigationUtil.activateFileWithPsiElement(element);
      return true;
    }
    return true;
  }

  protected JComponent createDefaultRightComponent() {
    if (!isShowDoc()) {
      return null;
    }
    return super.createDefaultRightComponent();
  }

  @Nullable
  public JComponent createRightComponent(UrlMappingElement item) {
    if (!isShowDoc()) {
      return null;
    }
    DisposablePanel panel = new DisposablePanel(new BorderLayout(), this);
    PsiElement element = item.getDocumentationPsiElement();
    if (element == null || isDisposed()) {
      return panel;
    }
    JComponent documentationComponent = DocumentationComponent.createAndFetch(getProject(), element, panel);
    panel.add(documentationComponent, "Center");
    return panel;
  }

  private boolean isShowDoc() {
    return WebMvcViewSettings.getInstance(getProject()).isShowDoc();
  }
}

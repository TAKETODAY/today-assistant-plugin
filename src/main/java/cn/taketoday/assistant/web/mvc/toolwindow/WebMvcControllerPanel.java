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

import com.intellij.microservices.endpoints.EndpointsViewOpener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.NaturalComparator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Function;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.toolWindow.WebBeanPointerPanelBase;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMappingUtil;
import cn.taketoday.lang.Nullable;

public class WebMvcControllerPanel extends WebBeanPointerPanelBase {
  private final Module[] myModules;
  private static final Function<String, String> URL_TEXT = s -> {
    return StringUtil.endsWithChar(s, '/') ? s + "*" : s + "/*";
  };

  public WebMvcControllerPanel(Project project) {
    super(project, "WebMvcController");
    this.myModules = ModuleManager.getInstance(project).getModules();
    setNonBlockingLoad(true);
  }

  public WebMvcControllerPanel(FinderRecursivePanel<Module> parent, Module module) {
    super(parent);
    this.myModules = new Module[] { module };
    setNonBlockingLoad(true);
  }

  protected List<BeanPointer<?>> getListItems() {
    CommonProcessors.CollectUniquesProcessor<BeanPointer<?>> processor = new CommonProcessors.CollectUniquesProcessor<>();
    for (Module module : this.myModules) {
      WebMvcViewUtils.processControllers(module, processor);
    }
    ArrayList<BeanPointer<?>> pointers = new ArrayList<>(processor.getResults());
    pointers.sort(Comparator.comparing(this::getItemText, NaturalComparator.INSTANCE));
    return pointers;
  }

  public boolean hasChildren(BeanPointer<?> beanPointer) {
    return true;
  }

  public void doCustomizeCellRenderer(SimpleColoredComponent comp, JList list, BeanPointer<?> value, int index, boolean selected, boolean hasFocus) {
    if (!value.isValid()) {
      return;
    }
    comp.clear();
    comp.setIcon(getItemIcon(value));
    if (value instanceof JamBeanPointer) {
      renderController(comp, value);
      return;
    }
    comp.append(getItemText(value));
    comp.append(" (" + value.getContainingFile().getName() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  private void renderController(SimpleColoredComponent comp, BeanPointer<?> beanPointer) {
    PsiClass controllerClass = beanPointer.getBeanClass();
    if (controllerClass != null) {
      RequestMapping<PsiClass> classMapping = RequestMappingUtil.getClassLevelMapping(controllerClass);
      if (classMapping != null) {
        comp.append(StringUtil.join(classMapping.getUrls(), URL_TEXT, ", "), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
      else {
        comp.append(getItemText(beanPointer));
      }
      String packageName = controllerClass.getQualifiedName();
      comp.append(" (" + packageName + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }

  @Nullable
  public JComponent createRightComponent(BeanPointer<?> beanPointer) {
    return new WebMvcRequestMappingsPanel(this, beanPointer);
  }

  protected JComponent createLeftComponent() {
    JComponent component = super.createLeftComponent();
    this.myList.getEmptyText().setText(InfraAppBundle.message("web.mvc.controllers.not.found"));
    if (EndpointsViewOpener.isAvailable(getProject())) {
      this.myList.getEmptyText().appendSecondaryText(InfraAppBundle.message("web.mvc.check.all.endpoints"), SimpleTextAttributes.LINK_ATTRIBUTES, e -> {
        EndpointsViewOpener.showAllEndpoints(getProject());
      });
    }
    return component;
  }
}

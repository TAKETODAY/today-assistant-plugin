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
package cn.taketoday.assistant.toolWindow;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.ui.FinderRecursivePanel;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

/**
 * Provides unified rendering, editing and DataContext-integration.
 */
public abstract class WebBeanPointerPanelBase extends FinderRecursivePanel<BeanPointer<?>> {

  protected WebBeanPointerPanelBase(FinderRecursivePanel parent) {
    super(parent);
  }

  protected WebBeanPointerPanelBase(Project project, @Nullable String groupId) {
    super(project, groupId);
  }

  protected WebBeanPointerPanelBase(Project project, @Nullable FinderRecursivePanel parent, @Nullable String groupId) {
    super(project, parent, groupId);
  }

  @Override
  protected String getItemText(BeanPointer<?> pointer) {
    return isNotDumb() && pointer.isValid()
           ? InfraPresentationProvider.getBeanName(pointer)
           : message("bean.invalid");
  }

  @Nullable
  @Override
  protected Icon getItemIcon(BeanPointer<?> pointer) {
    return isNotDumb() && pointer.isValid() ? InfraPresentationProvider.getInfraIcon(pointer) : null;
  }

  @Nullable
  @Override
  protected VirtualFile getContainingFile(BeanPointer<?> pointer) {
    return pointer.isValid() ? pointer.getContainingFile().getVirtualFile() : null;
  }

  @Nullable
  @Override
  protected String getItemTooltipText(BeanPointer<?> pointer) {
    if (!isNotDumb())
      return null;

    CommonInfraBean bean = pointer.isValid() ? pointer.getBean() : null;
    if (bean != null) {
      PsiType beanType = bean.getBeanType(true);
      if (beanType != null) {
        return beanType.getCanonicalText();
      }
    }

    return null;
  }

  @Override
  protected boolean performEditAction() {
    BeanPointer<?> beanPointer = getSelectedValue();
    if (beanPointer == null)
      return true;

    PsiElement navigationElement = beanPointer.getPsiElement();
    if (navigationElement != null) {
      NavigationUtil.activateFileWithPsiElement(navigationElement);
    }
    return true;
  }

  @Nullable
  @Override
  public Object getData(String dataId) {
    BeanPointer<?> selectedValue = getSelectedValue();
    if (selectedValue != null && selectedValue.isValid()) {
      if (CommonDataKeys.NAVIGATABLE.is(dataId) || CommonDataKeys.PSI_ELEMENT.is(dataId)) {
        return selectedValue.getBean().getIdentifyingPsiElement();
      }

      if (CommonDataKeys.PSI_FILE.is(dataId)) {
        return selectedValue.getContainingFile();
      }
    }

    return super.getData(dataId);
  }

  protected boolean isNotDumb() {
    return !getProject().isDisposed() &&
            !DumbService.isDumb(getProject());
  }
}

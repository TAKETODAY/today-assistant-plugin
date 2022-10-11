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

package cn.taketoday.assistant.app.run.lifecycle.beans.tab;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.beans.BeansEndpoint;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.lang.Nullable;

abstract class LifecycleFinderRecursivePanel<T> extends FinderRecursivePanel<T> {

  private final InfraApplicationRunConfig myRunConfiguration;

  private final ProcessHandler myProcessHandler;
  private JBList<T> myList;

  protected LifecycleFinderRecursivePanel(Project project, @Nullable FinderRecursivePanel parent, @Nullable String groupId,
          InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    super(project, parent, groupId);
    this.myRunConfiguration = runConfiguration;
    this.myProcessHandler = processHandler;
  }

  protected final InfraApplicationRunConfig getRunConfiguration() {
    return this.myRunConfiguration;
  }

  protected final ProcessHandler getProcessHandler() {
    return this.myProcessHandler;
  }

  protected boolean performEditAction() {
    T selectedValue = getSelectedValue();
    updateItem(selectedValue);
    Navigatable data = CommonDataKeys.NAVIGATABLE.getData(this);
    if (data != null && data.canNavigate()) {
      data.navigate(true);
      return false;
    }
    else if (selectedValue != null) {
      String message = getEditActionHintMessage(selectedValue);
      if (StringUtil.isNotEmpty(message)) {
        showHint(message);
        return false;
      }
      return false;
    }
    else {
      return false;
    }
  }

  protected JBList<T> createList() {
    this.myList = super.createList();
    return this.myList;
  }

  @Nullable
  protected final LiveBeansModel getModel() {
    InfraApplicationInfo info = InfraApplicationLifecycleManager.from(getProject()).getInfraApplicationInfo(myProcessHandler);
    if (info != null) {
      return info.getEndpointData(BeansEndpoint.of()).getValue();
    }
    return null;
  }

  protected final void updateItem(@Nullable T t) {
    if (DumbService.isDumb(getProject()) || t == null) {
      return;
    }
    boolean changed = doUpdateItem(t);
    if (changed) {
      updateRightComponent(!hasChildren(t));
    }
  }

  protected boolean doUpdateItem(T t) {
    return false;
  }

  protected void showHint(@NlsContexts.HintText String message) {
    if (this.myList == null) {
      return;
    }
    JComponent messageComponent = HintUtil.createInformationLabel(message);
    BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(messageComponent);
    Balloon balloon = builder.setRequestFocus(false).setFadeoutTime(3000L).setFillColor(HintUtil.getInformationColor()).createBalloon();
    int selectedIndex = this.myList.getSelectedIndex();
    Rectangle cellBounds = this.myList.getCellBounds(selectedIndex, selectedIndex);
    if (cellBounds == null) {
      balloon.showInCenterOf(this.myList);
      return;
    }
    Point point = new Point(cellBounds.x + (cellBounds.width / 2), cellBounds.y + (cellBounds.height / 2));
    balloon.show(new RelativePoint(this.myList, point), Balloon.Position.below);
  }

  @NlsContexts.HintText
  protected String getEditActionHintMessage(T t) {
    return null;
  }

  public final void updateComponent() {
    if (!isDisposed()) {
      updatePanel();
    }
  }
}

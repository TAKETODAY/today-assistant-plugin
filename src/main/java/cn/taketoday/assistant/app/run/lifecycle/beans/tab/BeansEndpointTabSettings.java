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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "BeansEndpointTabSettings", storages = { @Storage("$WORKSPACE_FILE$") })
public final class BeansEndpointTabSettings implements PersistentStateComponent<BeansEndpointTabSettings> {
  private boolean diagramMode;
  private boolean showLibraryBeans = true;
  private boolean showDoc = true;
  private boolean showLiveBeansGraph = true;
  private boolean showContexts = true;
  private boolean showFiles = true;

  public static BeansEndpointTabSettings getInstance(Project project) {
    return project.getService(BeansEndpointTabSettings.class);
  }

  public BeansEndpointTabSettings getState() {
    return this;
  }

  public void loadState(BeansEndpointTabSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isDiagramMode() {
    return this.diagramMode;
  }

  public void setDiagramMode(boolean diagramMode) {
    this.diagramMode = diagramMode;
  }

  public boolean isShowLibraryBeans() {
    return this.showLibraryBeans;
  }

  public void setShowLibraryBeans(boolean showLibraryBeans) {
    this.showLibraryBeans = showLibraryBeans;
  }

  public boolean isShowDoc() {
    return this.showDoc;
  }

  public void setShowDoc(boolean value) {
    this.showDoc = value;
  }

  public boolean isShowLiveBeansGraph() {
    return this.showLiveBeansGraph;
  }

  public void setShowLiveBeansGraph(boolean value) {
    this.showLiveBeansGraph = value;
  }

  public boolean isShowContexts() {
    return this.showContexts;
  }

  public void setShowContexts(boolean value) {
    this.showContexts = value;
  }

  public boolean isShowFiles() {
    return this.showFiles;
  }

  public void setShowFiles(boolean value) {
    this.showFiles = value;
  }
}

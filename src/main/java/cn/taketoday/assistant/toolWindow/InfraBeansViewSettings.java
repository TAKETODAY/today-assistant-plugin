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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;

import cn.taketoday.lang.Nullable;

@State(name = "InfraBeansView", storages = { @Storage("$WORKSPACE_FILE$") })
public class InfraBeansViewSettings implements PersistentStateComponent<InfraBeansViewSettings.Settings> {
  private final MessageBus myMessageBus;
  private final Settings mySettings;
  public static final Topic<Listener> TOPIC = new Topic<>("Infrastructure Beans View settings", Listener.class);

  public void fireSettingsChanged(ChangeType changeType) {
    this.myMessageBus.syncPublisher(TOPIC).settingsChanged(changeType);
  }

  public static InfraBeansViewSettings from(Project project) {
    return project.getService(InfraBeansViewSettings.class);
  }

  public InfraBeansViewSettings(Project project) {

    this.mySettings = new Settings();
    this.myMessageBus = project.getMessageBus();
  }

  @Nullable
  public Settings getState() {
    return this.mySettings;
  }

  public void loadState(Settings settings) {
    XmlSerializerUtil.copyBean(settings, this.mySettings);
  }

  public boolean isShowModules() {
    return this.mySettings.showModules;
  }

  public void setShowModules(boolean value) {
    this.mySettings.showModules = value;
  }

  public boolean isShowFileSets() {
    return this.mySettings.showFilesets;
  }

  public void setShowFileSets(boolean value) {
    this.mySettings.showFilesets = value;
  }

  public boolean isShowFiles() {
    return this.mySettings.showFiles;
  }

  public void setShowFiles(boolean value) {
    this.mySettings.showFiles = value;
  }

  public boolean isShowImplicitBeans() {
    return this.mySettings.showImplicitBeans;
  }

  public void setShowImplicitBeans(boolean value) {
    this.mySettings.showImplicitBeans = value;
  }

  public boolean isShowInfrastructureBeans() {
    return this.mySettings.showInfrastructureBeans;
  }

  public void setShowInfrastructureBeans(boolean value) {
    this.mySettings.showInfrastructureBeans = value;
  }

  public boolean isShowDoc() {
    return this.mySettings.showDoc;
  }

  public void setShowDoc(boolean value) {
    this.mySettings.showDoc = value;
  }

  public boolean isShowGraph() {
    return this.mySettings.showGraph;
  }

  public void setShowGraph(boolean value) {
    this.mySettings.showGraph = value;
  }

  public void setBeanDetailsProportion(float proportion) {
    this.mySettings.beanDetailsProportion = proportion;
  }

  public float getBeanDetailsProportion() {
    return this.mySettings.beanDetailsProportion;
  }

  public static class Settings {
    public boolean showModules = true;
    public boolean showFilesets = true;
    public boolean showFiles = true;
    public boolean showImplicitBeans = true;
    public boolean showInfrastructureBeans = true;
    public boolean showDoc = true;
    public boolean showGraph = true;
    public float beanDetailsProportion = -1.0F;

  }

  public enum ChangeType {
    FULL,
    UPDATE_LIST,
    UPDATE_DETAILS,
    FORCE_UPDATE_RIGHT_COMPONENT;

  }

  public interface Listener {
    void settingsChanged(ChangeType var1);
  }
}

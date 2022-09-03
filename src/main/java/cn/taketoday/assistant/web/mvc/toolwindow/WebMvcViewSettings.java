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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.EnumSet;
import java.util.Set;

import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;

@State(name = "InfraMvcView", storages = { @Storage("$WORKSPACE_FILE$") })
public class WebMvcViewSettings implements PersistentStateComponent<WebMvcViewSettings.Settings> {
  private final Settings mySettings = new Settings();
  public static final Topic<Listener> TOPIC = new Topic<>("Web MVC View settings", Listener.class);

  public enum ChangeType {
    FULL,
    UPDATE_LIST,
    UPDATE_DETAILS,
    REPLACE_CHILD_UPDATE
  }

  public interface Listener {
    void settingsChanged(ChangeType changeType);
  }

  public static class Settings {
    public boolean showModules = true;
    public boolean showControllers = true;
    public boolean showDoc = true;
    public Set<RequestMethod> requestMethods = EnumSet.allOf(RequestMethod.class);
  }

  public static void fireSettingsChanged(Project project, ChangeType changeType) {
    project.getMessageBus().syncPublisher(TOPIC).settingsChanged(changeType);
  }

  public static WebMvcViewSettings getInstance(Project project) {
    return project.getService(WebMvcViewSettings.class);
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

  public boolean isShowDoc() {
    return this.mySettings.showDoc;
  }

  public void setShowDoc(boolean value) {
    this.mySettings.showDoc = value;
  }

  public boolean isShowControllers() {
    return this.mySettings.showControllers;
  }

  public void setShowControllers(boolean value) {
    this.mySettings.showControllers = value;
  }

  public Set<RequestMethod> getRequestMethods() {
    return this.mySettings.requestMethods;
  }
}

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

package cn.taketoday.assistant.app.run;

import com.intellij.execution.ShortenCommandLine;
import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.XCollection;

import java.util.List;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.MutablePropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.properties.ReadWriteProperty;
import kotlin.reflect.KMutableProperty1;

public class InfraApplicationRunConfigurationOptions extends JvmMainMethodRunConfigurationOptions {

  static final KMutableProperty1[] properties = {
          create("isIncludeProvidedScope", "isIncludeProvidedScope()Z", 0),
          create("shortenCommandLine", "getShortenCommandLine()Lcom/intellij/execution/ShortenCommandLine;", 0),
          create("mainClass", "getMainClass()Ljava/lang/String;", 0),
          create("debugMode", "getDebugMode()Z", 0),
          create("enableLaunchOptimization", "getEnableLaunchOptimization()Z", 0),
          create("hideBanner", "getHideBanner()Z", 0),
          create("enableJmxAgent", "getEnableJmxAgent()Z", 0),
          create("activeProfiles", "getActiveProfiles()Ljava/lang/String;", 0),
          create("urlPath", "getUrlPath()Ljava/lang/String;", 0),
          create("updateActionUpdatePolicy", "getUpdateActionUpdatePolicy()Ljava/lang/String;", 0),
          create("frameDeactivationUpdatePolicy", "getFrameDeactivationUpdatePolicy()Ljava/lang/String;", 0),
          create("additionalParameter", "getAdditionalParameter()Ljava/util/List;", 0)
  };

  static KMutableProperty1 create(String name, String signature, int flags) {
    return Reflection.mutableProperty1(
            new MutablePropertyReference1Impl(InfraApplicationRunConfigurationOptions.class, name, signature, flags));
  }

  @Nullable
  private final ReadWriteProperty shortenCommandLine$delegate = property(null, Objects::isNull).provideDelegate(this, properties[1]);

  private final ReadWriteProperty debugMode$delegate = property(false).provideDelegate(this, properties[3]);

  private final ReadWriteProperty enableLaunchOptimization$delegate = property(true).provideDelegate(this, properties[4]);

  private final ReadWriteProperty hideBanner$delegate = property(false).provideDelegate(this, properties[5]);

  private final ReadWriteProperty enableJmxAgent$delegate = property(true).provideDelegate(this, properties[6]);
  @Nullable
  private final ReadWriteProperty activeProfiles$delegate = string("").provideDelegate(this, properties[7]);
  @Nullable
  private final ReadWriteProperty urlPath$delegate = string("").provideDelegate(this, properties[8]);
  @Nullable
  private final ReadWriteProperty updateActionUpdatePolicy$delegate = string(null).provideDelegate(this, properties[9]);
  @Nullable
  private final ReadWriteProperty frameDeactivationUpdatePolicy$delegate = string(null).provideDelegate(this, properties[10]);

  private final ReadWriteProperty additionalParameter$delegate = list().provideDelegate(this, properties[11]);

  @OptionTag("SHORTEN_COMMAND_LINE")
  @Nullable
  public final ShortenCommandLine getShortenCommandLine() {
    return (ShortenCommandLine) this.shortenCommandLine$delegate.getValue(this, properties[1]);
  }

  public final void setShortenCommandLine(@Nullable ShortenCommandLine shortenCommandLine) {
    this.shortenCommandLine$delegate.setValue(this, properties[1], shortenCommandLine);
  }

  @OptionTag("DEBUG_MODE")
  public final boolean getDebugMode() {
    return (Boolean) this.debugMode$delegate.getValue(this, properties[3]);
  }

  public final void setDebugMode(boolean z) {
    this.debugMode$delegate.setValue(this, properties[3], z);
  }

  @OptionTag("ENABLE_LAUNCH_OPTIMIZATION")
  public final boolean getEnableLaunchOptimization() {
    return (Boolean) this.enableLaunchOptimization$delegate.getValue(this, properties[4]);
  }

  public final void setEnableLaunchOptimization(boolean z) {
    this.enableLaunchOptimization$delegate.setValue(this, properties[4], z);
  }

  @OptionTag("HIDE_BANNER")
  public final boolean getHideBanner() {
    return (Boolean) this.hideBanner$delegate.getValue(this, properties[5]);
  }

  public final void setHideBanner(boolean z) {
    this.hideBanner$delegate.setValue(this, properties[5], z);
  }

  @OptionTag("ENABLE_JMX_AGENT")
  public final boolean getEnableJmxAgent() {
    return (Boolean) this.enableJmxAgent$delegate.getValue(this, properties[6]);
  }

  public final void setEnableJmxAgent(boolean z) {
    this.enableJmxAgent$delegate.setValue(this, properties[6], z);
  }

  @OptionTag("ACTIVE_PROFILES")
  @Nullable
  public final String getActiveProfiles() {
    return (String) this.activeProfiles$delegate.getValue(this, properties[7]);
  }

  public final void setActiveProfiles(@Nullable String str) {
    this.activeProfiles$delegate.setValue(this, properties[7], str);
  }

  @OptionTag("URL_PATH")
  @Nullable
  public final String getUrlPath() {
    return (String) this.urlPath$delegate.getValue(this, properties[8]);
  }

  public final void setUrlPath(@Nullable String str) {
    this.urlPath$delegate.setValue(this, properties[8], str);
  }

  @OptionTag("UPDATE_ACTION_UPDATE_POLICY")
  @Nullable
  public final String getUpdateActionUpdatePolicy() {
    return (String) this.updateActionUpdatePolicy$delegate.getValue(this, properties[9]);
  }

  public final void setUpdateActionUpdatePolicy(@Nullable String str) {
    this.updateActionUpdatePolicy$delegate.setValue(this, properties[9], str);
  }

  @OptionTag("FRAME_DEACTIVATION_UPDATE_POLICY")
  @Nullable
  public final String getFrameDeactivationUpdatePolicy() {
    return (String) this.frameDeactivationUpdatePolicy$delegate.getValue(this, properties[10]);
  }

  public final void setFrameDeactivationUpdatePolicy(@Nullable String str) {
    this.frameDeactivationUpdatePolicy$delegate.setValue(this, properties[10], str);
  }

  @XCollection(propertyElementName = "additionalParameters")
  public final List<InfraAdditionalParameter> getAdditionalParameter() {
    return (List) this.additionalParameter$delegate.getValue(this, properties[11]);
  }

  public final void setAdditionalParameter(List<InfraAdditionalParameter> list) {
    Intrinsics.checkNotNullParameter(list, "<set-?>");
    this.additionalParameter$delegate.setValue(this, properties[11], list);
  }
}

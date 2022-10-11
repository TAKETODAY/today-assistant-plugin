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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.target.TargetEnvironment;
import com.intellij.execution.target.TargetEnvironmentRequest;
import com.intellij.execution.target.TargetProgressIndicator;
import com.intellij.execution.target.local.LocalTargetEnvironmentRequest;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.microservices.http.request.NavigatorHttpRequest;
import com.intellij.microservices.jvm.config.ConfigTunnelPortMapping;
import com.intellij.microservices.utils.PortBindingNotificationPanel;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SlowOperations;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.concurrency.Promise;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationUrlUtil;
import cn.taketoday.assistant.app.run.update.TriggerFilePolicy;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager.InfoListener;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager.from;

class InfraCommandLineState extends ApplicationCommandLineState<InfraApplicationRunConfiguration> {
  static final String DEBUG_PARAMETER = "-Ddebug";
  static final String TIERED_STOP_AT_LEVEL_1_PARAMETER = "-XX:TieredStopAtLevel=1";
  static final String NO_VERIFY_PARAMETER = "-noverify";
  static final String HIDE_BANNER_PARAMETER_1_5 = "-Dapp.main.banner-mode=OFF";
  static final String ACTIVE_PROFILES_PARAMETER = "-Dcontext.profiles.active";
  static final String TRIGGER_FILE_PARAMETER = "-Ddevtools.restart.trigger-file";
  static final String OUTPUT_ANSI_ENABLED_PARAMETER = "-Doutput.ansi.enabled=always";
  static final String JMX_REMOTE_PARAMETER = "-Dcom.sun.management.jmxremote";
  static final String JMX_PARAMETER = "-Dinfra.jmx.enabled=true";
  static final String LIVE_BEAN_PARAMETER = "-DliveBeansView.mbeanDomain";
  static final String LIFECYCLE_PARAMETER = "-Dapp.admin.enabled=true";
  private static final int DEFAULT_SERVER_PORT = 8080;
  private static final String SERVER_PORT_PROPERTY = "server.port";
  private final AtomicBoolean myTargetParametersPrepared;
  private volatile boolean myJmxEnabled;
  private volatile InfraJmxSetup myJmxSetup;
  private volatile int myServerPort;
  private TargetEnvironment myResolvedEnvironment;

  InfraCommandLineState(InfraApplicationRunConfiguration configuration, ExecutionEnvironment environment) {
    super(configuration, environment);
    this.myTargetParametersPrepared = new AtomicBoolean(false);
    this.myJmxEnabled = false;
    this.myServerPort = -1;
  }

  public <T> Promise<T> prepareTargetToCommandExecution(ExecutionEnvironment env, Logger logger, String logFailureMessage,
          ThrowableComputable<? extends T, ? extends ExecutionException> afterPreparation) throws ExecutionException {
    ThrowableComputable<? extends T, ? extends ExecutionException> wrapper = () -> {
      this.prepareTargetParameters();
      return afterPreparation.compute();
    };
    return super.prepareTargetToCommandExecution(env, logger, logFailureMessage, wrapper);
  }

  public void prepareTargetEnvironmentRequest(TargetEnvironmentRequest request, TargetProgressIndicator targetProgressIndicator) throws ExecutionException {
    prepareTargetParameters();
    JavaParameters javaParams = getJavaParameters();
    exposeServerHttpPort(javaParams, request);
    if (this.myJmxEnabled) {
      this.myJmxSetup = request instanceof LocalTargetEnvironmentRequest ? new InfraJmxSetup.LocalSetup() : new InfraJmxSetup.RemoteSetup();
      this.myJmxSetup.setupJmxParameters(getJavaParameters(), request);
    }
    super.prepareTargetEnvironmentRequest(request, targetProgressIndicator);
  }

  protected JavaParameters createJavaParameters() throws ExecutionException {
    JavaParameters params = super.createJavaParameters();
    applyInfraSettings(params);
    return params;
  }

  protected boolean isProvidedScopeIncluded() {
    return this.myConfiguration.isProvidedScopeIncluded();
  }

  private void applyInfraSettings(JavaParameters params) {
    ParametersList vmParametersList = params.getVMParametersList();
    for (InfraAdditionalParameter parameter : this.myConfiguration.getAdditionalParameters()) {
      if (parameter.isEnabled()) {
        String parameterValue = parameter.getValue();
        if (StringUtil.isNotEmpty(parameterValue)) {
          vmParametersList.add("-D" + parameter.getName() + "=" + parameterValue);
        }
        else {
          vmParametersList.add("-D" + parameter.getName());
        }
      }
    }
    if (this.myConfiguration.isDebugMode()) {
      vmParametersList.add(DEBUG_PARAMETER);
    }
    if (this.myConfiguration.isEnableLaunchOptimization()) {
      vmParametersList.add(TIERED_STOP_AT_LEVEL_1_PARAMETER);
      if (!JavaSdkUtil.isJdkAtLeast(params.getJdk(), JavaSdkVersion.JDK_13)) {
        vmParametersList.add(NO_VERIFY_PARAMETER);
      }
    }
    if (this.myConfiguration.isHideBanner()) {
      vmParametersList.add(HIDE_BANNER_PARAMETER_1_5);
    }
    if (StringUtil.isNotEmpty(this.myConfiguration.getActiveProfiles())) {
      vmParametersList.add(ACTIVE_PROFILES_PARAMETER + '=' + this.myConfiguration.getActiveProfiles());
    }
    if (this.myConfiguration.getUpdateActionUpdatePolicy() instanceof TriggerFilePolicy) {
      vmParametersList.add(TRIGGER_FILE_PARAMETER + "=.restartTriggerFile");
    }
    vmParametersList.add(OUTPUT_ANSI_ENABLED_PARAMETER);
  }

  private void setupJmxParameters(JavaParameters params) {
    if (!this.myConfiguration.isEnableJmxAgent()) {
      return;
    }
    SlowOperations.assertSlowOperationsAreAllowed();
    ParametersList vmParametersList = params.getVMParametersList();
    vmParametersList.add(JMX_REMOTE_PARAMETER);
    vmParametersList.add(JMX_PARAMETER);
    vmParametersList.add(LIVE_BEAN_PARAMETER);
    vmParametersList.add(LIFECYCLE_PARAMETER);
    this.myJmxEnabled = true;
  }

  public void handleCreatedTargetEnvironment(TargetEnvironment environment, TargetProgressIndicator targetProgressIndicator) {
    super.handleCreatedTargetEnvironment(environment, targetProgressIndicator);
    this.myResolvedEnvironment = environment;
    if (this.myJmxSetup != null) {
      this.myJmxSetup.handleCreatedEnvironment(environment);
    }
  }

  public ExecutionResult execute(Executor executor, ProgramRunner<?> runner) throws ExecutionException {
    ExecutionResult result = super.execute(executor, runner);
    if (this.myJmxSetup != null) {
      this.myJmxSetup.setupProcessHandler(result.getProcessHandler());
      InfraApplicationLifecycleManager.from(getConfiguration().getProject());
    }
    if (this.myResolvedEnvironment == null) {
      return result;
    }
    Map<TargetEnvironment.TargetPortBinding, Integer> portsMap = this.myResolvedEnvironment.getTargetPortBindings();
    ConfigTunnelPortMapping mapping = remotePort -> {
      return portsMap.entrySet()
              .stream()
              .filter(entry -> entry.getKey().getTarget() == remotePort).findAny()
              .map(Map.Entry::getValue)
              .orElse(remotePort);
    };
    result.getProcessHandler().putUserData(ConfigTunnelPortMapping.MAPPING_KEY, mapping);
    if (this.myServerPort <= 0) {
      return result;
    }
    int resolvedServerPort = mapping.getLocalPort(this.myServerPort);
    if (resolvedServerPort == this.myServerPort) {
      return result;
    }
    ExecutionConsole console = result.getExecutionConsole();
    if (!(console instanceof ConsoleViewImpl)) {
      return result;
    }
    addPortBindingPanel((ConsoleViewImpl) console, result.getProcessHandler(), this.myServerPort, resolvedServerPort);
    return result;
  }

  private void addPortBindingPanel(ConsoleViewImpl console, ProcessHandler handler, int applicationPort, int resolvedPort) {
    ApplicationManager.getApplication().invokeLater(() -> {
      PortBindingNotificationPanel notificationPanel = new PortBindingNotificationPanel(this.myConfiguration.getProject(),
              null, this.myConfiguration.getName(), applicationPort, resolvedPort);
      notificationPanel.registerProcessListener(handler, console);
      NonOpaquePanel wrapper = new NonOpaquePanel(notificationPanel);
      wrapper.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
      console.addNotificationComponent(wrapper);
      from(this.myConfiguration.getProject())
              .addInfoListener(new InfoListener() {
                @Override
                public void infoAdded(ProcessHandler processHandler, InfraApplicationInfo info) {
                  if (processHandler.equals(handler)) {
                    info.getApplicationUrl().addPropertyListener(() -> {
                      String applicationUrl = info.getApplicationUrl().getValue();
                      if (applicationUrl != null) {
                        InfraApplicationUrlUtil urlUtil = InfraApplicationUrlUtil.getInstance();
                        String mappingPath = myConfiguration.getUrlPath();
                        String servletPath = urlUtil.getServletPath(info, mappingPath);
                        String requestUrl = urlUtil.getMappingUrl(applicationUrl, servletPath, mappingPath);
                        NavigatorHttpRequest request = new NavigatorHttpRequest(requestUrl, "GET", Collections.emptyList(), Collections.emptyList());
                        AppUIUtil.invokeOnEdt(() -> notificationPanel.setRequest(request));
                      }

                    });
                  }
                }

                @Override
                public void infoRemoved(ProcessHandler processHandler, InfraApplicationInfo info) {
                  if (processHandler.equals(handler)) {
                    InfraApplicationLifecycleManager.from(myConfiguration.getProject())
                            .removeInfoListener(this);
                  }
                }
              });
    }, (o) -> Disposer.isDisposed(console));
  }

  protected AnAction[] createActions(ConsoleView console, ProcessHandler processHandler, Executor executor) {
    AnAction action;
    AnAction[] actions = super.createActions(console, processHandler, executor);
    if (this.myConfiguration.getUpdateActionUpdatePolicy() != null
            && (action = ActionManager.getInstance().getAction("UpdateRunningApplication")) != null) {
      return ArrayUtil.append(actions, action);
    }
    return actions;
  }

  private void prepareTargetParameters() throws ExecutionException {
    if (this.myTargetParametersPrepared.compareAndSet(false, true)) {
      JavaParameters javaParams = getJavaParameters();
      setupJmxParameters(javaParams);
    }
  }

  private void exposeServerHttpPort(JavaParameters javaParams, TargetEnvironmentRequest request) {
    if ((request instanceof LocalTargetEnvironmentRequest) || !InfraLibraryUtil.hasRequestMappings(this.myConfiguration.getModule())) {
      return;
    }
    SlowOperations.assertSlowOperationsAreAllowed();
    try {
      String serverPortString = Optional.ofNullable(findServerPort(javaParams)).orElse(String.valueOf(DEFAULT_SERVER_PORT));
      this.myServerPort = Integer.parseInt(serverPortString);
    }
    catch (NumberFormatException e) {
      this.myServerPort = DEFAULT_SERVER_PORT;
    }
    request.getTargetPortBindings().add(new TargetEnvironment.TargetPortBinding(null, this.myServerPort));
  }

  @Nullable
  private String findServerPort(JavaParameters javaParams) {
    InfraApplicationRunConfiguration runConfig = getConfiguration();
    InfraAdditionalParameter additionalParameter = ContainerUtil.find(runConfig.getAdditionalParameters(), next -> {
      return next.isEnabled() && SERVER_PORT_PROPERTY.equals(next.getName());
    });
    if (additionalParameter != null) {
      return additionalParameter.getValue();
    }
    String fromVmOptions = javaParams.getVMParametersList().getPropertyValue(SERVER_PORT_PROPERTY);
    if (fromVmOptions != null && !fromVmOptions.isBlank()) {
      return fromVmOptions;
    }
    return ReadAction.compute(this::findServerPortInConfigs);
  }

  private String findServerPortInConfigs() {
    InfraApplicationRunConfiguration runConfig = getConfiguration();
    Set<String> profiles = ProfileUtils.profilesFromString(runConfig.getActiveProfiles());
    Ref<String> result = new Ref<>();
    InfraConfigValueSearcher.productionForProfiles(runConfig.getModule(), SERVER_PORT_PROPERTY, profiles).process(nextResult -> {
      String value = nextResult.getValueText();
      if (value != null && !value.isEmpty() && !value.isBlank()) {
        result.set(value);
      }
      return result.isNull();
    });
    return result.get();
  }
}

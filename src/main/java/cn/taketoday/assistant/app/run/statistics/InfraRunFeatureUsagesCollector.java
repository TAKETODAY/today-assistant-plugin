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

package cn.taketoday.assistant.app.run.statistics;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.ShortenCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.internal.statistic.beans.MetricEvent;
import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.events.EventField;
import com.intellij.internal.statistic.eventLog.events.EventFields;
import com.intellij.internal.statistic.eventLog.events.EventId1;
import com.intellij.internal.statistic.eventLog.events.EventId2;
import com.intellij.internal.statistic.eventLog.events.StringEventField;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.internal.statistic.utils.StatisticsUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.run.InfraAdditionalParameter;
import cn.taketoday.assistant.app.run.InfraApplicationConfigurationType;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.assistant.app.run.lifecycle.beans.tab.BeansEndpointTabSettings;
import cn.taketoday.assistant.app.run.lifecycle.beans.tab.LiveBeansPanelContent;
import cn.taketoday.assistant.app.run.update.InfraApplicationUpdatePolicy;
import cn.taketoday.lang.Nullable;

final class InfraRunFeatureUsagesCollector extends ProjectUsagesCollector {

  private static final List<Integer> steps = Arrays.asList(1, 2, 3, 4, 5, 10, 100, 200, 500, 1000);
  private static final EventLogGroup GROUP = new EventLogGroup("infra.boot.run", 4);
  private static final EventId1<Boolean> CONFIG_ACTIVE_PROFILES_SET = GROUP.registerEvent("config.active.profiles.set", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_HIDE_BANNER = GROUP.registerEvent("config.hide.banner", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_DEBUG_MODE = GROUP.registerEvent("config.debug.mode", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_ENABLE_LAUNCH_OPTIMIZATION = GROUP.registerEvent("config.enable.launch.optimization", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_ENABLE_JMX_AGENT = GROUP.registerEvent("config.enable.jmx.agent", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_LOG_FILES = GROUP.registerEvent("config.log.files", EventFields.Enabled);
  private static final EventId1<Boolean> RUN_DASHBOARD = GROUP.registerEvent("run.dashboard", EventFields.Enabled);
  private static final EventId1<Boolean> ENDPOINTS_BEANS_DIAGRAM = GROUP.registerEvent("endpoints.beans.diagram", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_VM_OPTIONS = GROUP.registerEvent("config.vm.options", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_PROGRAM_ARGUMENTS = GROUP.registerEvent("config.program.arguments", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_WORKING_DIRECTORY = GROUP.registerEvent("config.working.directory", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_ENVIRONMENT_VARIABLES = GROUP.registerEvent("config.environment.variables", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_INCLUDE_PROVIDED_SCOPE = GROUP.registerEvent("config.include.provided.scope", EventFields.Enabled);
  private static final EventId1<Boolean> CONFIG_ALTERNATIVE_JRE_PATH_ENABLED = GROUP.registerEvent("config.alternative.jre.path.enabled", EventFields.Enabled);
  private static final EventField<String> COUNT_GROUP = new StringEventField("count_group") {

    public List<String> getValidationRule() {
      return List.of("{regexp#count}", "{enum:<1}");
    }
  };
  private static final EventId2<Integer, String> SPRING_BOOT_RUN_CONFIGS = GROUP.registerEvent("infra.run.configs", EventFields.Count, COUNT_GROUP);
  public static final StringEventField UPDATE_POLICY = EventFields.String("value",
          List.of("Nothing", "third.party", "UpdateClassesAndResources", "UpdateClassesAndTriggerFile", "UpdateResources", "UpdateTriggerFile"));
  private static final EventId1<String> CONFIG_UPDATE_ACTION_UPDATE_POLICY = GROUP.registerEvent("config.update.action.update.policy", UPDATE_POLICY);
  private static final EventId1<String> CONFIG_FRAME_DEACTIVATION_UPDATE_POLICY = GROUP.registerEvent("config.frame.deactivation.update.policy", UPDATE_POLICY);
  private static final EventId2<Integer, String> CONFIG_ADDITIONAL_PARAMS_TOTAL = GROUP.registerEvent("config.additional.params.total", EventFields.Count, COUNT_GROUP);
  private static final EventId1<Long> CONFIG_ADDITIONAL_PARAMS_ENABLED = GROUP.registerEvent("config.additional.params.enabled", EventFields.Long("params"));
  private static final EventId1<Long> CONFIG_ADDITIONAL_PARAMS_DISABLED = GROUP.registerEvent("config.additional.params.disabled", EventFields.Long("params"));
  private static final EventId2<Integer, String> CONFIGS_MAIN_CLASS = GROUP.registerEvent("configs.main.class", EventFields.Count, COUNT_GROUP);
  private static final EventId1<ShortenCommandLine> CONFIG_SHORTEN_COMMAND_LINE = GROUP.registerEvent("config.shorten.command.line", EventFields.Enum("value", ShortenCommandLine.class));

  InfraRunFeatureUsagesCollector() {
  }

  public EventLogGroup getGroup() {
    return GROUP;
  }

  protected boolean requiresReadAccess() {
    return true;
  }

  public Set<MetricEvent> getMetrics(Project project) {
    if (!InfraLibraryUtil.hasFrameworkLibrary(project)) {
      return Collections.emptySet();
    }
    Set<MetricEvent> metrics = new LinkedHashSet<>();
    List<RunnerAndConfigurationSettings> runnerAndConfigurationSettings = RunManager.getInstance(project).getConfigurationSettingsList(InfraApplicationConfigurationType.of());
    metrics.add(SPRING_BOOT_RUN_CONFIGS.metric(runnerAndConfigurationSettings.size(), StatisticsUtil.INSTANCE.getCountingStepName(runnerAndConfigurationSettings.size(), steps)));
    Map<String, Integer> mainClassCounter = new HashMap<>();
    for (RunnerAndConfigurationSettings settings : runnerAndConfigurationSettings) {
      ProgressManager.checkCanceled();
      RunConfiguration configuration = settings.getConfiguration();
      if (configuration instanceof InfraApplicationRunConfiguration sbRunConfig) {
        mainClassCounter.merge(sbRunConfig.getInfraMainClass(), 1, (integer, integer2) -> {
          return integer + 1;
        });
        metrics.add(CONFIG_ACTIVE_PROFILES_SET.metric(StringUtil.isNotEmpty(sbRunConfig.getActiveProfiles())));
        metrics.add(CONFIG_HIDE_BANNER.metric(sbRunConfig.isHideBanner()));
        metrics.add(CONFIG_DEBUG_MODE.metric(sbRunConfig.isDebugMode()));
        metrics.add(CONFIG_ENABLE_LAUNCH_OPTIMIZATION.metric(sbRunConfig.isEnableLaunchOptimization()));
        metrics.add(CONFIG_ENABLE_JMX_AGENT.metric(sbRunConfig.isEnableJmxAgent()));
        metrics.add(CONFIG_UPDATE_ACTION_UPDATE_POLICY.metric(getUpdatePolicyDescriptor(sbRunConfig.getUpdateActionUpdatePolicy())));
        metrics.add(CONFIG_FRAME_DEACTIVATION_UPDATE_POLICY.metric(getUpdatePolicyDescriptor(sbRunConfig.getFrameDeactivationUpdatePolicy())));
        List<InfraAdditionalParameter> parameters = sbRunConfig.getAdditionalParameters();
        int additionalParamsSize = parameters.size();
        metrics.add(CONFIG_ADDITIONAL_PARAMS_TOTAL.metric(Integer.valueOf(additionalParamsSize), StatisticsUtil.INSTANCE.getCountingStepName(additionalParamsSize, steps)));
        if (additionalParamsSize > 0) {
          long enabledParams = parameters.stream().filter(InfraAdditionalParameter::isEnabled).count();
          long disabledParams = parameters.size() - enabledParams;
          metrics.add(CONFIG_ADDITIONAL_PARAMS_ENABLED.metric(enabledParams));
          metrics.add(CONFIG_ADDITIONAL_PARAMS_DISABLED.metric(disabledParams));
        }
        addConfigEnvironmentMetrics(metrics, sbRunConfig);
        metrics.add(CONFIG_LOG_FILES.metric(!sbRunConfig.getLogFiles().isEmpty()));
      }
    }
    boolean showRunDashboard = RunDashboardManager.getInstance(project).getTypes().contains(InfraApplicationConfigurationType.of().getId());
    metrics.add(RUN_DASHBOARD.metric(showRunDashboard));
    for (Integer integer3 : mainClassCounter.values()) {
      metrics.add(CONFIGS_MAIN_CLASS.metric(integer3, StatisticsUtil.INSTANCE.getCountingStepName(integer3, steps)));
    }
    metrics.add(ENDPOINTS_BEANS_DIAGRAM.metric(
            LiveBeansPanelContent.EP_NAME.getExtensions().length != 0 && BeansEndpointTabSettings.getInstance(project).isDiagramMode()));
    return metrics;
  }

  private static String getUpdatePolicyDescriptor(@Nullable InfraApplicationUpdatePolicy policy) {
    return policy == null ? "Nothing" : PluginInfoDetectorKt.getPluginInfo(policy.getClass()).isDevelopedByJetBrains() ? policy.getId() : "third.party";
  }

  private static void addConfigEnvironmentMetrics(Set<MetricEvent> metrics, InfraApplicationRunConfiguration sbRunConfig) {
    metrics.add(CONFIG_VM_OPTIONS.metric(StringUtil.isNotEmpty(sbRunConfig.getVMParameters())));
    metrics.add(CONFIG_PROGRAM_ARGUMENTS.metric(StringUtil.isNotEmpty(sbRunConfig.getProgramParameters())));
    metrics.add(CONFIG_WORKING_DIRECTORY.metric(StringUtil.isNotEmpty(sbRunConfig.getWorkingDirectory())));
    metrics.add(CONFIG_ENVIRONMENT_VARIABLES.metric(!sbRunConfig.getEnvs().isEmpty()));
    metrics.add(CONFIG_INCLUDE_PROVIDED_SCOPE.metric(sbRunConfig.isProvidedScopeIncluded()));
    metrics.add(CONFIG_ALTERNATIVE_JRE_PATH_ENABLED.metric(sbRunConfig.isAlternativeJrePathEnabled()));
    ShortenCommandLine shortenCommandLine = sbRunConfig.getShortenCommandLine();
    if (shortenCommandLine == null) {
      shortenCommandLine = ShortenCommandLine.NONE;
    }
    metrics.add(CONFIG_SHORTEN_COMMAND_LINE.metric(shortenCommandLine));
  }
}

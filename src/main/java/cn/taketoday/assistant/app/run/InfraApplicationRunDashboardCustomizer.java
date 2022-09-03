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

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardCustomizer;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.xdebugger.XDebugSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManagerImpl;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationUrlUtil;
import cn.taketoday.lang.Nullable;

final class InfraApplicationRunDashboardCustomizer extends RunDashboardCustomizer {

  private static final String DEVTOOLS_TAIL = " [devtools]";

  public boolean isApplicable(RunnerAndConfigurationSettings settings, @Nullable RunContentDescriptor descriptor) {
    return settings.getConfiguration() instanceof InfraApplicationRunConfigurationBase;
  }

  public boolean updatePresentation(PresentationData presentation, RunDashboardRunConfigurationNode node) {
    RunConfiguration configuration = node.getConfigurationSettings().getConfiguration();
    if (!(configuration instanceof InfraApplicationRunConfigurationBase configurationBase)) {
      return false;
    }
    if (!DumbService.getInstance(node.getProject()).isDumb()) {
      ApplicationManager.getApplication().runReadAction(() -> {
        if (hasDevtools(configurationBase)) {
          presentation.addText(DEVTOOLS_TAIL, SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
      });
    }
    InfraApplicationInfo info = null;
    RunContentDescriptor descriptor = node.getDescriptor();
    if (descriptor != null && descriptor.getProcessHandler() != null) {
      ProcessHandler handler = descriptor.getProcessHandler();
      if (!handler.isProcessTerminated()) {
        info = InfraApplicationLifecycleManager.from(node.getProject()).getInfraApplicationInfo(handler);
      }
    }
    if (info != null) {
      if (Boolean.FALSE.equals(info.getReadyState().getValue())) {
        XDebugSession session = InfraApplicationLifecycleManagerImpl.getDebugSession(node.getProject(), descriptor.getProcessHandler());
        if (session == null || !session.isPaused()) {
          presentation.setIcon(AnimatedIcon.Default.INSTANCE);
        }
        else {
          presentation.setIcon(AllIcons.Process.Step_4);
        }
      }
      Integer serverPort = info.getServerPort().getValue();
      if (serverPort != null && serverPort > 0) {
        presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        String path = configurationBase.getUrlPath();
        String applicationLink = ":" + serverPort + (StringUtil.isEmpty(path) ? "/" : path);
        presentation.addText(applicationLink, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        Map<Object, Object> links = new HashMap<>();
        links.put(applicationLink, new ApplicationLinkListener(info, configurationBase));
        node.putUserData(NODE_LINKS, links);
        return true;
      }
      node.putUserData(NODE_LINKS, null);
      return true;
    }
    node.putUserData(NODE_LINKS, null);
    return true;
  }

  private static boolean hasDevtools(InfraApplicationRunConfigurationBase springBootRunConfiguration) {
    Module module = springBootRunConfiguration.getModule();
    if (module == null || module.isDisposed()) {
      return false;
    }
    Project project = module.getProject();
    DevtoolsState state = CachedValuesManager.getManager(project).getCachedValue(module, () -> {
      DevtoolsState result = new DevtoolsState(project);
      ReadAction.nonBlocking(() -> {
        return JavaPsiFacade.getInstance(project)
                .findClass(InfraClassesConstants.REMOTE_SPRING_APPLICATION, GlobalSearchScope.moduleRuntimeScope(module, false));
      }).finishOnUiThread(ModalityState.defaultModalityState(), psiClass -> {
        result.setDevtools(psiClass != null);
      }).inSmartMode(project).coalesceBy(new Object[] { module }).expireWith(module).submit(NonUrgentExecutor.getInstance());
      return CachedValueProvider.Result.createSingleDependency(result, PsiModificationTracker.MODIFICATION_COUNT);
    });
    AppUIUtil.invokeOnEdt(() -> {
      state.addConfiguration(springBootRunConfiguration);
    });
    return state.hasDevtools();
  }

  private static class ApplicationLinkListener implements Runnable {
    final InfraApplicationInfo myLinkInfo;
    final InfraApplicationRunConfigurationBase myConfiguration;

    ApplicationLinkListener(InfraApplicationInfo linkInfo, InfraApplicationRunConfigurationBase configuration) {
      this.myLinkInfo = linkInfo;
      this.myConfiguration = configuration;
    }

    @Override
    public void run() {
      String applicationUrl = this.myLinkInfo.getApplicationUrl().getValue();
      if (applicationUrl != null) {
        InfraApplicationUrlUtil urlUtil = InfraApplicationUrlUtil.getInstance();
        String mappingPath = this.myConfiguration.getUrlPath();
        String servletPath = urlUtil.getServletPath(this.myLinkInfo, mappingPath);
        BrowserUtil.browse(urlUtil.getMappingUrl(applicationUrl, servletPath, mappingPath));
      }
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o != null && getClass() == o.getClass()) {
        return this.myLinkInfo.equals(((ApplicationLinkListener) o).myLinkInfo);
      }
      return false;
    }

    public int hashCode() {
      return this.myLinkInfo.hashCode();
    }
  }

  private static class DevtoolsState {
    private final Project myProject;
    private volatile boolean myDevtools;
    private Set<RunConfiguration> myConfigurations = new HashSet();

    DevtoolsState(Project project) {
      this.myProject = project;
    }

    boolean hasDevtools() {
      return this.myDevtools;
    }

    public void setDevtools(boolean hasDevtools) {
      this.myDevtools = hasDevtools;
      for (RunConfiguration configuration : this.myConfigurations) {
        this.myProject.getMessageBus().syncPublisher(RunDashboardManager.DASHBOARD_TOPIC).configurationChanged(configuration, false);
      }
      this.myConfigurations = null;
    }

    public void addConfiguration(RunConfiguration configuration) {
      if (this.myConfigurations != null) {
        this.myConfigurations.add(configuration);
      }
    }
  }
}

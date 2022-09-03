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

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.diagnostic.logging.LogConsoleManagerBase;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.Executor;
import com.intellij.execution.ExternalizablePath;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.ShortenCommandLine;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.AdditionalTabComponentManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.junit.RefactoringListeners;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.target.TargetEnvironmentConfigurations;
import com.intellij.execution.target.java.JavaLanguageRuntimeConfiguration;
import com.intellij.execution.target.java.JavaLanguageRuntimeType;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.assistant.app.run.editor.ApplicationRunConfigurationEditor;
import cn.taketoday.assistant.app.run.editor.ApplicationRunConfigurationFragmentedEditor;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.tabs.ApplicationEndpointsTab;
import cn.taketoday.assistant.app.run.update.InfraApplicationUpdatePolicy;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.profiles.InfraProfileTarget;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

public final class InfraApplicationRunConfiguration extends ApplicationConfiguration implements InfraApplicationRunConfigurationBase {

  public InfraApplicationRunConfiguration(Project project, ConfigurationFactory factory, String name) {
    super(name, project, factory);
  }

  public Collection<Module> getValidModules() {
    return JavaRunConfigurationModule.getModulesForClass(getProject(), getInfraMainClass());
  }

  public String suggestedName() {
    String mainClass = getOptions().getMainClassName();
    if (mainClass == null) {
      return null;
    }
    return JavaExecutionUtil.getPresentableClassName(mainClass);
  }

  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    if (Registry.is("ide.new.run.config", true)) {
      return new ApplicationRunConfigurationFragmentedEditor(this);
    }
    SettingsEditorGroup<InfraApplicationRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new ApplicationRunConfigurationEditor(getProject()));
    JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group);
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel());
    return group;
  }

  public void checkConfiguration() throws RuntimeConfigurationException {
    getConfigurationModule().checkForWarning();
    checkClass();
    if (TargetEnvironmentConfigurations.getEffectiveTargetName(this, getProject()) == null) {
      JavaParametersUtil.checkAlternativeJRE(this);
    }
    ProgramParametersUtil.checkWorkingDirectoryExist(this, getProject(), getModule());
    JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
    checkAdditionalParams();
    checkUpdateActionUpdatePolicy();
    checkFrameDeactivationUpdatePolicy();
    validateRunTarget(getProject());
  }

  public JavaRunConfigurationModule checkClass() throws RuntimeConfigurationException {
    JavaRunConfigurationModule configurationModule = getConfigurationModule();
    PsiClass mainClass = configurationModule.checkClassName(
            getOptions().getMainClassName(),
            message("infra.application.run.configuration.class.not.specified"));
    if (!InfraApplicationService.of().isInfraApplication(mainClass)) {
      throw new RuntimeConfigurationException(message("infra.application.run.configuration.invalid.class"));
    }
    if (!InfraApplicationService.of().hasMainMethod(mainClass)) {
      throw new RuntimeConfigurationException(ExecutionBundle.message("main.method.not.found.in.class.error.message", getOptions().getMainClassName()));
    }
    return configurationModule;
  }

  public void checkAdditionalParams() throws RuntimeConfigurationException {
    List<InfraAdditionalParameter> parameters = getOptions().getAdditionalParameter();
    for (int i = 0; i < parameters.size(); i++) {
      InfraAdditionalParameter parameter = parameters.get(i);
      if (StringUtil.isEmptyOrSpaces(parameter.getName())) {
        int index = i;
        throw new RuntimeConfigurationError(message("infra.application.run.configuration.invalid.parameter", i + 1), () -> {
          parameters.remove(index);
        });
      }
    }
  }

  public void checkUpdateActionUpdatePolicy() throws RuntimeConfigurationException {
    InfraApplicationUpdatePolicy policy;
    if (!DumbService.isDumb(getProject()) && (policy = getUpdateActionUpdatePolicy()) != null && !policy.isAvailableForConfiguration(this)) {
      throw new RuntimeConfigurationWarning(message("infra.application.run.configuration.policy.not.available.on.update.action", policy.getName()));
    }
  }

  public void checkFrameDeactivationUpdatePolicy() throws RuntimeConfigurationException {
    InfraApplicationUpdatePolicy policy = getFrameDeactivationUpdatePolicy();
    if (policy != null && !policy.isAvailableForConfiguration(this)) {
      throw new RuntimeConfigurationWarning(message("infra.application.run.configuration.policy.not.available.on.frame.deactivation", policy.getName()));
    }
  }

  public RunProfileState getState(Executor executor, ExecutionEnvironment environment) {
    InfraCommandLineState infraCommandLineState = new InfraCommandLineState(this, environment);
    JavaRunConfigurationModule module = getConfigurationModule();
    infraCommandLineState.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), module.getSearchScope()));
    return infraCommandLineState;
  }

  public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
    RefactoringElementListener listener = RefactoringListeners.getClassOrPackageListener(element, new RefactoringListeners.SingleClassConfigurationAccessor(this));
    if (listener != null) {
      return RunConfigurationExtension.wrapRefactoringElementListener(element, this, listener);
    }
    return getProfileListener(element);
  }

  private RefactoringElementListener getProfileListener(PsiElement element) {
    String profileName = getProfileName(element);
    if (profileName != null && ProfileUtils.profilesFromString(getActiveProfiles()).contains(profileName)) {
      return new InfraProfileRefactoringListener(profileName);
    }
    return null;
  }

  public void setMainClass(PsiClass psiClass) {
    Module originalModule = getConfigurationModule().getModule();
    setMainClassName(JavaExecutionUtil.getRuntimeQualifiedName(psiClass));
    setModule(JavaExecutionUtil.findModule(psiClass));
    restoreOriginalModule(originalModule);
  }

  @Override
  @Nullable
  public PsiClass getMainClass() {
    return InfraApplicationService.of().findMainClassCandidate(getConfigurationModule().findClass(getInfraMainClass()));
  }

  public void setMainClassName(String qualifiedName) {
    setInfraMainClass(qualifiedName);
  }

  @Nullable
  public String getRunClass() {
    PsiClass mainClass = getMainClass();
    return mainClass != null ? mainClass.getQualifiedName() : getInfraMainClass();
  }

  @Nullable
  public String getPackage() {
    return null;
  }

  public void setWorkingDirectory(@Nullable String value) {
    getOptions().setWorkingDirectory(ExternalizablePath.urlValue(value));
  }

  public String getWorkingDirectory() {
    return ExternalizablePath.localPathValue(getOptions().getWorkingDirectory());
  }

  @Override
  public Module getModule() {
    return getConfigurationModule().getModule();
  }

  @Override
  public GlobalSearchScope getSearchScope() {
    return getConfigurationModule().getSearchScope();
  }

  @Override
  public void setInfraMainClass(String mainClass) {
    getOptions().setMainClassName(mainClass);
  }

  @Override
  public String getInfraMainClass() {
    return getOptions().getMainClassName();
  }

  public boolean isDebugMode() {
    return getOptions().getDebugMode();
  }

  public void setDebugMode(boolean value) {
    getOptions().setDebugMode(value);
  }

  public boolean isEnableLaunchOptimization() {
    return getOptions().getEnableLaunchOptimization();
  }

  public void setEnableLaunchOptimization(boolean value) {
    getOptions().setEnableLaunchOptimization(value);
  }

  public boolean isHideBanner() {
    return getOptions().getHideBanner();
  }

  public void setHideBanner(boolean value) {
    getOptions().setHideBanner(value);
  }

  public boolean isEnableJmxAgent() {
    return getOptions().getEnableJmxAgent();
  }

  public void setEnableJmxAgent(boolean value) {
    getOptions().setEnableJmxAgent(value);
  }

  public List<InfraAdditionalParameter> getAdditionalParameters() {
    return getOptions().getAdditionalParameter();
  }

  public void setAdditionalParameters(List<InfraAdditionalParameter> values) {
    List<InfraAdditionalParameter> copy = new ArrayList<>();
    for (InfraAdditionalParameter value : values) {
      copy.add(new InfraAdditionalParameter(value.isEnabled(), value.getName(), value.getValue()));
    }
    getOptions().setAdditionalParameter(copy);
  }

  public String getActiveProfiles() {
    return getOptions().getActiveProfiles();
  }

  public void setActiveProfiles(String value) {
    getOptions().setActiveProfiles(value);
  }

  @Override
  public String getUrlPath() {
    return getOptions().getUrlPath();
  }

  @Override
  public void setUrlPath(String value) {
    getOptions().setUrlPath(value);
  }

  public InfraApplicationUpdatePolicy getUpdateActionUpdatePolicy() {
    return InfraApplicationUpdatePolicy.findPolicy(getOptions().getUpdateActionUpdatePolicy());
  }

  public void setUpdateActionUpdatePolicy(InfraApplicationUpdatePolicy updatePolicy) {
    getOptions().setUpdateActionUpdatePolicy(updatePolicy == null ? null : updatePolicy.getId());
  }

  public InfraApplicationUpdatePolicy getFrameDeactivationUpdatePolicy() {
    return InfraApplicationUpdatePolicy.findPolicy(getOptions().getFrameDeactivationUpdatePolicy());
  }

  public void setFrameDeactivationUpdatePolicy(InfraApplicationUpdatePolicy updatePolicy) {
    getOptions().setFrameDeactivationUpdatePolicy(updatePolicy == null ? null : updatePolicy.getId());
  }

  @Nullable
  public ShortenCommandLine getShortenCommandLine() {
    return getOptions().getShortenCommandLine();
  }

  public void setShortenCommandLine(@Nullable ShortenCommandLine mode) {
    getOptions().setShortenCommandLine(mode);
  }

  public void createAdditionalTabComponents(AdditionalTabComponentManager manager, ProcessHandler startedProcess) {
    if (InfraApplicationLifecycleManager.from(getProject()).isLifecycleManagementEnabled(startedProcess) && (manager instanceof LogConsoleManagerBase)) {
      InfraApplicationRunConfiguration clone = (InfraApplicationRunConfiguration) clone();
      ApplicationEndpointsTab tab = new ApplicationEndpointsTab(clone, startedProcess);
      Content content = ((LogConsoleManagerBase) manager).addAdditionalTabComponent(tab, message("infra.application.endpoints.tab.title"),
              InfraRunIcons.SpringBootEndpoint, false);
      ContentManager contentManager = content.getManager();
      if (contentManager == null) {
        return;
      }
      ContentManagerListener listener = new ContentManagerListener() {

        public void selectionChanged(ContentManagerEvent event) {
          if (event.getContent() == content && event.getOperation() == ContentManagerEvent.ContentOperation.add) {
            tab.setSelected();
          }
        }
      };
      contentManager.addContentManagerListener(listener);
      Disposer.register(tab, new Disposable() {
        public void dispose() {
          contentManager.removeContentManagerListener(listener);
        }
      });
    }
  }

  public boolean canRunOn(TargetEnvironmentConfiguration target) {
    return target.getRuntimes().findByType(JavaLanguageRuntimeConfiguration.class) != null;
  }

  @Nullable
  public LanguageRuntimeType<?> getDefaultLanguageRuntimeType() {
    return LanguageRuntimeType.EXTENSION_NAME.findExtension(JavaLanguageRuntimeType.class);
  }

  @Nullable
  public String getDefaultTargetName() {
    return getOptions().getRemoteTarget();
  }

  public void setDefaultTargetName(@Nullable String targetName) {
    getOptions().setRemoteTarget(targetName);
  }

  private static String getProfileName(PsiElement psiElement) {
    if (!(psiElement instanceof PomTargetPsiElement)) {
      return null;
    }
    PomTarget target = ((PomTargetPsiElement) psiElement).getTarget();
    if (target instanceof InfraProfileTarget profileTarget) {
      return profileTarget.getName();
    }
    return null;
  }

  public InfraApplicationRunConfigurationOptions getOptions() {
    return (InfraApplicationRunConfigurationOptions) super.getOptions();
  }

  private class InfraProfileRefactoringListener implements RefactoringElementListener {
    private final String profile;

    InfraProfileRefactoringListener(String profile) {
      this.profile = profile;
    }

    public void elementMoved(PsiElement newElement) {
    }

    public void elementRenamed(PsiElement newElement) {
      String newName = getProfileName(newElement);
      if (StringUtil.isEmptyOrSpaces(newName)) {
        return;
      }
      String profiles = InfraApplicationRunConfiguration.this.getActiveProfiles();
      StringBuilder result = new StringBuilder();
      int i = 0;
      while (true) {
        int partStart = i;
        if (partStart < profiles.length()) {
          int partEnd = profiles.indexOf(44, partStart);
          if (partEnd < 0) {
            partEnd = profiles.length();
          }
          String part = profiles.substring(partStart, partEnd);
          if (part.trim().equals(this.profile)) {
            result.append(part.replace(this.profile, newName));
          }
          else {
            result.append(part);
          }
          if (partEnd != profiles.length()) {
            result.append(',');
          }
          i = partEnd + 1;
        }
        else {
          setActiveProfiles(result.toString());
          return;
        }
      }
    }
  }
}

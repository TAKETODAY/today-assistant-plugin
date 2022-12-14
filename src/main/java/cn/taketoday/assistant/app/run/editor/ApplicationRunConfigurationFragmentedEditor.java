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

package cn.taketoday.assistant.app.run.editor;

import com.intellij.application.options.ModulesCombo;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.diagnostic.logging.LogsGroupFragment;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.ShortenCommandLine;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ClassEditorField;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.ui.BeforeRunComponent;
import com.intellij.execution.ui.BeforeRunFragment;
import com.intellij.execution.ui.CommandLinePanel;
import com.intellij.execution.ui.CommonJavaFragments;
import com.intellij.execution.ui.CommonParameterFragments;
import com.intellij.execution.ui.CommonTags;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.FragmentedSettingsUtil;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.execution.ui.ModuleClasspathCombo;
import com.intellij.execution.ui.RunConfigurationFragmentedEditor;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.execution.ui.SettingsEditorFragmentType;
import com.intellij.execution.ui.ShortenCommandLineModeCombo;
import com.intellij.execution.ui.TagButton;
import com.intellij.execution.ui.TargetPathFragment;
import com.intellij.execution.ui.VariantTagFragment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.macro.MacrosDialog;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.TableUtil;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.SmartList;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.assistant.app.run.InfraAdditionalParameter;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.assistant.app.run.editor.ApplicationRunConfigurationEditor.InfraClassBrowser;
import cn.taketoday.assistant.app.run.update.InfraApplicationUpdatePolicy;
import cn.taketoday.assistant.profiles.InfraProfileCompletionProvider;
import cn.taketoday.lang.Nullable;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;
import static cn.taketoday.assistant.app.run.editor.ApplicationRunConfigurationFragmentedEditorKt.LAUNCH_OPTIMIZATION_13_DESCRIPTION;
import static cn.taketoday.assistant.app.run.editor.ApplicationRunConfigurationFragmentedEditorKt.LAUNCH_OPTIMIZATION_DESCRIPTION;

public final class ApplicationRunConfigurationFragmentedEditor extends RunConfigurationFragmentedEditor<InfraApplicationRunConfiguration> {
  private final SmartList<Consumer<JrePathEditor.JreComboBoxItem>> jrePathListeners;

  public ApplicationRunConfigurationFragmentedEditor(InfraApplicationRunConfiguration runConfiguration) {
    super(runConfiguration, JavaRunConfigurationExtensionManager.Companion.getInstance());
    this.jrePathListeners = new SmartList<>();
  }

  protected List<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> createRunFragments() {
    SettingsEditorFragment<InfraApplicationRunConfiguration, ModuleClasspathCombo> moduleClasspath = CommonJavaFragments.moduleClasspath();
    ModuleClasspathCombo moduleClasspathCombo = moduleClasspath.component();
    moduleClasspath.setValidation(new Function<InfraApplicationRunConfiguration, List<ValidationInfo>>() {
      @Override
      public List<ValidationInfo> apply(InfraApplicationRunConfiguration it) {
        return List.of(RuntimeConfigurationException.validate(moduleClasspathCombo, () -> {
          it.getConfigurationModule().checkForWarning();
        }));
      }
    });

    Consumer<Consumer<Module>> moduleListenerHolder = consumer -> {
      moduleClasspathCombo.addActionListener(event -> consumer.accept(moduleClasspathCombo.getSelectedModule()));
    };

    var fragments = new ArrayList<>(createInfraFragments(moduleListenerHolder));
    fragments.add(CommonTags.parallelRun());
    fragments.addAll(createBeforeRunFragments());
    fragments.add(new LogsGroupFragment<>());
    fragments.addAll(createEnvironmentFragments(moduleClasspath, moduleListenerHolder));
    fragments.add(new TargetPathFragment<>());
    fragments.add(createAdditionalParamsFragment(moduleListenerHolder));
    return fragments;
  }

  public void targetChanged(@Nullable String targetName) {
    super.targetChanged(targetName);
    SettingsEditorFragment editorFragment = null;
    for (var next : getFragments()) {
      if (Intrinsics.areEqual("jrePath", next.getId())) {
        editorFragment = next;
        break;
      }
    }

    if (editorFragment != null) {
      JrePathEditor component = (JrePathEditor) editorFragment.component();
      if (!component.updateModel(getProject(), targetName)) {
        return;
      }
      editorFragment.resetFrom(this.mySettings);
    }
  }

  public boolean isInplaceValidationSupported() {
    return true;
  }

  private List<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> createBeforeRunFragments() {
    var fragments = new ArrayList<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>>();
    BeforeRunComponent beforeRunComponent = new BeforeRunComponent(this);
    fragments.add(BeforeRunFragment.createBeforeRun(beforeRunComponent, CompileStepBeforeRun.ID));
    fragments.addAll(BeforeRunFragment.createGroup());
    fragments.add(CommonJavaFragments.createBuildBeforeRun(beforeRunComponent, this));
    return fragments;
  }

  private List<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> createEnvironmentFragments(
          SettingsEditorFragment<InfraApplicationRunConfiguration, ModuleClasspathCombo> settingsEditorFragment, Consumer<Consumer<Module>> consumer) {
    ArrayList<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> fragments = new ArrayList<>();
    ModuleClasspathCombo moduleClasspathCombo = settingsEditorFragment.component();

    Computable<Boolean> hasModule = () -> moduleClasspathCombo.getSelectedModule() != null;

    SettingsEditorFragment<InfraApplicationRunConfiguration, EditorTextField> mainClass = createMainClass(moduleClasspathCombo);
    DefaultJreSelector.Companion companion = DefaultJreSelector.Companion;
    EditorTextField component = mainClass.component();
    SettingsEditorFragment jrePath = CommonJavaFragments.createJrePath(companion.fromSourceRootsDependencies(moduleClasspathCombo, component));
    JComponent editorComponent = jrePath.getEditorComponent();
    if (editorComponent == null) {
      throw new NullPointerException("null cannot be cast to non-null type com.intellij.openapi.ui.ComboBox<*>");
    }
    ComboBox jrePathCombobox = (ComboBox) editorComponent;
    jrePathCombobox.addItemListener(event -> {
      SmartList<Consumer<JrePathEditor.JreComboBoxItem>> smartList;
      if (event.getStateChange() == ItemEvent.SELECTED) {
        smartList = ApplicationRunConfigurationFragmentedEditor.this.jrePathListeners;
        for (Consumer<JrePathEditor.JreComboBoxItem> jrePathListener : smartList) {
          JrePathEditor.JreComboBoxItem item = (JrePathEditor.JreComboBoxItem) event.getItem();
          if (item == null) {
            throw new NullPointerException("null cannot be cast to non-null type com.intellij.execution.ui.JrePathEditor.JreComboBoxItem");
          }
          jrePathListener.accept(item);
        }
      }
    });
    consumer.accept(it -> {
      for (Consumer<JrePathEditor.JreComboBoxItem> jrePathListener : jrePathListeners) {
        JrePathEditor.JreComboBoxItem selectedItem = (JrePathEditor.JreComboBoxItem) jrePathCombobox.getSelectedItem();
        if (selectedItem == null) {
          throw new NullPointerException("null cannot be cast to non-null type com.intellij.execution.ui.JrePathEditor.JreComboBoxItem");
        }
        jrePathListener.accept(selectedItem);
      }
    });
    fragments.add(jrePath);
    fragments.add(settingsEditorFragment);
    fragments.add(mainClass);
    fragments.add(CommonJavaFragments.vmOptions(hasModule));
    fragments.add(createProgramArguments(hasModule));
    fragments.add(SettingsEditorFragment.createTag("include.provided", ExecutionBundle.message("application.configuration.include.provided.scope"),
            ExecutionBundle.message("group.java.options"), ApplicationConfiguration::isProvidedScopeIncluded,
            new BiConsumer<InfraApplicationRunConfiguration, Boolean>() {
              @Override
              public void accept(InfraApplicationRunConfiguration configuration, Boolean value) {
                configuration.setIncludeProvidedScope(value);
              }
            }));
    JComponent component2 = jrePath.component();
    fragments.add(createShortenClasspath(moduleClasspathCombo, (JrePathEditor) component2, mainClass.component()));
    fragments.add(createWorkingDirectory(moduleClasspathCombo, hasModule));
    fragments.add(createEnvParameters());
    return fragments;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, EditorTextField> createMainClass(ModulesCombo modulesCombo) {
    InfraConfigurationModuleSelector moduleSelector = new InfraConfigurationModuleSelector(
            this, modulesCombo, getProject(), modulesCombo);
    JavaCodeFragment.VisibilityChecker visibilityChecker = (declaration, element) -> {
      if ((declaration instanceof PsiClass) && InfraApplicationService.of().isInfraApplication((PsiClass) declaration) && InfraApplicationService.of()
              .hasMainMethod((PsiClass) declaration)) {
        return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE;
      }
      return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE;
    };

    ClassEditorField classEditorField = ClassEditorField.createClassField(getProject(), null, visibilityChecker,
            new InfraClassBrowser(getProject(), moduleSelector));
    classEditorField.setBackground(UIUtil.getTextFieldBackground());
    classEditorField.setShowPlaceholderWhenFocused(true);
    CommonParameterFragments.setMonospaced(classEditorField);
    String placeholder = ExecutionBundle.message("application.configuration.main.class.placeholder");
    classEditorField.setPlaceholder(placeholder);
    AccessibleContext accessibleContext = classEditorField.getAccessibleContext();
    accessibleContext.setAccessibleName(placeholder);
    CommandLinePanel.setMinimumWidth(classEditorField, 300);
    var mainClassFragment = new SettingsEditorFragment("mainClass", ExecutionBundle.message("application.configuration.main.class"), null, classEditorField,
            20, new BiConsumer<InfraApplicationRunConfiguration, EditorTextField>() {
      @Override
      public void accept(InfraApplicationRunConfiguration configuration, EditorTextField component) {
        String mainClass = configuration.getInfraMainClass();
        if (mainClass != null) {
          mainClass = StringsKt.replace(mainClass, '$', '.', false);
          component.setText(mainClass);
        }
      }
    }, new BiConsumer<InfraApplicationRunConfiguration, EditorTextField>() {
      @Override
      public void accept(InfraApplicationRunConfiguration configuration, EditorTextField component) {
        String className = component.getText();
        PsiClass aClass = moduleSelector.findClass(className);
        configuration.setInfraMainClass(aClass != null ? JavaExecutionUtil.getRuntimeQualifiedName(aClass) : className);
      }
    }, (Predicate<?>) o -> true);

    mainClassFragment.setRemovable(false);
    mainClassFragment.setEditorGetter(new Function<EditorTextField, JComponent>() {
      @Override
      public JComponent apply(EditorTextField it) {
        Editor editor = it.getEditor();
        if (editor != null) {
          return editor.getContentComponent();
        }
        return it;
      }
    });
    mainClassFragment.setValidation(new Function<InfraApplicationRunConfiguration, List<ValidationInfo>>() {
      @Override
      public List<ValidationInfo> apply(InfraApplicationRunConfiguration it) {
        return List.of(RuntimeConfigurationException.validate(classEditorField, it::checkClass));
      }
    });
    classEditorField.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(DocumentEvent event) {
        Project project = getProject();
        DumbService dumbService = DumbService.getInstance(project);
        if (dumbService.isDumb()) {
          return;
        }
        String text = classEditorField.getText();
        if (moduleSelector.getModule() != null && moduleSelector.findClass(text) != null) {
          return;
        }
        JavaRunConfigurationModule configurationModule = new JavaRunConfigurationModule(project, false);
        PsiElement findClass = configurationModule.findClass(text);
        if (findClass == null) {
          return;
        }
        Module module = ModuleUtilCore.findModuleForPsiElement(findClass);
        if (moduleSelector.isModuleAccepted(module)) {
          modulesCombo.setSelectedModule(module);
        }
      }
    });
    return mainClassFragment;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, LabeledComponent<ShortenCommandLineModeCombo>> createShortenClasspath(
          ModuleClasspathCombo modulesCombo, JrePathEditor jrePathEditor, EditorTextField mainClassField) {
    Project project = getProject();

    Supplier<Module> supplier = modulesCombo::getSelectedModule;
    Consumer<ActionListener> consumer = modulesCombo::addActionListener;

    JComponent create = LabeledComponent.create(new ShortenCommandLineModeCombo(project, jrePathEditor, supplier, consumer) {
      protected boolean productionOnly() {
        Module module = modulesCombo.getSelectedModule();
        if (module != null) {
          Boolean isClassInProductionSources = JavaParametersUtil.isClassInProductionSources(mainClassField.getText(), module);
          return Objects.requireNonNullElse(isClassInProductionSources, false);
        }
        return false;
      }
    }, ExecutionBundle.message("application.configuration.shorten.command.line.label"), "West");
    var fragment = new SettingsEditorFragment("shorten.command.line",
            ExecutionBundle.message("application.configuration.shorten.command.line"),
            ExecutionBundle.message("group.java.options"), create,
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<ShortenCommandLineModeCombo>>() {
              @Override
              public void accept(InfraApplicationRunConfiguration t, LabeledComponent<ShortenCommandLineModeCombo> labeledComponent) {
                ShortenCommandLineModeCombo component = labeledComponent.getComponent();
                component.setItem(t.getShortenCommandLine());
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<ShortenCommandLineModeCombo>>() {
              @Override
              public void accept(InfraApplicationRunConfiguration t, LabeledComponent<ShortenCommandLineModeCombo> labeledComponent) {
                ShortenCommandLine shortenCommandLine;
                if (labeledComponent.isVisible()) {
                  ShortenCommandLineModeCombo component = labeledComponent.getComponent();
                  shortenCommandLine = component.getSelectedItem();
                }
                else {
                  shortenCommandLine = null;
                }
                t.setShortenCommandLine(shortenCommandLine);
              }
            },
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                return it.getShortenCommandLine() != null && it.getShortenCommandLine() != ShortenCommandLine.NONE;
              }
            });
    fragment.setActionHint(ExecutionBundle.message("select.a.method.to.shorten.the.command.if.it.exceeds.the.os.limit"));
    return fragment;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, RawCommandLineEditor> createProgramArguments(Computable<Boolean> computable) {
    RawCommandLineEditor rawCommandLineEditor = new RawCommandLineEditor();
    CommandLinePanel.setMinimumWidth(rawCommandLineEditor, 400);
    String message = ExecutionBundle.message("run.configuration.program.parameters.placeholder");
    ExpandableTextField editorField = rawCommandLineEditor.getEditorField();
    StatusText emptyText = editorField.getEmptyText();
    emptyText.setText(message);
    ExpandableTextField editorField2 = rawCommandLineEditor.getEditorField();
    AccessibleContext accessibleContext = editorField2.getAccessibleContext();
    accessibleContext.setAccessibleName(message);
    FragmentedSettingsUtil.setupPlaceholderVisibility(rawCommandLineEditor.getEditorField());
    CommonParameterFragments.setMonospaced(rawCommandLineEditor.getTextField());
    MacrosDialog.addMacroSupport(rawCommandLineEditor.getEditorField(), MacrosDialog.Filters.ALL, new Computable() {
      public Boolean compute() {
        return computable.compute() != null;
      }
    });
    SettingsEditorFragment parameters = new SettingsEditorFragment<>("commandLineParameters", ExecutionBundle.message("run.configuration.program.parameters.name"),
            ExecutionBundle.message("group.java.options"), rawCommandLineEditor, 100,
            new BiConsumer<InfraApplicationRunConfiguration, RawCommandLineEditor>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, RawCommandLineEditor component) {
                component.setText(settings.getProgramParameters());
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, RawCommandLineEditor>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, RawCommandLineEditor component) {
                settings.setProgramParameters(component.isVisible() ? component.getText() : null);
              }
            },
            it -> {
              String programParameters = it.getProgramParameters();
              return !(programParameters == null || programParameters.length() == 0);
            });
    parameters.setEditorGetter(new Function<RawCommandLineEditor, JComponent>() {
      @Override
      public JComponent apply(RawCommandLineEditor editor) {
        return editor.getEditorField();
      }
    });
    parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"));
    return parameters;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, LabeledComponent<TextFieldWithBrowseButton>> createWorkingDirectory(
          ModulesCombo modulesCombo, Computable<Boolean> computable) {
    ExtendableTextField extendableTextField = new ExtendableTextField(10);
    MacrosDialog.addMacroSupport(extendableTextField, MacrosDialog.Filters.DIRECTORY_PATH, computable);

    TextFieldWithBrowseButton textFieldWithBrowseButton = new TextFieldWithBrowseButton(extendableTextField);
    textFieldWithBrowseButton.addBrowseFolderListener(ExecutionBundle.message("select.working.directory.message"), null, getProject(),
            FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    JComponent create = LabeledComponent.create(textFieldWithBrowseButton, ExecutionBundle.message("run.configuration.working.directory.label"), "West");
    SettingsEditorFragment workingDirectorySettings = new SettingsEditorFragment("workingDirectory", ExecutionBundle.message("run.configuration.working.directory.name"),
            ExecutionBundle.message("group.operating.system"), create,
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<TextFieldWithBrowseButton>>() {
              public void accept(InfraApplicationRunConfiguration settings, LabeledComponent<TextFieldWithBrowseButton> labeledComponent) {
                labeledComponent.getComponent().setText(settings.getWorkingDirectory());
              }
            }
            ,
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<TextFieldWithBrowseButton>>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, LabeledComponent<TextFieldWithBrowseButton> labeledComponent) {
                TextFieldWithBrowseButton component = labeledComponent.getComponent();
                settings.setWorkingDirectory(component.getText());
              }
            },
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                String workingDirectory = it.getWorkingDirectory();
                return !(workingDirectory == null || workingDirectory.length() == 0);
              }
            });

    workingDirectorySettings.setValidation(new Function<InfraApplicationRunConfiguration, List<ValidationInfo>>() {
      @Override
      public List<ValidationInfo> apply(InfraApplicationRunConfiguration settings) {
        ThrowableRunnable<Exception> runnable = () -> {
          Project project = getProject();
          ProgramParametersUtil.checkWorkingDirectoryExist(settings, project, modulesCombo.getSelectedModule());
        };
        ValidationInfo validationInfo = RuntimeConfigurationException.validate(extendableTextField, runnable);
        return List.of(validationInfo);
      }
    });

    return workingDirectorySettings;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, JComponent> createEnvParameters() {
    EnvironmentVariablesComponent environmentVariablesComponent = new EnvironmentVariablesComponent();
    environmentVariablesComponent.setLabelLocation("West");
    TextFieldWithBrowseButton component = environmentVariablesComponent.getComponent();
    CommonParameterFragments.setMonospaced(component.getTextField());
    SettingsEditorFragment fragment = new SettingsEditorFragment("environmentVariables", ExecutionBundle.message("environment.variables.fragment.name"),
            ExecutionBundle.message("group.operating.system"), environmentVariablesComponent,
            new BiConsumer<InfraApplicationRunConfiguration, JComponent>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, JComponent $noName_1) {
                environmentVariablesComponent.reset(settings);
              }
            }, new BiConsumer<InfraApplicationRunConfiguration, JComponent>() {
      @Override
      public void accept(InfraApplicationRunConfiguration settings, JComponent $noName_1) {
        if (!environmentVariablesComponent.isVisible()) {
          settings.setEnvs(MapsKt.emptyMap());
          settings.setPassParentEnvs(true);
          return;
        }
        environmentVariablesComponent.apply(settings);
      }
    }, new Predicate<InfraApplicationRunConfiguration>() {
      @Override
      public boolean test(InfraApplicationRunConfiguration it) {
        var envs = it.getEnvs();
        return !envs.isEmpty();
      }
    });
    fragment.setCanBeHidden(true);
    fragment.setHint(ExecutionBundle.message("environment.variables.fragment.hint"));
    fragment.setActionHint(ExecutionBundle.message("set.custom.environment.variables.for.the.process"));
    return fragment;
  }

  private List<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> createInfraFragments(Consumer<Consumer<Module>> consumer) {
    var fragments = new ArrayList<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>>();
    fragments.add(createActiveProfilesFragment(consumer));
    fragments.addAll(createInfraTags());
    fragments.addAll(createUpdateGroups());
    return fragments;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, ?> createActiveProfilesFragment(Consumer<Consumer<Module>> consumer) {
    InfraProfileCompletionProvider completionProvider = new InfraProfileCompletionProvider(false);
    consumer.accept(module -> completionProvider.setContext(module == null ? Collections.emptyList() : new SmartList<>(module)));

    JComponent create = LabeledComponent.create(new TextFieldWithAutoCompletion<>(mySettings.getProject(), completionProvider, true, ""),
            message("infra.application.run.configuration.active.profiles"), "West");
    var fragment = new SettingsEditorFragment("infra.profiles", message("infra.application.run.configuration.active.profiles"),
            message("infra.run.config.fragment.framework.group"), create,
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<TextFieldWithAutoCompletion<String>>>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, LabeledComponent<TextFieldWithAutoCompletion<String>> labeledComponent) {
                labeledComponent.getComponent().setText(settings.getActiveProfiles());
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<TextFieldWithAutoCompletion<String>>>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, LabeledComponent<TextFieldWithAutoCompletion<String>> labeledComponent) {
                if (!labeledComponent.isVisible()) {
                  settings.setActiveProfiles("");
                  return;
                }
                TextFieldWithAutoCompletion<String> component = labeledComponent.getComponent();
                settings.setActiveProfiles(component.getText());
              }
            },
            o -> true);
    fragment.setCanBeHidden(true);
    fragment.setHint(message("infra.application.run.configuration.active.profiles.tooltip"));
    return fragment;
  }

  private List<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> createInfraTags() {
    ArrayList fragments = new ArrayList();
    SettingsEditorFragment debugOutput = SettingsEditorFragment.createTag("infra.debug.output",
            message("infra.application.run.configuration.debug.output"),
            message("infra.run.config.fragment.framework.group"),
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                return it.isDebugMode();
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, Boolean>() {
              @Override
              public void accept(InfraApplicationRunConfiguration configuration, Boolean value) {
                configuration.setDebugMode(value);
              }
            });
    debugOutput.setActionDescription("-Ddebug");
    debugOutput.setActionHint(StringUtil.removeHtmlTags(message("infra.application.run.configuration.debug.output.tooltip")));
    fragments.add(debugOutput);
    SettingsEditorFragment hideBanner = SettingsEditorFragment.createTag("infra.banner", message("infra.application.run.configuration.hide.banner"),
            message("infra.run.config.fragment.framework.group"),
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                return it.isHideBanner();
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, Boolean>() {
              @Override
              public void accept(InfraApplicationRunConfiguration configuration, Boolean value) {
                configuration.setHideBanner(value.booleanValue());
              }
            });
    hideBanner.setActionDescription("-Dapp.main.banner-mode=OFF");
    hideBanner.setActionHint(StringUtil.removeHtmlTags(message("infra.application.run.configuration.hide.banner.tooltip")));
    fragments.add(hideBanner);
    SettingsEditorFragment launchOptimization = SettingsEditorFragment.createTag("infra.launch.optimization",
            message("infra.run.config.fragment.launch.optimization"),
            message("infra.run.config.fragment.framework.group"),
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                return !it.isEnableLaunchOptimization();
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, Boolean>() {
              @Override
              public void accept(InfraApplicationRunConfiguration configuration, Boolean value) {
                configuration.setEnableLaunchOptimization(!value.booleanValue());
              }
            });
    launchOptimization.setActionDescription(LAUNCH_OPTIMIZATION_DESCRIPTION);
    launchOptimization.setActionHint(StringUtil.removeHtmlTags(message("infra.application.run.configuration.launch.optimization.tooltip")));
    this.jrePathListeners.add(item -> {
      String it = item.getVersion();
      JavaSdkVersion version = it != null ? JavaSdkVersion.fromVersionString(it) : null;
      boolean noVerify = version == null || !version.isAtLeast(JavaSdkVersion.JDK_13);
      if (noVerify) {
        launchOptimization.setActionDescription(LAUNCH_OPTIMIZATION_DESCRIPTION);
        return;
      }
      launchOptimization.setActionDescription(LAUNCH_OPTIMIZATION_13_DESCRIPTION);
    });
    fragments.add(launchOptimization);
    SettingsEditorFragment jmxAgent = SettingsEditorFragment.createTag("infra.jmx.agent", message("infra.run.config.fragment.jmx.agent"),
            message("infra.run.config.fragment.framework.group"),
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                return !it.isEnableJmxAgent();
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, Boolean>() {
              @Override
              public void accept(InfraApplicationRunConfiguration configuration, Boolean value) {
                configuration.setEnableJmxAgent(!value);
              }
            });
    jmxAgent.setActionHint(StringUtil.removeHtmlTags(message("infra.application.run.configuration.jmx.agent.tooltip")));
    fragments.add(jmxAgent);
    if (AdvancedSettings.Companion.getBoolean("compiler.automake.allow.when.app.running")) {
      SettingsEditorFragment devToolsMessage = new SettingsEditorFragment("infra.dev.tools.message",
              message("infra.run.config.settings.background.compilation.enabled"), null,
              new JLabel(message("infra.run.config.settings.background.compilation.enabled"), AllIcons.General.BalloonError, SwingConstants.CENTER), 1,
              SettingsEditorFragmentType.TAG, (o, o2) -> {

      },
              (o, o2) -> {

              },
              o -> true);
      devToolsMessage.setRemovable(false);
      fragments.add(devToolsMessage);
    }
    return fragments;
  }

  private List<SettingsEditorFragment<InfraApplicationRunConfiguration, ?>> createUpdateGroups() {
    ArrayList fragments = new ArrayList();
    Function nameProvider = new Function<InfraApplicationUpdatePolicy, String>() {
      @Override
      public String apply(InfraApplicationUpdatePolicy it) {
        return UIUtil.removeMnemonic(it.getName());
      }
    };
    Function hintProvider = new Function<InfraApplicationUpdatePolicy, String>() {
      @Override
      public String apply(InfraApplicationUpdatePolicy it) {
        String description = it.getDescription();
        if (description != null) {
          return StringUtil.removeHtmlTags(description, true);
        }
        return null;
      }
    };
    VariantTagFragment updateAction = VariantTagFragment.createFragment("infra.update.action",
            message("infra.application.run.configuration.on.update.action"),
            message("infra.run.config.fragment.framework.group"),
            new Supplier<InfraApplicationUpdatePolicy[]>() {
              @Override
              public InfraApplicationUpdatePolicy[] get() {
                return InfraApplicationUpdatePolicy.getAvailablePolicies(false).toArray(new InfraApplicationUpdatePolicy[0]);
              }
            },
            new Function<InfraApplicationRunConfiguration, InfraApplicationUpdatePolicy>() {
              @Override
              public InfraApplicationUpdatePolicy apply(InfraApplicationRunConfiguration it) {
                InfraApplicationUpdatePolicy updateActionUpdatePolicy = it.getUpdateActionUpdatePolicy();
                return Objects.requireNonNullElse(updateActionUpdatePolicy, ApplicationRunConfigurationFragmentedEditorKt.DO_NOTHING);
              }
            },
            new BiConsumer<InfraApplicationRunConfiguration, InfraApplicationUpdatePolicy>() {
              @Override
              public void accept(InfraApplicationRunConfiguration configuration, InfraApplicationUpdatePolicy policy) {
                configuration.setUpdateActionUpdatePolicy(Intrinsics.areEqual(
                        policy, ApplicationRunConfigurationFragmentedEditorKt.DO_NOTHING) ? null : policy);
              }
            },
            it -> it.getUpdateActionUpdatePolicy() != null);
    updateAction.setVariantNameProvider(nameProvider);
    updateAction.setVariantHintProvider(hintProvider);
    updateAction.setValidation(new Function<InfraApplicationRunConfiguration, List<ValidationInfo>>() {
      @Override
      public List<ValidationInfo> apply(InfraApplicationRunConfiguration it) {
        return List.of(RuntimeConfigurationException.validate(TagButton.COMPONENT_VALIDATOR_TAG_PROVIDER.apply(updateAction.component()), new ThrowableRunnable() {
          public void run() throws Exception {
            it.checkUpdateActionUpdatePolicy();
          }
        }));
      }
    });
    fragments.add(updateAction);
    VariantTagFragment frameDeactivation = VariantTagFragment.createFragment("infra.frame.deactivation",
            message("infra.application.run.configuration.on.frame.deactivation"),
            message("infra.run.config.fragment.framework.group"),
            new Supplier<InfraApplicationUpdatePolicy[]>() {
              @Override
              public InfraApplicationUpdatePolicy[] get() {
                ArrayList<InfraApplicationUpdatePolicy> policies = new ArrayList<>();
                policies.add(ApplicationRunConfigurationFragmentedEditorKt.DO_NOTHING);
                policies.addAll(InfraApplicationUpdatePolicy.getAvailablePolicies(true));
                return policies.toArray(new InfraApplicationUpdatePolicy[0]);
              }
            },
            new Function<InfraApplicationRunConfiguration, InfraApplicationUpdatePolicy>() {
              @Override
              public InfraApplicationUpdatePolicy apply(InfraApplicationRunConfiguration it) {
                InfraApplicationUpdatePolicy frameDeactivationUpdatePolicy = it.getFrameDeactivationUpdatePolicy();
                if (frameDeactivationUpdatePolicy != null) {
                  return frameDeactivationUpdatePolicy;
                }
                return ApplicationRunConfigurationFragmentedEditorKt.DO_NOTHING;
              }
            },
            (configuration, policy) -> {
              configuration.setFrameDeactivationUpdatePolicy(Intrinsics.areEqual(
                      policy, ApplicationRunConfigurationFragmentedEditorKt.DO_NOTHING) ? null : policy);
            },
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                return it.getFrameDeactivationUpdatePolicy() != null;
              }
            });
    frameDeactivation.setVariantNameProvider(nameProvider);
    frameDeactivation.setVariantHintProvider(hintProvider);
    frameDeactivation.setValidation(new Function<InfraApplicationRunConfiguration, List<ValidationInfo>>() {
      @Override
      public List<ValidationInfo> apply(InfraApplicationRunConfiguration it) {
        return List.of(RuntimeConfigurationException.validate(TagButton.COMPONENT_VALIDATOR_TAG_PROVIDER.apply(frameDeactivation.component()), new ThrowableRunnable() {
          public void run() throws Exception {
            it.checkFrameDeactivationUpdatePolicy();
          }
        }));
      }
    });
    fragments.add(frameDeactivation);
    return fragments;
  }

  private SettingsEditorFragment<InfraApplicationRunConfiguration, ?> createAdditionalParamsFragment(Consumer<Consumer<Module>> consumer) {
    AdditionalParamsTableView toolbarDecorator = new AdditionalParamsTableView(getProject());
    consumer.accept(toolbarDecorator::setModule);
    JPanel createPanel = ToolbarDecorator.createDecorator(toolbarDecorator)
            .setAddAction(it -> toolbarDecorator.addAdditionalParameter())
            .setRemoveAction(it -> TableUtil.removeSelectedItems(toolbarDecorator))
            .createPanel();

    JComponent create = LabeledComponent.create(createPanel, message("infra.run.config.fragment.override.properties"));
    SettingsEditorFragment fragment = new SettingsEditorFragment("infra.additional.params",
            message("infra.run.config.fragment.override.properties"),
            message("infra.run.config.fragment.framework.group"), create,
            new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<JPanel>>() {
              @Override
              public void accept(InfraApplicationRunConfiguration settings, LabeledComponent<JPanel> labeledComponent) {
                toolbarDecorator.setAdditionalParameters(settings.getAdditionalParameters());
              }
            }, new BiConsumer<InfraApplicationRunConfiguration, LabeledComponent<JPanel>>() {
      @Override
      public void accept(InfraApplicationRunConfiguration settings, LabeledComponent<JPanel> labeledComponent) {
        if (!labeledComponent.isVisible()) {
          settings.setAdditionalParameters(new SmartList());
          return;
        }
        settings.setAdditionalParameters(toolbarDecorator.getAdditionalParameters());
      }
    },
            new Predicate<InfraApplicationRunConfiguration>() {
              @Override
              public boolean test(InfraApplicationRunConfiguration it) {
                List<InfraAdditionalParameter> additionalParameters = it.getAdditionalParameters();
                return !additionalParameters.isEmpty();
              }
            });
    fragment.setEditorGetter(new Function<LabeledComponent<JPanel>, JComponent>() {
      @Override
      public JComponent apply(LabeledComponent<JPanel> labeledComponent) {
        return toolbarDecorator;
      }
    });
    fragment.setValidation(new Function<InfraApplicationRunConfiguration, List<ValidationInfo>>() {
      @Override
      public List<ValidationInfo> apply(InfraApplicationRunConfiguration it) {
        return List.of(RuntimeConfigurationException.validate(toolbarDecorator, new ThrowableRunnable() {
          public void run() throws Exception {
            if (!toolbarDecorator.isEditing()) {
              ReadAction.run(it::checkAdditionalParams);
            }
          }
        }));
      }
    });
    return fragment;
  }
}

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

package cn.taketoday.assistant.profiles;

import com.intellij.CommonBundle;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.SmartList;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.ui.JBUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

class ChangeActiveProfileDialog extends DialogWrapper {
  private final Project myProject;
  private final Module myModule;
  private final InfraFileSet myFileSet;
  private final InfraProfileCompletionProvider myCompletionProvider;
  private final ChangeActiveProfileDialogUi myUi;
  private String myActiveProfiles;
  private final Map<ActiveProfileScope, String> myScopeActiveProfiles;

  public enum ActiveProfileScope {
    PROJECT,
    MODULE,
    CONTEXT
  }

  public ChangeActiveProfileDialog(Project project, @Nullable Module module, @Nullable InfraFileSet fileSet, boolean includeTests) {
    super(project);
    this.myActiveProfiles = "";
    this.myProject = project;
    this.myModule = module;
    this.myFileSet = fileSet;
    this.myCompletionProvider = new InfraProfileCompletionProvider(includeTests);
    final TextFieldWithAutoCompletion<String> profilesField = new TextFieldWithAutoCompletion<>(this.myProject, this.myCompletionProvider, true, "aaaaa");
    profilesField.addDocumentListener(new DocumentListener() {

      public void documentChanged(DocumentEvent event) {
        setOKActionEnabled(!ChangeActiveProfileDialog.this.myActiveProfiles.equals(profilesField.getText()));
      }
    });
    profilesField.setPreferredWidth(JBUI.scale(210));
    this.myUi = new ChangeActiveProfileDialogUi(profilesField);
    this.myScopeActiveProfiles = FactoryMap.create(scope -> {
      Set<String> activeProfiles;
      switch (scope) {
        case CONTEXT -> {
          assert this.myFileSet != null;
          activeProfiles = this.myFileSet.getActiveProfiles();
        }
        case MODULE -> {
          assert this.myModule != null;
          activeProfiles = InfraManager.from(this.myProject).getCombinedModel(this.myModule).getActiveProfiles();
        }
        case PROJECT -> {
          activeProfiles = new LinkedHashSet<>();
          Module[] var3 = ModuleManager.getInstance(this.myProject).getModules();
          for (Module each : var3) {
            Set<String> profiles = InfraManager.from(this.myProject).getCombinedModel(each).getActiveProfiles();
            if (profiles != null) {
              activeProfiles.addAll(profiles);
            }
          }
          return ProfileUtils.profilesAsString(activeProfiles);
        }
        default -> activeProfiles = null;
      }
      return ProfileUtils.profilesAsString(activeProfiles);
    });

    setTitle(message("change.active.profile.dialog.profiles"));
    setOKButtonText(CommonBundle.getApplyButtonText());
    setOKActionEnabled(false);
    initScopeSelection();
    updateActiveProfiles();
    init();
    pack();
  }

  protected void doOKAction() {
    Set<String> activeProfiles = ProfileUtils.profilesFromString(this.myUi.getProfilesText().getText());
    switch (getSelectedScope()) {
      case CONTEXT -> this.myFileSet.setActiveProfiles(activeProfiles);
      case MODULE -> setActiveProfilesForModule(this.myModule, activeProfiles);
      case PROJECT -> {
        Module[] modules = ModuleManager.getInstance(this.myProject).getModules();
        for (Module module : modules) {
          setActiveProfilesForModule(module, activeProfiles);
        }
      }
    }

    super.doOKAction();
  }

  private static void setActiveProfilesForModule(Module module, Set<String> activeProfiles) {
    InfraFacet infraFacet = InfraFacet.from(module);
    if (infraFacet == null) {
      return;
    }
    Set<InfraFileSet> sets = InfraFileSetService.of().getAllSets(infraFacet);
    for (InfraFileSet fileSet : sets) {
      fileSet.setActiveProfiles(activeProfiles);
    }
  }

  @Nullable
  protected String getHelpId() {
    return "change.active.infra.profiles";
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return this.myUi.getMainPanel();
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return this.myUi.getProfilesText();
  }

  private void initScopeSelection() {
    ActionListener updateProfilesListListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ChangeActiveProfileDialog.this.updateActiveProfiles();
      }
    };
    this.myUi.getProjectRadioButton().addActionListener(updateProfilesListListener);
    if (this.myModule == null) {
      this.myUi.getProjectRadioButton().setSelected(true);
      return;
    }
    this.myUi.getModuleRadioButton().setEnabled(true);
    this.myUi.getModuleRadioButton().addActionListener(updateProfilesListListener);
    this.myUi.getModuleRadioButton().setText(message("change.active.profile.dialog.module", this.myModule.getName()));
    if (this.myFileSet == null) {
      this.myUi.getModuleRadioButton().setSelected(true);
      return;
    }
    this.myUi.getContextRadioButton().setEnabled(true);
    this.myUi.getContextRadioButton().addActionListener(updateProfilesListListener);
    this.myUi.getContextRadioButton().setText(message("change.active.profile.dialog.context", this.myFileSet.getName()));
    this.myUi.getContextRadioButton().setSelected(true);
  }

  private ActiveProfileScope getSelectedScope() {
    if (this.myFileSet != null && this.myUi.getContextRadioButton().isSelected()) {
      return ActiveProfileScope.CONTEXT;
    }
    if (this.myModule != null && this.myUi.getModuleRadioButton().isSelected()) {
      return ActiveProfileScope.MODULE;
    }
    return ActiveProfileScope.PROJECT;
  }

  private void updateActiveProfiles() {
    ActiveProfileScope scope = getSelectedScope();
    if (scope == ActiveProfileScope.PROJECT) {
      List<Module> modules = new SmartList<>(ModuleManager.getInstance(this.myProject).getModules());
      this.myCompletionProvider.setContext(modules);
    }
    else {
      this.myCompletionProvider.setContext(this.myModule != null ? new SmartList<>(this.myModule) : Collections.emptyList());
    }
    this.myActiveProfiles = this.myScopeActiveProfiles.get(scope);
    this.myUi.getProfilesText().setText(this.myActiveProfiles);
  }
}

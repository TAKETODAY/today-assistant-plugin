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

package cn.taketoday.assistant.model.highlighting.config;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.facet.FacetManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.model.SideEffectGuard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.LightFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.xml.highlighting.DomElementAnnotationsManager;

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetImpl;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.editor.FileSetEditor;
import cn.taketoday.assistant.facet.searchers.CodeConfigSearcher;
import cn.taketoday.assistant.facet.searchers.XmlConfigSearcher;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

class ConfigureFileSetFix implements LocalQuickFix {
  protected final Module myModule;
  private final VirtualFile myVirtualFile;

  public ConfigureFileSetFix(Module module, VirtualFile virtualFile) {
    this.myModule = module;
    this.myVirtualFile = virtualFile;
  }

  public String getFamilyName() {
    return message("infra.facet.inspection.configure.context.for.file");
  }

  public boolean startInWriteAction() {
    return false;
  }

  public void applyFix(Project project, ProblemDescriptor descriptor) {

    SideEffectGuard.checkSideEffectAllowed(SideEffectGuard.EffectType.PROJECT_MODEL);
    final Ref<Boolean> fileSetsInMultipleModules = Ref.create(Boolean.FALSE);
    final Set<InfraFileSet> sets = new LinkedHashSet<>();
    ModuleUtilCore.visitMeAndDependentModules(this.myModule, (module) -> {
      InfraFacet facet = InfraFacet.from(module);
      if (facet != null) {
        for (InfraFileSet set : InfraFileSetService.of().getAllSets(facet)) {
          if (!set.isAutodetected()) {
            sets.add(set);
            if (!this.myModule.equals(module)) {
              fileSetsInMultipleModules.set(Boolean.TRUE);
            }
          }
        }
      }

      return true;
    });
    ArrayList<InfraFileSet> list = new ArrayList<>(sets);
    InfraFacet facet = InfraFacet.from(this.myModule);
    InfraFileSetImpl fakeNewSet;
    if (facet != null) {
      fakeNewSet = new InfraFileSetImpl(InfraFileSetService.of().getUniqueId(sets),
              message("infra.facet.inspection.context.create"), facet) {
        public boolean isNew() {
          return true;
        }

        protected VirtualFilePointer createVirtualFilePointer(String url) {

          return new LightFilePointer(url);
        }
      };
      list.add(fakeNewSet);
    }
    else {
      fakeNewSet = null;
    }

    BaseListPopupStep<InfraFileSet> step = new BaseListPopupStep<>(message("infra.facet.inspection.context.choose"), list) {
      public PopupStep onChosen(InfraFileSet selectedValue, boolean finalChoice) {
        return this.doFinalStep(() -> {
          ConfigureFileSetFix.this.onChosen(selectedValue, fakeNewSet, sets, facet);
        });
      }

      public String getTextFor(InfraFileSet fileSet) {
        if (!this.isFakeNewSet(fileSet) && fileSetsInMultipleModules.get()) {
          Module filesetModule = fileSet.getFacet().getModule();
          return fileSet.getName() + " [" + filesetModule.getName() + "]";
        }
        else {
          return fileSet.getName();
        }
      }

      public Icon getIconFor(InfraFileSet fileSet) {
        return isFakeNewSet(fileSet) ? LayeredIcon.create(fileSet.getIcon(), AllIcons.Actions.New) : fileSet.getIcon();
      }

      public @Nullable ListSeparator getSeparatorAbove(InfraFileSet fileSet) {
        return this.isFakeNewSet(fileSet) ? new ListSeparator() : null;
      }

      public boolean isSpeedSearchEnabled() {
        return true;
      }

      private boolean isFakeNewSet(InfraFileSet fileSet) {
        return fileSet.equals(fakeNewSet);
      }
    };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      assert fakeNewSet != null;
      this.onChosen(list.get(0), fakeNewSet, sets, facet);
    }
    else {
      ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
      Component component = SwingHelper.getComponentFromRecentMouseEvent();
      if (component != null) {
        ApplicationManager.getApplication().invokeLater(() -> {
          popup.showUnderneathOf(component);
        });
      }
      else {
        DataManager.getInstance()
                .getDataContextFromFocusAsync()
                .onSuccess(popup::showInBestPositionFor);
      }
    }
  }

  private void onChosen(@Nullable InfraFileSet selectedValue,
          @Nullable InfraFileSet fakeNewSet,
          Set<InfraFileSet> existingSets, InfraFacet facet) {
    if (selectedValue == null) {
      return;
    }
    if (selectedValue == fakeNewSet) {
      editNewSet(facet, existingSets, fakeNewSet);
    }
    else {
      selectedValue.addFile(this.myVirtualFile);
    }
    FacetManager.getInstance(facet.getModule()).facetConfigurationChanged(facet);
    Project project = facet.getModule().getProject();
    DomElementAnnotationsManager.getInstance(project).dropAnnotationsCache();
    DaemonCodeAnalyzer.getInstance(project).restart();
    EditorNotifications.getInstance(project).updateAllNotifications();
  }

  private void editNewSet(InfraFacet facet, Set<InfraFileSet> sets, InfraFileSet fakeNewSet) {
    String defaultName = StringUtil.capitalizeWords(this.myVirtualFile.getNameWithoutExtension().replace('-', ' '), true);
    String uniqueName = InfraFileSetService.of().getUniqueName(defaultName, sets);
    fakeNewSet.setName(uniqueName);
    fakeNewSet.addFile(this.myVirtualFile);
    if (DumbService.isDumb(this.myModule.getProject()) || ApplicationManager.getApplication().isUnitTestMode()) {
      addNewSet(facet, fakeNewSet);
      return;
    }
    Set<InfraFileSet> currentModuleFileSets = InfraFileSetService.of().getAllSets(facet);
    FileSetEditor editor = new FileSetEditor(this.myModule, fakeNewSet, currentModuleFileSets, new XmlConfigSearcher(this.myModule, false), new CodeConfigSearcher(this.myModule, false));
    if (editor.showAndGet()) {
      addNewSet(facet, editor.getEditedFileSet());
    }
    else {
      Disposer.dispose(fakeNewSet);
    }
  }

  private static void addNewSet(InfraFacet facet, InfraFileSet fileSet) {
    facet.addFileSet(fileSet);
    facet.getConfiguration().setModified();
    FacetManager.getInstance(facet.getModule()).facetConfigurationChanged(facet);
  }
}

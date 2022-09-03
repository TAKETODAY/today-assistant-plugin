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

package cn.taketoday.assistant.app.application.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.refactoring.rename.naming.NameSuggester;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.profiles.InfraProfileTarget;
import cn.taketoday.lang.Nullable;

public class InfraProfileConfigFileAutomaticRenamerFactory implements AutomaticRenamerFactory {

  public boolean isApplicable(PsiElement element) {
    PsiElement psiElement;
    if (!(element instanceof PomTargetPsiElement)) {
      return false;
    }
    PomTarget target = ((PomTargetPsiElement) element).getTarget();
    return (target instanceof InfraProfileTarget profileTarget)
            && (psiElement = profileTarget.getElement()) != null
            & InfraLibraryUtil.hasFrameworkLibrary(ModuleUtilCore.findModuleForPsiElement(psiElement));
  }

  @Nullable
  public String getOptionName() {
    return InfraAppBundle.message("application.config.rename.option");
  }

  public boolean isEnabled() {
    return true;
  }

  public void setEnabled(boolean enabled) {
  }

  public AutomaticRenamer createRenamer(PsiElement element, String newName, Collection<UsageInfo> usages) {
    InfraProfileTarget target = (InfraProfileTarget) ((PomTargetPsiElement) element).getTarget();
    String oldName = target.getName();
    String configFileSuffix = "-" + oldName;
    PsiElement targetElement = target.getElement();
    Module module = ModuleUtilCore.findModuleForPsiElement(targetElement);
    PsiManager psiManager = targetElement.getManager();
    SmartList smartList = new SmartList();
    if (module != null) {
      List<VirtualFile> matchingConfigFiles = ContainerUtil.filter(InfraConfigurationFileService.of().findConfigFiles(module, true), file -> {
        return StringUtil.endsWith(file.getNameWithoutExtension(), configFileSuffix);
      });
      for (VirtualFile configFile : matchingConfigFiles) {
        ContainerUtil.addIfNotNull(smartList, psiManager.findFile(configFile));
      }
    }
    return new MyAutomaticRenamer(smartList, oldName, newName);
  }

  private static final class MyAutomaticRenamer extends AutomaticRenamer {
    private final String myOldName;

    private MyAutomaticRenamer(List<PsiFile> configFiles, String oldName, String newName) {
      for (PsiFile file : configFiles) {
        this.myElements.add(file);
        suggestAllNames(oldName, newName);
      }
      this.myOldName = oldName;
    }

    public String getDialogTitle() {
      return InfraAppBundle.message("application.config.rename.title");
    }

    public String getDialogDescription() {
      return InfraAppBundle.message("application.config.rename.description", this.myOldName);
    }

    public String entityName() {
      return InfraAppBundle.message("application.config.rename.entity.name");
    }

    protected String suggestNameForElement(PsiNamedElement element, NameSuggester suggester, String newClassName, String oldClassName) {
      String name = element.getName();
      return StringUtil.replace(name, "-" + oldClassName, "-" + newClassName);
    }

    public boolean isSelectedByDefault() {
      return true;
    }

    public boolean allowChangeSuggestedName() {
      return false;
    }
  }
}

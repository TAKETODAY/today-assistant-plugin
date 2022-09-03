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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.highlighting.DomElementsInspection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InactiveProfileHighlightingInspection extends DomElementsInspection<Beans> {

  public InactiveProfileHighlightingInspection() {
    super(Beans.class);
  }

  protected ProblemDescriptor[] checkDomFile(DomFileElement<Beans> domFileElement, InspectionManager manager, boolean isOnTheFly) {
    SmartList smartList = new SmartList();
    PsiFile file = domFileElement.getFile();
    Beans beans = domFileElement.getRootElement();
    InfraModel infraModel = InfraManager.from(file.getProject()).getInfraModelByFile(file);
    if (infraModel != null) {
      Set<String> activeProfiles = infraModel.getActiveProfiles();
      if (ProfileUtils.isActiveProfile(beans, activeProfiles)) {
        for (Beans innerBeans : beans.getBeansProfiles()) {
          if (!ProfileUtils.isActiveProfile(innerBeans, activeProfiles)) {
            smartList.add(createProblem(manager, innerBeans, infraModel));
          }
        }
      }
      else {
        smartList.add(createProblem(manager, beans, infraModel));
      }
    }
    return (ProblemDescriptor[]) smartList.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  public ProblemDescriptor createProblem(InspectionManager manager, Beans beans, InfraModel infraModel) {
    return manager.createProblemDescriptor(beans.getXmlTag(),
            message("SpringInactiveProfilesHighlightingPass.inactive.profile"), true,
            getFixes(beans.getProfile().getExpressions(),
                    infraModel.getFileSet()), ProblemHighlightType.LIKE_UNUSED_SYMBOL);
  }

  public LocalQuickFix[] getFixes(Set<String> names, @Nullable InfraFileSet set) {
    if (set == null) {
      return LocalQuickFix.EMPTY_ARRAY;
    }
    List<LocalQuickFix> fixes = new ArrayList<>(names.size());
    for (String name : names) {
      if (name.startsWith("!")) {
        fixes.add(new DeactivateProfileQuickFix(name.substring(1), set));
      }
      else {
        fixes.add(new ActivateProfileQuickFix(name, set));
      }
    }
    return fixes.toArray(LocalQuickFix.EMPTY_ARRAY);
  }

  public static final class ActivateProfileQuickFix implements LocalQuickFix {
    private final String myProfileName;
    private final InfraFileSet myFileSet;

    private ActivateProfileQuickFix(String name, InfraFileSet set) {
      this.myProfileName = name;
      this.myFileSet = set;
    }

    public String getName() {
      return message("ActivateSpringProfileIntentionAction.activate.profile", this.myProfileName);
    }

    public String getFamilyName() {
      return message("ActivateSpringProfileIntentionAction.activate.profile.family.name");
    }

    public boolean startInWriteAction() {
      return false;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      Set<String> activeProfiles = new HashSet<>(this.myFileSet.getActiveProfiles());
      activeProfiles.add(this.myProfileName);
      this.myFileSet.setActiveProfiles(activeProfiles);
      ProfileUtils.notifyProfilesChanged(project);
    }
  }

  public static final class DeactivateProfileQuickFix implements LocalQuickFix {
    private final String myProfileName;
    private final InfraFileSet myFileSet;

    private DeactivateProfileQuickFix(String name, InfraFileSet set) {
      this.myProfileName = name;
      this.myFileSet = set;
    }

    public String getName() {
      return message("ActivateSpringProfileIntentionAction.deactivate.profile", this.myProfileName);
    }

    public String getFamilyName() {
      return message("ActivateSpringProfileIntentionAction.deactivate.profile.family.name");
    }

    public boolean startInWriteAction() {
      return false;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      Set<String> activeProfiles = new HashSet<>(this.myFileSet.getActiveProfiles());
      activeProfiles.remove(this.myProfileName);
      this.myFileSet.setActiveProfiles(activeProfiles);
      ProfileUtils.notifyProfilesChanged(project);
    }
  }
}

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

package cn.taketoday.assistant.code;

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElementVisitor;

import org.jetbrains.uast.UElement;

import cn.taketoday.assistant.TodayLibraryUtil;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 23:37
 */
public abstract class AbstractInfraLocalInspection extends AbstractBaseUastLocalInspectionTool {

  protected AbstractInfraLocalInspection() {
    super();
  }

  @SafeVarargs
  protected AbstractInfraLocalInspection(Class<? extends UElement>... uElementTypesHint) {
    super(uElementTypesHint);
  }

  @Override
  public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session) {
    if (isEnabledForModule(ModuleUtilCore.findModuleForFile(holder.getFile()))) {
      return super.buildVisitor(holder, isOnTheFly, session);
    }
    return PsiElementVisitor.EMPTY_VISITOR;
  }

  public boolean isEnabledForModule(Module moduleForFile) {
    return TodayLibraryUtil.hasLibrary(moduleForFile);
  }

}
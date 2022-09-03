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

package cn.taketoday.assistant;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Registry of all configuration file inspections provided by Spring-* plugins.
 *
 * @author Yann C&eacute;bron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/26 20:39
 */
public abstract class InfraInspectionsRegistry {

  /**
   * Provide plugin-specific configuration file inspections.
   */
  public interface Contributor {

    ExtensionPointName<Contributor> EP_NAME =
            ExtensionPointName.create("cn.taketoday.assistant.inspectionsRegistryContributor");

    Class<? extends LocalInspectionTool>[] getInspectionClasses();
  }

  /**
   * Allows adding additional configuration files as well as additional highlighting to Spring validation context.
   */
  public abstract static class AdditionalFilesContributor {

    public static final ExtensionPointName<InfraInspectionsRegistry.AdditionalFilesContributor> EP_NAME =
            ExtensionPointName.create("cn.taketoday.assistant.inspectionsRegistryAdditionalFilesContributor");

    public abstract Collection<VirtualFile> getAdditionalFilesToProcess(Project project, CompileContext context);

    /**
     * Add additional highlighting (e.g. provided by Annotators) for the given file.
     * <p/>
     * NOTE: all files from current validation context will be passed in, not only additional ones provided by this EP.
     *
     * @param psiFile File to check.
     * @return Additional highlighting info.
     */
    public Map<ProblemDescriptor, HighlightDisplayLevel> checkAdditionally(PsiFile psiFile) {
      return Collections.emptyMap();
    }
  }

  public static InfraInspectionsRegistry getInstance() {
    return ApplicationManager.getApplication().getService(InfraInspectionsRegistry.class);
  }

  /**
   * Returns all registered inspections. Used by Compiler|Validation "Spring Model".
   *
   * @return Inspections.
   */
  public abstract Class<? extends LocalInspectionTool>[] getInfraInspectionClasses();

  @TestOnly
  public abstract Class<? extends LocalInspectionTool>[] getTestInfraInspectionClasses();

  @TestOnly
  public abstract Class<? extends LocalInspectionTool> getTestInfraModelInspectionClass();
}

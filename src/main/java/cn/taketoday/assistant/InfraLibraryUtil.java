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

import com.intellij.codeInsight.JavaLibraryModificationTracker;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.jam.JavaLibraryUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.JarVersionDetectionUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;

import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

import java.io.File;
import java.util.Set;

import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 00:25
 */
public class InfraLibraryUtil {

  public static final PatternCondition<PsiElement> IS_WEB_MVC_PROJECT
          = new PatternCondition<>("isWebMVCEnabledProject") {

    public boolean accepts(PsiElement element, ProcessingContext context) {
      return isWebMVCEnabled(element.getProject());
    }
  };

  public static boolean hasLibrary(Project project) {
    return JavaLibraryUtils.hasLibraryClass(project, InfraVersion.ANY.getDetectionClassFqn());
  }

  public static boolean hasLibrary(@Nullable Module module) {
    return isAtLeastVersion(module, InfraVersion.ANY);
  }

  public static boolean hasFrameworkLibrary(Project project) {
    return JavaLibraryUtils.hasLibraryClass(project, "cn.taketoday.framework.Application");
  }

  public static boolean hasFrameworkLibrary(@Nullable Module module) {
    return JavaLibraryUtils.hasLibraryClass(module, "cn.taketoday.framework.Application");
  }

  public static boolean hasWebMvcLibrary(Project project) {
    return JavaLibraryUtils.hasLibraryClass(project, "cn.taketoday.web.RequestContext");
  }

  public static boolean hasWebMvcLibrary(@Nullable Module module) {
    return JavaLibraryUtils.hasLibraryClass(module, "cn.taketoday.web.RequestContext");
  }

  public static boolean isWebMVCEnabled(Project project) {
    return InfraUtils.hasFacets(project)
            && hasWebMvcLibrary(project);
  }

  public static boolean isAtLeastVersion(@Nullable Module module, InfraVersion version) {
    if (module == null) {
      return false;
    }
    else if (!hasLibrary(module.getProject())) {
      return false;
    }
    else {
      InfraVersion cached = getCachedVersion(module);
      return cached != null && cached.isAtLeast(version);
    }
  }

  public static boolean hasRequestMappings(@Nullable Module module) {
    return InfraUtils.findLibraryClass(module, "cn.taketoday.web.HandlerMapping") != null;
  }

  @Nullable
  public static String getVersionFromJar(Module module) {
    return JarVersionDetectionUtil.detectJarVersion(InfraVersion.V_4_0.getDetectionClassFqn(), module);
  }

  public static boolean hasConfigurationMetadataAnnotationProcessor(Module module) {
    AnnotationProcessingConfiguration configuration = CompilerConfiguration.getInstance(module.getProject()).getAnnotationProcessingConfiguration(module);
    if (configuration.isEnabled()) {
      Set<String> processors = configuration.getProcessors();
      if (!processors.isEmpty() && !processors.contains(
              "cn.taketoday.config.processor.ConfigurationMetadataAnnotationProcessor")) {
        return false;
      }
      else if (configuration.isObtainProcessorsFromClasspath()) {
        PsiClass processor = InfraUtils.findLibraryClass(module, "cn.taketoday.config.processor.ConfigurationMetadataAnnotationProcessor");
        return processor != null;
      }
      else {
        Iterable<String> segments = StringUtil.tokenize(configuration.getProcessorPath(), File.pathSeparator);
        for (String segment : segments) {
          if (segment.endsWith(".jar")) {
            int fileNameIndex = Integer.max(segment.lastIndexOf(47), segment.lastIndexOf(92));
            if (fileNameIndex >= 0 && fileNameIndex < segment.length() - 1) {
              segment = segment.substring(fileNameIndex + 1);
            }

            if (segment.contains("infra-configuration-processor")) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  public static InfraVersion getVersion(Module module) {
    return getCachedVersion(module);
  }

  @Nullable
  private static InfraVersion getCachedVersion(Module module) {
    Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> {
      InfraVersion detected = null;
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, false);
      InfraVersion[] versions = ArrayUtil.reverseArray(InfraVersion.values());
      for (InfraVersion version : versions) {
        PsiClass psiClass = javaPsiFacade.findClass(version.getDetectionClassFqn(), scope);
        if (psiClass != null) {
          detected = version;
          break;
        }
      }

      return Result.create(detected, JavaLibraryModificationTracker.getInstance(project));
    });
  }

  public static boolean hasActuators(Module module) {
    return InfraUtils.findLibraryClass(module, "cn.taketoday.actuate.endpoint.Endpoint") != null;
  }

  public static boolean isBelowVersion(@Nullable Module module, InfraVersion version) {
    return !isAtLeastVersion(module, version);
  }

}

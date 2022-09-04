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
package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.json.psi.JsonFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Processor;

import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.List;

import cn.taketoday.assistant.app.InfraConfigFileConstants;

public class InfraAdditionalConfigUtils {

  private final Module myModule;
  private final List<VirtualFile> myResourceRoots;

  public InfraAdditionalConfigUtils(Module module) {
    myModule = module;

    boolean testMode = ApplicationManager.getApplication().isUnitTestMode();
    JpsModuleSourceRootType<?> sourceRootType = testMode ? JavaSourceRootType.SOURCE : JavaResourceRootType.RESOURCE;
    myResourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(sourceRootType);
  }

  boolean hasResourceRoots() {
    return !myResourceRoots.isEmpty();
  }

  List<VirtualFile> getResourceRoots() {
    return myResourceRoots;
  }

  public boolean processAllAdditionalMetadataFiles(Processor<? super PsiFile> processor) {
    for (VirtualFile root : myResourceRoots) {
      VirtualFile vf = root.findFileByRelativePath("META-INF/" +
              InfraConfigFileConstants.ADDITIONAL_CONFIGURATION_METADATA_JSON);
      if (vf == null)
        continue;
      PsiFile psiFile = PsiManager.getInstance(myModule.getProject()).findFile(vf);
      if (!processor.process(psiFile))
        return false;
    }
    return true;
  }

  public boolean processAdditionalMetadataFiles(Processor<? super JsonFile> processor) {
    return processAllAdditionalMetadataFiles(psiFile -> {
      if (psiFile instanceof JsonFile) {
        return processor.process((JsonFile) psiFile);
      }
      return true;
    });
  }

  private static boolean OVERRIDE;

  @TestOnly
  public static void setOverrideDetection(boolean value) {
    OVERRIDE = value;
  }

  public static boolean isAdditionalMetadataFile(PsiFile psiFile) {
    if (OVERRIDE)
      return true;

    return psiFile instanceof JsonFile
            && psiFile.getName().equals(InfraConfigFileConstants.ADDITIONAL_CONFIGURATION_METADATA_JSON);
  }
}

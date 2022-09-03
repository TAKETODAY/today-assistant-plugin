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
package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.xml.XmlFile;

import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;

public abstract class InfraTestContextUtil {

  public static InfraTestContextUtil of() {
    return ApplicationManager.getApplication().getService(InfraTestContextUtil.class);
  }

  public abstract boolean isTestContextConfigurationClass(PsiClass psiClass);

  public abstract CommonInfraModel getTestingModel(PsiClass testClass);

  public abstract void discoverConfigFiles(ContextConfiguration contextConfiguration,
          Set<XmlFile> appContexts, Set<PsiClass> configurationContexts, PsiClass... contexts);
}

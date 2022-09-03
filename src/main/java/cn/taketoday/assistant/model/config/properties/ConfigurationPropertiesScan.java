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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.beans.stereotype.InfraJamComponentScan;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;

public class ConfigurationPropertiesScan extends InfraJamComponentScan {

  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraClassesConstants.CONFIGURATION_PROPERTIES_SCAN, ARCHETYPE, META_KEY);
  public static final SemKey<ConfigurationPropertiesScan> JAM_KEY = COMPONENT_SCAN_JAM_KEY.subKey("ConfigurationPropertiesScan");
  public static final JamClassMeta<ConfigurationPropertiesScan> META = new JamClassMeta<>(null, ConfigurationPropertiesScan.class, JAM_KEY)
          .addAnnotation(ANNOTATION_META);

  public ConfigurationPropertiesScan(PsiClass psiElement) {
    super(psiElement);
  }

  public ConfigurationPropertiesScan(PsiAnnotation annotation) {
    super(annotation);
  }

  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }

  protected Set<CommonInfraBean> getScannedBeans(Module module) {
    return InfraJamService.of().filterComponentScannedStereotypes(module, this, getConfigurationProperties(module));
  }

  static List<CommonInfraBean> getConfigurationProperties(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      List<ConfigurationProperties> properties = JamService.getJamService(module.getProject()).getJamClassElements(
              ConfigurationProperties.CLASS_META, InfraClassesConstants.CONFIGURATION_PROPERTIES, scope);
      SmartList<CommonInfraBean> smartList = new SmartList<>();
      for (ConfigurationProperties configurationProperties : properties) {
        PsiElement psiElement = configurationProperties.getPsiElement();
        if ((psiElement instanceof PsiClass psiClass) && !InfraUtils.isComponentOrMeta(psiClass)) {
          smartList.add(ConfigurationPropertiesDiscoverer.createConfigurationPropertiesBean(configurationProperties, psiClass));
        }
      }
      return CachedValueProvider.Result.create(smartList, InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies());
    });
  }
}

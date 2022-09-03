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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.context.model.LocalXmlModelImpl;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 12:23
 */
public class LocalModelFactory {
  private static final Logger LOG = Logger.getInstance(LocalModelFactory.class);
  private static final Key<CachedValue<Map<Pair<Module, String>, LocalXmlModel>>> LOCAL_XML_MODEL_KEY = Key.create("LOCAL_XML_MODEL_KEY");
  private static final Key<CachedValue<Map<Pair<Module, String>, LocalAnnotationModel>>> LOCAL_ANNO_MODEL_KEY = Key.create("LOCAL_ANN_MODEL_KEY");

  public static LocalModelFactory of() {
    return ApplicationManager.getApplication().getService(LocalModelFactory.class);
  }

  @Nullable
  public LocalXmlModel getOrCreateLocalXmlModel(XmlFile configFile, Module module, @Nullable Set<String> activeProfiles) {
    if (module.isDisposed()) {
      return null;
    }
    else if (!InfraDomUtils.isInfraXml(configFile)) {
      return null;
    }
    else {
      Map<Pair<Module, String>, LocalXmlModel> localXmlModelsMap = CachedValuesManager.getManager(module.getProject()).getCachedValue(configFile, LOCAL_XML_MODEL_KEY, () -> {
        Map<Pair<Module, String>, LocalXmlModel> result = ConcurrentFactoryMap.createMap((cacheKey) -> {
          Set<String> profiles = getProfilesFromString(cacheKey.second);
          return new LocalXmlModelImpl(configFile, cacheKey.first, profiles);
        });
        return CachedValueProvider.Result.create(result, getLocalXmlModelDependencies(configFile));
      }, false);
      Pair<Module, String> key = Pair.create(module, ProfileUtils.profilesAsString(activeProfiles));
      return localXmlModelsMap.get(key);
    }
  }

  @Nullable
  public LocalAnnotationModel getOrCreateLocalAnnotationModel(
          PsiClass psiClass, Module module, Set<String> activeProfiles) {

    if (!module.isDisposed() && psiClass.isValid()) {
      var localAnnotationModelsMap = CachedValuesManager.getManager(module.getProject()).getCachedValue(psiClass, LOCAL_ANNO_MODEL_KEY, () -> {
        Map<Pair<Module, String>, LocalAnnotationModel> result = ConcurrentFactoryMap.createMap((cacheKey) -> {
          checkValidConfigClass(psiClass);
          Set<String> profiles = Collections.unmodifiableSet(getProfilesFromString(cacheKey.second));

          for (LocalModelProducer producer : LocalModelProducer.EP_NAME.getExtensionList()) {
            LocalAnnotationModel customLocalModel = producer.create(psiClass, cacheKey.first, profiles);

            if (customLocalModel != null) {
              return customLocalModel;
            }
          }

          return new LocalAnnotationModel(psiClass, cacheKey.first, profiles);
        });
        return CachedValueProvider.Result.create(result, getLocalAnnotationModelDependencies(psiClass));
      }, false);
      Pair<Module, String> key = Pair.create(module, ProfileUtils.profilesAsString(activeProfiles));
      return localAnnotationModelsMap.get(key);
    }
    else {
      return null;
    }
  }

  private static void checkValidConfigClass(PsiClass psiClass) {

    if (psiClass instanceof PsiTypeParameter || psiClass.getQualifiedName() == null || PsiUtil.isLocalOrAnonymousClass(psiClass)) {
      LOG.error("invalid config class: " + psiClass + " in " + psiClass.getContainingFile().getVirtualFile().getPath());
    }

  }

  private static Set<String> getProfilesFromString(String key) {
    if (key.isEmpty()) {
      return Collections.emptySet();
    }
    else {
      return ContainerUtil.map2Set(StringUtil.split(key, ","), StringUtil::trim);
    }
  }

  public static Object[] getLocalXmlModelDependencies(XmlFile psiFile) {

    Project project = psiFile.getProject();
    InfraModificationTrackersManager infraModificationTrackersManager = InfraModificationTrackersManager.from(project);
    Object[] dependencies = infraModificationTrackersManager.getOuterModelsDependencies();
    dependencies = ArrayUtil.append(dependencies, psiFile);
    dependencies = ArrayUtil.append(dependencies, infraModificationTrackersManager.getCustomBeanParserModificationTracker());

    return dependencies;
  }

  private static Object[] getLocalAnnotationModelDependencies(PsiClass psiClass) {
    return ArrayUtil.append(InfraModificationTrackersManager.from(psiClass.getProject()).getOuterModelsDependencies(), psiClass);
  }
}

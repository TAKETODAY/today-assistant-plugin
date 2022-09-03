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
package cn.taketoday.assistant.model.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

import static com.intellij.util.containers.ContainerUtil.isEmpty;

public final class ProfileUtils {
  public static boolean isActiveProfile(Beans beans, @Nullable Set<String> activeProfiles) {
    if (isEmptyOrTestDefault(activeProfiles))
      return true;

    if (beans.getProfile().matches(activeProfiles)) {
      //IDEA-67463
      Beans parentBeans = DomUtil.getParentOfType(beans, Beans.class, true);
      return parentBeans == null || isActiveProfile(parentBeans, activeProfiles);
    }
    return false;
  }

  public static String profilesAsString(@Nullable Set<String> activeProfiles) {
    if (isEmpty(activeProfiles))
      return "";

    Set<String> profiles = new TreeSet<>();

    for (String activeProfile : activeProfiles) {
      if (!InfraProfile.DEFAULT_PROFILE_NAME.equals(activeProfile)) {
        profiles.add(activeProfile);
      }
    }

    if (profiles.size() > 1) {
      return StringUtil.join(profiles, ", ");
    }
    return ContainerUtil.getFirstItem(profiles, "");
  }

  public static Set<String> profilesFromString(@Nullable String text) {
    if (StringUtil.isEmptyOrSpaces(text))
      return Collections.emptySet();

    Set<String> activeProfiles = new LinkedHashSet<>();
    String[] names = text.split(",");
    for (String name : names) {
      name = name.trim();
      if (!name.isEmpty()) {
        activeProfiles.add(name);
      }
    }
    return activeProfiles;
  }

  public static <T extends CommonInfraBean> List<T> filterBeansInActiveProfiles(Collection<? extends T> allBeans,
          @Nullable Set<String> activeProfiles) {
    if (isEmptyOrTestDefault(activeProfiles))
      return new SmartList<>(allBeans);

    return ContainerUtil.filter(allBeans, bean -> bean.isValid() && bean.getProfile().matches(activeProfiles));
  }

  public static boolean isInActiveProfiles(CommonInfraBean bean, @Nullable Set<String> activeProfiles) {
    return isEmptyOrTestDefault(activeProfiles) || bean.getProfile().matches(activeProfiles);
  }

  public static boolean isEmptyOrTestDefault(@Nullable Set<String> activeProfiles) {
    return activeProfiles == null || activeProfiles.isEmpty() ||
            (activeProfiles.size() == 1 && InfraProfile.DEFAULT_TEST_PROFILE_NAME.equals(activeProfiles.iterator().next()));
  }

  public static void notifyProfilesChanged(Project project) {
    InfraModificationTrackersManager.from(project).fireActiveProfilesChanged();
    PsiManager.getInstance(project).dropPsiCaches();
    EditorNotifications.getInstance(project).updateAllNotifications();
    project.getMessageBus().syncPublisher(InfraFileSetService.TOPIC).activeProfilesChanged();
  }
}

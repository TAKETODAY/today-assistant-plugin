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
package cn.taketoday.assistant.profiles;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.utils.ProfileUtils;

/**
 * provides list of defined profiles
 */
public final class InfraProfileCompletionProvider extends TextFieldWithAutoCompletionListProvider<String> {
  private final boolean myIncludeTests;
  private final Map<Module, Collection<String>> myProfiles = FactoryMap.create(this::getProfiles);
  private Collection<Module> myContext = Collections.emptyList();

  public InfraProfileCompletionProvider(boolean includeTests) {
    super(null);
    myIncludeTests = includeTests;
  }

  /**
   * @param modules collection of modules to search profiles for completion
   */
  public void setContext(Collection<Module> modules) {
    myContext = modules;
  }

  @Override
  protected String getLookupString(String item) {
    return item;
  }

  @Override
  protected Icon getIcon(String item) {
    return Icons.SpringProfile;
  }

  @Override
  public Collection<String> getItems(String prefix, boolean cached, CompletionParameters parameters) {
    if (prefix == null || myContext.isEmpty())
      return Collections.emptyList();

    String text = filterOutCompletingProfile(parameters.getEditor().getDocument().getText(), parameters.getOffset());
    Set<String> activatedProfiles = ProfileUtils.profilesFromString(text);

    Set<String> profiles = new HashSet<>();
    for (Module module : myContext) {
      profiles.addAll(myProfiles.get(module));
    }
    profiles.removeAll(activatedProfiles);
    profiles.remove(InfraProfile.DEFAULT_PROFILE_NAME);
    List<String> result = new ArrayList<>(profiles);
    result.sort(StringUtil::naturalCompare);
    return result;
  }

  @Override
  public String getPrefix(String text, int offset) {
    int space = text.lastIndexOf(' ', offset - 1) + 1;
    int comma = text.lastIndexOf(',', offset - 1) + 1;
    return text.substring(Math.max(space, comma), offset);
  }

  private Collection<String> getProfiles(Module module) {
    return ApplicationManager.getApplication().runReadAction((Computable<Collection<String>>) () -> {
      List<InfraProfileTarget> targets = InfraProfilesFactory.getInstance().findProfileTargets(module, myIncludeTests);
      return ContainerUtil.map2Set(targets, InfraProfileTarget::getName);
    });
  }

  private static String filterOutCompletingProfile(String text, int offset) {
    int space = text.lastIndexOf(' ', offset - 1) + 1;
    int comma = text.lastIndexOf(',', offset - 1) + 1;
    int before = Math.max(space, comma);
    String result = text.substring(0, before);

    space = text.indexOf(' ', offset);
    comma = text.indexOf(',', offset);
    int after = space < 0 ? comma : comma < 0 ? space : Math.min(space, comma);
    if (after >= 0) {
      result += text.substring(after);
    }
    return result;
  }
}

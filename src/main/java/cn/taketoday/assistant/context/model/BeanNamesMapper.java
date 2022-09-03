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

package cn.taketoday.assistant.context.model;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.intellij.xml.util.PsiElementPointer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.Alias;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

class BeanNamesMapper {
  private final Map<String, BeanPointer<?>> myBeansMap;
  private final Map<String, Pair<String, PsiElementPointer>> myAliasesMap = new HashMap();
  private final MultiMap<String, String> myAllBeanNames;

  public BeanNamesMapper(LocalXmlModel model) {
    Collection<BeanPointer<?>> ownBeans = new HashSet<>(model.getLocalBeans());
    this.myBeansMap = new HashMap(ownBeans.size());
    this.myAllBeanNames = MultiMap.createSet();
    for (BeanPointer<?> bean : ownBeans) {
      String beanName = bean.getName();
      if (StringUtil.isNotEmpty(beanName)) {
        this.myBeansMap.put(beanName, bean);
        this.myAllBeanNames.putValue(beanName, beanName);
        for (String alias : bean.getAliases()) {
          registerAlias(beanName, alias, bean, null);
        }
      }
    }
    PlaceholderUtils placeholderUtils = PlaceholderUtils.getInstance();
    List<Alias> aliases = getAliases(model.getRoot());
    for (Alias anAlias : aliases) {
      String aliasedBean = anAlias.getAliasedBean().getRawText();
      String alias2 = anAlias.getAlias().getRawText();
      if (!placeholderUtils.isDefaultPlaceholder(aliasedBean) && !placeholderUtils.isDefaultPlaceholder(alias2)) {
        registerAlias(aliasedBean, alias2, null, anAlias);
      }
    }
  }

  @Nullable
  BeanPointer<?> getBean(String beanName) {
    String curName = beanName;
    Set<String> visited = null;
    while (true) {
      BeanPointer<?> bean = this.myBeansMap.get(curName);
      if (bean != null) {
        return bean.derive(beanName);
      }
      Pair<String, PsiElementPointer> newName = this.myAliasesMap.get(curName);
      if (newName == null) {
        return null;
      }
      if (visited != null && visited.contains(curName)) {
        return null;
      }
      if (visited == null) {
        visited = new HashSet<>();
      }
      visited.add(curName);
      curName = newName.getFirst();
    }
  }

  public Set<String> getAllBeanNames(String beanName) {
    return (Set) this.myAllBeanNames.get(beanName);
  }

  private void registerAlias(String beanName, String alias, @Nullable BeanPointer<?> bean, @Nullable Alias anAlias) {
    if (!StringUtil.isNotEmpty(alias) || !StringUtil.isNotEmpty(beanName) || Objects.equals(beanName, alias)) {
      return;
    }
    PsiElementPointer elementPointer = bean == null ? DomService.getInstance().createAnchor(anAlias) : bean;
    this.myAliasesMap.put(alias, Pair.create(beanName, elementPointer));
    this.myBeansMap.get(alias);
    HashSet<String> aliases = new HashSet<>();
    aliases.add(alias);
    while (!this.myBeansMap.containsKey(beanName)) {
      Pair<String, PsiElementPointer> pair = this.myAliasesMap.get(beanName);
      if (pair == null) {
        return;
      }
      beanName = pair.getFirst();
      aliases.add(beanName);
    }
    this.myAllBeanNames.putValues(beanName, aliases);
  }

  private static List<Alias> getAliases(@Nullable DomFileElement<Beans> fileElement) {
    if (fileElement == null) {
      return Collections.emptyList();
    }
    List<Alias> list = fileElement.getRootElement().getAliases();
    if (list.isEmpty()) {
      return Collections.emptyList();
    }
    return new SmartList<>(list);
  }
}

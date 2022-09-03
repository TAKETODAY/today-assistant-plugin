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

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.NotNullFunction;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnClass;
import cn.taketoday.assistant.model.config.autoconfigure.jam.AutoConfigureAfter;
import cn.taketoday.assistant.model.config.autoconfigure.jam.AutoConfigureBefore;
import cn.taketoday.assistant.model.config.autoconfigure.jam.AutoConfigureOrder;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/28 13:18
 */
public class AutoConfigClassSorter {
  private final List<PsiClass> myConfigurationClasses;
  private final Integer myAutoConfigureOrderDefault;
  private final GlobalSearchScope mySearchScope;
  private final JavaPsiFacade myJavaPsiFacade;
  private final Map<PsiClass, Integer> myAutoConfigureOrderCache = FactoryMap.create(new NotNullFunction<PsiClass, Integer>() {

    public Integer fun(PsiClass psiClass) {
      AutoConfigureMetadataIndex.AutoConfigureMetadata metadata = AutoConfigClassSorter.this.findMetadata(psiClass);
      if (metadata != null) {
        return ObjectUtils.notNull(metadata.getAutoConfigureOrder(), AutoConfigClassSorter.this.myAutoConfigureOrderDefault);
      }
      AutoConfigureOrder autoConfigureOrder = AutoConfigureOrder.CLASS_META.getJamElement(psiClass);
      if (autoConfigureOrder == null) {
        return AutoConfigClassSorter.this.myAutoConfigureOrderDefault;
      }
      return ObjectUtils.notNull(autoConfigureOrder.getValue(), AutoConfigClassSorter.this.myAutoConfigureOrderDefault);
    }
  });
  private final Comparator<PsiClass> AUTOCONFIGURE_ORDER_COMPARATOR = (o1, o2) -> {
    int order1 = this.myAutoConfigureOrderCache.get(o1);
    int order2 = this.myAutoConfigureOrderCache.get(o2);
    return Comparing.compare(order1, order2);
  };

  private final Map<PsiClass, List<PsiClass>> autoConfigureBeforeClassesCache = new HashMap();
  private final Map<PsiClass, AutoConfigureMetadataIndex.AutoConfigureMetadata> metadataCache = FactoryMap.create(AutoConfigureMetadataIndex::findMetadata);

  AutoConfigClassSorter(Module module, List<PsiClass> configClasses) {
    this.myAutoConfigureOrderDefault = getOrderDefaultValue(module);
    this.mySearchScope = GlobalSearchScope.moduleRuntimeScope(module, true);
    this.myJavaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
    this.myConfigurationClasses = InfraAutoConfigClassFilterService.of().filterByConditionalOnClass(module, configClasses);
    for (PsiClass config : this.myConfigurationClasses) {
      AutoConfigureMetadataIndex.AutoConfigureMetadata metadata = findMetadata(config);
      if (metadata != null) {
        List<String> autoConfigureBefore = metadata.getAutoConfigureBefore();
        if (!autoConfigureBefore.isEmpty()) {
          List<PsiClass> before = new ArrayList<>(autoConfigureBefore.size());
          for (String fqn : autoConfigureBefore) {
            ContainerUtil.addIfNotNull(before, this.myJavaPsiFacade.findClass(fqn, this.mySearchScope));
          }
          this.autoConfigureBeforeClassesCache.put(config, before);
        }
      }
      else {
        AutoConfigureBefore autoConfigureBefore2 = AutoConfigureBefore.META.getJamElement(config);
        if (autoConfigureBefore2 != null) {
          this.autoConfigureBeforeClassesCache.put(config, autoConfigureBefore2.getClasses());
        }
      }
    }
  }

  List<PsiClass> getSortedConfigs() {
    List<PsiClass> configs = new ArrayList<>(this.myConfigurationClasses);
    configs.sort(InfraOrderClassSorter.CLASS_NAME_COMPARATOR);
    configs.sort(this.AUTOCONFIGURE_ORDER_COMPARATOR);
    return sortByAutoConfigureAfterBefore(configs);
  }

  static boolean passesConditionalClassMatch(PsiClass config) {
    return passesConditionalClassMatch(ConditionalOnClass.CLASS_META.getJamElement(config));
  }

  static boolean passesConditionalClassMatch(PsiMethod bean) {
    return passesConditionalClassMatch(ConditionalOnClass.METHOD_META.getJamElement(bean));
  }

  private static boolean passesConditionalClassMatch(ConditionalOnClass conditionalOnClass) {
    if (conditionalOnClass == null) {
      return true;
    }
    for (PsiClass psiClass : conditionalOnClass.getValue()) {
      if (psiClass == null) {
        return false;
      }
    }
    for (PsiClass psiClass2 : conditionalOnClass.getName()) {
      if (psiClass2 == null) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  private AutoConfigureMetadataIndex.AutoConfigureMetadata findMetadata(PsiClass psiClass) {
    return this.metadataCache.get(psiClass);
  }

  private static Integer getOrderDefaultValue(Module module) {
    PsiClass autoConfigureOrder = InfraUtils.findLibraryClass(module, InfraConfigConstant.AUTO_CONFIGURE_ORDER);
    if (autoConfigureOrder == null) {
      return getOrderDefaultValueFallbackByVersion(module);
    }
    PsiMethod[] methods = autoConfigureOrder.findMethodsByName("value", false);
    PsiMethod element = ArrayUtil.getFirstElement(methods);
    if (element instanceof PsiAnnotationMethod psiAnnotationMethod) {
      PsiAnnotationMemberValue annotationMemberValue = psiAnnotationMethod.getDefaultValue();
      Integer defaultValue = JamCommonUtil.getObjectValue(annotationMemberValue, Integer.class);
      if (defaultValue != null) {
        return defaultValue;
      }
    }
    return getOrderDefaultValueFallbackByVersion(module);
  }

  private static Integer getOrderDefaultValueFallbackByVersion(Module module) {
    return 0;
  }

  private List<PsiClass> sortByAutoConfigureAfterBefore(List<PsiClass> configs) {
    Set<PsiClass> toSort = new LinkedHashSet<>(configs);
    List<PsiClass> sorted = new ArrayList<>();
    while (!toSort.isEmpty()) {
      PsiClass next = ContainerUtil.getFirstItem(toSort);
      doSortByAfterAnnotation(toSort, sorted, next);
    }
    return sorted;
  }

  private void doSortByAfterAnnotation(Set<PsiClass> toSort, List<PsiClass> sorted, PsiClass current) {
    toSort.remove(current);
    for (PsiClass after : getClassesRequestedAfter(current)) {
      if (toSort.contains(after)) {
        doSortByAfterAnnotation(toSort, sorted, after);
      }
    }
    sorted.add(current);
  }

  private Set<PsiClass> getClassesRequestedAfter(PsiClass config) {
    Set<PsiClass> rtn = getAutoConfigureAfterValues(config);
    for (Map.Entry<PsiClass, List<PsiClass>> entry : this.autoConfigureBeforeClassesCache.entrySet()) {
      if (entry.getValue().contains(config)) {
        rtn.add(entry.getKey());
      }
    }
    return rtn;
  }

  private Set<PsiClass> getAutoConfigureAfterValues(PsiClass config) {
    Set<PsiClass> rtn = new LinkedHashSet<>();
    AutoConfigureMetadataIndex.AutoConfigureMetadata metadata = findMetadata(config);
    if (metadata != null) {
      for (String qn : metadata.getAutoConfigureAfter()) {
        ContainerUtil.addIfNotNull(rtn, this.myJavaPsiFacade.findClass(qn, this.mySearchScope));
      }
      return rtn;
    }
    AutoConfigureAfter autoConfigureAfter = AutoConfigureAfter.META.getJamElement(config);
    if (autoConfigureAfter != null) {
      rtn.addAll(autoConfigureAfter.getClasses());
    }
    return rtn;
  }
}

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

package cn.taketoday.assistant.beans.stereotype;

import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.jam.converters.PackageJamConverter;
import cn.taketoday.assistant.model.jam.stereotype.AbstractComponentScan;
import cn.taketoday.assistant.model.jam.stereotype.FilterType;
import cn.taketoday.assistant.model.jam.stereotype.ComponentScanFilter;
import cn.taketoday.assistant.model.jam.utils.filters.ExcludeAnnotationsFilter;
import cn.taketoday.assistant.model.jam.utils.filters.ExcludeAssignableFilter;
import cn.taketoday.assistant.model.jam.utils.filters.IncludeAnnotationsFilter;
import cn.taketoday.assistant.model.jam.utils.filters.IncludeAssignableFilter;
import cn.taketoday.assistant.model.jam.utils.filters.IncludeCustomFilter;
import cn.taketoday.assistant.model.jam.utils.filters.InfraContextFilter;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 22:43
 */
public abstract class InfraJamComponentScan extends AbstractComponentScan {
  public static final SemKey<JamAnnotationMeta> META_KEY = COMPONENT_SCAN_META_KEY.subKey("JamComponentScan");

  protected static final JamStringAttributeMeta.Collection<Collection<PsiPackage>> VALUE_ATTRIBUTE_META =
          new JamStringAttributeMeta.Collection<>(VALUE_ATTR_NAME, new PackageJamConverter());

  protected static final JamStringAttributeMeta.Collection<Collection<PsiPackage>> BASE_PACKAGE_ATTR_META =
          new JamStringAttributeMeta.Collection<>(BASE_PACKAGES_ATTR_NAME, new PackageJamConverter());

  private static final JamClassAttributeMeta.Collection BASE_PACKAGE_CLASS_ATTR_META =
          JamAttributeMeta.classCollection(BASE_PACKAGE_CLASSES_ATTR_NAME);

  protected static final JamBooleanAttributeMeta USE_DEFAULT_FILTERS_META =
          JamAttributeMeta.singleBoolean("useDefaultFilters", true);

  protected static final JamBooleanAttributeMeta LAZY_INIT_ATTR_META =
          JamAttributeMeta.singleBoolean("lazyInit", false);

  protected static final JamStringAttributeMeta.Single<String> RESOURCE_PATTERN_META =
          JamAttributeMeta.singleString("resourcePattern");

  protected static final JamEnumAttributeMeta.Single<ScopedProxyMode> SCOPED_PROXY_META =
          JamAttributeMeta.singleEnum("scopedProxy", ScopedProxyMode.class);

  protected static final JamClassAttributeMeta.Single NAME_GENERATOR_META =
          JamAttributeMeta.singleClass("nameGenerator");

  protected static final JamClassAttributeMeta.Single SCOPE_RESOLVER_META =
          JamAttributeMeta.singleClass("scopeResolver");

  protected static final JamAttributeMeta<List<ComponentScanFilter>> INCLUDE_FILTERS_ATTR_META =
          JamAttributeMeta.annoCollection("includeFilters", ComponentScanFilter.ANNOTATION_META, ComponentScanFilter.class);

  protected static final JamAttributeMeta<List<ComponentScanFilter>> EXCLUDE_FILTERS_ATTR_META =
          JamAttributeMeta.annoCollection("excludeFilters", ComponentScanFilter.ANNOTATION_META, ComponentScanFilter.class);

  public static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype()
          .addAttribute(VALUE_ATTRIBUTE_META)
          .addAttribute(BASE_PACKAGE_ATTR_META)
          .addAttribute(USE_DEFAULT_FILTERS_META)
          .addAttribute(LAZY_INIT_ATTR_META)
          .addAttribute(RESOURCE_PATTERN_META)
          .addAttribute(SCOPED_PROXY_META)
          .addAttribute(NAME_GENERATOR_META)
          .addAttribute(SCOPE_RESOLVER_META)
          .addAttribute(INCLUDE_FILTERS_ATTR_META)
          .addAttribute(EXCLUDE_FILTERS_ATTR_META);

  private final PsiElementRef<PsiAnnotation> myAnnotation;

  public InfraJamComponentScan(PsiClass psiElement) {
    super(psiElement);
    myAnnotation = getAnnotationMeta().getAnnotationRef(psiElement);
  }

  public InfraJamComponentScan(PsiAnnotation annotation) {
    super(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true));
    myAnnotation = PsiElementRef.real(annotation);
  }

  @Override
  protected JamClassAttributeMeta.Collection getBasePackageClassMeta() {
    return BASE_PACKAGE_CLASS_ATTR_META;
  }

  /**
   * Returns all attributes containing package definitions to scan.
   */
  @Override

  public List<JamStringAttributeMeta.Collection<Collection<PsiPackage>>> getPackageJamAttributes() {
    return ContainerUtil.immutableList(VALUE_ATTRIBUTE_META, BASE_PACKAGE_ATTR_META);
  }

  @Override
  public boolean useDefaultFilters() {
    return USE_DEFAULT_FILTERS_META.getJam(myAnnotation).getValue();
  }

  public boolean isLazyInit() {
    return LAZY_INIT_ATTR_META.getJam(myAnnotation).getValue();
  }

  @Nullable
  public String getResourcePattern() {
    return RESOURCE_PATTERN_META.getJam(myAnnotation).getValue();
  }

  @Nullable
  public ScopedProxyMode getScopedProxy() {
    return SCOPED_PROXY_META.getJam(myAnnotation).getValue();
  }

  @Nullable
  public PsiClass getNameGenerator() {
    return NAME_GENERATOR_META.getJam(myAnnotation).getValue();
  }

  @Nullable
  public PsiClass getScopeResolver() {
    return SCOPE_RESOLVER_META.getJam(myAnnotation).getValue();
  }

  @Override
  public Set<InfraContextFilter.Exclude> getExcludeContextFilters() {
    Set<InfraContextFilter.Exclude> excludes = new LinkedHashSet<>();
    for (ComponentScanFilter filter : EXCLUDE_FILTERS_ATTR_META.getJam(myAnnotation)) {
      final FilterType value = filter.getFilterType();
      final Set<PsiClass> classes = filter.getFilteredClasses();
      if (value == FilterType.ASSIGNABLE_TYPE) {
        excludes.add(new ExcludeAssignableFilter(classes));
      }
      else if (value == FilterType.ANNOTATION) {
        excludes.add(new ExcludeAnnotationsFilter(classes));
      }
    }

    return excludes;
  }

  @Override
  public Set<InfraContextFilter.Include> getIncludeContextFilters() {
    Set<InfraContextFilter.Include> includes = new LinkedHashSet<>();
    for (ComponentScanFilter filter : INCLUDE_FILTERS_ATTR_META.getJam(myAnnotation)) {
      final FilterType value = filter.getFilterType();
      final Set<PsiClass> classes = filter.getFilteredClasses();
      if (value == FilterType.ASSIGNABLE_TYPE) {
        includes.add(new IncludeAssignableFilter(classes));
      }
      else if (value == FilterType.ANNOTATION) {
        includes.add(new IncludeAnnotationsFilter(classes));
      }
      else if (value == FilterType.CUSTOM) {
        includes.add(new IncludeCustomFilter(classes));
      }
    }

    return includes;
  }

  @Override

  public PsiElementRef<PsiAnnotation> getAnnotationRef() {
    return myAnnotation;
  }

  /**
   * {@link cn.taketoday.context.annotation.ScopedProxyMode}
   */
  public enum ScopedProxyMode {
    DEFAULT,
    NO,
    INTERFACES,
    TARGET_CLASS
  }
}

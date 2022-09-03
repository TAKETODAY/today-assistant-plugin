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

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifier;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;

import java.util.Collection;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.stereotype.PropertySource;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:53
 */
@Presentation(typeName = PresentationConstant.CONFIGURATION)
public class Configuration extends InfraMetaStereotypeComponent {

  private static final String PROXY_BEAN_METHODS_ATTR_NAME = "proxyBeanMethods";

  public static final SemKey<JamMemberMeta<PsiClass, Configuration>> META_KEY =
          JamService.ALIASING_MEMBER_META_KEY.subKey("ConfigurationMeta");

  /**
   * @see InfraUtils#isConfiguration(PsiClass)
   * @see InfraUtils#isConfigurationOrMeta(PsiClass)
   */
  public static final SemKey<Configuration> JAM_KEY = JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("ConfigurationBean");

  public static final JamClassMeta<Configuration> META =
          new JamClassMeta<>(null, Configuration.class, JAM_KEY);

  private static final Function<Module, Collection<String>> ANNOTATIONS =
          module -> getAnnotations(module, AnnotationConstant.CONFIGURATION);

  private static final JamBooleanAttributeMeta PROXY_BEAN_METHODS_ATTR_META =
          new JamBooleanAttributeMeta(PROXY_BEAN_METHODS_ATTR_NAME, true);

  public Configuration(PsiClass psiClass) {
    super(AnnotationConstant.CONFIGURATION, psiClass);
  }

  public Configuration(Pair<String, PsiClass> pair) {
    super(pair.getFirst(), pair.getSecond());
  }

  public Configuration(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public Collection<PropertySource> getPropertySources() {
    return InfraJamService.of().getPropertySources(getPsiElement());
  }

  public static Function<Module, Collection<String>> getAnnotations() {
    return ANNOTATIONS;
  }

  @Nullable
  private PsiElementRef<PsiAnnotation> getPsiAnnotationRef() {
    JamAnnotationMeta meta = getMeta();
    return meta == null ? null : meta.getAnnotationRef(getPsiElement());
  }

  public boolean isProxyBeanMethods() {
    PsiElementRef<PsiAnnotation> annotationRef = getPsiAnnotationRef();
    if (annotationRef != null) {
      return PROXY_BEAN_METHODS_ATTR_META.getJam(annotationRef).getValue();
    }
    return false;
  }

  @Override
  @Nullable
  public String getBeanName() {
    if (!isValid()) {
      return null;
    }
    return StringUtil.decapitalize(StringUtil.notNullize(getConfigurationName(getPsiElement())));
  }

  @Nullable
  private static String getConfigurationName(PsiClass psiClass) {
    if (psiClass.hasModifierProperty(PsiModifier.STATIC)) {
      PsiClass containingClass = psiClass.getContainingClass();
      if (containingClass != null) {
        return getConfigurationName(containingClass) + "." + psiClass.getName();
      }
    }
    return psiClass.getName();
  }
}

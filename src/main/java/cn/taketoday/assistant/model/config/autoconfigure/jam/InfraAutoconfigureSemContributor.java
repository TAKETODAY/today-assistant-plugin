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

package cn.taketoday.assistant.model.config.autoconfigure.jam;

import com.intellij.jam.JamElement;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.Conditional;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnBean;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnClass;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnEnabledHealthIndicator;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnEnabledInfoContributor;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnEnabledResourceChain;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnExpression;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnManagementPort;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnMissingBean;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnMissingClass;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnNotWebApplication;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnProperty;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnResource;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnSingleCandidate;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnWebApplication;
import cn.taketoday.assistant.model.config.properties.ConfigurationProperties;
import cn.taketoday.assistant.model.config.properties.ConfigurationPropertiesScan;
import cn.taketoday.assistant.model.config.properties.CustomConfigurationPropertiesScan;
import cn.taketoday.assistant.model.config.properties.EnableConfigurationProperties;
import cn.taketoday.assistant.model.config.properties.NestedConfigurationProperty;
import cn.taketoday.assistant.model.jam.SemContributorUtil;

import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.AUTO_CONFIGURE_AFTER;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.AUTO_CONFIGURE_BEFORE;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.AUTO_CONFIGURE_ORDER;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_BEAN;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_CLASS;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR_SB2;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_ENABLED_INFO_CONTRIBUTOR;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_ENABLED_INFO_CONTRIBUTOR_SB2;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_ENABLED_RESOURCE_CHAIN;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_EXPRESSION;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_MANAGEMENT_PORT;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_MISSING_BEAN;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_MISSING_CLASS;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_NOT_WEB_APPLICATION;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_PROPERTY;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_RESOURCE;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_SINGLE_CANDIDATE;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.CONDITIONAL_ON_WEB_APPLICATION;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.ENABLE_AUTO_CONFIGURATION;
import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.IMPORT_AUTO_CONFIGURATION;

final class InfraAutoconfigureSemContributor extends SemContributor {

  @Override
  public void registerSemProviders(SemRegistrar registrar, Project project) {
    InfraApplication.register(registrar);

    SemService semService = SemService.getSemService(project);

    ConfigurationProperties.CLASS_META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(InfraClassesConstants.CONFIGURATION_PROPERTIES));
    ConfigurationProperties.Method.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraClassesConstants.CONFIGURATION_PROPERTIES));
    NestedConfigurationProperty.FIELD_META.register(registrar, PsiJavaPatterns.psiField().withoutModifiers("static").withAnnotation(InfraClassesConstants.NESTED_CONFIGURATION_PROPERTY));

    EnableConfigurationProperties.CLASS_META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(InfraClassesConstants.ENABLE_CONFIGURATION_PROPERTIES));

    registerConfigurationPropertiesScan(registrar, semService);
    registerEnableAutoConfiguration(registrar, semService);
    registerImportAutoConfiguration(registrar, semService);

    AutoConfigureOrder.CLASS_META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AUTO_CONFIGURE_ORDER));
    AutoConfigureOrder.FIELD_META.register(registrar, PsiJavaPatterns.psiField().withAnnotation(AUTO_CONFIGURE_ORDER));
    AutoConfigureOrder.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(AUTO_CONFIGURE_ORDER));
    AutoConfigureAfter.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AUTO_CONFIGURE_AFTER));
    AutoConfigureBefore.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AUTO_CONFIGURE_BEFORE));

    registerConditionalOn(registrar, AnnotationConstant.CONDITIONAL, Conditional.CLASS_META, Conditional.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_BEAN, ConditionalOnBean.CLASS_META, ConditionalOnBean.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_MISSING_BEAN, ConditionalOnMissingBean.CLASS_META, ConditionalOnMissingBean.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_CLASS, ConditionalOnClass.CLASS_META, ConditionalOnClass.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_MISSING_CLASS, ConditionalOnMissingClass.CLASS_META, ConditionalOnMissingClass.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_EXPRESSION, ConditionalOnExpression.CLASS_META, ConditionalOnExpression.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_PROPERTY, ConditionalOnProperty.CLASS_META, ConditionalOnProperty.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_RESOURCE, ConditionalOnResource.CLASS_META, ConditionalOnResource.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_SINGLE_CANDIDATE, ConditionalOnSingleCandidate.CLASS_META, ConditionalOnSingleCandidate.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_WEB_APPLICATION, ConditionalOnWebApplication.CLASS_META, ConditionalOnWebApplication.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_NOT_WEB_APPLICATION, ConditionalOnNotWebApplication.CLASS_META, ConditionalOnNotWebApplication.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_ENABLED_RESOURCE_CHAIN, ConditionalOnEnabledResourceChain.CLASS_META, ConditionalOnEnabledResourceChain.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR, ConditionalOnEnabledHealthIndicator.CLASS_META,
            ConditionalOnEnabledHealthIndicator.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR_SB2, ConditionalOnEnabledHealthIndicator.CLASS_META,
            ConditionalOnEnabledHealthIndicator.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_ENABLED_INFO_CONTRIBUTOR, ConditionalOnEnabledInfoContributor.CLASS_META,
            ConditionalOnEnabledInfoContributor.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_ENABLED_INFO_CONTRIBUTOR_SB2, ConditionalOnEnabledInfoContributor.CLASS_META,
            ConditionalOnEnabledInfoContributor.METHOD_META);
    registerConditionalOn(registrar, CONDITIONAL_ON_MANAGEMENT_PORT, ConditionalOnManagementPort.CLASS_META, ConditionalOnManagementPort.METHOD_META);
  }

  private static void registerEnableAutoConfiguration(SemRegistrar registrar, SemService semService) {
    EnableAutoConfiguration.META.register(registrar,
            PsiJavaPatterns.psiClass().withAnnotation(ENABLE_AUTO_CONFIGURATION));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), EnableAutoConfiguration.META_KEY,
            EnableAutoConfiguration.JAM_KEY, SemContributorUtil.createFunction(
                    EnableAutoConfiguration.JAM_KEY, EnableAutoConfiguration.class,
                    EnableAutoConfiguration.getAnnotations(), pair -> new EnableAutoConfiguration(pair.first, pair.getSecond()), null));
  }

  private static void registerImportAutoConfiguration(SemRegistrar registrar, SemService semService) {
    ImportAutoConfiguration.META.register(registrar,
            PsiJavaPatterns.psiClass().withAnnotation(IMPORT_AUTO_CONFIGURATION));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), ImportAutoConfiguration.META_KEY,
            ImportAutoConfiguration.JAM_KEY, SemContributorUtil.createFunction(
                    ImportAutoConfiguration.JAM_KEY, ImportAutoConfiguration.class,
                    ImportAutoConfiguration.getAnnotations(), pair -> {
                      return new ImportAutoConfiguration(pair.first, pair.getSecond());
                    }, null));
  }

  private static void registerConfigurationPropertiesScan(SemRegistrar registrar, SemService semService) {
    ConfigurationPropertiesScan.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(InfraClassesConstants.CONFIGURATION_PROPERTIES_SCAN));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), CustomConfigurationPropertiesScan.META_KEY, CustomConfigurationPropertiesScan.JAM_KEY,
            SemContributorUtil.createFunction(CustomConfigurationPropertiesScan.JAM_KEY, CustomConfigurationPropertiesScan.class,
                    SemContributorUtil.getCustomMetaAnnotations(InfraClassesConstants.CONFIGURATION_PROPERTIES_SCAN),
                    pair -> {
                      return new CustomConfigurationPropertiesScan(pair.first, pair.second);
                    }, null, AliasForUtils.getAnnotationMetaProducer(CustomConfigurationPropertiesScan.JAM_ANNO_META_KEY, ConfigurationPropertiesScan.META)));
  }

  private static <T extends JamElement> void registerConditionalOn(SemRegistrar registrar, String annotationFQN, JamClassMeta<T> classMeta, JamMethodMeta<T> methodMeta) {
    classMeta.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(annotationFQN));
    methodMeta.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(annotationFQN));
  }
}

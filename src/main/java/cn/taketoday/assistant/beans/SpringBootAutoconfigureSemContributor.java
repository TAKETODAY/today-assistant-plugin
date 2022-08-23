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

package cn.taketoday.assistant.beans;

import com.intellij.jam.JamElement;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.Conditional;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnBean;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnClass;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnEnabledHealthIndicator;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnEnabledInfoContributor;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnEnabledResourceChain;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnExpression;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnInitializedRestarter;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnManagementPort;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnMissingBean;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnMissingClass;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnNotWebApplication;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnProperty;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnRepositoryType;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnResource;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnSingleCandidate;
import com.intellij.spring.boot.model.autoconfigure.conditions.jam.ConditionalOnWebApplication;
import com.intellij.spring.boot.model.autoconfigure.jam.AutoConfigureAfter;
import com.intellij.spring.boot.model.autoconfigure.jam.AutoConfigureBefore;
import com.intellij.spring.boot.model.autoconfigure.jam.AutoConfigureOrder;
import com.intellij.spring.boot.model.autoconfigure.jam.EnableAutoConfiguration;
import com.intellij.spring.boot.model.autoconfigure.jam.ImportAutoConfiguration;
import com.intellij.spring.boot.model.autoconfigure.jam.SpringBootApplication;
import com.intellij.spring.boot.model.properties.jam.ConfigurationProperties;
import com.intellij.spring.boot.model.properties.jam.ConfigurationPropertiesScan;
import com.intellij.spring.boot.model.properties.jam.CustomConfigurationPropertiesScan;
import com.intellij.spring.boot.model.properties.jam.EnableConfigurationProperties;
import com.intellij.spring.boot.model.properties.jam.NestedConfigurationProperty;
import com.intellij.spring.boot.model.testing.jam.SpringBootstrapWithTest;
import com.intellij.spring.boot.model.testing.jam.custom.SpringApplicationConfiguration;
import com.intellij.spring.boot.model.testing.jam.mock.CustomJamMockBean;
import com.intellij.spring.boot.model.testing.jam.mock.SpringJamMockBean;
import com.intellij.spring.boot.model.testing.jam.mock.SpringJamMockBeans;
import com.intellij.util.Consumer;

import cn.taketoday.assistant.AliasForUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 21:24
 */

final class SpringBootAutoconfigureSemContributor extends SemContributor {

  public void registerSemProviders(SemRegistrar registrar, Project project) {

    SpringBootApplication.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.autoconfigure.SpringBootApplication"));
    SemService semService = SemService.getSemService(project);
    registerTesting(registrar, semService);
    ConfigurationProperties.CLASS_META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.context.properties.ConfigurationProperties"));
    ConfigurationProperties.Method.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation("cn.taketoday.boot.context.properties.ConfigurationProperties"));
    NestedConfigurationProperty.FIELD_META.register(registrar,
            PsiJavaPatterns.psiField().withoutModifiers(new String[] { "static" }).withAnnotation("cn.taketoday.boot.context.properties.NestedConfigurationProperty"));
    EnableConfigurationProperties.CLASS_META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.context.properties.EnableConfigurationProperties"));
    registerConfigurationPropertiesScan(registrar, semService);
    registerEnableAutoConfiguration(registrar, semService);
    registerImportAutoConfiguration(registrar, semService);

    AutoConfigureOrder.CLASS_META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.autoconfigure.AutoConfigureOrder"));
    AutoConfigureOrder.FIELD_META.register(registrar, PsiJavaPatterns.psiField().withAnnotation("cn.taketoday.boot.autoconfigure.AutoConfigureOrder"));
    AutoConfigureOrder.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation("cn.taketoday.boot.autoconfigure.AutoConfigureOrder"));
    AutoConfigureAfter.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.autoconfigure.AutoConfigureAfter"));
    AutoConfigureBefore.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.autoconfigure.AutoConfigureBefore"));

    registerConditionalOn(registrar, "cn.taketoday.context.annotation.Conditional", Conditional.CLASS_META, Conditional.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnBean", ConditionalOnBean.CLASS_META, ConditionalOnBean.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnMissingBean", ConditionalOnMissingBean.CLASS_META, ConditionalOnMissingBean.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnClass", ConditionalOnClass.CLASS_META, ConditionalOnClass.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnMissingClass", ConditionalOnMissingClass.CLASS_META, ConditionalOnMissingClass.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnExpression", ConditionalOnExpression.CLASS_META, ConditionalOnExpression.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnProperty", ConditionalOnProperty.CLASS_META, ConditionalOnProperty.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnResource", ConditionalOnResource.CLASS_META, ConditionalOnResource.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnSingleCandidate", ConditionalOnSingleCandidate.CLASS_META,
            ConditionalOnSingleCandidate.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnWebApplication", ConditionalOnWebApplication.CLASS_META, ConditionalOnWebApplication.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.context.condition.ConditionalOnNotWebApplication", ConditionalOnNotWebApplication.CLASS_META, ConditionalOnNotWebApplication.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.devtools.restart.ConditionalOnInitializedRestarter", ConditionalOnInitializedRestarter.CLASS_META,
            ConditionalOnInitializedRestarter.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.autoconfigure.web.ConditionalOnEnabledResourceChain", ConditionalOnEnabledResourceChain.CLASS_META,
            ConditionalOnEnabledResourceChain.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.autoconfigure.data.ConditionalOnRepositoryType", ConditionalOnRepositoryType.CLASS_META, ConditionalOnRepositoryType.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.actuate.autoconfigure.ConditionalOnEnabledHealthIndicator", ConditionalOnEnabledHealthIndicator.CLASS_META,
            ConditionalOnEnabledHealthIndicator.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator", ConditionalOnEnabledHealthIndicator.CLASS_META,
            ConditionalOnEnabledHealthIndicator.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.actuate.autoconfigure.ConditionalOnEnabledInfoContributor", ConditionalOnEnabledInfoContributor.CLASS_META,
            ConditionalOnEnabledInfoContributor.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor", ConditionalOnEnabledInfoContributor.CLASS_META,
            ConditionalOnEnabledInfoContributor.METHOD_META);
    registerConditionalOn(registrar, "cn.taketoday.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort", ConditionalOnManagementPort.CLASS_META,
            ConditionalOnManagementPort.METHOD_META);
  }

  private static void registerTesting(SemRegistrar registrar, SemService semService) {

    registerSpringApplicationConfiguration(registrar, semService);
    registerBootstrapWithTests(registrar, semService);
    registerMockBeans(registrar, semService);
  }

  private static void registerSpringApplicationConfiguration(SemRegistrar registrar, SemService semService) {

    SpringApplicationConfiguration.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.test.SpringApplicationConfiguration"));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), SpringApplicationConfiguration.META_KEY,
            SpringApplicationConfiguration.SPRING_APP_JAM_KEY,
            SemContributorUtil.createFunction(SpringApplicationConfiguration.SPRING_APP_JAM_KEY, SpringApplicationConfiguration.class,
                    SemContributorUtil.getCustomMetaAnnotations("cn.taketoday.boot.test.SpringApplicationConfiguration", true, false), (pair) -> {
                      return new SpringApplicationConfiguration(pair.first, (PsiClass) pair.second);
                    }, (Consumer) null));
  }

  private static void registerBootstrapWithTests(SemRegistrar registrar, SemService semService) {

    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), SpringBootstrapWithTest.META_KEY, SpringBootstrapWithTest.JAM_KEY,
            SemContributorUtil.createFunction(SpringBootstrapWithTest.JAM_KEY, SpringBootstrapWithTest.class,
                    SemContributorUtil.getCustomMetaAnnotations("cn.taketoday.test.context.BootstrapWith", true, false), (pair) -> {
                      return new SpringBootstrapWithTest(pair.first, (PsiClass) pair.second);
                    }, (Consumer) null));
  }

  private static void registerMockBeans(SemRegistrar registrar, SemService semService) {

    registrar.registerSemElementProvider(SpringJamMockBean.JAM_KEY, PsiJavaPatterns.psiAnnotation().qName("cn.taketoday.boot.test.mock.mockito.MockBean"), SpringJamMockBean::new);
    SpringJamMockBeans.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.test.mock.mockito.MockBeans"));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), CustomJamMockBean.META_KEY, CustomJamMockBean.JAM_KEY,
            SemContributorUtil.createFunction(CustomJamMockBean.JAM_KEY, CustomJamMockBean.class,
                    SemContributorUtil.getCustomMetaAnnotations("cn.taketoday.boot.test.mock.mockito.MockBean", true, false), (pair) -> {
                      return new CustomJamMockBean(pair.first, (PsiClass) pair.second);
                    }, (Consumer) null));
  }

  private static void registerEnableAutoConfiguration(SemRegistrar registrar, SemService semService) {
    EnableAutoConfiguration.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.autoconfigure.EnableAutoConfiguration"));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), EnableAutoConfiguration.META_KEY, EnableAutoConfiguration.JAM_KEY,
            SemContributorUtil.createFunction(EnableAutoConfiguration.JAM_KEY, EnableAutoConfiguration.class, EnableAutoConfiguration.getAnnotations(), (pair) -> {
              return new EnableAutoConfiguration(pair.first, (PsiClass) pair.getSecond());
            }, (Consumer) null));
  }

  private static void registerImportAutoConfiguration(SemRegistrar registrar, SemService semService) {
    ImportAutoConfiguration.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.autoconfigure.ImportAutoConfiguration"));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), ImportAutoConfiguration.META_KEY, ImportAutoConfiguration.JAM_KEY,
            SemContributorUtil.createFunction(ImportAutoConfiguration.JAM_KEY, ImportAutoConfiguration.class, ImportAutoConfiguration.getAnnotations(), (pair) -> {
              return new ImportAutoConfiguration(pair.first, (PsiClass) pair.getSecond());
            }, (Consumer) null));
  }

  private static void registerConfigurationPropertiesScan(SemRegistrar registrar, SemService semService) {
    ConfigurationPropertiesScan.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation("cn.taketoday.boot.context.properties.ConfigurationPropertiesScan"));
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass(), CustomConfigurationPropertiesScan.META_KEY,
            CustomConfigurationPropertiesScan.JAM_KEY,
            SemContributorUtil.createFunction(CustomConfigurationPropertiesScan.JAM_KEY, CustomConfigurationPropertiesScan.class,
                    SemContributorUtil.getCustomMetaAnnotations("cn.taketoday.boot.context.properties.ConfigurationPropertiesScan"), (pair) -> {
                      return new CustomConfigurationPropertiesScan(pair.first, (PsiClass) pair.second);
                    }, (Consumer) null, AliasForUtils.getAnnotationMetaProducer(CustomConfigurationPropertiesScan.JAM_ANNO_META_KEY, ConfigurationPropertiesScan.META)));
  }

  private static <T extends JamElement> void registerConditionalOn(SemRegistrar registrar, String annotationFQN, JamClassMeta<T> classMeta, JamMethodMeta<T> methodMeta) {
    classMeta.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(annotationFQN));
    methodMeta.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(annotationFQN));
  }
}


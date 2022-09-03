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

import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.PsiMember;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.stereotype.Component;
import cn.taketoday.assistant.beans.stereotype.ComponentScan;
import cn.taketoday.assistant.beans.stereotype.ComponentScans;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.ContextImport;
import cn.taketoday.assistant.beans.stereotype.Controller;
import cn.taketoday.assistant.beans.stereotype.ImportResource;
import cn.taketoday.assistant.beans.stereotype.Repository;
import cn.taketoday.assistant.beans.stereotype.Service;
import cn.taketoday.assistant.beans.stereotype.javaee.CdiJakartaNamed;
import cn.taketoday.assistant.beans.stereotype.javaee.CdiJavaxNamed;
import cn.taketoday.assistant.beans.stereotype.javaee.JakartaManagedBean;
import cn.taketoday.assistant.beans.stereotype.javaee.JavaxManagedBean;
import cn.taketoday.assistant.code.event.jam.BeanEventListenerElement;
import cn.taketoday.assistant.code.event.jam.JamEventListenerElement;
import cn.taketoday.assistant.model.extensions.myBatis.InfraMyBatisMapper;
import cn.taketoday.assistant.model.jam.InfraOrder;
import cn.taketoday.assistant.model.jam.SemContributorUtil;
import cn.taketoday.assistant.model.jam.contexts.CustomContextJavaBean;
import cn.taketoday.assistant.model.jam.dependsOn.InfraJamDependsOn;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.jam.lookup.InfraLookupInjection;
import cn.taketoday.assistant.model.jam.profile.CustomContextProfile;
import cn.taketoday.assistant.model.jam.profile.InfraJamProfile;
import cn.taketoday.assistant.model.jam.stereotype.JamPropertySource;
import cn.taketoday.assistant.model.jam.stereotype.PropertySources;
import cn.taketoday.assistant.model.jam.testContexts.InfraContextConfiguration;
import cn.taketoday.assistant.model.jam.testContexts.InfraContextHierarchy;
import cn.taketoday.assistant.model.jam.testContexts.TransactionConfiguration;
import cn.taketoday.assistant.model.jam.testContexts.dirtiesContexts.InfraTestingDirtiesContext;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSql;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSqlConfig;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSqlGroup;
import cn.taketoday.assistant.model.jam.testContexts.profiles.InfraCustomActiveProfiles;
import cn.taketoday.assistant.model.jam.testContexts.profiles.InfraJamActiveProfiles;
import cn.taketoday.assistant.model.jam.testContexts.propertySources.TestPropertySource;
import cn.taketoday.assistant.model.jam.transaction.TransactionalComponent;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 21:03
 */
final class InfraSemContributor extends SemContributor {

  @Override
  public void registerSemProviders(SemRegistrar registrar, Project project) {
    PsiClassPattern nonAnnoClass = PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private");
    PsiClassPattern classPattern = PsiJavaPatterns.psiClass().withoutModifiers("private");

    Component.register(registrar, nonAnnoClass);

    SemService semService = SemService.getSemService(project);
    registerConfigurations(registrar, semService);
    registerServices(registrar, nonAnnoClass, semService);
    registerControllers(registrar, nonAnnoClass, semService);
    registerRepositories(registrar, nonAnnoClass, semService);
    registerProfiles(registrar, semService);
    registerActiveProfiles(registrar, semService);

    InfraJamDependsOn.META.register(registrar, PsiJavaPatterns.psiClass().withoutModifiers(new String[] { "private" }).withAnnotation(AnnotationConstant.DEPENDS_ON));
    CdiJavaxNamed.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAVAX_NAMED));
    CdiJakartaNamed.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAKARTA_NAMED));
    registerContextJavaBeans(registrar, semService);
    registerEventListeners(registrar, semService);

    InfraAliasFor.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withModifiers(new String[] { "public" }).withAnnotation(AnnotationConstant.ALIAS_FOR));

    InfraOrder.CLASS_META.register(registrar, classPattern.withAnnotation(AnnotationConstant.ORDER));
    InfraOrder.FIELD_META.register(registrar, PsiJavaPatterns.psiField().withAnnotation(AnnotationConstant.ORDER));
    InfraOrder.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(AnnotationConstant.ORDER));
    ContextImport.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_IMPORT));
    ImportResource.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_IMPORT_RESOURCE));

    // ComponentScans
    ComponentScan.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.COMPONENT_SCAN));
    registrar.registerSemElementProvider(
            ComponentScan.REPEATABLE_ANNO_JAM_KEY, PsiJavaPatterns.psiAnnotation().qName(AnnotationConstant.COMPONENT_SCAN), ComponentScan::new);
    ComponentScans.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.COMPONENT_SCANS));

    registrar.registerSemElementProvider(JamPropertySource.REPEATABLE_ANNO_JAM_KEY, PsiJavaPatterns.psiAnnotation().qName(AnnotationConstant.PROPERTY_SOURCE), JamPropertySource::new);
    PropertySources.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.PROPERTY_SOURCES));
    JamPropertySource.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.PROPERTY_SOURCE));
    InfraContextConfiguration.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_CONFIGURATION));
    InfraContextHierarchy.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.CONTEXT_HIERARCHY));
    TestPropertySource.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AnnotationConstant.TEST_PROPERTY_SOURCE));
    registrar.registerSemElementProvider(InfraTestingSql.REPEATABLE_ANNO_JAM_KEY, PsiJavaPatterns.psiAnnotation().qName(AnnotationConstant.TEST_SQL), InfraTestingSql::new);
    InfraTestingSqlGroup.CLASS_META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TEST_SQL_GROUP));
    InfraTestingSqlGroup.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.TEST_SQL_GROUP));
    InfraTestingSql.CLASS_META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TEST_SQL));
    InfraTestingSql.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.TEST_SQL));
    InfraTestingDirtiesContext.CLASS_META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.DIRTIES_CONTEXT));
    InfraTestingDirtiesContext.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.DIRTIES_CONTEXT));
    InfraTestingSqlConfig.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TEST_SQL_CONFIG));
    TransactionConfiguration.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TRANSACTION_CONFIGURATION));
    TransactionalComponent.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TRANSACTIONAL));
    JavaxManagedBean.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAVAX_MANAGED_BEAN));
    JakartaManagedBean.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAKARTA_MANAGED_BEAN));
    InfraLookupInjection.META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.LOOKUP_ANNOTATION));
    InfraMyBatisMapper.META.register(registrar, classPattern.withAnnotation(InfraMyBatisMapper.MAPPER_ANNOTATION));
  }

  private static void registerEventListeners(SemRegistrar registrar, SemService semService) {
    PsiMethodPattern methodPattern = PsiJavaPatterns.psiMethod().constructor(false).withModifiers("public");
    PsiMethodPattern onEventMethodPattern = methodPattern.withName(InfraConstant.ON_APPLICATION_EVENT_METHOD).withParameterCount(1)
            .definedInClass(PsiJavaPatterns.psiClass().nonAnnotationType().inheritorOf(true, InfraConstant.APPLICATION_LISTENER));

    JamEventListenerElement.METHOD_META.register(registrar, methodPattern.withAnnotation(AnnotationConstant.EVENT_LISTENER));
    BeanEventListenerElement.METHOD_META.register(registrar, onEventMethodPattern);

  }

  private static void registerProfiles(SemRegistrar registrar, SemService semService) {
    InfraJamProfile.META.register(registrar, PsiJavaPatterns.psiClass().withoutModifiers(new String[] { "private" }).withAnnotation(AnnotationConstant.PROFILE));
    PsiMethodPattern methodPattern = PsiJavaPatterns.psiMethod().constructor(false).withModifiers("public");
    InfraJamProfile.META.register(registrar, methodPattern.withAnnotation(AnnotationConstant.PROFILE));
    registerCustomProfiles(registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private"), semService);
    registerCustomProfiles(registrar, methodPattern, semService);
  }

  private static void registerActiveProfiles(SemRegistrar registrar, SemService semService) {
    InfraJamActiveProfiles.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AnnotationConstant.ACTIVE_PROFILES));
    registerCustomActiveProfiles(registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private"), semService);
  }

  private static void registerContextJavaBeans(SemRegistrar registrar, SemService semService) {
    PsiMethodPattern beanMethodPattern = PsiJavaPatterns.psiMethod().withoutModifiers("private").constructor(false);
    ContextJavaBean.METHOD_META.register(registrar, beanMethodPattern.withAnnotation(AnnotationConstant.COMPONENT));
    registerCustomContextJavaBean(registrar, beanMethodPattern.andNot(PsiJavaPatterns.psiMethod().withAnnotation(AnnotationConstant.COMPONENT)), semService);
  }

  private static void registerCustomProfiles(SemRegistrar registrar, ElementPattern<? extends PsiMember> pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, CustomContextProfile.META_KEY, CustomContextProfile.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextProfile.JAM_KEY, CustomContextProfile.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.PROFILE, true),
                    pair -> new CustomContextProfile(pair.first, pair.second), null,
                    AliasForUtils.getAnnotationMetaProducer(InfraJamProfile.JAM_ANNO_META_KEY, InfraJamProfile.META)));
  }

  private static void registerCustomActiveProfiles(SemRegistrar registrar, ElementPattern<? extends PsiMember> pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, InfraCustomActiveProfiles.META_KEY, InfraCustomActiveProfiles.JAM_KEY,
            SemContributorUtil.createFunction(InfraCustomActiveProfiles.JAM_KEY, InfraCustomActiveProfiles.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.ACTIVE_PROFILES, true),
                    pair -> new InfraCustomActiveProfiles(pair.first, pair.second), null,
                    AliasForUtils.getAnnotationMetaProducer(InfraCustomActiveProfiles.JAM_ANNO_META_KEY, InfraJamActiveProfiles.META)));
  }

  private static void registerCustomContextJavaBean(SemRegistrar registrar, PsiMethodPattern pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, CustomContextJavaBean.META_KEY, CustomContextJavaBean.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextJavaBean.JAM_KEY, CustomContextJavaBean.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.COMPONENT), pair -> new CustomContextJavaBean(pair.first, pair.second), null,
                    AliasForUtils.getAnnotationMetaProducer(CustomContextJavaBean.JAM_ANNO_META_KEY, ContextJavaBean.METHOD_META)));
  }

  private static void registerConfigurations(SemRegistrar registrar, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("abstract"),
            Configuration.META_KEY, Configuration.JAM_KEY,
            SemContributorUtil.createFunction(Configuration.JAM_KEY, Configuration.class, Configuration.getAnnotations(),
                    Configuration::new, SemContributorUtil.createStereotypeConsumer()));

  }

  private static void registerControllers(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, Controller.META_KEY, Controller.JAM_KEY,
            SemContributorUtil.createFunction(Controller.JAM_KEY, Controller.class, Controller.getControllerAnnotations(),
                    Controller::new, SemContributorUtil.createStereotypeConsumer()));
  }

  private static void registerRepositories(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, Repository.META_KEY, Repository.JAM_KEY,
            SemContributorUtil.createFunction(Repository.JAM_KEY, Repository.class, Repository.getRepositoryAnnotations(), Repository::new,
                    SemContributorUtil.createStereotypeConsumer()));
  }

  private static void registerServices(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {

    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, Service.META_KEY, Service.JAM_KEY,
            SemContributorUtil.createFunction(Service.JAM_KEY, Service.class, Service.getServiceAnnotations(), Service::new,
                    SemContributorUtil.createStereotypeConsumer()));

  }
}

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
import com.intellij.spring.model.extensions.myBatis.SpringMyBatisMapper;
import com.intellij.spring.model.jam.SpringOrder;
import com.intellij.spring.model.jam.contexts.CustomContextJavaBean;
import com.intellij.spring.model.jam.dependsOn.SpringJamDependsOn;
import com.intellij.spring.model.jam.javaConfig.ContextJavaBean;
import com.intellij.spring.model.jam.lookup.SpringLookupInjection;
import com.intellij.spring.model.jam.profile.CustomContextProfile;
import com.intellij.spring.model.jam.profile.SpringJamProfile;
import com.intellij.spring.model.jam.stereotype.SpringContextImport;
import com.intellij.spring.model.jam.stereotype.SpringImportResource;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScan;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScans;
import com.intellij.spring.model.jam.stereotype.SpringJamPropertySource;
import com.intellij.spring.model.jam.stereotype.SpringPropertySources;
import com.intellij.spring.model.jam.stereotype.javaee.SpringCdiJakartaNamed;
import com.intellij.spring.model.jam.stereotype.javaee.SpringCdiJavaxNamed;
import com.intellij.spring.model.jam.stereotype.javaee.SpringJakartaManagedBean;
import com.intellij.spring.model.jam.stereotype.javaee.SpringJavaxManagedBean;
import com.intellij.spring.model.jam.testContexts.SpringContextConfiguration;
import com.intellij.spring.model.jam.testContexts.SpringContextHierarchy;
import com.intellij.spring.model.jam.testContexts.SpringTransactionConfiguration;
import com.intellij.spring.model.jam.testContexts.dirtiesContexts.SpringTestingDirtiesContext;
import com.intellij.spring.model.jam.testContexts.jdbc.SpringTestingSql;
import com.intellij.spring.model.jam.testContexts.jdbc.SpringTestingSqlConfig;
import com.intellij.spring.model.jam.testContexts.jdbc.SpringTestingSqlGroup;
import com.intellij.spring.model.jam.testContexts.profiles.SpringCustomActiveProfiles;
import com.intellij.spring.model.jam.testContexts.profiles.SpringJamActiveProfiles;
import com.intellij.spring.model.jam.testContexts.propertySources.SpringTestPropertySource;
import com.intellij.spring.model.jam.transaction.SpringTransactionalComponent;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.stereotype.Component;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.Controller;
import cn.taketoday.assistant.beans.stereotype.ImportResource;
import cn.taketoday.assistant.beans.stereotype.Repository;
import cn.taketoday.assistant.beans.stereotype.Service;
import cn.taketoday.assistant.code.event.jam.BeanEventListenerElement;
import cn.taketoday.assistant.code.event.jam.JamEventListenerElement;

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

    SpringJamDependsOn.META.register(registrar, PsiJavaPatterns.psiClass().withoutModifiers(new String[] { "private" }).withAnnotation(AnnotationConstant.DEPENDS_ON));
    SpringCdiJavaxNamed.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAVAX_NAMED));
    SpringCdiJakartaNamed.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAKARTA_NAMED));
    registerContextJavaBeans(registrar, semService);
    registerEventListeners(registrar, semService);

    InfraAliasFor.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withModifiers(new String[] { "public" }).withAnnotation(AnnotationConstant.ALIAS_FOR));

    SpringOrder.CLASS_META.register(registrar, classPattern.withAnnotation(AnnotationConstant.ORDER));
    SpringOrder.FIELD_META.register(registrar, PsiJavaPatterns.psiField().withAnnotation(AnnotationConstant.ORDER));
    SpringOrder.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(AnnotationConstant.ORDER));
    SpringContextImport.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_IMPORT));
    ImportResource.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_IMPORT_RESOURCE));

    SpringJamComponentScan.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.COMPONENT_SCAN));
    registrar.registerSemElementProvider(SpringJamComponentScan.REPEATABLE_ANNO_JAM_KEY, PsiJavaPatterns.psiAnnotation().qName(AnnotationConstant.COMPONENT_SCAN), SpringJamComponentScan::new);
    SpringJamComponentScans.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.COMPONENT_SCANS));
    registrar.registerSemElementProvider(SpringJamPropertySource.REPEATABLE_ANNO_JAM_KEY, PsiJavaPatterns.psiAnnotation().qName(AnnotationConstant.PROPERTY_SOURCE), SpringJamPropertySource::new);
    SpringPropertySources.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.PROPERTY_SOURCES));
    SpringJamPropertySource.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.PROPERTY_SOURCE));
    SpringContextConfiguration.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_CONFIGURATION));
    SpringContextHierarchy.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.CONTEXT_HIERARCHY));
    SpringTestPropertySource.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AnnotationConstant.TEST_PROPERTY_SOURCE));
    registrar.registerSemElementProvider(SpringTestingSql.REPEATABLE_ANNO_JAM_KEY, PsiJavaPatterns.psiAnnotation().qName(AnnotationConstant.TEST_SQL), SpringTestingSql::new);
    SpringTestingSqlGroup.CLASS_META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TEST_SQL_GROUP));
    SpringTestingSqlGroup.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.TEST_SQL_GROUP));
    SpringTestingSql.CLASS_META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TEST_SQL));
    SpringTestingSql.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.TEST_SQL));
    SpringTestingDirtiesContext.CLASS_META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.DIRTIES_CONTEXT));
    SpringTestingDirtiesContext.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.DIRTIES_CONTEXT));
    SpringTestingSqlConfig.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TEST_SQL_CONFIG));
    SpringTransactionConfiguration.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TRANSACTION_CONFIGURATION));
    SpringTransactionalComponent.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.TRANSACTIONAL));
    SpringJavaxManagedBean.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAVAX_MANAGED_BEAN));
    SpringJakartaManagedBean.META.register(registrar, nonAnnoClass.withAnnotation(JavaeeConstant.JAKARTA_MANAGED_BEAN));
    SpringLookupInjection.META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.LOOKUP_ANNOTATION));
    SpringMyBatisMapper.META.register(registrar, classPattern.withAnnotation(SpringMyBatisMapper.MAPPER_ANNOTATION));
  }

  private static void registerEventListeners(SemRegistrar registrar, SemService semService) {
    PsiMethodPattern methodPattern = PsiJavaPatterns.psiMethod().constructor(false).withModifiers("public");
    PsiMethodPattern onEventMethodPattern = methodPattern.withName(InfraConstant.ON_APPLICATION_EVENT_METHOD).withParameterCount(1)
            .definedInClass(PsiJavaPatterns.psiClass().nonAnnotationType().inheritorOf(true, InfraConstant.APPLICATION_LISTENER));

    JamEventListenerElement.METHOD_META.register(registrar, methodPattern.withAnnotation(AnnotationConstant.EVENT_LISTENER));
    BeanEventListenerElement.METHOD_META.register(registrar, onEventMethodPattern);

  }

  private static void registerProfiles(SemRegistrar registrar, SemService semService) {
    SpringJamProfile.META.register(registrar, PsiJavaPatterns.psiClass().withoutModifiers(new String[] { "private" }).withAnnotation(AnnotationConstant.PROFILE));
    PsiMethodPattern methodPattern = PsiJavaPatterns.psiMethod().constructor(false).withModifiers("public");
    SpringJamProfile.META.register(registrar, methodPattern.withAnnotation(AnnotationConstant.PROFILE));
    registerCustomProfiles(registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private"), semService);
    registerCustomProfiles(registrar, methodPattern, semService);
  }

  private static void registerActiveProfiles(SemRegistrar registrar, SemService semService) {
    SpringJamActiveProfiles.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(AnnotationConstant.ACTIVE_PROFILES));
    registerCustomActiveProfiles(registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private"), semService);
  }

  private static void registerContextJavaBeans(SemRegistrar registrar, SemService semService) {
    PsiMethodPattern beanMethodPattern = PsiJavaPatterns.psiMethod().withoutModifiers("private").constructor(false);
    ContextJavaBean.METHOD_META.register(registrar, beanMethodPattern.withAnnotation(AnnotationConstant.BEAN));
    registerCustomContextJavaBean(registrar, beanMethodPattern.andNot(PsiJavaPatterns.psiMethod().withAnnotation(AnnotationConstant.BEAN)), semService);
  }

  private static void registerCustomProfiles(SemRegistrar registrar, ElementPattern<? extends PsiMember> pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, CustomContextProfile.META_KEY, CustomContextProfile.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextProfile.JAM_KEY, CustomContextProfile.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.PROFILE, true),
                    pair -> new CustomContextProfile(pair.first, pair.second), null,
                    AliasForUtils.getAnnotationMetaProducer(SpringJamProfile.JAM_ANNO_META_KEY, SpringJamProfile.META)));
  }

  private static void registerCustomActiveProfiles(SemRegistrar registrar, ElementPattern<? extends PsiMember> pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, SpringCustomActiveProfiles.META_KEY, SpringCustomActiveProfiles.JAM_KEY,
            SemContributorUtil.createFunction(SpringCustomActiveProfiles.JAM_KEY, SpringCustomActiveProfiles.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.ACTIVE_PROFILES, true),
                    pair -> new SpringCustomActiveProfiles(pair.first, pair.second), null,
                    AliasForUtils.getAnnotationMetaProducer(SpringCustomActiveProfiles.JAM_ANNO_META_KEY, SpringJamActiveProfiles.META)));
  }

  private static void registerCustomContextJavaBean(SemRegistrar registrar, PsiMethodPattern pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, CustomContextJavaBean.META_KEY, CustomContextJavaBean.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextJavaBean.JAM_KEY, CustomContextJavaBean.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.BEAN), pair -> new CustomContextJavaBean(pair.first, pair.second), null,
                    AliasForUtils.getAnnotationMetaProducer(CustomContextJavaBean.JAM_ANNO_META_KEY, ContextJavaBean.METHOD_META)));
  }

  private static void registerConfigurations(SemRegistrar registrar, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("abstract"),
            Configuration.META_KEY,
            Configuration.JAM_KEY, SemContributorUtil.createFunction(Configuration.JAM_KEY, Configuration.class, Configuration.getAnnotations(),
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

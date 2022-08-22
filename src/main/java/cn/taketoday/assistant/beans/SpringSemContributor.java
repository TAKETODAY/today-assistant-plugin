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
import com.intellij.spring.constants.SpringConstants;
import com.intellij.spring.constants.SpringJavaeeConstants;
import com.intellij.spring.model.aliasFor.SpringAliasFor;
import com.intellij.spring.model.aliasFor.SpringAliasForUtils;
import com.intellij.spring.model.events.jam.CustomSpringEventListener;
import com.intellij.spring.model.events.jam.SpringBeanEventListener;
import com.intellij.spring.model.events.jam.SpringJamEventListener;
import com.intellij.spring.model.extensions.myBatis.SpringMyBatisMapper;
import com.intellij.spring.model.jam.SpringOrder;
import com.intellij.spring.model.jam.contexts.CustomContextJavaBean;
import com.intellij.spring.model.jam.dependsOn.SpringJamDependsOn;
import com.intellij.spring.model.jam.javaConfig.ContextJavaBean;
import com.intellij.spring.model.jam.lookup.SpringLookupInjection;
import com.intellij.spring.model.jam.profile.CustomContextProfile;
import com.intellij.spring.model.jam.profile.SpringJamProfile;
import com.intellij.spring.model.jam.stereotype.CustomJamComponentScan;
import com.intellij.spring.model.jam.stereotype.CustomSpringComponent;
import com.intellij.spring.model.jam.stereotype.CustomSpringImport;
import com.intellij.spring.model.jam.stereotype.SpringConfiguration;
import com.intellij.spring.model.jam.stereotype.SpringContextImport;
import com.intellij.spring.model.jam.stereotype.SpringController;
import com.intellij.spring.model.jam.stereotype.SpringImportResource;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScan;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScans;
import com.intellij.spring.model.jam.stereotype.SpringJamPropertySource;
import com.intellij.spring.model.jam.stereotype.SpringPropertySources;
import com.intellij.spring.model.jam.stereotype.SpringRepository;
import com.intellij.spring.model.jam.stereotype.SpringService;
import com.intellij.spring.model.jam.stereotype.javaee.SpringCdiJakartaNamed;
import com.intellij.spring.model.jam.stereotype.javaee.SpringCdiJavaxNamed;
import com.intellij.spring.model.jam.stereotype.javaee.SpringJakartaManagedBean;
import com.intellij.spring.model.jam.stereotype.javaee.SpringJavaxManagedBean;
import com.intellij.spring.model.jam.testContexts.CustomContextConfiguration;
import com.intellij.spring.model.jam.testContexts.SpringContextConfiguration;
import com.intellij.spring.model.jam.testContexts.SpringContextHierarchy;
import com.intellij.spring.model.jam.testContexts.SpringTransactionConfiguration;
import com.intellij.spring.model.jam.testContexts.dirtiesContexts.SpringTestingDirtiesContext;
import com.intellij.spring.model.jam.testContexts.jdbc.SpringTestingSql;
import com.intellij.spring.model.jam.testContexts.jdbc.SpringTestingSqlConfig;
import com.intellij.spring.model.jam.testContexts.jdbc.SpringTestingSqlGroup;
import com.intellij.spring.model.jam.testContexts.profiles.SpringCustomActiveProfiles;
import com.intellij.spring.model.jam.testContexts.profiles.SpringJamActiveProfiles;
import com.intellij.spring.model.jam.testContexts.propertySources.CustomTestPropertySource;
import com.intellij.spring.model.jam.testContexts.propertySources.SpringTestPropertySource;
import com.intellij.spring.model.jam.transaction.SpringTransactionalComponent;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.beans.stereotype.ComponentBean;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 21:03
 */
final class SpringSemContributor extends SemContributor {

  @Override
  public void registerSemProviders(SemRegistrar registrar, Project project) {
    PsiClassPattern nonAnnoClass = PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private");
    PsiClassPattern classPattern = PsiJavaPatterns.psiClass().withoutModifiers("private");
    ComponentBean.META.register(registrar, nonAnnoClass.withAnnotation(AnnotationConstant.COMPONENT));
    SemService semService = SemService.getSemService(project);
    registerConfigurations(registrar, semService);
    registerServices(registrar, nonAnnoClass, semService);
    registerControllers(registrar, nonAnnoClass, semService);
    registerRepositories(registrar, nonAnnoClass, semService);
    registerProfiles(registrar, semService);
    registerActiveProfiles(registrar, semService);
    registerCustomComponents(registrar, nonAnnoClass, semService);
    registerCustomContextConfigurations(registrar, nonAnnoClass, semService);
    registerCustomComponentScans(registrar, nonAnnoClass, semService);
    registerCustomImports(registrar, classPattern, semService);
    registerCustomTestingPropertySources(registrar, nonAnnoClass, semService);
    SpringJamDependsOn.META.register(registrar, PsiJavaPatterns.psiClass().withoutModifiers(new String[] { "private" }).withAnnotation(AnnotationConstant.DEPENDS_ON));
    SpringCdiJavaxNamed.META.register(registrar, nonAnnoClass.withAnnotation(SpringJavaeeConstants.JAVAX_NAMED));
    SpringCdiJakartaNamed.META.register(registrar, nonAnnoClass.withAnnotation(SpringJavaeeConstants.JAKARTA_NAMED));
    registerContextJavaBeans(registrar, semService);
    registerEventListeners(registrar, semService);
    SpringAliasFor.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withModifiers(new String[] { "public" }).withAnnotation(AnnotationConstant.ALIAS_FOR));
    SpringOrder.CLASS_META.register(registrar, classPattern.withAnnotation(AnnotationConstant.ORDER));
    SpringOrder.FIELD_META.register(registrar, PsiJavaPatterns.psiField().withAnnotation(AnnotationConstant.ORDER));
    SpringOrder.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(AnnotationConstant.ORDER));
    SpringContextImport.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_IMPORT));
    SpringImportResource.META.register(registrar, classPattern.withAnnotation(AnnotationConstant.CONTEXT_IMPORT_RESOURCE));
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
    SpringJavaxManagedBean.META.register(registrar, nonAnnoClass.withAnnotation(SpringJavaeeConstants.JAVAX_MANAGED_BEAN));
    SpringJakartaManagedBean.META.register(registrar, nonAnnoClass.withAnnotation(SpringJavaeeConstants.JAKARTA_MANAGED_BEAN));
    SpringLookupInjection.META.register(registrar, PsiJavaPatterns.psiMethod().constructor(false).withAnnotation(AnnotationConstant.LOOKUP_ANNOTATION));
    SpringMyBatisMapper.META.register(registrar, classPattern.withAnnotation(SpringMyBatisMapper.MAPPER_ANNOTATION));
  }

  private static void registerEventListeners(SemRegistrar registrar, SemService semService) {
    PsiMethodPattern methodPattern = PsiJavaPatterns.psiMethod().constructor(false).withModifiers("public");
    PsiMethodPattern onEventMethodPattern = methodPattern.withName(SpringConstants.ON_APPLICATION_EVENT_METHOD).withParameterCount(1)
            .definedInClass(PsiJavaPatterns.psiClass().nonAnnotationType().inheritorOf(true, SpringConstants.APPLICATION_LISTENER));
    SpringJamEventListener.METHOD_META.register(registrar, methodPattern.withAnnotation(AnnotationConstant.EVENT_LISTENER));
    SpringBeanEventListener.METHOD_META.register(registrar, onEventMethodPattern);
    registerCustomEventListeners(registrar, methodPattern, semService);
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

  private static void registerCustomComponents(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, CustomSpringComponent.META_KEY, CustomSpringComponent.JAM_KEY,
            SemContributorUtil.createFunction(CustomSpringComponent.JAM_KEY, CustomSpringComponent.class, new SemContributorUtil.UserDefinedCustomAnnotationFunction(), pair -> {
              return new CustomSpringComponent(pair.first, pair.second);
            }, SemContributorUtil.createStereotypeConsumer()));
  }

  private static void registerCustomComponentScans(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, CustomJamComponentScan.META_KEY, CustomJamComponentScan.JAM_KEY,
            SemContributorUtil.createFunction(CustomJamComponentScan.JAM_KEY, CustomJamComponentScan.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.COMPONENT_SCAN), pair -> {
                      return new CustomJamComponentScan(pair.first, pair.second);
                    }, null, SpringAliasForUtils.getAnnotationMetaProducer(CustomJamComponentScan.JAM_ANNO_META_KEY, SpringJamComponentScan.META)));
  }

  private static void registerCustomContextConfigurations(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, CustomContextConfiguration.META_KEY, CustomContextConfiguration.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextConfiguration.JAM_KEY, CustomContextConfiguration.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.CONTEXT_CONFIGURATION, true), pair -> {
                      return new CustomContextConfiguration(pair.first, pair.second);
                    }, null, SpringAliasForUtils.getAnnotationMetaProducer(CustomContextConfiguration.JAM_ANNO_META_KEY, SpringContextConfiguration.META)));
  }

  private static void registerCustomImports(
          SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerRepeatableMetaComponents(semService, registrar, psiClassPattern, CustomSpringImport.META_KEY, CustomSpringImport.JAM_KEY,
            SemContributorUtil.createRepeatableFunction(CustomSpringImport.JAM_KEY, SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.CONTEXT_IMPORT, true), pair -> {
              return new CustomSpringImport(pair.first, pair.second);
            }, null, null));
  }

  private static void registerCustomTestingPropertySources(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, CustomTestPropertySource.META_KEY, CustomTestPropertySource.JAM_KEY,
            SemContributorUtil.createFunction(CustomTestPropertySource.JAM_KEY, CustomTestPropertySource.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.TEST_PROPERTY_SOURCE, true), pair -> {
                      return new CustomTestPropertySource(pair.first, pair.second);
                    }, null, SpringAliasForUtils.getAnnotationMetaProducer(CustomTestPropertySource.JAM_ANNO_META_KEY, SpringTestPropertySource.META)));
  }

  private static void registerCustomEventListeners(SemRegistrar registrar, PsiMethodPattern psiMethodPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiMethodPattern, CustomSpringEventListener.META_KEY, CustomSpringEventListener.JAM_KEY,
            SemContributorUtil.createFunction(CustomSpringEventListener.JAM_KEY, CustomSpringEventListener.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.EVENT_LISTENER, true), pair -> {
                      return new CustomSpringEventListener(pair.first, pair.second);
                    }, null));
  }

  private static void registerCustomProfiles(SemRegistrar registrar, ElementPattern<? extends PsiMember> pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, CustomContextProfile.META_KEY, CustomContextProfile.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextProfile.JAM_KEY, CustomContextProfile.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.PROFILE, true), pair -> {
                      return new CustomContextProfile(pair.first, pair.second);
                    }, null, SpringAliasForUtils.getAnnotationMetaProducer(SpringJamProfile.JAM_ANNO_META_KEY, SpringJamProfile.META)));
  }

  private static void registerCustomActiveProfiles(SemRegistrar registrar, ElementPattern<? extends PsiMember> pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, SpringCustomActiveProfiles.META_KEY, SpringCustomActiveProfiles.JAM_KEY,
            SemContributorUtil.createFunction(SpringCustomActiveProfiles.JAM_KEY, SpringCustomActiveProfiles.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.ACTIVE_PROFILES, true), pair -> {
                      return new SpringCustomActiveProfiles(pair.first, pair.second);
                    }, null, SpringAliasForUtils.getAnnotationMetaProducer(SpringCustomActiveProfiles.JAM_ANNO_META_KEY, SpringJamActiveProfiles.META)));
  }

  private static void registerCustomContextJavaBean(SemRegistrar registrar, PsiMethodPattern pattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, pattern, CustomContextJavaBean.META_KEY, CustomContextJavaBean.JAM_KEY,
            SemContributorUtil.createFunction(CustomContextJavaBean.JAM_KEY, CustomContextJavaBean.class,
                    SemContributorUtil.getCustomMetaAnnotations(AnnotationConstant.BEAN), pair -> {
                      return new CustomContextJavaBean(pair.first, pair.second);
                    }, null, SpringAliasForUtils.getAnnotationMetaProducer(CustomContextJavaBean.JAM_ANNO_META_KEY, ContextJavaBean.METHOD_META)));
  }

  private static void registerConfigurations(SemRegistrar registrar, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("abstract"), SpringConfiguration.META_KEY,
            SpringConfiguration.JAM_KEY, SemContributorUtil.createFunction(SpringConfiguration.JAM_KEY, SpringConfiguration.class, SpringConfiguration.getAnnotations(), pair -> {
              return new SpringConfiguration(pair.first, pair.second);
            }, SemContributorUtil.createStereotypeConsumer()));
  }

  private static void registerControllers(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, SpringController.META_KEY, SpringController.JAM_KEY,
            SemContributorUtil.createFunction(SpringController.JAM_KEY, SpringController.class, SpringController.getControllerAnnotations(), pair -> {
              return new SpringController(pair.first, pair.second);
            }, SemContributorUtil.createStereotypeConsumer()));
  }

  private static void registerRepositories(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, SpringRepository.META_KEY, SpringRepository.JAM_KEY,
            SemContributorUtil.createFunction(SpringRepository.JAM_KEY, SpringRepository.class, SpringRepository.getRepositoryAnnotations(), pair -> {
              return new SpringRepository(pair.first, pair.second);
            }, SemContributorUtil.createStereotypeConsumer()));
  }

  private static void registerServices(SemRegistrar registrar, PsiClassPattern psiClassPattern, SemService semService) {
    SemContributorUtil.registerMetaComponents(semService, registrar, psiClassPattern, SpringService.META_KEY, SpringService.JAM_KEY,
            SemContributorUtil.createFunction(SpringService.JAM_KEY, SpringService.class, SpringService.getServiceAnnotations(), pair -> {
              return new SpringService(pair.first, pair.second);
            }, SemContributorUtil.createStereotypeConsumer()));
  }
}

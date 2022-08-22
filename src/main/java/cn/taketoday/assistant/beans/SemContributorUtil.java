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

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.JamElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;
import com.intellij.spring.constants.SpringJavaeeConstants;
import com.intellij.spring.model.jam.JamCustomImplementationBean;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;
import com.intellij.spring.model.jam.utils.JamAnnotationTypeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.NotNullFunction;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.TodayLibraryUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.AnnotationConstant.CONFIGURATION;
import static cn.taketoday.assistant.AnnotationConstant.CONTROLLER;
import static cn.taketoday.assistant.AnnotationConstant.REPOSITORY;
import static cn.taketoday.assistant.AnnotationConstant.SERVICE;

public final class SemContributorUtil {
  // predefined component annotations
  public static final String[] DEFAULT_COMPONENTS =
          new String[] {
                  AnnotationConstant.COMPONENT,
                  CONTROLLER,
                  REPOSITORY,
                  SERVICE,
                  CONFIGURATION,
                  SpringJavaeeConstants.JAVAX_NAMED,
                  SpringJavaeeConstants.JAKARTA_NAMED
          };

  public static <T extends JamElement, Psi extends PsiMember> void registerMetaComponents(
          SemService semService,
          SemRegistrar registrar,
          ElementPattern<? extends Psi> place,
          SemKey<JamMemberMeta<Psi, T>> metaKey,
          SemKey<T> semKey,
          NullableFunction<? super Psi, ? extends JamMemberMeta<Psi, T>> metaFunction) {
    registrar.registerSemElementProvider(metaKey, place, metaFunction);

    registrar.registerSemElementProvider(semKey, place,
            (NullableFunction<Psi, T>) member -> {
              JamMemberMeta<Psi, T> memberMeta = semService.getSemElement(metaKey, member);
              return memberMeta != null ? memberMeta.createJamElement(PsiElementRef.real(member)) : null;
            }
    );
  }

  public static <T extends JamElement, Psi extends PsiMember> void registerRepeatableMetaComponents(SemService semService,
          SemRegistrar registrar,
          ElementPattern<? extends Psi> place,
          SemKey<JamMemberMeta<Psi, T>> metaKey,
          SemKey<T> semKey,
          NullableFunction<? super Psi, ? extends Collection<JamMemberMeta<Psi, T>>> metaFunction) {
    registrar.registerRepeatableSemElementProvider(metaKey, place, metaFunction);

    registrar.registerRepeatableSemElementProvider(semKey, place,
            (NullableFunction<Psi, Collection<T>>) member -> {
              List<JamMemberMeta<Psi, T>> memberMetas =
                      semService.getSemElements(metaKey, member);
              Collection<T> metas = new HashSet<>();
              for (JamMemberMeta<Psi, T> memberMeta : memberMetas) {
                ContainerUtil
                        .addIfNotNull(metas, memberMeta.createJamElement(PsiElementRef.real(member)));
              }
              return metas.isEmpty() ? null : metas;
            }
    );
  }

  public static <T extends JamElement, Psi extends PsiMember> NullableFunction<Psi, JamMemberMeta<Psi, T>> createFunction(SemKey<T> semKey,
          Class<? extends T> jamClass,
          Function<? super Module, ? extends Collection<String>> annotationsGetter,
          Function<? super Pair<String, Psi>, ? extends T> producer,
          @Nullable Consumer<? super JamMemberMeta<Psi, T>> metaConsumer) {
    return createFunction(semKey, jamClass, annotationsGetter, producer, metaConsumer, null);
  }

  /**
   * @see AliasForUtils#getAnnotationMetaProducer(SemKey, JamMemberMeta[])
   */
  public static <T extends JamElement, Psi extends PsiMember> NullableFunction<Psi, JamMemberMeta<Psi, T>> createFunction(
          SemKey<T> semKey,
          Class<? extends T> jamClass,
          Function<? super Module, ? extends Collection<String>> annotationsGetter,
          Function<? super Pair<String, Psi>, ? extends T> producer,
          @Nullable Consumer<? super JamMemberMeta<Psi, T>> metaConsumer,
          @Nullable NotNullFunction<? super Pair<String, Project>, ? extends JamAnnotationMeta> annotationMeta) {
    return psiMember -> {
      Project project = psiMember.getProject();
      if (DumbService.isDumb(project))
        return null;
      if (psiMember instanceof PsiClass && ((PsiClass) psiMember).isAnnotationType())
        return null;
      if (!TodayLibraryUtil.hasLibrary(project))
        return null;

      Module module = ModuleUtilCore.findModuleForPsiElement(psiMember);
      if (module != null && !TodayLibraryUtil.hasLibrary(module))
        return null;
      for (String anno : annotationsGetter.fun(module)) {
        if (AnnotationUtil.isAnnotated(psiMember, anno, AnnotationUtil.CHECK_HIERARCHY)) {
          return getMeta(semKey, producer, metaConsumer, annotationMeta, psiMember, anno);
        }
      }
      if (psiMember instanceof ClsClassImpl) {
        return getMetaForLibraryClass(semKey, annotationsGetter, producer, metaConsumer, annotationMeta, psiMember);
      }

      return null;
    };
  }

  @Nullable
  private static <T extends JamElement, Psi extends PsiMember> JamMemberMeta<Psi, T> getMetaForLibraryClass(
          SemKey<T> semKey,
          Function<? super Module, ? extends Collection<String>> annotationsGetter,
          Function<? super Pair<String, Psi>, ? extends T> producer,
          @Nullable Consumer<? super JamMemberMeta<Psi, T>> metaConsumer,
          @Nullable NotNullFunction<? super Pair<String, Project>, ? extends JamAnnotationMeta> annotationMeta,
          Psi aClass) {
    if (aClass instanceof ClsClassImpl && annotationsGetter instanceof CustomRootAnnotationsProvider) {
      for (PsiAnnotation psiAnnotation : aClass.getAnnotations()) {
        String qualifiedName = psiAnnotation.getQualifiedName();
        if (qualifiedName == null || isFrameworkAnnotation(qualifiedName))
          continue;

        PsiClass annotationType = psiAnnotation.resolveAnnotationType();
        if (annotationType != null) {
          // performance issue: only check first-level custom annotations (annotated @Component/@Service/@ComponentScan)
          for (String rootAnnotation : ((CustomRootAnnotationsProvider) annotationsGetter).getRootAnnotations()) {
            if (annotationType.hasAnnotation(rootAnnotation)) {
              return getMeta(semKey, producer, metaConsumer, annotationMeta, aClass, qualifiedName);
            }
          }
        }
      }
    }
    return null;
  }

  private static boolean isFrameworkAnnotation(String qualifiedName) {
    return qualifiedName.startsWith("cn.taketoday") ||
            qualifiedName.startsWith("javax") ||
            qualifiedName.startsWith("jakarta") ||
            qualifiedName.startsWith("java.lang") ||
            qualifiedName.startsWith("org.jetbrains");
  }

  public static <T extends JamElement, Psi extends
          PsiMember> NullableFunction<Psi, Collection<JamMemberMeta<Psi, T>>> createRepeatableFunction(
          SemKey<T> semKey,
          Function<? super Module, ? extends Collection<String>> annotationsGetter,
          Function<? super Pair<String, Psi>, ? extends T> producer,
          @Nullable Consumer<? super JamMemberMeta<Psi, T>> metaConsumer,
          @Nullable NotNullFunction<? super Pair<String, Project>, ? extends JamAnnotationMeta> annotationMeta) {
    return psiMember -> {
      Project project = psiMember.getProject();
      if (DumbService.isDumb(project))
        return null;
      if (psiMember instanceof PsiClass && ((PsiClass) psiMember).isAnnotationType())
        return null;
      if (!TodayLibraryUtil.hasLibrary(project))
        return null;

      Module module = ModuleUtilCore.findModuleForPsiElement(psiMember);
      if (module != null && !TodayLibraryUtil.hasLibrary(module))
        return null;

      Collection<JamMemberMeta<Psi, T>> metas = new HashSet<>();
      for (String anno : annotationsGetter.fun(module)) {
        if (AnnotationUtil.isAnnotated(psiMember, anno, AnnotationUtil.CHECK_HIERARCHY)) {
          JamMemberMeta<Psi, T> meta = getMeta(semKey, producer, metaConsumer, annotationMeta, psiMember, anno);

          metas.add(meta);
        }
      }
      return metas.isEmpty() ? null : metas;
    };
  }

  private static <T extends JamElement, Psi extends PsiMember> JamMemberMeta<Psi, T> getMeta(SemKey<T> semKey,
          Function<? super Pair<String, Psi>, ? extends T> producer,
          @Nullable Consumer<? super JamMemberMeta<Psi, T>> metaConsumer,
          @Nullable NotNullFunction<? super Pair<String, Project>, ? extends JamAnnotationMeta> annotationMeta,
          Psi psiMember, String anno) {
    JamMemberMeta<Psi, T> meta = new JamMemberMeta<>(null, semKey,
            ref -> producer.fun(Pair.create(anno, ref.getPsiElement())));
    if (metaConsumer != null) {
      metaConsumer.consume(meta);
    }
    if (annotationMeta != null)
      registerCustomAnnotationMeta(anno, meta, annotationMeta, psiMember.getProject());
    return meta;
  }

  private static <T extends JamElement, Psi extends PsiMember> void registerCustomAnnotationMeta(String anno,
          JamMemberMeta<Psi, T> meta,
          NotNullFunction<? super Pair<String, Project>, ? extends JamAnnotationMeta> metaNotNullFunction,
          Project project) {
    List<JamAnnotationMeta> annotations = meta.getAnnotations();
    for (JamAnnotationMeta annotationMeta : annotations) {
      if (anno.equals(annotationMeta.getAnnoName()))
        return;
    }
    meta.addAnnotation(metaNotNullFunction.fun(Pair.create(anno, project)));
  }

  /**
   * @param <T>
   * @return Consumer.
   * @see SpringStereotypeElement#addPomTargetProducer(JamMemberMeta)
   */
  public static <T extends SpringStereotypeElement, Psi extends
          PsiMember> Consumer<JamMemberMeta<Psi, T>> createStereotypeConsumer() {
    return SpringStereotypeElement::addPomTargetProducer;
  }

  /**
   * @param anno Annotation FQN.
   * @return Custom annotation types.
   */
  public static Function<Module, Collection<String>> getCustomMetaAnnotations(String anno) {
    return getCustomMetaAnnotations(anno, false);
  }

  /**
   * Returns all custom meta annotations in defined scope.
   *
   * @param anno Annotation FQN.
   * @param withTests Whether to include annotations located in test scope.
   * @return Custom annotation types.
   */
  public static Function<Module, Collection<String>> getCustomMetaAnnotations(String anno, boolean withTests) {
    return getCustomMetaAnnotations(anno, withTests, true);
  }

  /**
   * Returns all custom meta annotations in defined scope, optionally filtering custom JAM implementations.
   *
   * @param anno Annotation FQN.
   * @param withTests Whether to include annotations located in test scope.
   * @param filterCustomJamImplementations Whether to filter custom JAM implementations.
   * @return Custom annotation types.
   * @see JamCustomImplementationBean
   */
  public static Function<Module, Collection<String>> getCustomMetaAnnotations(String anno,
          boolean withTests,
          boolean filterCustomJamImplementations) {
    return new CustomMetaAnnotationsFunction(anno, filterCustomJamImplementations, withTests);
  }

  public interface CustomRootAnnotationsProvider {
    String[] getRootAnnotations();
  }

  public static class UserDefinedCustomAnnotationFunction implements Function<Module, Collection<String>>, CustomRootAnnotationsProvider {
    @Override
    public Collection<String> fun(Module module) {
      return module == null ? Collections.emptySet() : JamAnnotationTypeUtil.getInstance(module).getUserDefinedCustomComponentAnnotations();
    }

    @Override
    public String[] getRootAnnotations() { return DEFAULT_COMPONENTS; }
  }

  public static class CustomMetaAnnotationsFunction implements Function<Module, Collection<String>>, CustomRootAnnotationsProvider {
    private final String myRootAnnotation;
    private final boolean myFilterCustomJamImplementations;
    private final boolean myWithTests;

    public CustomMetaAnnotationsFunction(String anno, boolean filterCustomJamImplementations, boolean withTests) {
      myRootAnnotation = anno;
      myFilterCustomJamImplementations = filterCustomJamImplementations;
      myWithTests = withTests;
    }

    @Override
    public Collection<String> fun(Module module) {
      if (module == null)
        return Collections.emptySet();

      Collection<PsiClass> psiClasses = getAnnotationTypes(module, myRootAnnotation);

      Set<String> customMetaFQNs = getJamCustomFQNs(module);

      return ContainerUtil.mapNotNull(psiClasses, psiClass -> {
        String qualifiedName = psiClass.getQualifiedName();
        if (myRootAnnotation.equals(qualifiedName))
          return null;

        if (customMetaFQNs.contains(qualifiedName)) {
          return null;
        }
        return qualifiedName;
      });
    }

    private Set<String> getJamCustomFQNs(Module module) {
      if (!myFilterCustomJamImplementations) {
        return Collections.emptySet();
      }

      // FIXME JamCustomImplementationBean

      Set<String> customMetaFQNs = new HashSet<>();
//      for (JamCustomImplementationBean bean : JamCustomImplementationBean.EP_NAME.getExtensionList()) {
//        if (myRootAnnotation.equals(bean.baseAnnotationFqn)) {
//
//          String customMetaAnnotationFqn = bean.customMetaAnnotationFqn;
//          customMetaFQNs.add(customMetaAnnotationFqn);
//
//          Collection<PsiClass> customMetaClasses = getAnnotationTypes(module, customMetaAnnotationFqn);
//          for (PsiClass customMetaClass : customMetaClasses) {
//            ContainerUtil.addIfNotNull(customMetaFQNs, customMetaClass.getQualifiedName());
//          }
//        }
//      }
      return customMetaFQNs;
    }

    private Collection<PsiClass> getAnnotationTypes(Module module, String anno) {
      if (!TodayLibraryUtil.hasLibrary(module))
        return Collections.emptyList();

      return myWithTests ?
             JamAnnotationTypeUtil.getInstance(module).getAnnotationTypesWithChildrenIncludingTests(anno) :
             JamAnnotationTypeUtil.getInstance(module).getAnnotationTypesWithChildren(anno);
    }

    @Override
    public String[] getRootAnnotations() {
      return new String[] { myRootAnnotation };
    }
  }

}

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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.taketoday.assistant;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.semantic.SemKey;
import com.intellij.util.CommonProcessors;
import com.intellij.util.NotNullFunction;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
public final class AliasForUtils {

  public static @Nullable InfraAliasFor findAliasFor(
          @Nullable PsiElement context, @Nullable String toSearchInAnnotation,
          String aliasedClassName, String attrName) {
    if (context == null) {
      return null;
    }
    else {
      var findFirstProcessor = new CommonProcessors.FindFirstProcessor<InfraAliasFor>();
      getAliasFor(context.getProject(), context.getResolveScope(), toSearchInAnnotation, aliasedClassName, attrName, findFirstProcessor);
      return findFirstProcessor.getFoundValue();
    }
  }

  public static Collection<InfraAliasFor> findAliasForAttributes(
          @Nullable PsiElement context, @Nullable String toSearchInAnnotation,
          String aliasedClassName, String attrName) {
    if (context == null) {
      return Collections.emptySet();
    }
    else {
      var collectProcessor = new CommonProcessors.CollectProcessor<InfraAliasFor>();
      getAliasFor(context.getProject(), context.getResolveScope(), toSearchInAnnotation, aliasedClassName, attrName, collectProcessor);
      return collectProcessor.getResults();
    }
  }

  public static @Nullable InfraAliasFor findAliasFor(Project project,
          @Nullable String toSearchInAnnotation, String aliasedClassName, String attrName) {
    var findFirstProcessor = new CommonProcessors.FindFirstProcessor<InfraAliasFor>();
    getAliasFor(project, GlobalSearchScope.allScope(project), toSearchInAnnotation, aliasedClassName, attrName, findFirstProcessor);
    return findFirstProcessor.getFoundValue();
  }

  private static boolean getAliasFor(Project project, GlobalSearchScope scope,
          @Nullable String toSearchInAnnotation, String aliasedClassName,
          String attrName, Processor<InfraAliasFor> processor) {

    if (toSearchInAnnotation == null) {
      return true;
    }
    else {
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      PsiClass toSearchInClass = javaPsiFacade.findClass(toSearchInAnnotation, scope);
      if (toSearchInClass != null && toSearchInClass.isAnnotationType()) {
        PsiClass annotationClass = javaPsiFacade.findClass(aliasedClassName, scope);
        if (annotationClass != null && annotationClass.isAnnotationType()) {
          Iterator<InfraAliasFor> var9 = getAliasForAttributes(toSearchInClass).iterator();

          InfraAliasFor aliasFor;
          do {
            if (!var9.hasNext()) {
              return true;
            }

            aliasFor = var9.next();
          }
          while (!attrName.equals(aliasFor.getAttributeName()) || !annotationClass.equals(aliasFor.getAnnotationClass()) || processor.process(aliasFor));

          return false;
        }
        else {
          return true;
        }
      }
      else {
        return true;
      }
    }
  }

  private static List<InfraAliasFor> getAliasForAttributes(PsiClass psiClass) {
    return CachedValuesManager.getCachedValue(psiClass, () -> {
      List<InfraAliasFor> aliasForList = JamService.getJamService(psiClass.getProject())
              .getAnnotatedMembersList(psiClass, InfraAliasFor.SEM_KEY, JamService.CHECK_METHOD);
      return Result.create(aliasForList, psiClass);
    });
  }

  @Nullable
  public static PsiAnnotation findDefiningMetaAnnotation(
          @Nullable PsiElement context, String customAnnotationFqn, String baseMetaAnnotationFqn) {
    return findDefiningMetaAnnotation(context, customAnnotationFqn, baseMetaAnnotationFqn, false);
  }

  @Nullable
  public static PsiAnnotation findDefiningMetaAnnotation(
          @Nullable PsiElement context, String customAnnotationFqn,
          String baseMetaAnnotationFqn, boolean includingTests) {
    if (context == null) {
      return null;
    }
    else {
      Module module = ModuleUtilCore.findModuleForPsiElement(context);
      if (module == null) {
        return null;
      }
      else {
        PsiClass customAnnoClass = JavaPsiFacade.getInstance(module.getProject()).findClass(customAnnotationFqn, context.getResolveScope());
        Collection<PsiClass> allMetaAnnotations =
                includingTests ? JamAnnotationTypeUtil.getAnnotationTypesWithChildrenIncludingTests(module, baseMetaAnnotationFqn)
                               : JamAnnotationTypeUtil.getAnnotationTypesWithChildren(module, baseMetaAnnotationFqn);
        return findDefiningMetaAnnotation(customAnnoClass, baseMetaAnnotationFqn, allMetaAnnotations);
      }
    }
  }

  @Nullable
  public static PsiAnnotation findDefiningMetaAnnotation(@Nullable PsiClass customAnnoClass,
          String baseMetaAnnotationFqn, Collection<? extends PsiClass> allMetaAnnotations) {

    if (customAnnoClass != null && customAnnoClass.isAnnotationType()) {
      PsiAnnotation psiAnnotation = AnnotationUtil.findAnnotation(customAnnoClass, true, baseMetaAnnotationFqn);
      if (psiAnnotation != null) {
        return psiAnnotation;
      }
      else {
        Iterator<? extends PsiClass> var4 = allMetaAnnotations.iterator();

        PsiClass customMetaAnnoClass;
        String qualifiedName;
        do {
          if (!var4.hasNext()) {
            return null;
          }

          customMetaAnnoClass = var4.next();
          qualifiedName = customMetaAnnoClass.getQualifiedName();
        }
        while (qualifiedName == null || !AnnotationUtil.isAnnotated(customAnnoClass, qualifiedName, 1));

        return findDefiningMetaAnnotation(customMetaAnnoClass, baseMetaAnnotationFqn, allMetaAnnotations);
      }
    }
    else {
      return null;
    }
  }

  public static NotNullFunction<Pair<String, Project>, JamAnnotationMeta> getAnnotationMetaProducer(
          SemKey<JamAnnotationMeta> annoMetaKey, JamMemberMeta<?, ?>... parentMetas) {

    return (anno) -> new JamAnnotationMeta(anno.first, null, annoMetaKey) {

      @Override
      @Nullable
      public JamAttributeMeta<?> findAttribute(@Nullable String name) {
        JamAttributeMeta<?> attribute = super.findAttribute(name);
        if (attribute != null) {
          return attribute;
        }
        else {
          if (name == null) {
            name = "value";
          }

          return this.getAliasedAttributeMeta(name);
        }
      }

      @Nullable
      private JamAttributeMeta<?> getAliasedAttributeMeta(String name) {
        for (JamMemberMeta parentMeta : parentMetas) {
          List<JamAnnotationMeta> annotations = parentMeta.getAnnotations();
          for (JamAnnotationMeta annotationMeta : annotations) {
            List<JamAttributeMeta<?>> parentAnnotationAttributes = getRegisteredAttributes(annotationMeta);
            for (JamAttributeMeta<?> attributeMeta : parentAnnotationAttributes) {
              if (attributeMeta instanceof JamStringAttributeMeta meta) {
                JamConverter<?> converter = meta.getConverter();
                InfraAliasFor aliasFor = AliasForUtils.findAliasFor(anno.second, anno.first, annotationMeta.getAnnoName(), meta.getAttributeLink().getAttributeName());
                if (aliasFor != null) {
                  String aliasForMethodName = aliasFor.getMethodName();
                  if (name.equals(aliasForMethodName)) {
                    if (attributeMeta instanceof JamStringAttributeMeta.Single) {
                      return JamAttributeMeta.singleString(aliasForMethodName, converter);
                    }

                    if (attributeMeta instanceof JamStringAttributeMeta.Collection) {
                      return JamAttributeMeta.collectionString(aliasForMethodName, converter);
                    }
                  }
                }
              }
            }
          }
        }

        return null;
      }

      private List<JamAttributeMeta<?>> getRegisteredAttributes(JamAnnotationMeta annotationMeta) {
        SmartList<JamAttributeMeta<?>> attributeMetas = new SmartList<>();
        attributeMetas.addAll(annotationMeta.getAttributes());
        JamAnnotationArchetype archetype = annotationMeta.getArchetype();
        if (archetype != null) {
          attributeMetas.addAll(archetype.getAttributes());
        }

        return attributeMetas;
      }
    };
  }

}

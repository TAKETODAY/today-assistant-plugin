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
import com.intellij.spring.model.jam.utils.JamAnnotationTypeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.NotNullFunction;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
public final class AliasForUtils {

  public static @Nullable TodayAliasFor findAliasFor(@Nullable PsiElement context, @Nullable String toSearchInAnnotation, String aliasedClassName, String attrName) {
    if (context == null) {
      return null;
    }
    else {
      CommonProcessors.FindFirstProcessor<TodayAliasFor> findFirstProcessor = new CommonProcessors.FindFirstProcessor();
      getAliasFor(context.getProject(), context.getResolveScope(), toSearchInAnnotation, aliasedClassName, attrName, findFirstProcessor);
      return (TodayAliasFor) findFirstProcessor.getFoundValue();
    }
  }

  public static Collection<TodayAliasFor> findAliasForAttributes(@Nullable PsiElement context, @Nullable String toSearchInAnnotation, String aliasedClassName, String attrName) {

    if (context == null) {
      return Collections.emptySet();
    }
    else {
      CommonProcessors.CollectProcessor<TodayAliasFor> collectProcessor = new CommonProcessors.CollectProcessor();
      getAliasFor(context.getProject(), context.getResolveScope(), toSearchInAnnotation, aliasedClassName, attrName, collectProcessor);
      return collectProcessor.getResults();
    }
  }

  public static @Nullable TodayAliasFor findAliasFor(Project project, @Nullable String toSearchInAnnotation, String aliasedClassName, String attrName) {
    CommonProcessors.FindFirstProcessor<TodayAliasFor> findFirstProcessor = new CommonProcessors.FindFirstProcessor();
    getAliasFor(project, GlobalSearchScope.allScope(project), toSearchInAnnotation, aliasedClassName, attrName, findFirstProcessor);
    return findFirstProcessor.getFoundValue();
  }

  private static boolean getAliasFor(Project project, GlobalSearchScope scope, @Nullable String toSearchInAnnotation, String aliasedClassName, String attrName,
          Processor<TodayAliasFor> processor) {

    if (toSearchInAnnotation == null) {
      return true;
    }
    else {
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
      PsiClass toSearchInClass = javaPsiFacade.findClass(toSearchInAnnotation, scope);
      if (toSearchInClass != null && toSearchInClass.isAnnotationType()) {
        PsiClass annotationClass = javaPsiFacade.findClass(aliasedClassName, scope);
        if (annotationClass != null && annotationClass.isAnnotationType()) {
          Iterator var9 = getAliasForAttributes(toSearchInClass).iterator();

          TodayAliasFor aliasFor;
          do {
            if (!var9.hasNext()) {
              return true;
            }

            aliasFor = (TodayAliasFor) var9.next();
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

  private static List<TodayAliasFor> getAliasForAttributes(PsiClass psiClass) {

    List var10000 = (List) CachedValuesManager.getCachedValue(psiClass, () -> {
      List<TodayAliasFor> aliasForList = JamService.getJamService(psiClass.getProject()).getAnnotatedMembersList(psiClass, TodayAliasFor.SEM_KEY, 2);
      return Result.create(aliasForList, new Object[] { psiClass });
    });
    return var10000;
  }

  public static @Nullable PsiAnnotation findDefiningMetaAnnotation(@Nullable PsiElement context, String customAnnotationFqn, String baseMetaAnnotationFqn) {

    return findDefiningMetaAnnotation(context, customAnnotationFqn, baseMetaAnnotationFqn, false);
  }

  public static @Nullable PsiAnnotation findDefiningMetaAnnotation(@Nullable PsiElement context, String customAnnotationFqn, String baseMetaAnnotationFqn, boolean includingTests) {

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
        Collection<PsiClass> allMetaAnnotations = includingTests ? JamAnnotationTypeUtil.getInstance(module)
                .getAnnotationTypesWithChildrenIncludingTests(baseMetaAnnotationFqn) : JamAnnotationTypeUtil.getInstance(module).getAnnotationTypesWithChildren(baseMetaAnnotationFqn);
        return findDefiningMetaAnnotation(customAnnoClass, baseMetaAnnotationFqn, allMetaAnnotations);
      }
    }
  }

  public static @Nullable PsiAnnotation findDefiningMetaAnnotation(@Nullable PsiClass customAnnoClass, String baseMetaAnnotationFqn,
          Collection<? extends PsiClass> allMetaAnnotations) {

    if (customAnnoClass != null && customAnnoClass.isAnnotationType()) {
      PsiAnnotation psiAnnotation = AnnotationUtil.findAnnotation(customAnnoClass, true, new String[] { baseMetaAnnotationFqn });
      if (psiAnnotation != null) {
        return psiAnnotation;
      }
      else {
        Iterator var4 = allMetaAnnotations.iterator();

        PsiClass customMetaAnnoClass;
        String qualifiedName;
        do {
          if (!var4.hasNext()) {
            return null;
          }

          customMetaAnnoClass = (PsiClass) var4.next();
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

  public static NotNullFunction<Pair<String, Project>, JamAnnotationMeta> getAnnotationMetaProducer(SemKey<JamAnnotationMeta> annoMetaKey, JamMemberMeta<?, ?>... parentMetas) {

    return (anno) -> {
      return new JamAnnotationMeta((String) anno.first, (JamAnnotationArchetype) null, annoMetaKey) {
        public @Nullable JamAttributeMeta<?> findAttribute(@Nullable @NonNls String name) {
          JamAttributeMeta attribute = super.findAttribute(name);
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

        private @Nullable JamAttributeMeta<?> getAliasedAttributeMeta(String name) {

          JamMemberMeta[] var2 = parentMetas;
          int var3 = var2.length;

          for (int var4 = 0; var4 < var3; ++var4) {
            JamMemberMeta parentMeta = var2[var4];
            Iterator var6 = parentMeta.getAnnotations().iterator();

            while (var6.hasNext()) {
              JamAnnotationMeta annotationMeta = (JamAnnotationMeta) var6.next();
              List parentAnnotationAttributes = this.getRegisteredAttributes(annotationMeta);
              Iterator var9 = parentAnnotationAttributes.iterator();

              while (var9.hasNext()) {
                JamAttributeMeta attributeMeta = (JamAttributeMeta) var9.next();
                if (attributeMeta instanceof JamStringAttributeMeta) {
                  JamStringAttributeMeta meta = (JamStringAttributeMeta) attributeMeta;
                  JamConverter converter = meta.getConverter();
                  TodayAliasFor aliasFor = AliasForUtils.findAliasFor((Project) anno.second, (String) anno.first, annotationMeta.getAnnoName(), meta.getAttributeLink().getAttributeName());
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

          List attributeMetas = new SmartList();
          attributeMetas.addAll(annotationMeta.getAttributes());
          JamAnnotationArchetype archetype = annotationMeta.getArchetype();
          if (archetype != null) {
            attributeMetas.addAll(archetype.getAttributes());
          }

          return attributeMetas;
        }
      };
    };
  }
}

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


package cn.taketoday.assistant.spel;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.spring.el.contextProviders.SpringElInjectionContext;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AliasForElement;
import cn.taketoday.assistant.TodayLibraryUtil;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
public final class AliasedAttributeInjectionContext extends SpringElInjectionContext {
  private static final String CONDITION_ATTR_NAME = "condition";
  private static final String UNLESS_ATTR_NAME = "unless";
  private static final String KEY_ATTR_NAME = "key";

  private final Map<String, Set<String>> ourAliasAnnotationToAttributesMap = Map.of(
          "cn.taketoday.context.event.EventListener", Set.of(CONDITION_ATTR_NAME),
          "cn.taketoday.cache.annotation.Cacheable", Set.of(CONDITION_ATTR_NAME, UNLESS_ATTR_NAME, KEY_ATTR_NAME),
          "cn.taketoday.cache.annotation.CachePut", Set.of(CONDITION_ATTR_NAME, UNLESS_ATTR_NAME, KEY_ATTR_NAME),
          "cn.taketoday.cache.annotation.CacheEvict", Set.of(CONDITION_ATTR_NAME, UNLESS_ATTR_NAME)
  );

  public static boolean isCustomAnnotation(Module module, String annotationName, String baseAnnoName) {
    if (annotationName.equals(baseAnnoName)) {
      return true;
    }
    else {
      PsiClass annoClass = JavaPsiFacade.getInstance(module.getProject()).findClass(annotationName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
      return annoClass != null && MetaAnnotationUtil.isMetaAnnotated(annoClass, Collections.singleton(baseAnnoName));
    }
  }

  @Override
  public boolean isSpringElCompatibleHost(PsiLiteralExpression host) {
    PsiElement hostParent = host.getParent();
    if (!(hostParent instanceof PsiNameValuePair)) {
      return false;
    }
    else {
      PsiElement parent = hostParent.getParent();
      if (parent instanceof PsiAnnotationParameterList && !parent.getProject().isDefault()) {
        PsiElement element = parent.getParent();
        if (!(element instanceof PsiAnnotation psiAnnotation)) {
          return false;
        }
        else {
          String qualifiedName = psiAnnotation.getQualifiedName();
          if (qualifiedName == null) {
            return false;
          }
          else {
            Module module = ModuleUtilCore.findModuleForPsiElement(psiAnnotation);
            if (!TodayLibraryUtil.hasLibrary(module)) {
              return false;
            }
            else {
              String attributeName = ((PsiNameValuePair) hostParent).getName();
              if (StringUtil.isEmptyOrSpaces(attributeName)) {
                return false;
              }
              else {
                Iterator<Map.Entry<String, Set<String>>> var9 = ourAliasAnnotationToAttributesMap.entrySet().iterator();

                while (true) {
                  Map.Entry<String, Set<String>> entry;
                  String aliasedAnnoName;
                  do {
                    if (!var9.hasNext()) {
                      return false;
                    }

                    entry = var9.next();
                    aliasedAnnoName = entry.getKey();
                  }
                  while (!isCustomAnnotation(module, qualifiedName, aliasedAnnoName));

                  for (String aliasedAttrName : entry.getValue()) {
                    AliasForElement aliasFor = AliasForUtils.findAliasFor(psiAnnotation, qualifiedName, aliasedAnnoName, aliasedAttrName);
                    if (aliasFor != null && attributeName.equals(aliasFor.getMethodName())) {
                      return true;
                    }
                  }
                }
              }
            }
          }
        }
      }
      else {
        return false;
      }
    }
  }
}

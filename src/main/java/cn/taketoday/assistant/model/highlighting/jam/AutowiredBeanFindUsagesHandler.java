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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;

/**
 * Bean autowrite Usage
 */
public class AutowiredBeanFindUsagesHandler extends FindUsagesHandler {

  public AutowiredBeanFindUsagesHandler(PsiElement psiMember) {
    super(psiMember);
  }

  @Override
  public boolean processElementUsages(PsiElement element, Processor<? super UsageInfo> processor, FindUsagesOptions options) {
    if (super.processElementUsages(element, processor, options)) {
      BeanPointer<?> pointer = ReadAction.compute(() -> {
        CommonInfraBean bean = InfraBeanUtils.of().findBean(getPsiElement());
        if (bean == null) {
          return null;
        }
        return InfraBeanService.of().createBeanPointer(bean);
      });
      if (pointer != null) {
        return processAutowiredBeans(element, processor, options, pointer);
      }
      return true;
    }

    return false;
  }

  public static boolean processAutowiredBeans(
          PsiElement element, Processor<? super UsageInfo> processor,
          FindUsagesOptions options, BeanPointer<?> pointer) {
    return ReadAction.compute(() -> {
      Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module == null) {
        return true;
      }
      var project = element.getProject();
      PsiType[] effectiveBeanTypes = null;
      GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);

      var annotations = new ArrayList<>(AutowireUtil.getAutowiredAnnotations(module));

      var com = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, AnnotationConstant.COMPONENT, false);
//      annotations.addAll(com);
      List<String> fqns = ContainerUtil.mapNotNull(com, PsiClass::getQualifiedName);
      annotations.addAll(fqns);
      for (String anno : annotations) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(anno, scope);
        if (psiClass != null) {
          var psiMembers = AnnotatedElementsSearch.searchPsiMembers(psiClass, options.searchScope);
          for (PsiMember member : psiMembers) {
            PsiClass containingClass = member.getContainingClass();
            if (containingClass != null) {
              if (effectiveBeanTypes == null) {
                effectiveBeanTypes = pointer.getEffectiveBeanTypes();
              }
              if (member instanceof PsiField field) {
                if (isAutowiredCandidate(processor, effectiveBeanTypes, field)) {
                  return false; // terminate
                }
              }
              else if (member instanceof PsiMethod method) {
                for (PsiParameter parameter : method.getParameterList().getParameters()) {
                  if (isAutowiredCandidate(processor, effectiveBeanTypes, parameter)) {
                    return false; // terminate
                  }
                }
              }
            }
          }
        }
      }

      // next
      return true;
    });
  }

  private static boolean isAutowiredCandidate(
          Processor<? super UsageInfo> processor, PsiType[] effectiveBeanTypes, PsiVariable element) {
    PsiType variableType = AutowireUtil.getAutowiredEffectiveBeanTypes(element.getType());
    for (PsiType psiType : effectiveBeanTypes) {
      if (variableType.isAssignableFrom(psiType)) {
        return !processor.process(new AutowiredMemberUsageInfo(element));
      }
    }
    return false;
  }
}

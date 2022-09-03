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

package cn.taketoday.assistant.model.highlighting.providers;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.xml.DomFileElement;

import java.util.Objects;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import kotlin.jvm.functions.Function1;

public final class InfraImplicitSubclassProviderKt {
  public static final String ADVISE_MODE_ASPECTJ = "cn.taketoday.context.annotation.AdviceMode.ASPECTJ";

  public static boolean hasAspectJTransactionMode(CommonInfraModel springModel) {
    if (springModel instanceof LocalAnnotationModel config) {
      PsiClass getConfig = config.getConfig();
      return isAspectJAdviceMode(getConfig);
    }
    else if (springModel instanceof LocalXmlModel model) {
      var findFirstProcessor = findFirstProcessor(new Function1<CommonInfraBean, Boolean>() {
        @Override
        public Boolean invoke(CommonInfraBean it) {
          // FIXME
          return false;
        }
      });
      DomFileElement<Beans> root = model.getRoot();
      if (root != null) {
        InfraBeanUtils.of()
                .processChildBeans(root.getRootElement(), false, findFirstProcessor);
      }
      return findFirstProcessor.isFound();
    }
    else if (springModel instanceof InfraModel) {
      var processor = findFirstProcessor(new Function1<CommonInfraModel, Boolean>() {
        @Override
        public Boolean invoke(CommonInfraModel it) {
          if (!(it instanceof InfraModel)) {
            return hasAspectJTransactionMode(it);
          }
          return false;
        }
      });
      InfraModelVisitors.visitRelatedModels(springModel, InfraModelVisitorContext.context(
              processor, new InfraModelVisitorContext.Exec<>() {
                @Override
                public boolean run(CommonInfraModel m, Processor<? super CommonInfraModel> processor) {
                  return processor.process(m);
                }
              }), false);
      return processor.isFound();
    }
    else {
      return false;
    }

  }

  private static <T> CommonProcessors.FindFirstProcessor<T> findFirstProcessor(Function1<? super T, Boolean> func) {
    return new CommonProcessors.FindFirstProcessor<>() {
      protected boolean accept(T t) {
        return func.invoke(t);
      }
    };
  }

  private static boolean isAspectJAdviceMode(PsiClass springAnnotationConfig) {
    PsiAnnotationMemberValue memberValue;
    label35:
    {
      PsiModifierList modifierList = springAnnotationConfig.getModifierList();
      if (modifierList != null) {
        PsiAnnotation mode = modifierList.findAnnotation(AnnotationConstant.ENABLE_TRANSACTION_MANAGEMENT);
        if (mode != null) {
          memberValue = mode.findAttributeValue("mode");
          break label35;
        }
      }

      memberValue = null;
    }

    PsiElement psiElement = null;
    if (memberValue instanceof PsiReferenceExpression psiReferenceExpression) {
      psiElement = psiReferenceExpression.resolve();
    }

    String var8 = null;
    if (psiElement instanceof PsiEnumConstant enumConstant) {
      var8 = enumConstant.getContainingClass().getQualifiedName() + "." + enumConstant.getName();
    }

    return Objects.equals(var8, ADVISE_MODE_ASPECTJ);
  }
}

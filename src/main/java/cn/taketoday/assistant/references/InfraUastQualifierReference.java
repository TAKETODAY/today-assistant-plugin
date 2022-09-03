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

package cn.taketoday.assistant.references;

import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.DefaultInfraBeanQualifier;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;

public final class InfraUastQualifierReference extends PsiReferenceBase.Poly<PsiLanguageInjectionHost> implements HighlightedReference {
  private final UExpression uLiteral;

  private final PsiLanguageInjectionHost host;

  public PsiLanguageInjectionHost getHost() {
    return this.host;
  }

  public InfraUastQualifierReference(UExpression uLiteral, PsiLanguageInjectionHost host) {
    super(host);
    this.uLiteral = uLiteral;
    this.host = host;
  }

  public Object[] getVariants() {
    UAnnotation uAnnotation = UastUtils.getParentOfType(this.uLiteral, UAnnotation.class, true);
    if (uAnnotation == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    else {
      PsiClass psiAnnoClass = InfraUastQualifierReferenceKt.findAnnotationClass(uAnnotation, this.host);
      if (psiAnnoClass == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      else {
        boolean strict$iv = true;
        UVariable uVariable = UastUtils.getParentOfType(this.uLiteral, UVariable.class, strict$iv);
        if (uVariable == null) {
          return PsiReference.EMPTY_ARRAY;
        }
        else {
          CommonInfraModel model = this.getInfraModel();
          Set<BeanPointer<?>> pointers = AutowireUtil.autowireByType(model, uVariable.getType());
          HashSet<LookupElement> variants = new HashSet<>();

          for (BeanPointer beanPointer : pointers) {
            CommonInfraBean bean = beanPointer.getBean();

            Collection<InfraQualifier> infraQualifiers = bean.getInfraQualifiers();
            Iterator<InfraQualifier> infraQualifier = infraQualifiers.iterator();

            LookupElement lookupElement;
            while (infraQualifier.hasNext()) {
              label:
              {
                InfraQualifier qualifier = infraQualifier.next();

                String qualifierValue = qualifier.getQualifierValue();
                if (qualifierValue != null) {
                  if (qualifier instanceof DefaultInfraBeanQualifier
                          || Comparing.equal(qualifier.getQualifierType(), psiAnnoClass)) {
                    lookupElement = InfraConverterUtil.createCompletionVariant(beanPointer, qualifierValue);
                    break label;
                  }
                }

                lookupElement = null;
              }

              if (lookupElement != null) {
                variants.add(lookupElement);
              }
            }

            if (variants.isEmpty()) {
              lookupElement = InfraConverterUtil.createCompletionVariant(beanPointer);
              if (lookupElement != null) {
                variants.add(lookupElement);
              }
            }
          }

          return variants.toArray(new LookupElement[0]);
        }
      }
    }
  }

  public ResolveResult[] multiResolve(boolean incompleteCode) {
    boolean strict$iv = true;
    UAnnotation uAnnotation = UastUtils.getParentOfType(this.uLiteral, UAnnotation.class, strict$iv);
    if (uAnnotation == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    else {
      if (!(this.uLiteral.evaluate() instanceof String)) {
        return ResolveResult.EMPTY_ARRAY;
      }
      else if (InfraUastQualifierReferenceKt.findAnnotationClass(uAnnotation, this.host) == null) {
        return ResolveResult.EMPTY_ARRAY;
      }
      else {
        CommonInfraModel model = this.getInfraModel();
        UInfraQualifier qualifier = new UInfraQualifier(uAnnotation);
        Set<BeanPointer<?>> qualifiedBeans = InfraModelVisitorUtils.findQualifiedBeans(model, qualifier);
        ArrayList<PsiElementResolveResult> results = new ArrayList<>();

        for (BeanPointer<?> qualifiedBean : qualifiedBeans) {
          CommonInfraBean bean = qualifiedBean.getBean();

          for (InfraQualifier it : bean.getInfraQualifiers()) {
            PsiElementResolveResult resolveResult = new PsiElementResolveResult(it.getIdentifyingPsiElement());
            results.add(resolveResult);
          }
        }

        String var31 = qualifier.getQualifierValue();
        if (var31 != null) {
          BeanPointer<?> bean = InfraModelSearchers.findBean(model, var31);
          if (bean != null) {
            if (bean.isValid()) {
              PsiElement psiElement = bean.getPsiElement();
              if (psiElement != null) {
                results.add(new PsiElementResolveResult(psiElement));
              }
            }
          }
        }

        return results.toArray(new ResolveResult[0]);
      }
    }
  }

  private CommonInfraModel getInfraModel() {
    return InfraModelService.of().getModel(this.myElement);
  }

}

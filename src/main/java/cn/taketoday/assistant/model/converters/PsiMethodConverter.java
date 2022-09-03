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
package cn.taketoday.assistant.model.converters;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class PsiMethodConverter extends Converter<PsiMethod> implements CustomReferenceConverter<PsiMethod> {

  private final MethodAccepter myMethodAccepter;

  protected PsiMethodConverter(MethodAccepter accepter) {
    myMethodAccepter = accepter;
  }

  protected PsiMethodConverter() {
    this(new MethodAccepter());
  }

  protected static class MethodAccepter {
    public boolean accept(PsiMethod method) {
      return !method.isConstructor() &&
              method.hasModifierProperty(PsiModifier.PUBLIC) &&
              !method.hasModifierProperty(PsiModifier.STATIC);
    }
  }

  @Override
  public PsiMethod fromString(@Nullable final String methodName, final ConvertContext context) {
    if (StringUtil.isEmpty(methodName)) {
      return null;
    }
    final PsiClass psiClass = getPsiClass(context);
    if (psiClass == null) {
      return null;
    }
    final PsiMethod[] psiMethods = getMethodCandidates(methodName, psiClass);
    if (psiMethods.length == 0) {
      return null;
    }
    final MethodAccepter accepter = getMethodAccepter(context, false);
    for (PsiMethod method : psiMethods) {
      if (accepter.accept(method)) {
        return method;
      }
    }
    return null;
  }

  protected abstract PsiMethod[] getMethodCandidates(String methodIdentificator, PsiClass psiClass);

  @Override
  public String toString(@Nullable final PsiMethod psiMethods, final ConvertContext context) {
    return null;
  }

  @Nullable
  protected abstract PsiClass getPsiClass(final ConvertContext context);

  protected MethodAccepter getMethodAccepter(ConvertContext context, final boolean forCompletion) {
    return myMethodAccepter;
  }

  protected Object[] getVariants(ConvertContext context) {
    final PsiClass psiClass = getPsiClass(context);
    if (psiClass == null) {
      return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }
    final MethodAccepter methodAccepter = getMethodAccepter(context, true);
    final List<LookupElement> result = new ArrayList<>();
    Collection<HierarchicalMethodSignature> allMethodSigs = psiClass.getVisibleSignatures();

    for (HierarchicalMethodSignature signature : allMethodSigs) {
      final PsiMethod method = signature.getMethod();
      if (methodAccepter.accept(method)) {
        String tail = PsiFormatUtil.formatMethod(method,
                PsiSubstitutor.EMPTY,
                PsiFormatUtilBase.SHOW_PARAMETERS,
                PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_TYPE);
        LookupElementBuilder builder = LookupElementBuilder.create(method, getMethodIdentificator(method))
                .withIcon(method.getIcon(0))
                .withStrikeoutness(method.isDeprecated())
                .withTailText(tail);
        final PsiType returnType = method.getReturnType();
        if (returnType != null) {
          builder = builder.withTypeText(returnType.getPresentableText());
        }
        result.add(builder);
      }
    }
    return ArrayUtil.toObjectArray(result);
  }

  protected abstract String getMethodIdentificator(PsiMethod method);

  @Override
  public PsiReference[] createReferences(final GenericDomValue<PsiMethod> genericDomValue,
          final PsiElement element,
          final ConvertContext context) {

    return new PsiReference[] { new MyReference(element, genericDomValue, context) };
  }

  protected class MyReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider, LocalQuickFixProvider {
    private final GenericDomValue<? extends PsiMethod> myGenericDomValue;
    private final ConvertContext myContext;

    public MyReference(final PsiElement element,
            final GenericDomValue<? extends PsiMethod> genericDomValue,
            ConvertContext context) {
      super(element);
      myGenericDomValue = genericDomValue;
      myContext = context;
    }

    @Override
    public Object[] getVariants() {
      return PsiMethodConverter.this.getVariants(myContext);
    }

    @Override
    @Nullable
    public PsiElement resolve() {
      return myGenericDomValue.getValue();
    }

    @Override
    public boolean isSoft() {
      return true;
    }

    @Override
    public PsiElement bindToElement(final PsiElement element) throws IncorrectOperationException {
      if (!(element instanceof PsiMethod)) {
        throw new IncorrectOperationException("PsiMethod expected, but found: " + element);
      }
      final PsiMethod psiMethod = (PsiMethod) element;
      myGenericDomValue.setStringValue(getMethodIdentificator(psiMethod));
      return psiMethod;
    }

    @Override
    public LocalQuickFix[] getQuickFixes() {
      return PsiMethodConverter.this.getQuickFixes(myContext);
    }

    @Override

    public String getUnresolvedMessagePattern() {
      return InfraBundle.message("cannot.resolve.method", myGenericDomValue.getStringValue());
    }
  }

  protected LocalQuickFix[] getQuickFixes(final ConvertContext context) {
    return LocalQuickFix.EMPTY_ARRAY;
  }
}

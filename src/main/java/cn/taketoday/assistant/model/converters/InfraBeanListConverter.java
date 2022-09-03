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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.DelimitedListConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.converters.fixes.bean.InfraBeanResolveQuickFixManager;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

/**
 * @author Yann C&eacute;bron
 */
public class InfraBeanListConverter extends DelimitedListConverter<BeanPointer<?>> {
  public InfraBeanListConverter() {
    super(InfraUtils.INFRA_DELIMITERS);
  }

  @Override
  public boolean canResolveTo(Class<? extends PsiElement> elementClass) {
    return ReflectionUtil.isAssignable(BeanPointer.class, elementClass);
  }

  @Override
  @Nullable
  protected BeanPointer<?> convertString(@Nullable String s, ConvertContext context) {
    if (s == null)
      return null;

    CommonInfraModel model = InfraConverterUtil.getSpringModel(context);
    if (model == null)
      return null;

    return InfraModelSearchers.findBean(model, s);
  }

  @Override
  @Nullable
  protected String toString(@Nullable BeanPointer<?> beanPointer) {
    return beanPointer == null ? null : beanPointer.getName();
  }

  @Override
  @Nullable
  protected PsiElement resolveReference(BeanPointer<?> beanPointer,
          ConvertContext context) {
    return beanPointer == null ? null : beanPointer.getPsiElement();
  }

  @Override
  protected String getUnresolvedMessage(String value) {
    return message("model.bean.error.message", value);
  }

  @Override
  protected Object[] getReferenceVariants(ConvertContext context,
          GenericDomValue<? extends List<BeanPointer<?>>> genericDomValue) {
    CommonInfraModel model = InfraConverterUtil.getSpringModel(context);
    if (model == null)
      return EMPTY_ARRAY;

    List<BeanPointer<?>> variants = new ArrayList<>();

    DomInfraBean currentBean = InfraConverterUtil.getCurrentBean(context);

    Collection<BeanPointer<?>> allBeans = getVariantBeans(context, model);
    for (BeanPointer<?> pointer : allBeans) {
      if (pointer.isReferenceTo(currentBean))
        continue;

      for (String string : InfraModelVisitorUtils.getAllBeanNames(model, pointer)) {
        if (StringUtil.isNotEmpty(string)) {
          variants.add(pointer.derive(string));
        }
      }
    }

    List<BeanPointer<?>> existingBeans = genericDomValue.getValue();
    if (existingBeans != null) {
      for (Iterator<BeanPointer<?>> it = variants.iterator(); it.hasNext(); ) {
        CommonInfraBean variant = it.next().getBean();
        for (BeanPointer<?> existing : existingBeans) {
          if (existing.isReferenceTo(variant)) {
            it.remove();
            break;
          }
        }
      }
    }

    List<LookupElement> result = new ArrayList<>(variants.size());
    for (BeanPointer<?> pointer : variants) {
      ContainerUtil.addIfNotNull(result, InfraConverterUtil.createCompletionVariant(pointer));
    }

    return result.toArray();
  }

  protected Collection<BeanPointer<?>> getVariantBeans(ConvertContext convertContext,
          CommonInfraModel model) {
    List<PsiClassType> requiredBeanTypeClasses = InfraConverterUtil.getRequiredBeanTypeClasses(convertContext);
    if (!requiredBeanTypeClasses.isEmpty()) {
      CommonInfraBean currentBean = InfraConverterUtil.getCurrentBeanCustomAware(convertContext);
      return InfraConverterUtil.getSmartVariants(currentBean, requiredBeanTypeClasses, model);
    }
    return model.getAllCommonBeans();
  }

  @Override

  protected PsiReference createPsiReference(PsiElement element,
          int start,
          int end,
          ConvertContext context,
          GenericDomValue<List<BeanPointer<?>>> genericDomValue,
          boolean delimitersOnly) {
    return new MyFixableReference(element, getTextRange(genericDomValue, start, end), context, genericDomValue, delimitersOnly);
  }

  private final class MyFixableReference extends MyPsiReference implements LocalQuickFixProvider {

    private final Beans beans;

    private MyFixableReference(PsiElement element,
            TextRange range,
            ConvertContext context,
            GenericDomValue<List<BeanPointer<?>>> genericDomValue,
            boolean delimitersOnly) {
      super(element, range, context, genericDomValue, delimitersOnly);
      this.beans = genericDomValue.getParentOfType(Beans.class, false);
    }

    @Override
    public LocalQuickFix[] getQuickFixes() {
      return InfraBeanResolveQuickFixManager.of()
              .getQuickFixes(myContext, beans, getValue(),
                      InfraConverterUtil.getRequiredBeanTypeClasses(myContext));
    }
  }
}

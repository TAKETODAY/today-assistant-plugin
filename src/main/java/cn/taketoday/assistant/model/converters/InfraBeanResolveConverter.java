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
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ResolvingConverter;
import com.intellij.util.xml.impl.GenericDomValueReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.XmlInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.converters.fixes.bean.InfraBeanResolveQuickFixManager;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.TypeHolder;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public class InfraBeanResolveConverter extends ResolvingConverter<BeanPointer<?>> implements CustomReferenceConverter {
  @Override
  public boolean canResolveTo(Class<? extends PsiElement> elementClass) {
    return ReflectionUtil.isAssignable(BeanPointer.class, elementClass);
  }

  @Nullable
  protected CommonInfraModel getInfraModel(ConvertContext context) {
    return InfraManager.from(context.getFile().getProject()).getInfraModelByFile(context.getFile());
  }

  /**
   * Used to filter variants by bean class.
   *
   * @param context conversion context.
   * @return empty list if no requirement applied.
   */

  public List<PsiClassType> getRequiredClasses(ConvertContext context) {
    return InfraConverterUtil.getRequiredBeanTypeClasses(context);
  }

  @Override
  @Nullable
  public BeanPointer<?> fromString(@Nullable final String s, ConvertContext context) {
    if (s == null)
      return null;

    CommonInfraModel infraModel = getInfraModel(context);
    if (infraModel == null)
      return null;

    return InfraBeanUtils.of().findBean(infraModel, s);
  }

  @Override
  public String toString(@Nullable final BeanPointer<?> beanPointer, ConvertContext context) {
    return beanPointer == null ? null : beanPointer.getName();
  }

  @Override
  public LookupElement createLookupElement(BeanPointer beanPointer) {
    return InfraConverterUtil.createCompletionVariant(beanPointer);
  }

  @Override
  public PsiElement getPsiElement(@Nullable BeanPointer<?> resolvedValue) {
    if (resolvedValue == null || !resolvedValue.isValid())
      return null;

    return resolvedValue.getPsiElement();
  }

  @Override
  public String getErrorMessage(String s, ConvertContext context) {
    return InfraBundle.message("model.bean.error.message", s);
  }

  @Override
  public PsiReference[] createReferences(GenericDomValue value, PsiElement element, ConvertContext context) {
    if (isPlaceholder(context)) {
      return ArrayUtil.append(PlaceholderUtils.getInstance().createPlaceholderPropertiesReferences(value),
              new GenericDomValueReference<>(value));
    }
    return PsiReference.EMPTY_ARRAY;
  }

  @Override
  public LocalQuickFix[] getQuickFixes(ConvertContext context) {
    GenericDomValue element = (GenericDomValue) context.getInvocationElement();
    return InfraBeanResolveQuickFixManager.of().getQuickFixes(context,
            element.getParentOfType(Beans.class, false),
            null,
            getRequiredClasses(context));
  }

  @Override
  public Collection<BeanPointer<?>> getVariants(ConvertContext context) {
    if (isPlaceholder(context)) {
      return Collections.emptySet();
    }

    return getVariants(context, false, false, getRequiredClasses(context), getInfraModel(context));
  }

  protected static boolean isPlaceholder(ConvertContext context) {
    DomElement element = context.getInvocationElement();
    return element instanceof GenericDomValue
            && PlaceholderUtils.getInstance().isRawTextPlaceholder((GenericDomValue) element);
  }

  protected static Collection<BeanPointer<?>> getVariants(ConvertContext context,
          boolean parentBeans,
          boolean allowAbstracts,
          List<PsiClassType> requiredClasses,
          CommonInfraModel model) {
    if (model == null)
      return Collections.emptyList();

    List<BeanPointer<?>> variants = new ArrayList<>();
    CommonInfraBean currentBean = InfraConverterUtil.getCurrentBeanCustomAware(context);

    // remove java.lang.Object (e.g. "fallback type" via TypeHolder.getRequiredTypes)
    if (!requiredClasses.isEmpty()) {
      PsiClassType object = PsiType.getJavaLangObject(context.getPsiManager(), GlobalSearchScope.allScope(context.getProject()));
      if (requiredClasses.contains(object)) {
        requiredClasses = new ArrayList<>(requiredClasses); // guard against immutable
        requiredClasses.remove(object);
      }
    }

    Collection<BeanPointer<?>> pointers = getModelVariants(parentBeans, model, requiredClasses, currentBean);
    InfraConverterUtil.processBeans(model, variants, pointers, allowAbstracts, currentBean);
    return variants;
  }

  private static Collection<BeanPointer<?>> getModelVariants(boolean parentBeans,
          CommonInfraModel model,
          List<PsiClassType> requiredClasses,
          CommonInfraBean currentBean) {
    if (parentBeans) {
      if (model instanceof XmlInfraModel) {
        Collection<BeanPointer<?>> allBeans = new ArrayList<>();
        for (InfraModel infraModel : ((XmlInfraModel) model).getDependencies()) {
          if (requiredClasses.isEmpty()) {
            allBeans.addAll(infraModel.getAllCommonBeans());
          }
          else {
            allBeans.addAll(InfraConverterUtil.getSmartVariants(currentBean, requiredClasses, model));
          }
        }
        return allBeans;
      }
      return Collections.emptySet();
    }

    return requiredClasses.isEmpty()
           ? model.getAllCommonBeans()
           : InfraConverterUtil.getSmartVariants(currentBean, requiredClasses, model);
  }

  protected static List<PsiClassType> getValueClasses(ConvertContext context) {
    TypeHolder valueHolder = context.getInvocationElement().getParentOfType(TypeHolder.class, false);
    if (valueHolder == null) {
      return Collections.emptyList(); // invalid XML
    }
    return ContainerUtil.findAll(TypeHolderUtil.getRequiredTypes(valueHolder), PsiClassType.class);
  }

  public static class PropertyBean extends InfraBeanResolveConverter {

    @Override

    public List<PsiClassType> getRequiredClasses(ConvertContext context) {
      return getValueClasses(context);
    }
  }

  public static class Parent extends InfraBeanResolveConverter {

    @Override
    public Collection<BeanPointer<?>> getVariants(ConvertContext context) {
      return getVariants(context, false, true, getRequiredClasses(context), getInfraModel(context));
    }
  }
}

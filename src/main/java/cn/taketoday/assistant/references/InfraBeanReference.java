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

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.lang.Nullable;

/**
 * Reference to Infra Bean by name, optionally limited to required base-class.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class InfraBeanReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {

  @Nullable
  private final PsiClass requiredClass;
  private final boolean factoryBeanRef;

  public InfraBeanReference(PsiElement element) {
    this(element, ElementManipulators.getValueTextRange(element));
  }

  public InfraBeanReference(PsiElement element, TextRange range) {
    this(element, range, null, false);
  }

  public InfraBeanReference(PsiElement element,
          TextRange range,
          @Nullable PsiClass requiredClass,
          boolean isFactoryBeanRef) {
    super(element, range);
    this.requiredClass = requiredClass;
    this.factoryBeanRef = isFactoryBeanRef;
  }

  public boolean isFactoryBeanRef() {
    return factoryBeanRef;
  }

  protected CommonInfraModel getInfraModel() {
    return InfraModelService.of().getModel(getElement());
  }

  @Override
  public PsiElement resolve() {
    String beanName = getValue();

    CommonInfraModel infraModel = getInfraModel();

    BeanPointer<?> pointer = InfraModelSearchers.findBean(infraModel, beanName);
    return pointer == null || !pointer.isValid() ? null : pointer.getPsiElement();
  }

  @Override
  public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
    return getElement();
  }

  @Override
  public Object[] getVariants() {
    CommonInfraModel model = getInfraModel();

    Collection<BeanPointer<?>> beans = getBeanPointers(model);

    List<LookupElement> lookups = new ArrayList<>(beans.size());
    for (BeanPointer<?> bean : beans) {
      ContainerUtil.addIfNotNull(lookups, InfraConverterUtil.createCompletionVariant(bean));
    }
    return ArrayUtil.toObjectArray(lookups);
  }

  protected Collection<BeanPointer<?>> getBeanPointers(CommonInfraModel model) {
    Collection<BeanPointer<?>> beans;
    if (requiredClass != null && !CommonClassNames.JAVA_LANG_OBJECT.equals(requiredClass.getQualifiedName())) {
      ModelSearchParameters.BeanClass searchParameters =
              ModelSearchParameters.byClass(requiredClass).withInheritors().effectiveBeanTypes();
      beans = InfraModelSearchers.findBeans(model, searchParameters);
    }
    else {
      beans = model.getAllCommonBeans();
    }
    return beans;
  }

  @Override
  public String getUnresolvedMessagePattern() {
    return InfraBundle.message("model.bean.error.message", getValue());
  }
}

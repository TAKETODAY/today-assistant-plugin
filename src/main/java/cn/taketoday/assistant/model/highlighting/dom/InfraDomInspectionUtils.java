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
package cn.taketoday.assistant.model.highlighting.dom;

import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.ide.nls.NlsMessages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Function;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DefineAttributeQuickFix;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.RemoveDomElementQuickFix;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.RequiredBeanType;

/**
 * @author Yann C&eacute;bron
 */
public class InfraDomInspectionUtils {

  private static final Function<DomElement, String> DOM_NAME_FUNCTION = DomElement::getXmlElementName;

  private final DomElementAnnotationHolder holder;

  public InfraDomInspectionUtils(DomElementAnnotationHolder holder) {
    this.holder = holder;
  }

  public static boolean hasAny(DomElement... values) {
    for (DomElement value : values) {
      if (DomUtil.hasXml(value)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasMoreThanOne(DomElement... values) {
    short count = 0;
    for (DomElement value : values) {
      if (DomUtil.hasXml(value)) {
        count++;
        if (count > 1) {
          return true;
        }
      }
    }
    return false;
  }

  public void oneOfRequired(DomElement element,
          DomElement... elements) {
    if (hasMoreThanOne(elements) || !hasAny(elements)) {
      onlyOneOfProblem(element, elements);
    }
  }

  public void oneOfOrAllRequired(DomElement element,
          GenericAttributeValue... values) {
    if (!hasAny(values)) {
      holder.createProblem(element, getDomElementNamesMessage("dom.one.or.all.of.attributes", values));
    }
  }

  public boolean onlyOneOf(DomElement element,
          GenericAttributeValue... values) {
    if (hasMoreThanOne(values)) {
      onlyOneOfProblem(element, values);
      return true;
    }
    return false;
  }

  public void ifExistsOtherRequired(DomElement element,
          GenericAttributeValue exists,
          GenericAttributeValue required) {
    if (hasAny(exists) &&
            !hasAny(required)) {

      String requiredName = DOM_NAME_FUNCTION.fun(required);
      holder.createProblem(element,
              InfraBundle.message("dom.if.exists.other.required",
                      DOM_NAME_FUNCTION.fun(exists),
                      requiredName),
              new DefineAttributeQuickFix(requiredName));
    }
  }

  public void attributeSuperfluous(GenericAttributeValue value) {
    if (hasAny(value)) {
      holder.createProblem(value,
              InfraBundle.message("dom.superfluous.attribute",
                      DOM_NAME_FUNCTION.fun(value)),
              new RemoveDomElementQuickFix(value)).highlightWholeElement();
    }
  }

  public <T> void attributeWithDefaultSuperfluous(GenericAttributeValue<T> value,
          T defaultValue) {
    if (hasAny(value) &&
            defaultValue.equals(value.getValue())) {
      holder.createProblem(value,
              InfraBundle.message("dom.superfluous.attribute.with.default",
                      DOM_NAME_FUNCTION.fun(value),
                      value.getStringValue()),
              new RemoveDomElementQuickFix(value)).highlightWholeElement();
    }
  }

  private void onlyOneOfProblem(DomElement element, DomElement... elements) {
    holder.createProblem(element, getDomElementNamesMessage("dom.only.one.of", elements));
  }

  private static @InspectionMessage String getDomElementNamesMessage(String key,
          DomElement... elements) {
    List<String> elementNames = new ArrayList<>();
    for (DomElement element : elements) {
      String elementName = DOM_NAME_FUNCTION.fun(element);
      String name = element instanceof GenericAttributeValue ? "'" + elementName + "'" : "<" + elementName + ">";
      elementNames.add(name);
    }

    return InfraBundle.message(key, NlsMessages.formatOrList(elementNames));
  }

  public void beanOfType(GenericAttributeValue<BeanPointer<?>> springBeanAttribute, String beanClass) {
    BeanPointer<?> pointer = springBeanAttribute.getValue();
    if (pointer == null) {
      return;
    }

    if (!InheritanceUtil.isInheritor(pointer.getBeanClass(), false, beanClass)) {
      holder.createProblem(springBeanAttribute,
              InfraBundle.message("bean.must.be.of.type", beanClass));
    }
  }

  public void explicitBeanRequired(DomElement domElement,
          GenericAttributeValue<BeanPointer<?>> springBeanAttribute,
          String defaultBeanName) {
    if (hasAny(springBeanAttribute)) {
      return;
    }

    RequiredBeanType annotation = springBeanAttribute.getAnnotation(RequiredBeanType.class);
    assert annotation != null : springBeanAttribute;
    String[] classNames = annotation.value();
    for (String beanClass : classNames) {
      if (existsBean(domElement, beanClass, defaultBeanName)) {
        return;
      }
    }

    holder.createProblem(domElement,
            InfraBundle.message("dom.explicit.bean.reference.required",
                    springBeanAttribute.getPresentation().getTypeName(),
                    defaultBeanName),
            new DefineAttributeQuickFix(DOM_NAME_FUNCTION.fun(springBeanAttribute)));
  }

  public static boolean existsBean(DomElement domElement, String beanClass, String defaultBeanName) {
    PsiClass psiClass = DomJavaUtil.findClass(beanClass, domElement);
    if (psiClass == null) {
      return false;
    }

    ModelSearchParameters.BeanClass searchParameters =
            ModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes();
    if (!searchParameters.canSearch())
      return false;

    CommonInfraModel model = InfraModelService.of().getModel(domElement.getXmlTag());

    BeanPointer<?> beanWithDefaultName = InfraModelSearchers.findBean(model, defaultBeanName);
    if (beanWithDefaultName == null) {
      return false;
    }

    return !model.processByClass(searchParameters, pointer -> !defaultBeanName.equals(pointer.getName()));
  }
}

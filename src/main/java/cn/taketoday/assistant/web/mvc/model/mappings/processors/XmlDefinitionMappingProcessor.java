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

package cn.taketoday.assistant.web.mvc.model.mappings.processors;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.xml.DomAnchor;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.DomTarget;
import com.intellij.util.xml.GenericAttributeValue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.pom.InfraBeanPomTargetUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.Prop;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.mappings.UrlMappingPsiBasedElement;
import cn.taketoday.assistant.web.mvc.model.xml.ViewControllerBase;
import cn.taketoday.lang.Nullable;

public final class XmlDefinitionMappingProcessor {

  public static boolean processXmlDefinitions(Module module, Collection<InfraModel> models, Processor<? super UrlMappingElement> processor) {
    for (InfraModel model : models) {
      for (LocalXmlModel localXmlModel : InfraModelVisitorUtils.getLocalXmlModels(model)) {
        for (BeanPointer<?> beanPointer : searchLocalBeans(localXmlModel, createSearchParams(module, InfraMvcConstant.SERVLET_MVC_CONTROLLER))) {
          if (!processMvcController(processor, beanPointer)) {
            return false;
          }
        }
        for (BeanPointer<?> mapping : searchLocalBeans(localXmlModel, createSearchParams(module, InfraMvcConstant.SIMPLE_URL_HANDLER_MAPPING))) {
          if (!processSimpleMapping(processor, mapping)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static boolean processSimpleMapping(Processor<? super UrlMappingElement> processor, BeanPointer<?> mapping) {
    PsiElement mappingPsiElement;
    CommonInfraBean springBean = mapping.getBean();
    if (springBean instanceof InfraBean bean) {
      InfraPropertyDefinition beanProperty = bean.getProperty("urlMap");
      if (beanProperty instanceof InfraProperty property) {
        for (InfraEntry entry : property.getMap().getEntries()) {
          String key = entry.getKeyAttr().getValue();
          if (key != null && !processor.process(new XmlDefinitionUrlMappingBaseElement(key, entry.getKeyAttr()))) {
            return false;
          }
        }
      }
      InfraPropertyDefinition property2 = bean.getProperty("mappings");
      if (property2 == null || (mappingPsiElement = mapping.getPsiElement()) == null) {
        return true;
      }
      String value = property2.getValueAsString();
      if (value != null) {
        try {
          Properties properties = new Properties();
          properties.load(new StringReader(value));
          for (Map.Entry<Object, Object> entry2 : properties.entrySet()) {
            if (!processor.process(new BeanPointerUrlMappingBaseElement((String) entry2.getKey(), mappingPsiElement.getNavigationElement(), mapping))) {
              return false;
            }
          }
          return true;
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      else if (property2 instanceof InfraProperty holder) {
        for (Prop prop : holder.getProps().getProps()) {
          String url = prop.getKey().getStringValue();
          if (url != null && !processor.process(new XmlDefinitionUrlMappingBaseElement(url, prop.getKey()))) {
            return false;
          }
        }
        return true;
      }
      else {
        return true;
      }
    }
    return true;
  }

  private static class XmlDefinitionUrlMappingBaseElement extends UrlMappingPsiBasedElement {

    private final DomAnchor<GenericAttributeValue<String>> anchor;

    private XmlDefinitionUrlMappingBaseElement(String url, GenericAttributeValue<String> domElement) {
      super(url, domElement.ensureXmlElementExists(), null, url, RequestMethod.EMPTY_ARRAY);
      this.anchor = DomService.getInstance().createAnchor(domElement);
    }

    @Override
    @Nullable
    public PomNamedTarget getPomTarget() {
      DomTarget domTarget;
      PsiElement psiElement = this.anchor.getPsiElement();
      if (psiElement instanceof PomTargetPsiElement) {
        return toPomTarget(psiElement);
      }
      GenericAttributeValue<String> domElement = this.anchor.retrieveDomElement();
      if (domElement != null && (domTarget = DomTarget.getTarget(domElement)) != null) {
        return domTarget;
      }
      return super.getPomTarget();
    }

    @Override
    public boolean isDefinedInBean(BeanPointer<? extends CommonInfraBean> controllerBeanPointer) {
      PomNamedTarget pomTarget = getPomTarget();
      if (pomTarget != null) {
        CommonInfraBean targetBean = InfraBeanPomTargetUtils.getBean(pomTarget);
        return (targetBean instanceof DomInfraBean) && Comparing.equal(targetBean, controllerBeanPointer.getBean());
      }
      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      XmlDefinitionUrlMappingBaseElement element = (XmlDefinitionUrlMappingBaseElement) o;
      return Objects.equals(this.anchor.getPsiElement(), element.anchor.getPsiElement()) && super.equals(o);
    }
  }

  private static Collection<BeanPointer<?>> searchLocalBeans(LocalXmlModel localXmlModel, ModelSearchParameters.BeanClass searchParameters) {
    if (searchParameters == null) {
      return Collections.emptyList();
    }
    CommonProcessors.CollectProcessor<BeanPointer<?>> processor = new CommonProcessors.CollectProcessor<>();
    localXmlModel.processLocalBeansByClass(searchParameters, processor);
    return processor.getResults();
  }

  private static boolean processMvcController(Processor<? super UrlMappingElement> processor, BeanPointer<?> beanPointer) {
    PsiElement psiElement = beanPointer.getPsiElement();
    if (psiElement instanceof PomTargetPsiElement pomTargetPsiElement) {
      PomTarget target = pomTargetPsiElement.getTarget();
      if (target instanceof BeanPsiTarget beanPsiTarget) {
        CommonInfraBean springBean = beanPsiTarget.getInfraBean();
        PsiElement navigationElement = psiElement.getNavigationElement();
        if (springBean instanceof ViewControllerBase viewController) {
          String path = viewController.getPath().getStringValue();
          return path == null || processor.process(new BeanPointerUrlMappingBaseElement(path, navigationElement, beanPointer));
        }
        String name = beanPointer.getName();
        return name == null || processor.process(new BeanPointerUrlMappingBaseElement(name, navigationElement, beanPointer));
      }
      return true;
    }
    return true;
  }

  private static class BeanPointerUrlMappingBaseElement extends UrlMappingPsiBasedElement {

    private final BeanPointer<?> myBeanPointer;

    private BeanPointerUrlMappingBaseElement(String url, PsiElement navigationElement, BeanPointer<?> beanPointer) {
      super(url, navigationElement, null, url, RequestMethod.EMPTY_ARRAY);
      this.myBeanPointer = beanPointer;
    }

    @Override
    @Nullable
    public PomNamedTarget getPomTarget() {
      PsiElement domElement = this.myBeanPointer.getPsiElement();
      if (domElement instanceof PomTargetPsiElement) {
        return toPomTarget(domElement);
      }
      return super.getPomTarget();
    }

    @Override
    public boolean isDefinedInBean(BeanPointer<? extends CommonInfraBean> controllerBeanPointer) {
      return this.myBeanPointer.isReferenceTo(controllerBeanPointer.getBean());
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BeanPointerUrlMappingBaseElement element = (BeanPointerUrlMappingBaseElement) o;
      return Objects.equals(this.myBeanPointer.getPsiElement(), element.myBeanPointer.getPsiElement()) && super.equals(o);
    }
  }

  @Nullable
  public static ModelSearchParameters.BeanClass createSearchParams(Module module, String fqn) {
    PsiClass psiClass = InfraUtils.findLibraryClass(module, fqn);
    if (psiClass == null) {
      return null;
    }
    return ModelSearchParameters.byClass(psiClass).withInheritors();
  }
}

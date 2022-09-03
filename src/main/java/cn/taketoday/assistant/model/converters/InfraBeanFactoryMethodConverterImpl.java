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

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.scope.processor.MethodResolveProcessor;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;

import java.util.List;

import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.highlighting.xml.InfraConstructorArgResolveUtil;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraBeanFactoryMethodConverterImpl extends InfraBeanFactoryMethodConverter {
  private static final String ASPECT_OF_METHOD_NAME = "aspectOf";

  @Override
  public PsiMethod fromString(@Nullable String methodName, ConvertContext context) {
    if (ASPECT_OF_METHOD_NAME.equals(methodName)) {
      return createAspectOfLightMethod(context.getPsiManager());
    }
    return super.fromString(methodName, context);
  }

  @Override
  @Nullable
  public PsiClass getPsiClass(ConvertContext context) {
    InfraBean infraBean = getSpringBean(context);
    return getFactoryClass(infraBean);
  }

  @Override
  protected MethodAccepter getMethodAccepter(ConvertContext context, boolean forCompletion) {
    InfraBean infraBean = getSpringBean(context);
    boolean fromFactoryBean = infraBean.getFactoryBean().getValue() != null;
    return new MethodAccepter() {

      @Override
      public boolean accept(PsiMethod psiMethod) {
        if (psiMethod.isConstructor() || psiMethod.getReturnType() == null) {
          return false;
        }
        PsiClass psiClass = psiMethod.getContainingClass();
        assert psiClass != null;
        String containingClass = psiClass.getQualifiedName();
        return (!forCompletion || containingClass == null || !"java.lang.Object".equals(containingClass)) && FactoryBeansManager.of()
                .isValidFactoryMethod(psiMethod, fromFactoryBean) && (forCompletion || parametersResolved(infraBean, psiMethod));
      }
    };
  }

  private static boolean parametersResolved(InfraBean bean, PsiMethod method) {
    List<PsiMethod> resolvedMethods;
    if (AutowireUtil.isConstructorAutowire(bean)) {
      return true;
    }
    XmlElement beanXmlElement = bean.getXmlElement();
    return beanXmlElement != null && (resolvedMethods = RecursionManager.doPreventingRecursion(beanXmlElement, true,
            () -> InfraConstructorArgResolveUtil.findMatchingMethods(bean))) != null && resolvedMethods.contains(method);
  }

  public static List<PsiMethod> getFactoryMethodCandidates(InfraBean infraBean, String methodName) {
    PsiClass factoryClass = getFactoryClass(infraBean);
    SmartList<PsiMethod> smartList = new SmartList<>();
    if (factoryClass != null) {
      if (ASPECT_OF_METHOD_NAME.equals(methodName)) {
        smartList.add(createAspectOfLightMethod(infraBean.getPsiManager()));
      }
      PsiMethod[] methods = MethodResolveProcessor.findMethod(factoryClass, methodName);
      if (methods.length > 0) {
        boolean fromFactoryBean = infraBean.getFactoryBean().getValue() != null;
        for (PsiMethod method : methods) {
          if (FactoryBeansManager.of().isValidFactoryMethod(method, fromFactoryBean)) {
            smartList.add(method);
          }
        }
      }
    }
    return smartList;
  }

  private static LightMethodBuilder createAspectOfLightMethod(PsiManager manager) {
    return new LightMethodBuilder(manager, ASPECT_OF_METHOD_NAME).setModifiers("static", "public").setMethodReturnType("java.lang.Object");
  }

  @Nullable
  public static PsiClass getFactoryClass(InfraBean infraBean) {
    BeanPointer<?> factoryBeanPointer;
    PsiClass beanClass = null;
    if (!DomUtil.hasXml(infraBean.getFactoryBean())) {
      beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType(false));
    }
    else {
      String beanPointerStringValue = infraBean.getFactoryBean().getRawText();
      if (StringUtil.isEmptyOrSpaces(beanPointerStringValue) || PlaceholderUtils.getInstance()
              .isDefaultPlaceholder(beanPointerStringValue) || (factoryBeanPointer = infraBean.getFactoryBean().getValue()) == null) {
        return null;
      }
      CommonInfraBean factoryBean = factoryBeanPointer.getBean();
      if (!factoryBean.equals(infraBean)) {
        beanClass = RecursionManager.doPreventingRecursion(
                factoryBean, true, () -> PsiTypesUtil.getPsiClass(factoryBean.getBeanType(true)));
      }
    }
    if (beanClass != null && FactoryBeansManager.of().isFactoryBeanClass(beanClass)) {
      PsiType[] types = FactoryBeansManager.of().getObjectTypes(PsiTypesUtil.getClassType(beanClass), infraBean);
      for (PsiType type : types) {
        PsiClass aClass = PsiTypesUtil.getPsiClass(type);
        if (aClass != null) {
          return aClass;
        }
      }
    }
    return beanClass;
  }

  @Override
  public LocalQuickFix[] getQuickFixes(ConvertContext context) {
    InfraBean infraBean;
    PsiClass psiClass;
    GenericDomValue<?> element = (GenericDomValue<?>) context.getInvocationElement();
    String elementName = element.getStringValue();
    return (elementName == null || elementName.length() == 0 || (psiClass = getFactoryMethodClass(
            (infraBean = getSpringBean(context)))) == null || (psiClass instanceof PsiCompiledElement)) ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[] { getCreateNewMethodQuickFix(infraBean,
            psiClass, elementName) };
  }

  @Nullable
  private static PsiClass getFactoryMethodClass(InfraBean infraBean) {
    BeanPointer<?> factoryBeanPointer = infraBean.getFactoryBean().getValue();
    if (factoryBeanPointer != null) {
      return PsiTypesUtil.getPsiClass(factoryBeanPointer.getBean().getBeanType(false));
    }
    return PsiTypesUtil.getPsiClass(infraBean.getBeanType(false));
  }

  private static LocalQuickFix getCreateNewMethodQuickFix(InfraBean infraBean, PsiClass beanClass, String elementName) {
    return new LocalQuickFix() {

      public String getName() {
        return message("model.create.factory.method.quickfix.message", getSignature(infraBean, elementName));
      }

      public String getFamilyName() {
        return message("model.create.factory.method.quickfix.family.name");
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        try {
          PsiElementFactory elementFactory = JavaPsiFacade.getInstance(beanClass.getProject()).getElementFactory();
          String signature = getSignature(infraBean, elementName) + "{ return null; }";
          PsiMethod method = elementFactory.createMethodFromText(signature, null);
          beanClass.add(method);
        }
        catch (IncorrectOperationException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private static String getSignature(InfraBean infraBean, String elementName) {
    boolean isStatic = infraBean.getFactoryBean().getValue() == null;
    String params = InfraConstructorArgResolveUtil.suggestParamsForConstructorArgsAsString(infraBean);
    PsiClass psiClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    String returnType = psiClass == null ? "java.lang.String" : psiClass.getQualifiedName();
    return "public " + (isStatic ? "static" : "") + " " + returnType + " " + elementName + " (" + params + ")";
  }

  private static InfraBean getSpringBean(ConvertContext context) {
    return (InfraBean) InfraConverterUtil.getCurrentBean(context);
  }

  @Override
  public Object[] getVariants(ConvertContext context) {
    return ArrayUtil.append(super.getVariants(context), LookupElementBuilder.create(ASPECT_OF_METHOD_NAME));
  }
}

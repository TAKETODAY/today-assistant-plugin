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

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.RequiredBeanType;
import cn.taketoday.lang.Nullable;

public final class InfraConverterUtil {
  private InfraConverterUtil() {
  }

  @Nullable
  public static InfraModel getInfraModel(ConvertContext context) {
    return getInfraModel(context.getInvocationElement());
  }

  @Nullable
  public static InfraModel getInfraModel(DomElement element) {
    XmlFile xmlFile = (XmlFile) DomUtil.getFile(element).getOriginalFile();
    return InfraManager.from(xmlFile.getProject()).getInfraModelByFile(xmlFile);
  }

  @Nullable
  public static DomInfraBean getCurrentBean(ConvertContext context) {
    return getCurrentBean(context.getInvocationElement());
  }

  @Nullable
  public static CommonInfraBean getCurrentBeanCustomAware(ConvertContext context) {
    DomInfraBean bean = getCurrentBean(context);
    if (bean instanceof final CustomBeanWrapper wrapper) {
      List<CustomBean> list = wrapper.getCustomBeans();
      if (!list.isEmpty()) {
        return list.get(0);
      }
    }
    return bean;
  }

  @Nullable
  public static DomInfraBean getCurrentBean(DomElement element) {
    XmlTag tag = element.getXmlTag();
    if (tag != null) {
      XmlTag originalElement = CompletionUtil.getOriginalElement(tag);
      if (originalElement != tag) {
        DomElement domElement = DomManager.getDomManager(tag.getProject()).getDomElement(originalElement);
        if (domElement != null) {
          DomInfraBean infraBean = domElement.getParentOfType(DomInfraBean.class, false);
          if (infraBean != null) {
            return infraBean;
          }
        }
      }
    }
    return element.getParentOfType(DomInfraBean.class, false);
  }

  public static Collection<PsiPackage> getPsiPackages(PsiReference... psiReferences) {
    Collection<PsiPackage> list = new LinkedHashSet<>();
    for (PsiReference psiReference : psiReferences) {
      if (psiReference instanceof PsiPackageReference) {
        list.addAll(((PsiPackageReference) psiReference).getReferenceSet().resolvePackage());
      }
    }
    return list;
  }

  public static List<PsiClassType> getRequiredBeanTypeClasses(ConvertContext context) {
    DomElement element = context.getInvocationElement();
    RequiredBeanType type = element.getAnnotation(RequiredBeanType.class);
    if (type == null) {
      return Collections.emptyList();
    }

    List<PsiClassType> types = new SmartList<>();
    for (String className : type.value()) {
      PsiClass psiClass = DomJavaUtil.findClass(className, element);
      if (psiClass != null) {
        types.add(PsiTypesUtil.getClassType(psiClass));
      }
    }

    return types;
  }

  public static Collection<BeanPointer<?>> getSmartVariants(CommonInfraBean currentBean,
          List<? extends PsiClassType> requiredClasses,
          CommonInfraModel model) {
    List<BeanPointer<?>> variants = new SmartList<>();
    for (PsiClassType requiredClass : requiredClasses) {
      PsiClass psiClass = PsiUtil.resolveClassInType(requiredClass);
      if (isSearchableClass(psiClass)) {
        ModelSearchParameters.BeanClass searchParameters =
                ModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes();
        processBeans(model, variants, InfraModelSearchers.findBeans(model, searchParameters),
                false, currentBean);
      }
      PsiClass componentClass = PsiUtil.resolveClassInType(PsiUtil.extractIterableTypeParameter(requiredClass, false));
      if (isSearchableClass(componentClass)) {
        ModelSearchParameters.BeanClass searchParameters =
                ModelSearchParameters.byClass(componentClass).withInheritors().effectiveBeanTypes();
        processBeans(model, variants, InfraModelSearchers.findBeans(model, searchParameters),
                false, currentBean);
      }
    }
    return variants;
  }

  private static boolean isSearchableClass(@Nullable PsiClass psiClass) {
    return psiClass != null && psiClass.getQualifiedName() != null && !(psiClass instanceof PsiAnonymousClass);
  }

  public static void processBeans(CommonInfraModel model,
          List<BeanPointer<?>> variants,
          Collection<BeanPointer<?>> pointers,
          boolean acceptAbstract,
          @Nullable CommonInfraBean currentBean) {
    for (BeanPointer<?> bean : pointers) {
      if (!acceptAbstract && bean.isAbstract())
        continue;
      if (bean.isReferenceTo(currentBean))
        continue;

      for (String string : InfraModelVisitorUtils.getAllBeanNames(model, bean)) {
        if (StringUtil.isNotEmpty(string)) {
          variants.add(bean.derive(string));
        }
      }
    }
  }

  @Nullable
  public static LookupElement createCompletionVariant(BeanPointer<?> variant) {
    String name = variant.getName();
    if (name == null) {
      return null;
    }
    return createCompletionVariant(variant, name);
  }

  @Nullable
  public static LookupElement createCompletionVariant(BeanPointer<?> pointer, String name) {
    if (!pointer.isValid())
      return null;

    PsiElement element = pointer.getPsiElement();
    if (element instanceof PomTargetPsiElement) {
      PomTarget target = ((PomTargetPsiElement) element).getTarget();
      if (target instanceof PsiTarget) {
        element = ((PsiTarget) target).getNavigationElement();
      }
    }
    if (element == null) {
      return null;
    }

    LookupElementBuilder lookupElement = LookupElementBuilder.create(element, name)
            .withIcon(InfraPresentationProvider.getInfraIcon(pointer));
    PsiClass beanClass = pointer.getBeanClass();
    if (beanClass != null) {
      lookupElement = lookupElement
              .withTypeText(beanClass.getName())
              .withStrikeoutness(beanClass.isDeprecated());
    }

    return lookupElement.withTailText(" (" + InfraPresentationProvider.getInfraBeanLocation(pointer) + ")", true);
  }

  public static boolean containsPatternReferences(@Nullable String text) {
    return text != null && (StringUtil.containsChar(text, '*') || StringUtil.containsChar(text, '?'));
  }

  public static Collection<PsiPackage> getPackages(@Nullable String text, Project project) {
    return getPackages(text, ",; \n\t", project);
  }

  public static Collection<PsiPackage> getPackages(@Nullable String text, String delimiters, Project project) {
    if (StringUtil.isEmptyOrSpaces(text)) {
      return Collections.emptySet();
    }
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
    List<PsiPackage> list = new SmartList<>();
    new DelimitedListProcessor(delimiters) {
      @Override
      protected void processToken(int start, int end, boolean delimitersOnly) {
        String packageName = text.substring(start, end);
        ContainerUtil.addIfNotNull(list, psiFacade.findPackage(packageName.trim()));
      }
    }.processText(text);

    return list;
  }
}

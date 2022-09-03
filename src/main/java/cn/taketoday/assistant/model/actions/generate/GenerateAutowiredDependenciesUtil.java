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

package cn.taketoday.assistant.model.actions.generate;

import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupFocusDegree;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.LanguageAnnotationSupport;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationSupport;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

public final class GenerateAutowiredDependenciesUtil {

  public static List<InfraGenerateTemplatesHolder> generateAutowiredDependencies(
          PsiClass psiClass, List<? extends BeanPointer<?>> dependencies, CommonInfraModel model) {
    return WriteCommandAction.writeCommandAction(psiClass.getProject()).compute(() -> {
      List<InfraGenerateTemplatesHolder> springInjections = new ArrayList<>();
      for (BeanPointer<?> bean : dependencies) {
        InfraGenerateTemplatesHolder templatesHolder = createAutowiredDependency(psiClass, bean, model);
        if (templatesHolder != null) {
          springInjections.add(templatesHolder);
        }
      }
      return springInjections;
    });
  }

  @Nullable
  private static InfraGenerateTemplatesHolder createAutowiredDependency(PsiClass psiClass, BeanPointer<?> bean, CommonInfraModel model) {
    InfraGenerateTemplatesHolder templatesHolder = new InfraGenerateTemplatesHolder(psiClass.getProject());
    PsiField setter = createField(psiClass, bean, templatesHolder, model);
    if (setter != null) {
      return templatesHolder;
    }
    return null;
  }

  @Nullable
  private static PsiField createField(PsiClass currentBeanClass, BeanPointer<?> candidateBean, InfraGenerateTemplatesHolder templatesHolder,
          CommonInfraModel model) {
    PsiClass[] candidateBeanClasses = Arrays.stream(candidateBean.getEffectiveBeanTypes()).map(PsiTypesUtil::getPsiClass).filter(Objects::nonNull).toArray(PsiClass[]::new);
    if (candidateBeanClasses.length == 0 || !GenerateBeanDependenciesUtil.ensureFileWritable(currentBeanClass)) {
      return null;
    }
    PsiField field = createAutowiredField(candidateBean, currentBeanClass, candidateBeanClasses, model);
    addCreateFieldTemplate(field, candidateBean, candidateBeanClasses, templatesHolder);
    return field;
  }

  private static PsiField createAutowiredField(BeanPointer<?> candidateBean, PsiClass currentBeanClass, PsiClass[] candidateBeanClasses, CommonInfraModel model) {
    PsiClassType javaLangObject;
    try {
      String qualifierName = getQualifierName(candidateBean);
      PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(currentBeanClass.getProject());
      PsiClass candidateBeanClass = candidateBeanClasses[0];
      String beanName = candidateBean.getName();
      String name = (beanName == null || !psiNameHelper.isIdentifier(beanName)) ? candidateBeanClass.getName() : beanName;
      PsiManager psiManager = currentBeanClass.getManager();
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(psiManager.getProject()).getElementFactory();
      if (candidateBeanClass != null) {
        javaLangObject = elementFactory.createType(candidateBeanClass);
      }
      else {
        javaLangObject = PsiType.getJavaLangObject(psiManager, GlobalSearchScope.allScope(psiManager.getProject()));
      }
      PsiClassType type = javaLangObject;
      PsiField psiField = (PsiField) currentBeanClass.add(elementFactory.createField(StringUtil.decapitalize(StringUtil.notNullize(name)), type));
      psiField.getModifierList().addAnnotation(AnnotationConstant.AUTOWIRED);
      ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(candidateBeanClass).withInheritors().effectiveBeanTypes();
      if (InfraModelSearchers.findBeans(model, searchParameters).size() > 1 && !StringUtil.isEmptyOrSpaces(qualifierName)) {
        PsiAnnotation annotation = psiField.getModifierList().addAnnotation(AnnotationConstant.QUALIFIER);
        PsiAnnotationSupport support = LanguageAnnotationSupport.INSTANCE.forLanguage(annotation.getLanguage());
        annotation.setDeclaredAttributeValue("value", support.createLiteralValue(qualifierName, annotation));
      }
      GenerateBeanDependenciesUtil.reformat(psiField);
      return psiField;
    }
    catch (IncorrectOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static String getQualifierName(BeanPointer<?> candidateBean) {
    for (InfraQualifier qualifier : candidateBean.getBean().getInfraQualifiers()) {
      String value = qualifier.getQualifierValue();
      if (!StringUtil.isEmptyOrSpaces(value)) {
        return value;
      }
    }
    return candidateBean.getName();
  }

  private static void addCreateFieldTemplate(PsiField psiField, BeanPointer<?> candidateBean, PsiClass[] psiClasses, InfraGenerateTemplatesHolder templatesHolder) {
    Set<String> existedNames = getExistedNames(psiField);
    PsiType type = psiField.getType();
    Collection<String> suggestedNames = suggestPsiFieldNames(type, candidateBean, existedNames);
    templatesHolder.addTemplateFactory(psiField, () -> {
      Collection<PsiClass> variants = GenerateBeanDependenciesUtil.getSuperTypeVariants(psiClasses);
      PsiTypeElement typeElement = psiField.getTypeElement();
      Expression interfaces = getSuperTypesExpression(typeElement.getType().getCanonicalText(), variants);
      Expression ids = getPsiFieldSuggestNamesExpression(psiField, suggestedNames, existedNames);
      TemplateBuilderImpl builder = new TemplateBuilderImpl(psiField);
      if (variants.size() > 1) {
        builder.replaceElement(typeElement, "type", interfaces, true);
      }
      builder.replaceElement(psiField.getNameIdentifier(), "names", ids, true);
      return builder.buildInlineTemplate();
    });
  }

  private static Set<String> getExistedNames(PsiField psiField) {
    Set<String> existedNames = new HashSet<>();
    PsiClass containingClass = psiField.getContainingClass();
    if (containingClass != null) {
      for (PsiField field : containingClass.getAllFields()) {
        if (field != psiField) {
          existedNames.add(field.getName());
        }
      }
    }
    return existedNames;
  }

  private static Expression getPsiFieldSuggestNamesExpression(final PsiField psiField, final Collection<String> suggestions, Set<String> existedNames) {
    return new Expression() {

      public Result calculateResult(ExpressionContext context) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        PsiIdentifier psiIdentifier = psiField.getNameIdentifier();
        return new TextResult(psiIdentifier.getText());
      }

      public LookupElement[] calculateLookupItems(ExpressionContext context) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        LinkedHashSet<LookupElement> items = new LinkedHashSet<>();
        for (String name : suggestions) {
          items.add(LookupElementBuilder.create(name));
        }
        return items.toArray(LookupElement.EMPTY_ARRAY);
      }

      public LookupFocusDegree getLookupFocusDegree() {
        return LookupFocusDegree.UNFOCUSED;
      }
    };
  }

  private static Collection<String> suggestPsiFieldNames(PsiType psiType, BeanPointer<?> bean, Set<String> existedNames) {
    Project project = bean.getContainingFile().getProject();
    PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(project);
    Set<String> names = new HashSet<>();
    String beanName = bean.getName();
    if (beanName != null) {
      for (String name : bean.getAliases()) {
        if (psiNameHelper.isIdentifier(name) && !existedNames.contains(name)) {
          names.add(name);
        }
      }
    }
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
    Set<String> suggestions = BeanCoreUtils.getSanitizedBeanNameSuggestions(codeStyleManager, psiType);
    suggestions.removeAll(existedNames);
    names.addAll(suggestions);
    return names;
  }

  public static Expression getSuperTypesExpression(String psiType, Collection<? extends PsiClass> psiClasses) {
    return new ConstantNode(psiType).withLookupItems(ContainerUtil.map(psiClasses, psiClass -> {
      return JavaClassNameCompletionContributor.createClassLookupItem(psiClass, true);
    }));
  }
}

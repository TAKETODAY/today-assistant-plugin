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

package cn.taketoday.assistant.model.highlighting.autowire;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.jam.JamService;
import com.intellij.lang.jvm.JvmModifiersOwner;
import com.intellij.lang.jvm.actions.AnnotationAttributeRequest;
import com.intellij.lang.jvm.actions.AnnotationAttributeValueRequestKt;
import com.intellij.lang.jvm.actions.AnnotationRequest;
import com.intellij.lang.jvm.actions.JvmElementActionFactories;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiConstantEvaluationHelper;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.JavaElementKind;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.uast.UastSmartPointer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.SortedList;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationUtils;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UIdentifier;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastLiteralUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.context.chooser.InfraContextDescriptor;
import cn.taketoday.assistant.context.chooser.InfraMultipleContextsManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ConditionalEvaluationContextImpl;
import cn.taketoday.assistant.model.DefaultInfraBeanQualifier;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraBeanFactoryUtils;
import cn.taketoday.assistant.model.utils.InfraConditionalUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.references.InfraBeanReference;
import cn.taketoday.assistant.references.InfraBeanResourceReference;
import cn.taketoday.assistant.references.InfraUastQualifierReference;
import cn.taketoday.lang.Nullable;

public final class InfraInjectionPointsAutowiringInspection extends AbstractInfraLocalInspection {

  public InfraInjectionPointsAutowiringInspection() {
    super(UField.class, UMethod.class);
  }

  public ProblemDescriptor[] checkMethod(UMethod uMethod, InspectionManager manager, boolean isOnTheFly) {
    CommonInfraModel model;
    PsiMethod psiMethod = uMethod.getJavaPsi();
    PsiClass psiClass = psiMethod.getContainingClass();
    PsiElement sourcePsi = uMethod.getSourcePsi();
    if (sourcePsi != null && AutowireUtil.isAutowiringRelevantClass(psiClass) && AutowireUtil.isInjectionPoint(psiMethod) && (model = AutowireUtil.getProcessingInfraModel(
            psiClass)) != null) {
      ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
      boolean required = AutowireUtil.isRequired(psiMethod);
      if (AutowireUtil.getResourceAnnotation(psiMethod) != null) {
        checkResourceMethod(uMethod, holder, model, required);
      }
      else if (psiMethod.getParameterList().getParametersCount() == 0 && AutowireUtil.isAutowiredByAnnotation(psiMethod)) {
        PsiElement nameIdentifier = UElementKt.getSourcePsiElement(uMethod.getUastAnchor());
        if (nameIdentifier != null) {
          String msg = InfraBundle.message("bean.autowiring.by.type.no.parameter.for.autowired.method", JavaElementKind.fromElement(psiMethod).subject());
          holder.registerProblem(nameIdentifier, msg);
        }
      }
      else if (!uMethod.getUastParameters().isEmpty()) {
        BeanPointer<?> pointer = null;
        PsiType psiType = psiMethod.getReturnType();
        if ((psiType instanceof PsiClassType) && AutowireUtil.hasConditional(psiMethod)) {
          Iterator<BeanPointer<?>> it = InfraModelSearchers.findBeans(model, ModelSearchParameters.byType(psiType)).iterator();
          while (true) {
            if (!it.hasNext()) {
              break;
            }
            BeanPointer<?> beanPointer = it.next();
            if (beanPointer.isValid() && psiMethod.equals(beanPointer.getPsiElement())) {
              pointer = beanPointer;
              break;
            }
          }
        }
        Set<BeanPointer<?>> pointers = new HashSet<>();
        pointers.add(pointer);
        Set<BeanPointer<?>> filtered = filterConditionalBeans(model, pointers);
        if (!filtered.isEmpty()) {
          checkAutowiredMethodInjections(uMethod, holder, model, required);
        }
      }
      return holder.getResultsArray();
    }
    return null;
  }

  public ProblemDescriptor[] checkField(UField uField, InspectionManager manager, boolean isOnTheFly) {
    PsiField psiField = UElementKt.getAsJavaPsiElement(uField, PsiField.class);
    if (psiField != null && AutowireUtil.isAutowiringRelevantClass(psiField.getContainingClass()) && AutowireUtil.isAutowiredByAnnotation(psiField)) {
      CommonInfraModel model = AutowireUtil.getProcessingInfraModel(psiField.getContainingClass());
      PsiElement sourcePsi = uField.getSourcePsi();
      if (model != null && sourcePsi != null) {
        ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
        checkInjectionPoint(uField, psiField.getType(), holder, model, AutowireUtil.isRequired(psiField));
        return holder.getResultsArray();
      }
      return null;
    }
    return null;
  }

  private static void checkAutowiredMethodInjections(UMethod psiMethod, ProblemsHolder holder, CommonInfraModel springModel, boolean requiredForMethod) {
    if (isBeanFactoryInitializationCandidate(psiMethod)) {
      return;
    }
    for (UParameter parameter : psiMethod.getUastParameters()) {
      PsiElement param = parameter.getSourcePsi();
      if (param instanceof PsiParameter psiParameter
              && !AutowireUtil.isValueAnnoInjection(psiParameter)) {
        checkInjectionPoint(parameter, parameter.getType(), holder, springModel, requiredForMethod
                && AutowireUtil.isRequired(psiParameter) && !isNullableParameter(psiParameter));
      }
    }
  }

  private static boolean isBeanFactoryInitializationCandidate(UMethod method) {
    for (UParameter parameter : method.getUastParameters()) {
      if (InheritanceUtil.isInheritor(parameter.getType(), "java.lang.Class")) {
        return true;
      }
    }
    PsiMethod psiMethod = method.getJavaPsi();
    Module module = ModuleUtilCore.findModuleForPsiElement(psiMethod);
    if (module == null) {
      return false;
    }
    return isContextBeanInitializedByBeanFactory(psiMethod, module) || isStereotypeBeanWithDefaultConstructorInitializedByBeanFactory(psiMethod, module);
  }

  private static boolean isContextBeanInitializedByBeanFactory(PsiMethod psiMethod, Module module) {
    return couldBeInitializedByBeanFactory(JamService.getJamService(psiMethod.getProject()).getJamElement(ContextJavaBean.BEAN_JAM_KEY, psiMethod), module,
            InfraBeanFactoryUtils.getParamTypes(psiMethod));
  }

  private static boolean couldBeInitializedByBeanFactory(@Nullable CommonInfraBean commonInfraBean, Module module, PsiType... psiTypes) {
    if (commonInfraBean == null) {
      return false;
    }
    PsiType beanType = commonInfraBean.getBeanType();
    String beanName = commonInfraBean.getBeanName();
    return beanType != null && beanName != null && InfraBeanFactoryUtils.couldBeInitializedByBeanFactory(module, beanType, beanName, psiTypes);
  }

  private static boolean isStereotypeBeanWithDefaultConstructorInitializedByBeanFactory(PsiMethod psiMethod, Module module) {
    PsiClass containingClass;
    return psiMethod.hasParameters() && psiMethod.isConstructor() && (containingClass = psiMethod.getContainingClass()) != null && containingClass.getConstructors().length == 1 && couldBeInitializedByBeanFactory(
            JamService.getJamService(containingClass.getProject()).getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, containingClass), module,
            InfraBeanFactoryUtils.getParamTypes(psiMethod));
  }

  private static boolean isNullableParameter(PsiParameter psi) {
    return AnnotationUtil.isAnnotated(psi, AnnotationConstant.NULLABLE, 0);
  }

  private static void checkResourceMethod(UMethod psiMethod, ProblemsHolder holder, CommonInfraModel springModel, boolean required) {
    PsiType type = PropertyUtilBase.getPropertyType(psiMethod.getJavaPsi());
    if (type != null) {
      checkInjectionPoint(psiMethod, type, holder, springModel, required);
    }
  }

  public static void checkInjectionPoint(UDeclaration uDeclaration, PsiType psiType, ProblemsHolder holder, CommonInfraModel springModel, boolean required) {
    PsiModifierListOwner psiModifierListOwner;
    if (psiType.isValid() && (psiModifierListOwner = UElementKt.getAsJavaPsiElement(uDeclaration, PsiModifierListOwner.class)) != null) {
      PsiAnnotation resourceAnnotation = AutowireUtil.getResourceAnnotation(psiModifierListOwner);
      if (resourceAnnotation != null && (psiModifierListOwner instanceof PsiMember)) {
        checkResourceInjectionPoint(psiType, holder, springModel, resourceAnnotation);
        return;
      }
      UAnnotation annotation = getEffectiveQualifiedUAnnotation(uDeclaration);
      if (annotation != null) {
        checkQualifiedAutowiring(psiType, annotation, holder, springModel, required);
      }
      else {
        checkByTypeAutowire(uDeclaration, psiType, holder, springModel, required);
      }
    }
  }

  @Nullable
  private static UAnnotation getEffectiveQualifiedUAnnotation(UDeclaration uDeclaration) {
    PsiAnnotation annotation;
    PsiModifierListOwner psiModifierListOwner = UElementKt.getAsJavaPsiElement(uDeclaration, PsiModifierListOwner.class);
    if (psiModifierListOwner == null || (annotation = AutowireUtil.getEffectiveQualifiedAnnotation(psiModifierListOwner)) == null) {
      return null;
    }
    UAnnotation qualifiedAnnotation = null;
    String qualifiedName = annotation.getQualifiedName();
    if (qualifiedName != null) {
      qualifiedAnnotation = uDeclaration.findAnnotation(qualifiedName);
    }
    if (qualifiedAnnotation == null) {
      qualifiedAnnotation = UastContextKt.toUElement(annotation, UAnnotation.class);
    }
    if (qualifiedAnnotation == null) {
      PsiElement sourcePsi = uDeclaration.getSourcePsi();
      Logger.getInstance(InfraInjectionPointsAutowiringInspection.class)
              .error("Psi annotation '" + annotation + "' from '" + (sourcePsi != null ? sourcePsi.getText() : null) + "' was not converted to UAST");
    }
    return qualifiedAnnotation;
  }

  private static void checkResourceInjectionPoint(PsiType psiType, ProblemsHolder holder, CommonInfraModel springModel, PsiAnnotation resourceAnnotation) {
    PsiAnnotationMemberValue attributeValue = resourceAnnotation.findDeclaredAttributeValue("name");
    if (attributeValue != null) {
      checkByNameAutowiring(attributeValue, holder, springModel, psiType);
    }
  }

  private static void checkByNameAutowiring(PsiAnnotationMemberValue annotationMemberValue, ProblemsHolder holder, CommonInfraModel model, PsiType memberEffectiveType) {
    for (PsiReference springBeanReference : annotationMemberValue.getReferences()) {
      if (springBeanReference instanceof InfraBeanReference sbReference) {
        checkResourceBeanReference(holder, model, memberEffectiveType, springBeanReference, sbReference.getValue(), sbReference.isFactoryBeanRef());
        return;
      }
      if (springBeanReference instanceof PsiReferenceExpression) {
        PsiElement resolve = springBeanReference.resolve();
        if (resolve instanceof PsiField psiField) {
          PsiConstantEvaluationHelper helper = JavaPsiFacade.getInstance(resolve.getProject()).getConstantEvaluationHelper();
          Object o = helper.computeConstantExpression(psiField.getInitializer());
          if (o instanceof String) {
            checkResourceBeanReference(holder, model, memberEffectiveType, springBeanReference, (String) o, false);
          }
        }
      }
    }
  }

  private static void checkResourceBeanReference(ProblemsHolder holder, CommonInfraModel model, PsiType memberEffectiveType, PsiReference ref, @Nullable String beanName, boolean isFactoryBeanRef) {
    if (beanName != null) {
      BeanPointer<?> bean = InfraModelSearchers.findBean(model, beanName);
      if (bean != null) {
        PsiType[] beanTypes = getEffectiveBeanTypes(isFactoryBeanRef, bean);
        for (PsiType psiType : beanTypes) {
          if (AutowireUtil.canBeAutowiredByType(memberEffectiveType, psiType)) {
            return;
          }
        }
        holder.registerProblem(ref, InfraBundle.message("cannot.autowire.bean.of.type", memberEffectiveType.getCanonicalText()), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return;
      }
      else if (InfraBeanResourceReference.resolveResourceByFqn(beanName, model, holder.getProject()) != null) {
        return;
      }
    }
    if (ref instanceof InfraBeanReference) {
      holder.registerProblem(ref);
    }
    else if (beanName != null) {
      holder.registerProblem(ref.getElement(), InfraBundle.message("model.bean.error.message", beanName));
    }
  }

  private static PsiType[] getEffectiveBeanTypes(boolean factoryBeanRef, BeanPointer<?> bean) {
    PsiClass beanClass;
    return (!factoryBeanRef || (beanClass = bean.getBeanClass()) == null) ? bean.getEffectiveBeanTypes() : new PsiType[] { PsiTypesUtil.getClassType(beanClass) };
  }

  private static void checkQualifiedAutowiring(PsiType searchType, UAnnotation qualifiedAnnotation, @Nullable ProblemsHolder holder, CommonInfraModel model, boolean required) {
    PsiLanguageInjectionHost host;
    UExpression findDeclaredAttributeValue = qualifiedAnnotation.findDeclaredAttributeValue("value");
    PsiAnnotation psiAnnotation = UElementKt.getAsJavaPsiElement(qualifiedAnnotation, PsiAnnotation.class);
    if (psiAnnotation == null) {
      return;
    }
    String name = AutowireUtil.getQualifiedBeanName(psiAnnotation);
    PsiReference psiReference = null;
    if (required && (findDeclaredAttributeValue instanceof ULiteralExpression uLiteralExpression)
            && (host = UastLiteralUtils.getPsiLanguageInjectionHost(uLiteralExpression)) != null) {
      PsiReference[] references = host.getReferences();
      for (PsiReference reference : references) {
        if (reference instanceof InfraUastQualifierReference uastQualifierReference) {
          psiReference = reference;
          if (uastQualifierReference.multiResolve(false).length == 0) {
            if (holder != null && psiReference.getElement().isPhysical()) {
              holder.registerProblem(psiReference, InfraBundle.message("bean.class.unknown.qualifier.bean", name), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
              return;
            }
            return;
          }
        }
      }
    }
    Set<BeanPointer<?>> beanPointers = AutowireUtil.getQualifiedBeanPointers(psiAnnotation, model);
    PsiElement attributeValueSourcePsi = UElementKt.getSourcePsiElement(findDeclaredAttributeValue);
    PsiElement annotationSourcePsi = UElementKt.getSourcePsiElement(qualifiedAnnotation);
    if (beanPointers.size() == 0 && required) {
      if (holder != null) {
        if (attributeValueSourcePsi != null) {
          reportProblem(holder, psiReference, attributeValueSourcePsi, InfraBundle.message("bean.class.unknown.qualifier.bean", name));
          return;
        }
        String qualifiedName = qualifiedAnnotation.getQualifiedName();
        if (annotationSourcePsi != null) {
          assert qualifiedName != null;
          reportProblem(holder, psiReference, annotationSourcePsi,
                  InfraBundle.message("cannot.find.bean.qualified.by", "@" + StringUtil.getShortName(qualifiedName)));
          return;
        }
        return;
      }
      return;
    }
    Set<BeanPointer<?>> autowiredPointers = AutowireUtil.filterPointersByAutowiredType(searchType, beanPointers);
    if (beanPointers.size() > 0 && autowiredPointers.isEmpty() && AutowireUtil.getIterableBeanPointers(searchType, model).size() == 0 && holder != null) {
      String message = InfraBundle.message("bean.class.autowired.incorrect.qualifier.type", searchType.getPresentableText());
      PsiElement elementToReport = attributeValueSourcePsi == null ? annotationSourcePsi : attributeValueSourcePsi;
      if (elementToReport != null) {
        reportProblem(holder, psiReference, elementToReport, message);
      }
    }
  }

  private static void reportProblem(ProblemsHolder holder, @Nullable PsiReference qreference, PsiElement attributeValue, @InspectionMessage String text) {
    if (qreference == null) {
      holder.registerProblem(attributeValue, text);
    }
    else {
      holder.registerProblem(qreference, text, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }

  private static void checkByTypeAutowire(UDeclaration psiNameIdentifierOwner, PsiType searchType, @Nullable ProblemsHolder holder, CommonInfraModel model, boolean required) {
    PsiElement psiElement;
    UElement uIdentifier = psiNameIdentifierOwner.getUastAnchor();
    if (uIdentifier == null || (psiElement = UElementKt.getSourcePsiElement(uIdentifier)) == null || psiElement.getTextRange().isEmpty()) {
      return;
    }
    PsiElement psiElement1;
    String primaryCandidateName = ((UIdentifier) uIdentifier).getName();
    Set<BeanPointer<?>> beanPointers = AutowireUtil.autowireByType(model, searchType, primaryCandidateName);
    Set<BeanPointer<?>> iterableBeanPointers = AutowireUtil.getIterableBeanPointers(searchType, model);
    if (beanPointers.isEmpty() && iterableBeanPointers.isEmpty() && required) {
      if (holder != null && !AutowireUtil.isAutowiredByDefault(searchType) && !isObjectFactory(searchType) && !isInjectionPoint(searchType) && !AutowireUtil.isJavaUtilOptional(
              searchType) && !InheritanceUtil.isInheritor(searchType, "java.util.Collection")) {
        holder.registerProblem(psiElement, getBeansNotFoundMessage(searchType));
      }
    }
    else if (!iterableBeanPointers.isEmpty() || beanPointers.size() <= 1 || holder == null || (psiElement1 = psiNameIdentifierOwner.getJavaPsi()) == null) {
    }
    else {
      Set<BeanPointer<?>> filtered = filterSelfReferencedBeans(psiElement1, filterOverridenBeans(beanPointers));
      if (filtered.size() > 1) {
        filtered = filterByUncheckedConversion(searchType, filtered);
      }
      if (filtered.size() > 1) {
        filtered = filterConditionalBeans(model, filtered);
      }
      if (filtered.size() > 1) {
        filtered = filterBeansFromDifferentContexts(filtered);
      }
      if (filtered.size() > 1) {
        if (isNonDefinedActiveProfile(model.getActiveProfiles()) && isAllBeansInDifferentProfiles(filtered)) {
          return;
        }
        PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) psiElement1;

        String annotation = getQualifierAnnotation(modifierListOwner);
        CreateQualifierRequest annotationRequest = new CreateQualifierRequest(
                modifierListOwner, annotation, getBeanPointerName(beanPointers.iterator().next()));
        List<IntentionAction> actions = JvmElementActionFactories.createAddAnnotationActions(
                (JvmModifiersOwner) modifierListOwner, annotationRequest);
        if (!actions.isEmpty()) {
          holder.registerProblem(psiElement, getErrorMessage(searchType, filtered),
                  new AddBeanQualifierFix(psiNameIdentifierOwner, beanPointers, annotation, actions.get(0)));
        }
        else {
          holder.registerProblem(psiElement, getErrorMessage(searchType, filtered));
        }
      }
    }
  }

  @InspectionMessage
  private static String getBeansNotFoundMessage(PsiType searchType) {
    PsiType secondarySearchType = AutowireUtil.getIterableSearchType(searchType);
    if (secondarySearchType != null) {
      return InfraBundle.message("bean.autowiring.by.type.no.beans", secondarySearchType.getPresentableText(), searchType.getPresentableText());
    }
    return InfraBundle.message("bean.autowiring.by.type.none", searchType.getPresentableText());
  }

  private static boolean isObjectFactory(PsiType type) {
    PsiClass psiClass;
    return (type instanceof PsiClassType) && (psiClass = ((PsiClassType) type).resolve()) != null && InheritanceUtil.isInheritor(psiClass, InfraConstant.OBJECT_FACTORY_CLASS);
  }

  private static boolean isInjectionPoint(PsiType type) {
    PsiClass psiClass;
    return (type instanceof PsiClassType) && (psiClass = ((PsiClassType) type).resolve()) != null && InfraConstant.INJECTION_POINT.equals(psiClass.getQualifiedName());
  }

  @InspectionMessage
  private static String getErrorMessage(PsiType beanType, Set<BeanPointer<?>> autowiredPointers) {
    SortedList<Pair<String, String>> sortedList = new SortedList(Pair.comparingByFirst());
    for (BeanPointer<?> pointer : autowiredPointers) {
      sortedList.add(Pair.create(InfraPresentationProvider.getBeanName(pointer), InfraPresentationProvider.getInfraBeanLocation(pointer)));
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<html><table>");
    sb.append("<tr><td>");
    sb.append(InfraBundle.message("bean.class.autowired.by.type", beanType.getPresentableText()));
    sb.append("</td></tr>");
    sb.append("<tr><td>");
    sb.append("<table>");
    sb.append("<tr><td valign='top'>Beans:</td>");
    sb.append("<td>");
    for (Pair<String, String> pair : sortedList) {
      sb.append(pair.first);
      sb.append("&nbsp;&nbsp; (");
      sb.append(pair.second);
      sb.append(")<br>");
    }
    sb.append("</td></tr>");
    sb.append("</table>");
    sb.append("</td></tr>");
    sb.append("</table></html>");
    String sb2 = sb.toString();
    return sb2;
  }

  private static Set<BeanPointer<?>> filterSelfReferencedBeans(PsiElement psiNameIdentifierOwner, Set<BeanPointer<?>> pointers) {
    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiNameIdentifierOwner, PsiMethod.class);
    if (psiMethod == null) {
      return pointers;
    }
    Set<BeanPointer<?>> filtered = new LinkedHashSet<>();
    for (BeanPointer<?> pointer : pointers) {
      if (!psiMethod.equals(pointer.getBean().getIdentifyingPsiElement())) {
        filtered.add(pointer);
      }
    }
    return filtered;
  }

  private static Set<BeanPointer<?>> filterByUncheckedConversion(PsiType searchType, Set<BeanPointer<?>> pointers) {
    Set<BeanPointer<?>> filtered = new LinkedHashSet<>();
    for (BeanPointer<?> pointer : pointers) {
      PsiType[] effectiveBeanTypes = pointer.getEffectiveBeanTypes();
      int length = effectiveBeanTypes.length;
      int i = 0;
      while (true) {
        if (i < length) {
          PsiType psiType = effectiveBeanTypes[i];
          if (!TypeConversionUtil.isAssignable(searchType, psiType, false)) {
            i++;
          }
          else {
            filtered.add(pointer);
            break;
          }
        }
      }
    }
    return filtered.isEmpty() ? pointers : filtered;
  }

  private static Set<BeanPointer<?>> filterConditionalBeans(CommonInfraModel model, Set<BeanPointer<?>> pointers) {
    ConditionalEvaluationContextImpl context = new ConditionalEvaluationContextImpl(model);
    Set<BeanPointer<?>> filtered = new LinkedHashSet<>();
    for (BeanPointer<?> pointer : pointers) {
      if (pointer == null || InfraConditionalUtils.isActive(pointer, context)) {
        filtered.add(pointer);
      }
    }
    return filtered;
  }

  private static Set<BeanPointer<?>> filterBeansFromDifferentContexts(Set<BeanPointer<?>> pointers) {
    Map<InfraContextDescriptor, Set<BeanPointer<?>>> byDescriptor = FactoryMap.create(descriptor -> new HashSet<>());
    InfraMultipleContextsManager contextsManager = InfraMultipleContextsManager.of();
    for (BeanPointer<?> pointer : pointers) {
      List<InfraContextDescriptor> pointerDescriptors = contextsManager.getAllContextDescriptors(pointer.getContainingFile());
      for (InfraContextDescriptor descriptor2 : pointerDescriptors) {
        byDescriptor.get(descriptor2).add(pointer);
      }
    }
    Collection<Set<BeanPointer<?>>> arranged = byDescriptor.values();
    for (Set<BeanPointer<?>> descriptorPointers : arranged) {
      if (descriptorPointers.size() > 1) {
        return descriptorPointers;
      }
    }
    Set<BeanPointer<?>> filtered = ContainerUtil.getFirstItem(arranged);
    return filtered == null ? pointers : filtered;
  }

  private static boolean isAllBeansInDifferentProfiles(Set<BeanPointer<?>> pointers) {
    Set<String> names = new HashSet<>();
    for (BeanPointer<?> pointer : pointers) {
      for (String profileName : pointer.getBean().getProfile().getExpressions()) {
        if (names.contains(profileName)) {
          return false;
        }
        names.add(profileName);
      }
    }
    return true;
  }

  private static boolean isNonDefinedActiveProfile(@Nullable Set<String> activeProfiles) {
    if (activeProfiles == null || activeProfiles.isEmpty()) {
      return true;
    }
    boolean isDefaultProfile = activeProfiles.size() <= InfraProfile.DEFAULT_PROFILE_NAMES.size() && InfraProfile.DEFAULT_PROFILE_NAMES.containsAll(activeProfiles);
    return !isDefaultProfile;
  }

  private static Set<BeanPointer<?>> filterOverridenBeans(Set<BeanPointer<?>> pointers) {
    MultiMap<String, VirtualFile> names = new MultiMap<>();
    Set<BeanPointer<?>> map2SetNotNull = ContainerUtil.map2SetNotNull(pointers, pointer -> {
      boolean isOverriden = false;
      String name = pointer.getName();
      if (StringUtil.isNotEmpty(name)) {
        VirtualFile containingFile = pointer.getContainingFile().getVirtualFile();
        if (names.containsKey(name) && !names.values().contains(containingFile)) {
          isOverriden = true;
        }
        names.putValue(name, containingFile);
      }
      if (isOverriden) {
        return null;
      }
      return pointer;
    });
    return map2SetNotNull;
  }

  private static String getQualifierAnnotation(@Nullable PsiModifierListOwner psiModifierListOwner) {
    PsiMember psiMember;
    if (psiModifierListOwner != null && (psiMember = PsiTreeUtil.getParentOfType(psiModifierListOwner, PsiMember.class, false)) != null) {
      if (AnnotationUtil.isAnnotated(psiMember, JavaeeConstant.JAVAX_INJECT, 1)) {
        return JavaeeConstant.JAVAX_NAMED;
      }
      if (AnnotationUtil.isAnnotated(psiMember, JavaeeConstant.JAKARTA_INJECT, 1)) {
        return JavaeeConstant.JAKARTA_NAMED;
      }
      return AnnotationConstant.QUALIFIER;
    }
    return AnnotationConstant.QUALIFIER;
  }

  public static class AddBeanQualifierFix implements LocalQuickFix {
    private final UastSmartPointer<UDeclaration> myModifierListOwnerPointer;
    private final Collection<BeanPointer<?>> myBeanPointers;
    private final String myQualifierAnno;
    private final IntentionAction myCreateAnnotationAction;

    AddBeanQualifierFix(UDeclaration uDeclaration, Collection<BeanPointer<?>> beanPointers, String qualifierAnno, IntentionAction createAnnotationAction) {
      this.myModifierListOwnerPointer = new UastSmartPointer<>(uDeclaration, UDeclaration.class);
      this.myBeanPointers = beanPointers;
      this.myQualifierAnno = qualifierAnno;
      this.myCreateAnnotationAction = createAnnotationAction;
    }

    public String getFamilyName() {
      return InfraBundle.message("AutowiringInspection.add.qualifier.fix");
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      PsiElement sourcePsi;
      UAnnotation annotation;
      UDeclaration uDeclaration = this.myModifierListOwnerPointer.getElement();
      if (uDeclaration != null && (sourcePsi = uDeclaration.getSourcePsi()) != null && this.myBeanPointers.size() > 0) {
        Editor editor = getEditor(sourcePsi);
        this.myCreateAnnotationAction.invoke(project, editor, sourcePsi.getContainingFile());
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        UDeclaration modifiedDeclaration = this.myModifierListOwnerPointer.getElement();
        if (modifiedDeclaration != null && (annotation = modifiedDeclaration.findAnnotation(this.myQualifierAnno)) != null) {
          UExpression value = annotation.findDeclaredAttributeValue("value");
          if (UastLiteralUtils.isInjectionHost(value)) {
            TemplateManager manager = TemplateManager.getInstance(project);
            Template template = createQualifierNameTemplate(value);
            if (template != null) {
              manager.startTemplate(editor, template);
            }
          }
        }
      }
    }

    @Nullable
    private Template createQualifierNameTemplate(UExpression psiLiteral) {
      PsiElement sourcePsi = psiLiteral.getSourcePsi();
      if (sourcePsi == null) {
        return null;
      }
      TemplateBuilderImpl builder = new TemplateBuilderImpl(sourcePsi.getContainingFile());
      PsiLanguageInjectionHost host = UastLiteralUtils.getSourceInjectionHost(psiLiteral);
      if (host == null) {
        return null;
      }
      TextRange textRange = host.getTextRange();
      String valueText = ElementManipulators.getValueText(host);
      builder.replaceRange(TextRange.from(textRange.getStartOffset() + 1, valueText.length()),
              getQualifierNamesSuggestNamesExpression(psiLiteral, host, this.myBeanPointers));
      return builder.buildInlineTemplate();
    }

    public static Editor getEditor(PsiElement modifierListOwner) {
      PsiFile psiFile = modifierListOwner.getContainingFile();
      Project project = psiFile.getProject();
      VirtualFile virtualFile = psiFile.getVirtualFile();
      return FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, 0), false);
    }
  }

  private static Expression getQualifierNamesSuggestNamesExpression(UExpression psiLiteral, PsiLanguageInjectionHost host, Collection<BeanPointer<?>> beanPointers) {
    return new Expression() {
      public Result calculateResult(ExpressionContext context) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        return new TextResult(ElementManipulators.getValueText(host));
      }

      public LookupElement[] calculateLookupItems(ExpressionContext context) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        LinkedHashSet<LookupElement> items = new LinkedHashSet<>();
        PsiClass psiAnnoClass = findAnnoPsiClass(psiLiteral);
        for (BeanPointer<?> pointer : beanPointers) {
          CommonInfraBean bean = pointer.getBean();
          if (psiAnnoClass != null) {
            for (InfraQualifier qualifier : bean.getInfraQualifiers()) {
              String value = qualifier.getQualifierValue();
              if (value != null && ((qualifier instanceof DefaultInfraBeanQualifier) || Comparing.equal(qualifier.getQualifierType(), psiAnnoClass))) {
                items.add(InfraConverterUtil.createCompletionVariant(pointer, value));
              }
            }
          }
        }
        LookupElement[] elements = items.toArray(LookupElement.EMPTY_ARRAY);
        Arrays.sort(elements, Comparator.comparing(LookupElement::getLookupString));
        return elements;
      }
    };
  }

  @Nullable
  public static PsiClass findAnnoPsiClass(UExpression expression) {
    kotlin.Pair<UAnnotation, String> annotationEntry = UAnnotationUtils.getContainingUAnnotationEntry(expression);
    if (annotationEntry == null) {
      return null;
    }
    return annotationEntry.component1().resolve();
  }

  private static String getBeanPointerName(BeanPointer<?> beanPointer) {
    String name = beanPointer.getName();
    return StringUtil.isEmptyOrSpaces(name) ? "Unknown" : name;
  }

  public static class CreateQualifierRequest implements AnnotationRequest {
    private final String myQualifierAnno;
    private final SmartPsiElementPointer<PsiModifierListOwner> myModifierListOwnerPointer;
    private final List<AnnotationAttributeRequest> myAttributeRequests;

    CreateQualifierRequest(PsiModifierListOwner psiElement, String qualifierAnno, String beanPointerName) {
      this.myQualifierAnno = qualifierAnno;
      this.myModifierListOwnerPointer = SmartPointerManager.getInstance(psiElement.getProject()).createSmartPsiElementPointer(psiElement);
      this.myAttributeRequests = Collections.singletonList(AnnotationAttributeValueRequestKt.stringAttribute("value", beanPointerName));
    }

    public String getQualifiedName() {
      return this.myQualifierAnno;
    }

    public List<AnnotationAttributeRequest> getAttributes() {
      return this.myAttributeRequests;
    }

    public boolean isValid() {
      PsiModifierListOwner modifierListOwner = this.myModifierListOwnerPointer.getElement();
      return modifierListOwner != null && modifierListOwner.isValid() && modifierListOwner.getModifierList() != null;
    }
  }
}

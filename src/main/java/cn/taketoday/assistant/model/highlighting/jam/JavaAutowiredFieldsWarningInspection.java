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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class JavaAutowiredFieldsWarningInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    PsiFile file = aClass.getContainingFile();
    if (JamCommonUtil.isPlainJavaFile(file) && !ProjectRootsUtil.isInTestSource(file)) {
      Module module = ModuleUtilCore.findModuleForPsiElement(aClass);
      if (!InfraUtils.hasFacet(module) && !InfraModelService.of().hasAutoConfiguredModels(module)) {
        return ProblemDescriptor.EMPTY_ARRAY;
      }
      if (!InfraUtils.isStereotypeComponentOrMeta(aClass)) {
        return ProblemDescriptor.EMPTY_ARRAY;
      }
      for (JamBeanPointer pointer : JavaClassInfo.from(aClass).getStereotypeMappedBeans()) {
        if (pointer.getBean() instanceof ContextJavaBean) {
          return ProblemDescriptor.EMPTY_ARRAY;
        }
      }
      ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
      Set<String> annotations = AutowireUtil.getAutowiredAnnotations(module);
      Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> autowiredFields = getAutowiredFields(aClass, annotations);
      LocalQuickFix allFieldsFix = null;
      if (autowiredFields.size() > 1) {
        allFieldsFix = getFieldsAutowiredFix(aClass, autowiredFields, annotations);
      }
      for (Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>> pair : autowiredFields) {
        LocalQuickFix fieldFx = getFieldsAutowiredFix(aClass, Collections.singleton(pair), annotations);
        LocalQuickFix[] fixes = allFieldsFix != null ? new LocalQuickFix[] { fieldFx, allFieldsFix } : new LocalQuickFix[] { fieldFx };
        PsiAnnotation psiAnnotation = (PsiAnnotation) ((SmartPsiElementPointer) pair.getSecond()).getElement();
        if (psiAnnotation != null) {
          holder.registerProblem(psiAnnotation, message("field.injection.is.not.recommended"), fixes);
        }
      }
      return holder.getResultsArray();
    }
    return ProblemDescriptor.EMPTY_ARRAY;
  }

  private static Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> getAutowiredFields(PsiClass aClass, Set<String> annotations) {
    PsiAnnotation annotation;
    Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> pairs = new HashSet<>();
    SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(aClass.getProject());
    for (PsiField psiField : aClass.getFields()) {
      for (String autowiredAnno : annotations) {
        if (!autowiredAnno.equals(JavaeeConstant.JAVAX_RESOURCE) && !autowiredAnno.equals(JavaeeConstant.JAKARTA_RESOURCE) && (annotation = AnnotationUtil.findAnnotation(
                psiField, autowiredAnno)) != null) {
          smartPointerManager.createSmartPsiElementPointer(psiField);
          pairs.add(Pair.create(
                  smartPointerManager.createSmartPsiElementPointer(psiField),
                  smartPointerManager.createSmartPsiElementPointer(annotation)
          ));
        }
      }
    }
    return pairs;
  }

  private static LocalQuickFix getFieldsAutowiredFix(PsiClass aClass, Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> fields, Set<String> annotations) {
    PsiMethod constructor = findAutowiredConstructor(aClass, annotations);
    if (constructor != null) {
      return new AddParameterQuickFix(aClass, constructor, fields);
    }
    return new CreateAutowiredConstructorQuickFix(aClass, fields);
  }

  @Nullable
  private static PsiMethod findAutowiredConstructor(PsiClass aClass, Set<String> annotations) {
    PsiMethod[] constructors = aClass.getConstructors();
    if (constructors.length == 1) {
      if (!(constructors[0] instanceof LightElement)) {
        return constructors[0];
      }
      return null;
    }
    for (PsiMethod method : constructors) {
      if (!(method instanceof LightElement) && AnnotationUtil.isAnnotated(method, annotations, 0)) {
        return method;
      }
    }
    return null;
  }

  public static class CreateAutowiredConstructorQuickFix implements LocalQuickFix {
    private final SmartPsiElementPointer<PsiClass> myClass;
    private final Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> myParameterCandidates;
    private final String message;

    CreateAutowiredConstructorQuickFix(PsiClass containingClass, Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> pairs) {
      this.myClass = SmartPointerManager.getInstance(containingClass.getProject()).createSmartPsiElementPointer(containingClass);
      this.myParameterCandidates = pairs;
      this.message = getConstructorName(containingClass.getName(), this.myParameterCandidates);
    }

    public String getName() {
      return message("field.injection.create.constructor.injection", this.message);
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      PsiClass containingClass = this.myClass.getElement();
      if (containingClass == null) {
        return;
      }
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
      PsiMethod constructor = elementFactory.createConstructor();
      constructor.setName(containingClass.getName());

      PsiMethod addedConstructor = (PsiMethod) containingClass.add(constructor);

      PsiUtil.setModifierProperty(addedConstructor, "public", true);
      addParameters(this.myParameterCandidates, addedConstructor);
      addAutowiredAnnotationIfNeeded(containingClass, addedConstructor,
              getAutowiredAnnotation(this.myParameterCandidates.iterator().next().second));
    }

    public String getFamilyName() {
      return message("field.injection.create.constructor.family.warning");
    }
  }

  public static class AddParameterQuickFix implements LocalQuickFix {
    private final SmartPsiElementPointer<PsiMethod> myConstructor;
    private final Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> myParameterCandidates;

    AddParameterQuickFix(PsiClass containingClass, PsiMethod constructor, Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> pairs) {
      this.myConstructor = SmartPointerManager.getInstance(constructor.getProject()).createSmartPsiElementPointer(constructor);
      this.myParameterCandidates = pairs;
    }

    public String getName() {
      return message("field.injection.add.parameters",
              getConstructorName(this.myConstructor.getElement(), this.myParameterCandidates));
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      PsiMethod constructor = this.myConstructor.getElement();
      if (constructor != null && !this.myParameterCandidates.isEmpty()) {
        addParameters(this.myParameterCandidates, constructor);
      }
    }

    public String getFamilyName() {
      return message("field.injection.add.parameter.family.warning");
    }
  }

  private static void addAutowiredAnnotationIfNeeded(@Nullable PsiClass psiClass, PsiMethod constructor, String fqnAnno) {
    if (psiClass == null || psiClass.getConstructors().length == 1) {
      return;
    }
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(constructor.getProject()).getElementFactory();
    PsiAnnotation psiAnnotation = elementFactory.createAnnotationFromText("@" + fqnAnno, null);
    PsiModifierList modifierList = constructor.getModifierList();
    PsiElement element = modifierList.getFirstChild();
    if (element != null) {
      modifierList.addBefore(psiAnnotation, element);
    }
    else {
      modifierList.add(psiAnnotation);
    }
  }

  private static String getAutowiredAnnotation(SmartPsiElementPointer<PsiAnnotation> anno) {
    PsiAnnotation annotation = anno.getElement();
    if (annotation != null && annotation.isValid()) {
      String qualifiedName = annotation.getQualifiedName();
      if (!StringUtil.isNotEmpty(qualifiedName)) {
        return AnnotationConstant.AUTOWIRED;
      }
      return qualifiedName;
    }
    return AnnotationConstant.AUTOWIRED;
  }

  private static void addParameters(Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> myParameterCandidates, PsiMethod constructor) {
    PsiClass containingClass;
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(constructor.getProject()).getElementFactory();
    Map<Module, List<String>> qualifierAnnotationClassNames = FactoryMap.create(module -> {
      List<PsiClass> qualifierAnnotations2 = JamAnnotationTypeUtil.getQualifierAnnotationTypesWithChildren(module);
      return ContainerUtil.mapNotNull(qualifierAnnotations2, PsiClass::getQualifiedName);
    });
    for (Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>> pair : myParameterCandidates) {
      PsiField field = pair.first.getElement();
      PsiAnnotation annotation = pair.second.getElement();
      if (field != null && annotation != null && field.isValid() && annotation.isValid()) {
        String name = field.getName();
        if (StringUtil.isEmptyOrSpaces(name)) {
          return;
        }
        PsiParameter parameter = elementFactory.createParameter(name, field.getType());
        PsiModifierList modifierList = parameter.getModifierList();
        if (modifierList != null) {
          for (PsiAnnotation qualifierAnno : getQualifierAnnotations(field, qualifierAnnotationClassNames)) {
            modifierList.add(elementFactory.createAnnotationFromText(qualifierAnno.getText(), modifierList));
            qualifierAnno.delete();
          }
        }
        constructor.getParameterList().add(parameter);
        PsiCodeBlock body = constructor.getBody();
        if (body != null) {
          body.add(elementFactory.createStatementFromText("this." + name + " = " + name + ";", constructor));
        }
        PsiModifierList fieldModifierList = field.getModifierList();
        if (fieldModifierList != null && !fieldModifierList.hasModifierProperty(
                "final") && (containingClass = constructor.getContainingClass()) != null && containingClass.getConstructors().length == 1) {
          fieldModifierList.setModifierProperty("final", true);
        }
        annotation.delete();
      }
    }
  }

  private static PsiAnnotation[] getQualifierAnnotations(PsiField first, Map<Module, List<String>> qualifierAnnotationClassNames) {
    Module module = ModuleUtilCore.findModuleForPsiElement(first);
    if (module == null) {
      return PsiAnnotation.EMPTY_ARRAY;
    }
    List<String> annotations = qualifierAnnotationClassNames.get(module);
    List<PsiAnnotation> vs = ContainerUtil.mapNotNull(annotations, s -> AnnotationUtil.findAnnotation(first, s));
    return vs.toArray(PsiAnnotation.EMPTY_ARRAY);
  }

  private static String getConstructorName(@Nullable PsiMethod psiMethod, Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> parameterCandidates) {
    StringBuilder sb = new StringBuilder();
    if (psiMethod != null) {
      sb.append(psiMethod.getName());
      sb.append("(");
      sb.append(getConstructorParameters(psiMethod.getParameterList().getParameters()));
      sb.append(addNewParameters(parameterCandidates));
      sb.append(")");
    }
    return sb.toString();
  }

  private static String getConstructorParameters(PsiParameter[] parameters) {
    StringBuilder sb = new StringBuilder();
    int length = parameters.length;
    int i = 0;
    while (true) {
      if (i >= length) {
        break;
      }
      PsiParameter parameter = parameters[i];
      String presentableText = parameter.getType().getPresentableText();
      if (sb.length() + presentableText.length() < 53) {
        sb.append(presentableText);
        sb.append(",");
        i++;
      }
      else {
        sb.append("...,");
        break;
      }
    }
    return sb.toString();
  }

  private static String getConstructorName(@Nullable String name, Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> parameterCandidates) {
    return name + "(" + addNewParameters(parameterCandidates) + ")";
  }

  private static String addNewParameters(Set<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> parameterCandidates) {
    StringBuilder sb = new StringBuilder();
    byte current = 0;
    Iterator<Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>>> it = parameterCandidates.iterator();
    while (true) {
      if (!it.hasNext()) {
        break;
      }
      Pair<SmartPsiElementPointer<PsiField>, SmartPsiElementPointer<PsiAnnotation>> pair = it.next();
      PsiField psiField = pair.first.getElement();
      if (psiField != null) {
        String newParam = "<b>" + psiField.getType().getPresentableText() + "</b>";
        if (sb.length() + newParam.length() < 53 || current == parameterCandidates.size() - 1 || current == 0) {
          if (current > 0) {
            sb.append(",");
          }
          sb.append(newParam);
        }
        else {
          sb.append(" and ");
          sb.append(parameterCandidates.size() - current);
          sb.append(" more parameters");
          break;
        }
      }
      current = (byte) (current + 1);
    }
    return sb.toString();
  }
}

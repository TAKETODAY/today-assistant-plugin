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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.jvm.actions.AnnotationAttributeValueRequestKt;
import com.intellij.lang.jvm.actions.AnnotationRequestsKt;
import com.intellij.lang.jvm.actions.JvmElementActionFactories;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;

import org.jetbrains.annotations.TestOnly;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

public final class GenerateAutowiredDependenciesJvmKt {
  @TestOnly
  private static Function1<? super Set<InfraBeanClassMember>, ? extends List<? extends BeanPointer<?>>> beansFilter;

  public static void generateAutowiredDependenciesFor(UClass uClass) {
    List<BeanPointer<?>> dependencies;
    Intrinsics.checkNotNullParameter(uClass, "uClass");
    PsiClass psiClass = uClass.getJavaPsi();
    CommonInfraModel model = InfraModelService.of().getPsiClassModel(psiClass);
    Project project = psiClass.getProject();
    Set<InfraBeanClassMember> autowiredBeanCandidates = GenerateBeanDependenciesUtil.getAutowiredBeanCandidates(model,
            springBeanPointer -> !Intrinsics.areEqual(springBeanPointer.getContainingFile(), psiClass.getContainingFile()));

    Application application = ApplicationManager.getApplication();
    if (!application.isUnitTestMode()) {
      dependencies = GenerateBeanDependenciesUtil.chooseDependentBeans(autowiredBeanCandidates, project, true);
    }
    else {
      dependencies = (List<BeanPointer<?>>) beansFilter.invoke(autowiredBeanCandidates);
    }
    generateAutowiredDependencies(uClass, dependencies, model);
  }

  public static void generateAutowiredDependencies(UClass uClass, List<? extends BeanPointer<?>> list, CommonInfraModel model) {
    PsiFile containingFile;
    Editor editor;
    IntentionAction obj;
    UField[] fields;
    UField uField;
    PsiField field;
    PsiClass javaPsi = uClass.getJavaPsi();
    Project project = javaPsi.getProject();
    PsiElement sourcePsi = uClass.getSourcePsi();
    if (sourcePsi == null || (containingFile = sourcePsi.getContainingFile()) == null) {
      return;
    }
    FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(containingFile.getVirtualFile());
    if (!(selectedEditor instanceof TextEditor)) {
      selectedEditor = null;
    }
    TextEditor textEditor = (TextEditor) selectedEditor;
    if (textEditor == null || (editor = textEditor.getEditor()) == null) {
      return;
    }
    Intrinsics.checkNotNullExpressionValue(editor, "(FileEditorManager.getIn…Editor)?.editor ?: return");
    for (BeanPointer candidateBean : list) {
      PsiType[] effectiveBeanTypes = candidateBean.getEffectiveBeanTypes();
      Intrinsics.checkNotNullExpressionValue(effectiveBeanTypes, "candidateBean.effectiveBeanTypes");
      ArrayList<PsiClass> candidateBeanClasses = new ArrayList<>();
      for (PsiType psiType : effectiveBeanTypes) {
        PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
        if (psiClass != null) {
          candidateBeanClasses.add(psiClass);
        }
      }
      PsiClass candidateBeanClass = CollectionsKt.firstOrNull(candidateBeanClasses);
      if (candidateBeanClass == null) {
        return;
      }
      String qualifierName = GenerateAutowiredDependenciesUtil.getQualifierName(candidateBean);
      boolean specifyQualifier = !StringUtil.isEmptyOrSpaces(qualifierName) && InfraModelSearchers.findBeans(model,
              ModelSearchParameters.byClass(candidateBeanClass).withInheritors().effectiveBeanTypes()).size() > 1;
      PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(project);
      String beanName = candidateBean.getName();
      String name = (beanName == null || !psiNameHelper.isIdentifier(beanName)) ? candidateBeanClass.getName() : beanName;
      if (name == null) {
        return;
      }
      List<IntentionAction> createAddFieldActions = JvmElementActionFactories.createAddFieldActions(javaPsi,
              new AutowiredFieldInfo(javaPsi, candidateBeanClass, name, project));

      for (IntentionAction createAddFieldAction : createAddFieldActions) {
        String text = createAddFieldAction.getText();
        if (text.contains("lateinit")) {
          obj = createAddFieldAction;
          break;
        }

      }
      obj = null;
      Iterator<IntentionAction> it = createAddFieldActions.iterator();

      while (true) {
        if (!it.hasNext()) {
          obj = null;
          break;
        }
        IntentionAction next = it.next();
        String text = next.getText();
        if (text.contains("lateinit")) {
          obj = next;
          break;
        }
      }
      IntentionAction intentionAction = obj;
      if (intentionAction == null) {
        intentionAction = CollectionsKt.firstOrNull(createAddFieldActions);
      }
      if (intentionAction == null) {
        return;
      }
      IntentionAction action = intentionAction;
      ApplicationManager.getApplication().runWriteAction(new Computable<Unit>() {
        public Unit compute() {
          action.invoke(project, editor, containingFile);
          TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
          if (templateState != null) {
            templateState.gotoEnd(false);
            return Unit.INSTANCE;
          }
          return null;
        }
      });

      PsiElement it2 = uClass.getSourcePsi();
      if (it2 == null) {
        return;
      }
      PsiElement psiElement = it2.isValid() ? it2 : null;
      if (psiElement == null) {
        return;
      }
      UClass uElement = UastContextKt.toUElement(psiElement, UClass.class);
      if (uElement == null || (fields = uElement.getFields()) == null) {
        return;
      }
      int i = 0;
      int length = fields.length;
      while (true) {
        if (i >= length) {
          uField = null;
          break;
        }
        UField it3 = fields[i];
        if (Intrinsics.areEqual(it3.getName(), name)) {
          uField = it3;
          break;
        }
        i++;
      }
      if (uField == null || (field = UElementKt.getAsJavaPsiElement(uField, PsiField.class)) == null) {
        return;
      }
      PsiField finalField = field;
      ApplicationManager.getApplication().runWriteAction(new Computable<Unit>() {
        public Unit compute() {
          IntentionAction intentionAction2 = CollectionsKt.firstOrNull(
                  JvmElementActionFactories.createAddAnnotationActions(finalField,
                          AnnotationRequestsKt.annotationRequest(AnnotationConstant.AUTOWIRED)));
          if (intentionAction2 != null) {
            intentionAction2.invoke(project, editor, containingFile);
          }
          if (specifyQualifier) {
            IntentionAction value = CollectionsKt.firstOrNull(JvmElementActionFactories.createAddAnnotationActions(finalField,
                    AnnotationRequestsKt.annotationRequest(AnnotationConstant.QUALIFIER,
                            AnnotationAttributeValueRequestKt.stringAttribute("value", qualifierName))));
            if (value != null) {
              value.invoke(project, editor, containingFile);
            }
          }
          return Unit.INSTANCE;
        }
      });
    }
  }

  @TestOnly
  public static void withBeansFilter(Function1<? super Set<InfraBeanClassMember>, ? extends List<? extends BeanPointer<?>>> function1, Function0<Unit> function0) {
    Intrinsics.checkNotNullParameter(function1, "filter");
    Intrinsics.checkNotNullParameter(function0, "callback");
    Function1 prevBeansFilter = beansFilter;
    try {
      beansFilter = function1;
      function0.invoke();
      beansFilter = prevBeansFilter;
    }
    catch (Throwable th) {
      beansFilter = prevBeansFilter;
      throw th;
    }
  }
}

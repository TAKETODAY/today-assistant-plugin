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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.IntentionWrapper;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.jvm.actions.CreateConstructorRequest;
import com.intellij.lang.jvm.actions.JvmElementActionFactories;
import com.intellij.lang.jvm.actions.MethodRequestsKt;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.actions.generate.AbstractDomGenerateProvider;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.actions.generate.InfraTemplateBuilder;
import cn.taketoday.assistant.model.converters.InfraBeanFactoryMethodConverterImpl;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public final class InfraConstructorArgInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean springBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel springModel) {
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass != null) {
      checkConstructorArgType(springBean, holder);
    }
    checkConstructorResolve(springBean, holder, beanClass);
    checkConstructorArgIndexes(springBean, holder);
  }

  private static void checkConstructorResolve(InfraBean springBean, DomElementAnnotationHolder holder, @Nullable PsiClass beanClass) {
    String message;
    DomElement genericAttributeValue;
    if (springBean.isAbstract()) {
      return;
    }
    boolean instantiatedByFactory = isInstantiatedByFactory(springBean);
    if (!instantiatedByFactory && beanClass == null) {
      return;
    }
    ResolvedConstructorArgs resolvedArgs = springBean.getResolvedConstructorArgs();
    if (!resolvedArgs.isResolved()) {
      if (instantiatedByFactory) {
        message = InfraBundle.message("cannot.find.factory.method.with.parameters.count");
      }
      else {
        message = InfraBundle.message("cannot.find.bean.constructor.with.parameters.count", beanClass.getName());
      }
      String basicMessage = message;
      ResolvedConstructorArgsMessageBuilder messageBuilder = new ResolvedConstructorArgsMessageBuilder(basicMessage, getConstructors(instantiatedByFactory, beanClass, springBean), resolvedArgs);
      String message2 = messageBuilder.getMessage();
      if (!instantiatedByFactory && DomUtil.hasXml(springBean.getClazz())) {
        genericAttributeValue = springBean.getClazz();
      }
      else if (instantiatedByFactory && DomUtil.hasXml(springBean.getFactoryMethod())) {
        genericAttributeValue = springBean.getFactoryMethod();
      }
      else {
        genericAttributeValue = springBean;
      }
      List<LocalQuickFix> fixes = new ArrayList<>();
      InfraBean stableCopy = springBean.createStableCopy();
      if (!instantiatedByFactory && !(beanClass instanceof PsiCompiledElement)) {
        fixes.addAll(createConstructorFixes(stableCopy, beanClass));
      }
      fixes.addAll(getConstructorArgsQuickFixes(stableCopy, springBean.getInstantiationMethods()));
      holder.createProblem(genericAttributeValue, HighlightSeverity.ERROR, message2, fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
    }
  }

  private static PsiMethod[] getConstructors(boolean factory, @Nullable PsiClass aClass, InfraBean bean) {
    if (factory) {
      String factoryMethod = bean.getFactoryMethod().getStringValue();
      if (StringUtil.isNotEmpty(factoryMethod)) {
        List<PsiMethod> methodCandidates = InfraBeanFactoryMethodConverterImpl.getFactoryMethodCandidates(bean, factoryMethod);
        return methodCandidates.toArray(PsiMethod.EMPTY_ARRAY);
      }
    }
    else if (aClass != null) {
      return aClass.getConstructors();
    }
    return PsiMethod.EMPTY_ARRAY;
  }

  private static boolean isInstantiatedByFactory(InfraBean springBean) {
    return DomUtil.hasXml(springBean.getFactoryMethod());
  }

  private static void checkConstructorArgType(InfraBean springBean, DomElementAnnotationHolder holder) {
    List<ConstructorArg> list = springBean.getConstructorArgs();
    if (!list.isEmpty()) {
      List<PsiMethod> instantiationMethods = springBean.getInstantiationMethods();
      Iterator<ConstructorArg> var4 = list.iterator();

      while (true) {
        ConstructorArg arg;
        boolean parameterFound;
        label63:
        while (true) {
          PsiType argType;
          do {
            if (!var4.hasNext()) {
              return;
            }

            arg = var4.next();
            argType = arg.getType().getValue();
          }
          while (argType == null);

          Integer index = arg.getIndex().getValue();
          parameterFound = false;
          PsiParameter[] parameters;
          if (index != null) {
            int i = index;
            if (i >= 0) {
              Iterator<PsiMethod> var18 = instantiationMethods.iterator();

              while (true) {
                if (!var18.hasNext()) {
                  break label63;
                }

                PsiMethod method = var18.next();
                parameters = method.getParameterList().getParameters();
                if (i < parameters.length) {
                  parameterFound = true;
                  if (parameters[i].getType().isAssignableFrom(argType)) {
                    break;
                  }
                }
              }
            }
          }
          else {
            Iterator<PsiMethod> var9 = instantiationMethods.iterator();

            while (true) {
              if (!var9.hasNext()) {
                break label63;
              }

              PsiMethod method = var9.next();
              parameters = method.getParameterList().getParameters();

              for (PsiParameter param : parameters) {
                if (param.getType().isAssignableFrom(argType)) {
                  continue label63;
                }
              }
            }
          }
        }

        if (parameterFound) {
          String message = InfraBundle.message("constructor.arg.incorrect.value.type");
          holder.createProblem(arg.getType(), message);
        }
      }
    }
  }

  private static void checkConstructorArgIndexes(InfraBean springBean, DomElementAnnotationHolder holder) {
    ConstructorArg previous;
    List<ConstructorArg> list = springBean.getConstructorArgs();
    if (list.isEmpty()) {
      return;
    }
    Map<Integer, ConstructorArg> argsMap = new HashMap<>();
    SmartList smartList = new SmartList();
    for (ConstructorArg arg : list) {
      Integer index = arg.getIndex().getValue();
      if (index != null && (previous = argsMap.put(index, arg)) != null) {
        reportNotUniqueIndex(holder, arg);
        if (!smartList.contains(previous)) {
          reportNotUniqueIndex(holder, previous);
          smartList.add(previous);
        }
      }
    }
  }

  private static void reportNotUniqueIndex(DomElementAnnotationHolder holder, ConstructorArg arg) {
    String message = InfraBundle.message("incorrect.constructor.arg.index.not.unique");
    holder.createProblem(arg.getIndex(), message);
  }

  private static List<LocalQuickFix> createConstructorFixes(InfraBean springBean, PsiClass beanClass) {
    return IntentionWrapper.wrapToQuickFixes(createConstructorActions(springBean, beanClass), beanClass.getContainingFile());
  }

  private static List<IntentionAction> createConstructorActions(InfraBean springBean, PsiClass beanClass) {
    Project project = springBean.getManager().getProject();
    List<Pair<String, PsiType>> params = InfraConstructorArgResolveUtil.suggestConstructorParamsForBean(springBean);
    CreateConstructorRequest request = MethodRequestsKt.constructorRequest(project, params);
    return JvmElementActionFactories.createConstructorActions(beanClass, request);
  }

  private static List<LocalQuickFix> getConstructorArgsQuickFixes(InfraBean springBean, List<PsiMethod> ctors) {
    if (!springBean.getConstructorArgs().isEmpty() || !springBean.getCNamespaceConstructorArgDefinitions().isEmpty()) {
      return Collections.emptyList();
    }
    List<LocalQuickFix> quickFixes = new ArrayList<>();
    for (PsiMethod ctor : ctors) {
      if (ctor.getParameterList().getParametersCount() > 0) {
        quickFixes.add(new AddConstructorArgQuickFix(ctor, springBean));
      }
    }
    return quickFixes;
  }

  public static class AddConstructorArgQuickFix implements LocalQuickFix {
    private final InfraBean mySpringBean;
    private final String myMethodName;
    private final SmartPsiElementPointer<PsiMethod> myCtorPointer;

    public AddConstructorArgQuickFix(PsiMethod ctor, InfraBean springBean) {
      this.myCtorPointer = SmartPointerManager.getInstance(ctor.getProject()).createSmartPsiElementPointer(ctor);
      this.mySpringBean = springBean;
      this.myMethodName = PsiFormatUtil.formatMethod(ctor, PsiSubstitutor.EMPTY, 257, 2);
    }

    public String getName() {
      String message = InfraBundle.message("model.add.constructor.args.for.method.quickfix.message", this.myMethodName);
      return message;
    }

    public String getFamilyName() {
      String message = InfraBundle.message("model.add.constructor.args.for.method.quickfix.message.family.name");
      return message;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      PsiMethod ctor = this.myCtorPointer.getElement();
      if (ctor == null || !ctor.isValid() || !FileModificationService.getInstance().prepareFileForWrite(this.mySpringBean.getContainingFile())) {
        return;
      }
      PsiMethod[] myAllCtors = ctor.getContainingClass().getConstructors();
      PsiParameter[] parameters = ctor.getParameterList().getParameters();
      CommonInfraModel model = InfraModelService.of().getModel(this.mySpringBean);
      InfraTemplateBuilder builder = new InfraTemplateBuilder(project);
      Editor editor = InfraTemplateBuilder.getEditor(descriptor);
      InfraTemplateBuilder.preparePlace(editor, project, this.mySpringBean.addConstructorArg());
      for (int i = 0; i < parameters.length; i++) {
        PsiParameter parameter = parameters[i];
        builder.addTextSegment("<");
        builder.addVariableSegment("NS_PREFIX");
        builder.addTextSegment("constructor-arg");
        if (parameters.length > 1) {
          builder.addTextSegment(" index=\"" + i + "\"");
        }
        int length = myAllCtors.length;
        int i2 = 0;
        while (true) {
          if (i2 >= length) {
            break;
          }
          PsiMethod allCtor = myAllCtors[i2];
          if (allCtor != ctor) {
            PsiParameter[] params = allCtor.getParameterList().getParameters();
            if (params.length == parameters.length) {
              builder.addTextSegment(" type=\"" + parameters[i].getType().getCanonicalText() + "\"");
              break;
            }
          }
          i2++;
        }
        builder.createValueAndClose(parameter.getType(), model, "constructor-arg");
      }
      builder.startTemplate(editor, AbstractDomGenerateProvider.createNamespacePrefixMap(this.mySpringBean));
    }
  }
}

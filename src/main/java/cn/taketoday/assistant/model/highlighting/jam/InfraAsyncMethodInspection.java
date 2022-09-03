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

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.MakeVoidQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.util.SuperMethodWarningUtil;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.util.SmartList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.util.InfraUtils;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SpreadBuilder;

public final class InfraAsyncMethodInspection extends AbstractInfraJavaInspection {

  private static final List<String> supportedAsyncReturnTypes = List.of(
          "java.util.concurrent.Future",
          InfraConstant.INFRA_UTIL_CONCURRENT_LISTENABLE_FUTURE,
          InfraConstant.JAVA_UTIL_CONCURRENT_COMPLETABLE_FUTURE
  );

  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    Intrinsics.checkNotNullParameter(aClass, "aClass");
    Intrinsics.checkNotNullParameter(manager, "manager");
    if (!InfraUtils.isBeanCandidateClass(aClass) || !InfraLibraryUtil.hasLibrary(manager.getProject())) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    boolean classLevelAsync = MetaAnnotationUtil.isMetaAnnotated(aClass, CollectionsKt.listOf(AnnotationConstant.ASYNC));
    Lazy<List<PsiClass>> allowedReturnTypes = LazyKt.lazy(() -> {
      JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(aClass.getProject());
      ArrayList<PsiClass> psiClasses = new ArrayList<>();
      for (String it : supportedAsyncReturnTypes) {
        PsiClass findClass = psiFacade.findClass(it, aClass.getResolveScope());
        if (findClass != null) {
          psiClasses.add(findClass);
        }
      }
      return psiClasses;
    });
    Collection<ProblemDescriptor> smartList = new SmartList<>();
    for (PsiMethod method : aClass.getMethods()) {
      if (!method.hasModifierProperty("private")) {
        ProblemDescriptor[] it = checkMethodInternal(method, classLevelAsync, allowedReturnTypes.getValue(), manager, isOnTheFly);
        if (it != null) {
          CollectionsKt.addAll(smartList, it);
        }
      }
    }
    return smartList.toArray(new ProblemDescriptor[0]);
  }

  private ProblemDescriptor[] checkMethodInternal(
          PsiMethod psiMethod, boolean classLevelAsync,
          List<? extends PsiClass> list, InspectionManager manager, boolean isOnTheFly) {
    boolean z;
    boolean z2;
    if (Intrinsics.areEqual(PsiType.VOID, psiMethod.getReturnType())) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (!classLevelAsync && !MetaAnnotationUtil.isMetaAnnotated(psiMethod, CollectionsKt.listOf(AnnotationConstant.ASYNC))) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiType returnType = psiMethod.getReturnType();
    if (returnType == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiClass returnTypeClass = PsiTypesUtil.getPsiClass(returnType);
    if (returnTypeClass != null) {
      if (list == null || !list.isEmpty()) {
        Iterator<PsiClass> it = (Iterator<PsiClass>) list.iterator();
        while (true) {
          if (!it.hasNext()) {
            z = false;
            break;
          }
          PsiClass next = it.next();
          z2 = Intrinsics.areEqual(returnTypeClass, next) || returnTypeClass.isInheritor(next, true);
          if (z2) {
            z = true;
            break;
          }
        }
      }
      else {
        z = false;
      }
      if (z) {
        return ProblemDescriptor.EMPTY_ARRAY;
      }
    }
    ArrayList<String> arrayList = new ArrayList<>();
    for (PsiClass name : list) {
      String qualifiedName = name.getQualifiedName();
      if (qualifiedName != null) {
        arrayList.add(qualifiedName);
      }
    }
    ArrayList<MakeTypeAsyncFix> objects = new ArrayList<>(Math.max(arrayList.size(), 10));
    for (String it3 : arrayList) {
      objects.add(new MakeTypeAsyncFix(psiMethod, it3));
    }
    MakeTypeAsyncFix[] convertingFixes = objects.toArray(new MakeTypeAsyncFix[0]);
    PsiElement nameIdentifier = psiMethod.getNameIdentifier();
    if (nameIdentifier == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, psiMethod.getContainingFile(), isOnTheFly);
    String message = InfraBundle.message("AsyncMethodInspection.incorrect.signature");
    SpreadBuilder spreadBuilder = new SpreadBuilder(2);
    spreadBuilder.add(new MakeVoidQuickFix(null));
    spreadBuilder.addSpread(convertingFixes);
    holder.registerProblem(nameIdentifier, message, (LocalQuickFix[]) spreadBuilder.toArray(new LocalQuickFix[spreadBuilder.size()]));
    return holder.getResultsArray();
  }

  public static final class MakeTypeAsyncFix extends LocalQuickFixOnPsiElement {

    private final String forceReturn;

    public String getForceReturn() {
      return this.forceReturn;
    }

    public MakeTypeAsyncFix(PsiMethod method, String forceReturn) {
      super(method);
      this.forceReturn = forceReturn;
    }

    public String getFamilyName() {
      String message = InfraBundle.message("method.return.type.make.async.family.name");
      return message;
    }

    public void invoke(Project project, PsiFile file, PsiElement startElement, PsiElement endElement) {
      if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
        return;
      }
      PsiMethod it = (PsiMethod) startElement;
      final PsiMethod myMethod = SuperMethodWarningUtil.checkSuperMethod(it);
      if (myMethod == null) {
        return;
      }
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
      final PsiType typeParameter = boxedIfPrimitive(myMethod, myMethod.getReturnType());
      PsiClass forceReturnPsiClass = psiFacade.findClass(this.forceReturn, myMethod.getResolveScope());
      if (forceReturnPsiClass == null) {
        throw new IllegalArgumentException("class not found " + this.forceReturn);
      }

      final PsiClassType createType = psiFacade.getElementFactory().createType(forceReturnPsiClass, typeParameter);
      Project project2 = myMethod.getProject();
      String name = myMethod.getName();
      PsiParameterList parameterList = myMethod.getParameterList();
      PsiParameter[] parameters = parameterList.getParameters();

      ArrayList<ParameterInfoImpl> parameterInfos = new ArrayList<>(parameters.length);
      int index$iv$iv = 0;
      for (PsiParameter psiParameter : parameters) {
        int i = index$iv$iv;
        index$iv$iv++;
        ParameterInfoImpl create = ParameterInfoImpl.create(i);
        Intrinsics.checkNotNullExpressionValue(psiParameter, "p");
        parameterInfos.add(create.withName(psiParameter.getName()).withType(psiParameter.getType()));
      }
      ParameterInfoImpl[] array = parameterInfos.toArray(new ParameterInfoImpl[0]);
      ChangeSignatureProcessor processor = new ChangeSignatureProcessor(project2, myMethod, false, null, name, createType, array);
      processor.run();

      WriteAction.run(() -> {
        wrapReturn(myMethod, psiFacade, createType, typeParameter);
        for (PsiMethod oMethod : OverridingMethodsSearch.search(myMethod)) {
          wrapReturn(oMethod, psiFacade, createType, typeParameter);
        }
      });
    }

    private void wrapReturn(PsiMethod psiMethod, JavaPsiFacade psiFacade, PsiClassType createType, PsiType typeParameter) {
      PsiReturnStatement[] returnStatements = PsiUtil.findReturnStatements(psiMethod);
      for (PsiReturnStatement rts : returnStatements) {
        Intrinsics.checkNotNullExpressionValue(rts, "rts");
        PsiExpression it = rts.getReturnValue();
        if (it != null) {

          PsiElementFactory elementFactory = psiFacade.getElementFactory();
          PsiFile containingFile = psiMethod.getContainingFile();

          wrapReturn(elementFactory, createType, typeParameter, it, containingFile);
        }
      }
    }

    private PsiType boxedIfPrimitive(PsiMethod myMethod, PsiType returnType) {
      PsiType boxedType;
      PsiType psiType = returnType;
      if (!(psiType instanceof PsiPrimitiveType)) {
        psiType = null;
      }
      PsiPrimitiveType psiPrimitiveType = (PsiPrimitiveType) psiType;
      return (psiPrimitiveType == null || (boxedType = psiPrimitiveType.getBoxedType(myMethod)) == null) ? returnType : boxedType;
    }

    public void wrapReturn(PsiElementFactory factory, PsiClassType returnPsiType, PsiType typeParameter, PsiExpression previousReturn, PsiFile context) {
      String str;
      PsiClass resolve = returnPsiType.resolve();
      String qualifiedName = resolve != null ? resolve.getQualifiedName() : null;
      if (qualifiedName != null) {
        switch (qualifiedName.hashCode()) {
          case 311679446:
            if (!qualifiedName.equals(InfraConstant.INFRA_UTIL_CONCURRENT_LISTENABLE_FUTURE)) {
              return;
            }
            break;
          case 1346935898:
            if (!qualifiedName.equals("java.util.concurrent.Future")) {
              return;
            }
            break;
          case 1891130994:
            if (qualifiedName.equals(InfraConstant.JAVA_UTIL_CONCURRENT_COMPLETABLE_FUTURE)) {
              previousReturn.replace(factory.createExpressionFromText("java.util.concurrent.CompletableFuture.completedFuture(" + previousReturn.getText() + ")", context));
              return;
            }
            return;
          default:
            return;
        }
        str = "cn.taketoday.scheduling.annotation.AsyncResult.forValue()";

        PsiElement createExpressionFromText = factory.createExpressionFromText(str, context);
        PsiElement it = previousReturn.replace(createExpressionFromText);
        PsiElement shortenClassReferences = JavaCodeStyleManager.getInstance(context.getProject()).shortenClassReferences(it);
        if (shortenClassReferences instanceof PsiCall psiCall) {
          PsiExpressionList argumentList = psiCall.getArgumentList();
          argumentList.add(factory.createExpressionFromText(previousReturn.getText(), context));
        }
      }
    }

    public String getText() {
      return InfraBundle.message("method.return.type.make.async.fix.text", StringUtilRt.getShortName(this.forceReturn));
    }

    public boolean startInWriteAction() {
      return false;
    }
  }
}

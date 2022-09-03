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

package cn.taketoday.assistant.web.mvc;

import com.intellij.microservices.url.parameters.PathVariablePsiElement;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.UastReferenceRegistrar;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;

import org.jetbrains.concurrency.AsyncCache;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.jetbrains.uast.UArrayAccessExpression;
import org.jetbrains.uast.UBinaryExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastLiteralUtils;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.expressions.UInjectionHost;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.model.utils.light.InfraImplicitVariable;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.WebMVCModelAttribute;
import cn.taketoday.assistant.web.mvc.pathVariables.WebMvcPathVariableDeclarationSearcher;
import cn.taketoday.assistant.web.mvc.views.InfraMVCViewUastReferenceProvider;
import cn.taketoday.lang.Nullable;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

final class WebControllerModelVariablesCollector {

  private static final String MODEL = "cn.taketoday.ui.Model";

  private static final String MODEL_MAP = "cn.taketoday.ui.ModelMap";
  private static final UExpressionPattern<UExpression, ?> IMPLICIT_ATTRIBUTE_PATTERN = UastPatterns.uExpression()
          .andOr(UastPatterns.uExpression().methodCallParameter(0, PsiJavaPatterns.psiMethod().withName("addObject").withParameterCount(1).inClass(
                  InfraMvcConstant.MODEL_AND_VIEW), false), UastPatterns.uExpression().methodCallParameter(0,
                  PsiJavaPatterns.psiMethod().withName("addAttribute").withParameterCount(1).inClass(MODEL), false), UastPatterns.uExpression().methodCallParameter(0,
                  PsiJavaPatterns.psiMethod().withName("addAttribute", "addObject").withParameterCount(1).inClass(MODEL_MAP), false));

  // FIXME 
  private static final String REDIRECT_ATTRIBUTES = "cn.taketoday.web.view.RedirectModel";
  private static final UExpressionPattern<UExpression, ?> ATTRIBUTE_PATTERN = UastPatterns.injectionHostOrReferenceExpression().withSourcePsiCondition(InfraLibraryUtil.IS_WEB_MVC_PROJECT)
          .andOr(UastPatterns.uExpression().methodCallParameter(0, PsiJavaPatterns.psiMethod().withName("addObject").inClass(InfraMvcConstant.MODEL_AND_VIEW),
                  false), UastPatterns.uExpression().methodCallParameter(0, PsiJavaPatterns.psiMethod().withName("addAttribute").inClass(MODEL), false), UastPatterns.uExpression().methodCallParameter(
                  0, PsiJavaPatterns.psiMethod().withName(new String[] { "addAttribute", "addObject" }).inClass(MODEL_MAP), false), UastPatterns.uExpression().methodCallParameter(0,
                  PsiJavaPatterns.psiMethod().withName("addFlashAttribute").inClass(REDIRECT_ATTRIBUTES), false), UastPatterns.uExpression().methodCallParameter(0,
                  PsiJavaPatterns.psiMethod().withName("modelAttribute").inClass(
                          WebMvcFunctionalRoutingConstant.RENDERING_BUILDER), false), UastPatterns.uExpression().methodCallParameter(0,
                  PsiJavaPatterns.psiMethod().withName("put").definedInClass("java.util.Map"), false), UastPatterns.uExpression().annotationParams(InfraMvcConstant.MODEL_ATTRIBUTE,
                  PsiJavaPatterns.string().oneOf(RequestMapping.VALUE_ATTRIBUTE, "name")), UastPatterns.uExpression().constructorParameter(1,
                  InfraMvcConstant.MODEL_AND_VIEW), UastPatterns.uExpression().constructorParameter(0, MODEL_MAP), UastPatterns.uExpression().arrayAccessParameterOf(
                  PsiJavaPatterns.psiClass().inheritorOf(false, PsiJavaPatterns.psiClass().withQualifiedName(PsiJavaPatterns.string().oneOf(MODEL, MODEL_MAP)))));
  private static final UExpressionPattern<UExpression, ?> SESSION_ATTRIBUTES_PATTERN = UastPatterns.injectionHostUExpression()
          .annotationParams(InfraMvcConstant.SESSION_ATTRIBUTES, StandardPatterns.string().oneOf(RequestMapping.VALUE_ATTRIBUTE, "names"));
  private static final AsyncCache<PsiClass, MultiMap<String, PsiVariable>> ourVariablesCache = new AsyncCache<>(psiClass -> {
    return UastModificationTracker.getInstance(psiClass.getProject());
  }, WebControllerModelVariablesCollector::getVariableMap);

  WebControllerModelVariablesCollector() {
  }

  static Promise<MultiMap<String, PsiVariable>> getVariables(PsiClass psiClass) {
    return ourVariablesCache.computeOrGet(psiClass);
  }

  public static MultiMap<String, PsiVariable> getVariableMap(PsiClass psiClass) {
    UMethod uMethod;
    MultiMap<String, PsiVariable> result = new MultiMap<>();
    Set<PsiVariable> modelAttributeVariables = new HashSet<>();
    for (PsiMethod method : psiClass.getAllMethods()) {
      boolean modelAttributeMethod = !InfraControllerUtils.isInheritedController(psiClass) && InfraControllerUtils.isModelAttributeProvider(method);
      if ((modelAttributeMethod || InfraControllerUtils.isRequestHandler(psiClass, method)) && (uMethod = UastContextKt.toUElement(method, UMethod.class)) != null) {
        ViewsAndVariablesCollector visitor = new ViewsAndVariablesCollector(psiClass);
        visitor.collectFrom(uMethod);
        if (modelAttributeMethod) {
          modelAttributeVariables.addAll(visitor.getVisitorVariables());
        }
        else {
          List<? extends PsiVariable> pathVariables = collectPathVariables(psiClass, uMethod);
          for (String view : visitor.getVisitorViews()) {
            result.putValues(view, visitor.getVisitorVariables());
            result.putValues(view, pathVariables);
          }
        }
      }
    }
    for (String view2 : ArrayUtil.toStringArray(result.keySet())) {
      result.putValues(view2, modelAttributeVariables);
    }
    if (!InfraControllerUtils.isInheritedController(psiClass)) {
      processGlobalModelAttributes(psiClass, result);
    }
    return result;
  }

  private static List<? extends PsiVariable> collectPathVariables(PsiClass psiClass, UMethod uMethod) {
    return WebMvcPathVariableDeclarationSearcher.collectDeclarations(uMethod)
            .map(mvcPathVariableRef -> {
              PathVariablePsiElement variablePsiElement = mvcPathVariableRef.resolve();
              return new CommonFakePsiVariablePomTarget(psiClass.getProject(), variablePsiElement.getVariablePomTarget(),
                      PsiType.getJavaLangString(variablePsiElement.getManager(), variablePsiElement.getResolveScope()));
            }).toList();
  }

  private static void processGlobalModelAttributes(PsiClass psiClass, MultiMap<String, PsiVariable> result) {
    WebMVCModelAttribute modelAttribute;
    for (PsiMethod method : psiClass.getAllMethods()) {
      if (!method.isConstructor() && (modelAttribute = WebMVCModelAttribute.METHOD_META.getJamElement(method)) != null) {
        if (PsiType.VOID.equals(method.getReturnType())) {
          UMethod uMethod = UastContextKt.toUElement(method, UMethod.class);
          if (uMethod != null) {
            ViewsAndVariablesCollector visitor = new ViewsAndVariablesCollector(psiClass);
            visitor.collectFrom(uMethod);
            for (Map.Entry<String, Collection<PsiVariable>> entry : result.entrySet()) {
              entry.getValue().addAll(visitor.getVisitorVariables());
            }
          }
        }
        else {
          String name = modelAttribute.getName();
          if (name != null) {
            InfraImplicitVariable var = new InfraImplicitVariable(name, modelAttribute.getType(), PomService.convertToPsi(modelAttribute.getPsiTarget()));
            for (Map.Entry<String, Collection<PsiVariable>> entry2 : result.entrySet()) {
              entry2.getValue().add(var);
            }
          }
        }
      }
    }
  }

  @Nullable
  private static InfraImplicitVariable createVariable(PsiElement scope, UExpression declaration) {
    String name = UastUtils.evaluateString(declaration);
    if (name == null) {
      return null;
    }
    PsiElement sourceInjectionHost = UastLiteralUtils.getSourceInjectionHost(declaration);
    if (sourceInjectionHost == null) {
      sourceInjectionHost = declaration.getSourcePsi();
    }
    if (sourceInjectionHost == null) {
      return null;
    }
    PsiType type = null;
    UElement uastParent = declaration.getUastParent();
    if (uastParent instanceof UCallExpression callExpression) {
      List<UExpression> arguments = StreamEx.of(callExpression.getValueArguments()).map(UastLiteralUtils::wrapULiteral).toList();
      int i = arguments.indexOf(UastLiteralUtils.wrapULiteral(declaration));
      if (i != -1 && arguments.size() > i + 1) {
        type = arguments.get(i + 1).getExpressionType();
      }
    }
    else if (uastParent instanceof UArrayAccessExpression) {
      UElement uastParent2 = uastParent.getUastParent();
      if (uastParent2 instanceof UBinaryExpression binaryExpression) {
        type = binaryExpression.getRightOperand().getExpressionType();
      }
    }
    if (type == null) {
      UParameter psiParameter = UastUtils.getParentOfType(declaration, UParameter.class);
      if (psiParameter != null) {
        type = psiParameter.getType();
      }
      else {
        UMethod psiMethod = UastUtils.getParentOfType(declaration, UMethod.class);
        type = psiMethod == null ? null : psiMethod.getReturnType();
      }
    }
    if (type instanceof PsiPrimitiveType psiPrimitiveType) {
      type = psiPrimitiveType.getBoxedType(scope);
    }
    return new InfraImplicitVariable(name, type, sourceInjectionHost);
  }

  static void registerModelVariablesReferenceProvider(PsiReferenceRegistrar registrar) {
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, ATTRIBUTE_PATTERN,
            UastReferenceRegistrar.uastInjectionHostReferenceProvider((expression, host) -> {
              UClass psiClass = UastUtils.getContainingUClass(expression);
              return psiClass == null
                     ? PsiReference.EMPTY_ARRAY
                     : new PsiReference[] {
                             PsiReferenceBase.createSelfReference(host, createVariable(psiClass.getJavaPsi(), expression))
                     };
            }), 0.0d);
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, SESSION_ATTRIBUTES_PATTERN, UastReferenceRegistrar.uastReferenceProvider(UInjectionHost.class, (uhost, __) -> {
      String name = uhost.evaluateToString();
      return new PsiReference[] { new PsiPolyVariantReferenceBase<PsiLanguageInjectionHost>(uhost.getPsiLanguageInjectionHost(), false) {

        public ResolveResult[] multiResolve(boolean incompleteCode) {
          Stream<? extends PsiVariable> stream = getVariables().stream();
          return stream.filter(v -> Objects.equals(v.getName(), name))
                  .map(PsiElementResolveResult::new)
                  .collect(MoreCollectors.toArray(ResolveResult[]::new));
        }

        public Collection<? extends PsiVariable> getVariables() {
          UClass uClass = UastUtils.findContaining(getElement(), UClass.class);
          if (uClass == null) {
            return Collections.emptyList();
          }
          return (ProgressIndicatorUtils.awaitWithCheckCanceled(
                  Promises.asCompletableFuture(WebControllerModelVariablesCollector.getVariables(uClass.getJavaPsi())))).values();
        }

        public Object[] getVariants() {
          return getVariables().toArray();
        }
      } };
    }), 0.0d);
  }

  private static final class ViewsAndVariablesCollector {
    private final Set<PsiMethod> visited;

    private final PsiClass myPsiClass;
    private final Set<String> myVisitorViews;
    private final List<PsiVariable> myVisitorVariables;

    private ViewsAndVariablesCollector(PsiClass psiClass) {
      this.visited = new HashSet();
      this.myVisitorViews = new HashSet();
      this.myVisitorVariables = new SmartList();
      this.myPsiClass = psiClass;
    }

    public void collectFrom(UMethod handlerMethod) {
      PsiMethod method;
      UMethod uMethod;
      PsiType type;
      String variableName;
      Deque<PsiElement> psiElementQueue = new ArrayDeque<>();
      ContainerUtil.addIfNotNull(psiElementQueue, handlerMethod.getSourcePsi());
      while (!psiElementQueue.isEmpty()) {
        ProgressManager.checkCanceled();
        PsiElement psiElement = psiElementQueue.removeLast();
        psiElementQueue.addAll(ContainerUtil.reverse(Arrays.asList(psiElement.getChildren())));
        UExpression uCallExpression = UastContextKt.toUElementOfExpectedTypes(
                psiElement, UReferenceExpression.class, UCallExpression.class, UInjectionHost.class);
        if (uCallExpression != null && Objects.equals(uCallExpression.getSourcePsi(), psiElement)) {
          ProcessingContext ctx = new ProcessingContext();
          if ((uCallExpression instanceof UReferenceExpression) || UastLiteralUtils.isInjectionHost(uCallExpression)) {
            if (isViewPattern(uCallExpression, ctx)) {
              String literalName = UastUtils.evaluateString(uCallExpression);
              ContainerUtil.addIfNotNull(this.myVisitorViews, literalName);
            }
            else if (ATTRIBUTE_PATTERN.accepts(uCallExpression, ctx)) {
              InfraImplicitVariable variable = createVariable(this.myPsiClass.getContainingFile(), uCallExpression);
              ContainerUtil.addIfNotNull(this.myVisitorVariables, variable);
            }
          }
          if (IMPLICIT_ATTRIBUTE_PATTERN.accepts(uCallExpression, ctx) && (variableName = InfraControllerUtils.getVariableName(
                  (type = uCallExpression.getExpressionType()))) != null) {
            this.myVisitorVariables.add(new InfraImplicitVariable(variableName, type, uCallExpression.getSourcePsi()));
          }
          if ((uCallExpression instanceof UCallExpression callExpression)
                  && (method = callExpression.resolve()) != null &&
                  !isLibraryCode(method) && this.visited.add(method) && InheritanceUtil.isInheritorOrSelf(
                  this.myPsiClass, method.getContainingClass(), true) && (uMethod = UastContextKt.toUElement(method, UMethod.class)) != null) {
            ContainerUtil.addIfNotNull(psiElementQueue, uMethod.getSourcePsi());
          }
        }
      }
    }

    private static boolean isViewPattern(UExpression element, ProcessingContext ctx) {
      return InfraMVCViewUastReferenceProvider.VIEW_PATTERN.accepts(element, ctx)
              || StreamEx.iterate(element, Objects::nonNull, e -> {
                return (UExpression) e.getUastParent();
              }).limit(5L).takeWhile(e2 -> {
                return !(e2 instanceof UCallExpression) || InfraMVCViewUastReferenceProvider.isReactiveCall((UCallExpression) e2);
              })
              .select(UReturnExpression.class)
              .findFirst()
              .orElse(null) != null;
    }

    private static boolean isLibraryCode(PsiMethod method) {
      if (method instanceof PsiCompiledElement) {
        return true;
      }
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(method);
      return virtualFile != null && FileIndexFacade.getInstance(method.getProject()).isInLibrarySource(virtualFile);
    }

    private Set<String> getVisitorViews() {
      return this.myVisitorViews;
    }

    private List<PsiVariable> getVisitorVariables() {
      return this.myVisitorVariables;
    }
  }
}

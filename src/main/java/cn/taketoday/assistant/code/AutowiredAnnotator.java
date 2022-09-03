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

package cn.taketoday.assistant.code;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Icon;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.gutter.BeansPsiElementCellRenderer;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.impl.InfraAutoConfiguredModels;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraBeanFactoryUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.Autowire;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * Autowired candidate or dependencies Annotator
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 20:44
 */
public class AutowiredAnnotator extends AbstractInfraAnnotator {

  @Override
  public String getId() {
    return "AutowiredAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("core.autowired.annotator.name");
  }

  @Override
  public Icon getIcon() {
    return Icons.ShowAutowiredDependencies;
  }

  @Override
  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {

    UElement element = UastUtils.getUParentForIdentifier(psiElement);
    if (element instanceof UMethod) {
      this.annotateMethod((UMethod) element, result);
    }
    else if (element instanceof UField) {
      annotateField(result, (UField) element);
    }
    else if (element instanceof UParameter) {
      UElement parent = element.getUastParent();
      if (parent instanceof UMethod) {
        processAnnotatedMethod((UMethod) parent, psiElement, result);
      }

    }
    else {
      if (element instanceof UReferenceExpression) {
        element = element.getUastParent();
      }

      if (element instanceof UAnnotation) {
        annotateAnnotation(psiElement, result, (UAnnotation) element);
      }

    }
  }

  @Override
  protected void annotateMethod(UMethod uMethod, PsiElement identifier, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiMethod method = UElementKt.getAsJavaPsiElement(uMethod, PsiMethod.class);
    if (method != null) {
      PsiClass psiClass = method.getContainingClass();
      if (InfraUtils.isBeanCandidateClassInProject(psiClass)) {
        JavaClassInfo info = JavaClassInfo.from(psiClass);
        if (PropertyUtilBase.isSimplePropertySetter(method)) {
          if (info.isAutowired()) {
            checkAutowiredMethod(method, result, info, identifier);
          }
        }
        else if (uMethod.isConstructor()) {
          if (info.isMappedConstructor(method)) {
            addConstructorArgsGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
              return JavaClassInfo.from(psiClass).getMappedConstructorDefinitions(method);
            }));
          }
          else if (uMethod.getJavaPsi().getModifierList().hasModifierProperty("public") && info.isStereotypeJavaBean()) {
            PsiIdentifier nameIdentifier = uMethod.getJavaPsi().getNameIdentifier();
            if (nameIdentifier != null) {
              addStereotypeBeanFactoryCallsGutterIcon(result, uMethod.getJavaPsi(), nameIdentifier);
            }
          }
        }

      }
    }
  }

  private static void annotateAnnotation(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result, UAnnotation uAnnotation) {
    UElement annotatedElement = uAnnotation.getUastParent();
    if (annotatedElement instanceof UMethod uMethod) {
      PsiMethod method = uMethod.getJavaPsi();
      if (method.isConstructor() || PropertyUtilBase.isSimplePropertySetter(method)) {
        return;
      }

      // show
      ContextJavaBean bean = getStereotypeBean(method);
      if (bean != null) {
        UAnnotation annotationFromBean = UastContextKt.toUElement(bean.getPsiAnnotation(), UAnnotation.class);
        if (Objects.equals(annotationFromBean, uAnnotation)) {
          result.add(
                  getNavigateToAutowiredCandidatesBuilder(method, method.getReturnType())
                          .createRelatedMergeableLineMarkerInfo(psiElement)
          );
        }
      }
    }

  }

  private static ContextJavaBean getStereotypeBean(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(ContextJavaBean.BEAN_JAM_KEY, method);
  }

  public static boolean checkAutowiredMethod(PsiMethod method, @Nullable Collection<? super RelatedItemLineMarkerInfo<?>> result,
          JavaClassInfo info, PsiElement identifier) {

    CommonInfraModel model = AutowireUtil.getProcessingInfraModel(method.getContainingClass());
    if (model != null) {
      for (Autowire autowire : info.getAutowires()) {
        if (autowire == Autowire.BY_TYPE) {
          PsiType type = PropertyUtilBase.getPropertyType(method);
          if (type != null) {
            return processVariable(method, result, model, identifier, type);
          }
        }
        else if (autowire == Autowire.BY_NAME) {
          return annotateByNameAutowiredMethod(method, result, model, identifier);
        }
      }
    }

    return false;
  }

  private static boolean annotateByNameAutowiredMethod(
          PsiModifierListOwner owner, @Nullable Collection<? super RelatedItemLineMarkerInfo<?>> result,
          CommonInfraModel model, PsiElement identifier) {

    Collection<BeanPointer<?>> collection = getByNameAutowiredBean(owner, model);
    if (collection != null && !collection.isEmpty()) {
      if (result != null) {
        NavigationGutterIconBuilderUtil.addAutowiredDependenciesIcon(collection, result, identifier,
                InfraBundle.message("navigate.to.by.name.autowired.dependencies"));
      }

      return true;
    }
    else {
      return false;
    }
  }

  private static Collection<BeanPointer<?>> getByNameAutowiredBean(PsiModifierListOwner owner, CommonInfraModel model) {

    String name = null;
    if (owner instanceof PsiMethod) {
      name = PropertyUtilBase.getPropertyNameBySetter((PsiMethod) owner);
    }
    else if (owner instanceof PsiField) {
      name = ((PsiField) owner).getName();
    }

    if (name != null) {
      BeanPointer<?> bean = InfraModelSearchers.findBean(model, name);
      if (bean != null) {
        return Collections.singleton(bean.getBasePointer());
      }
    }

    return Collections.emptySet();
  }

  private static void annotateField(Collection<? super RelatedItemLineMarkerInfo<?>> result, UField ufield) {
    PsiElement identifier = UElementKt.getSourcePsiElement(ufield.getUastAnchor());
    if (identifier != null) {
      PsiField field = UElementKt.getAsJavaPsiElement(ufield, PsiField.class);
      if (field != null) {
        // final , @Autowired
        if (ufield.isFinal() || AutowireUtil.isAutowiredByAnnotation(field)) {
          CommonInfraModel model = AutowireUtil.getProcessingInfraModel(field.getContainingClass());
          if (model != null) {
            processVariable(field, result, model, identifier, field.getType());
          }
        }
      }
    }
  }

  private static void processAnnotatedMethod(UMethod uMethod, PsiElement identifier, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiMethod method = uMethod.getJavaPsi();
    if (AutowireUtil.isInjectionPoint(method)) {
      CommonInfraModel model = AutowireUtil.getProcessingInfraModel(method.getContainingClass());
      if (model != null) {
        if (AutowireUtil.getResourceAnnotation(method) != null && PropertyUtilBase.isSimplePropertySetter(method)) {
          UParameter uParameter = uMethod.getUastParameters().get(0);
          if (identifier == UElementKt.getSourcePsiElement(uParameter.getUastAnchor())) {
            processVariable(method, result, model, identifier, uParameter.getType());
          }
        }
        else {
          for (UParameter parameter : uMethod.getUastParameters()) {
            if (identifier == UElementKt.getSourcePsiElement(parameter.getUastAnchor())) {
              PsiParameter psiParameter = UElementKt.getAsJavaPsiElement(parameter, PsiParameter.class);
              processVariable(psiParameter, result, model, identifier, parameter.getType());
              break;
            }
          }
        }
      }
    }

  }

  private static boolean processVariable(
          PsiModifierListOwner variable, @Nullable Collection<? super RelatedItemLineMarkerInfo<?>> result,
          CommonInfraModel model, PsiElement identifier, PsiType type) {

    Collection<BeanPointer<?>> list = AutowireUtil.getAutowiredBeansFor(variable, getAutowiredType(type), model);
    if (!list.isEmpty()) {
      if (result != null) {
        NavigationGutterIconBuilderUtil.addAutowiredDependenciesIcon(list, result, identifier);
      }

      return true;
    }
    else {
      return false;
    }
  }

  private static PsiType getAutowiredType(PsiType type) {
    if (AutowireUtil.isJavaUtilOptional(type)) {
      PsiType optionalType = AutowireUtil.getOptionalType(type);
      if (optionalType != null) {
        return optionalType;
      }
    }
    return type;
  }

  public static GutterIconBuilder<PsiElement> getNavigateToAutowiredCandidatesBuilder(PsiMember psiMember, PsiType type) {
    var builder = GutterIconBuilder.create(Icons.Gutter.ShowAutowiredCandidates);
    builder.setPopupTitle(InfraBundle.message("choose.autowired.candidates.title"))
            .setEmptyPopupText(InfraBundle.message("navigate.no.matching.autowired.candidates"))
            .setTooltipText(InfraBundle.message("navigate.to.autowired.candidates.title"))
            .setTargets(NotNullLazyValue.lazy(() -> {
              if (!psiMember.isValid()) {
                return Collections.emptySet();
              }
              else {
                Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(psiMember);
                if (moduleForPsiElement != null) {
                  return AutowireUtil.getAutowiredMembers(type, moduleForPsiElement, psiMember);
                }
                else {
                  Set<PsiModifierListOwner> members = new LinkedHashSet<>();
                  for (Module module : getRelatedModules(psiMember)) {
                    members.addAll(AutowireUtil.getAutowiredMembers(type, module, psiMember));
                  }
                  return members;
                }
              }
            }));
    return builder;
  }

  private static void addStereotypeBeanFactoryCallsGutterIcon(Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiMethod method, PsiElement identifier) {

    if (method.isConstructor()) {
      Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(method);
      if (moduleForPsiElement != null) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
          PsiClassType psiClassType = JavaPsiFacade.getInstance(method.getProject()).getElementFactory().createType(containingClass);
          Set<PsiElement> callsForBean = InfraBeanFactoryUtils.findBeanFactoryCallsForBean(moduleForPsiElement, psiClassType, method.getName(), InfraBeanFactoryUtils.getParamTypes(method));
          if (!callsForBean.isEmpty()) {
            GutterIconBuilder<PsiElement> builder = GutterIconBuilder.create(Icons.Gutter.ShowAutowiredCandidates);
            builder.setPopupTitle(InfraBundle.message("gutter.choose.bean.factory.calls.title"))
                    .setEmptyPopupText(InfraBundle.message("gutter.navigate.no.bean.factory.calls"))
                    .setTooltipText(InfraBundle.message("gutter.navigate.to.bean.factory.calls.title")).setTargets(callsForBean);
            result.add(builder.createRelatedMergeableLineMarkerInfo(identifier));
          }
        }
      }
    }
  }

  private static void addConstructorArgsGutterIcon(Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement psiIdentifier,
          NotNullLazyValue<Collection<? extends BeanPointer<?>>> targets) {
    GutterIconBuilder<BeanPointer<?>> builder = GutterIconBuilder.create(
            Icons.Gutter.SpringBeanMethod,
            NavigationGutterIconBuilderUtil.BEAN_POINTER_CONVERTOR,
            NavigationGutterIconBuilderUtil.AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
    );
    builder.setTargets(targets)
            .setCellRenderer(BeansPsiElementCellRenderer::new)
            .setPopupTitle(InfraBundle.message("bean.constructor.navigate.choose.class.title"))
            .setTooltipText(InfraBundle.message("bean.constructor.tooltip.navigate.declaration"));
    result.add(builder.createRelatedMergeableLineMarkerInfo(psiIdentifier));
  }

  private static Set<Module> getRelatedModules(PsiElement element) {
    PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) {
      return Collections.emptySet();
    }
    else {
      VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
      if (virtualFile == null) {
        return Collections.emptySet();
      }
      else {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
        if (!fileIndex.isLibraryClassFile(virtualFile) && !fileIndex.isInLibrarySource(virtualFile)) {
          return Collections.emptySet();
        }
        else {
          boolean allowAutoConfig = InfraAutoConfiguredModels.isAllowAutoConfiguration(element.getProject());
          return fileIndex.getOrderEntriesForFile(virtualFile).stream().map(OrderEntry::getOwnerModule).filter((module) -> {
            return InfraUtils.hasFacet(module) || allowAutoConfig && InfraModelService.of().hasAutoConfiguredModels(module);
          }).collect(Collectors.toSet());
        }
      }
    }
  }
}


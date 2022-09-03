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

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.beans.stereotype.ContextImport;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.gutter.BeansPsiElementCellRenderer;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.impl.InfraAutoConfiguredModels;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.DomBeanPointer;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 20:26
 */
public class BeanAnnotator extends AbstractInfraAnnotator {

  @Override
  public String getId() {
    return "BeanAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("core.bean.annotator.name");
  }

  @Override
  public Icon getIcon() {
    return Icons.SpringBean;
  }

  @Override
  protected void annotateClass(Collection<? super RelatedItemLineMarkerInfo<?>> result, UClass uClass, PsiElement identifier) {
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (psiClass == null || !InfraUtils.isBeanCandidateClass(psiClass)) {
      return;
    }
    JavaClassInfo info1 = JavaClassInfo.from(psiClass);
    if (info1.isMappedDomBean()) {
      addSpringBeanGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
        List<DomBeanPointer> domBeans = JavaClassInfo.from(psiClass).getMappedDomBeans();
        domBeans.sort(BeanPointer.DISPLAY_COMPARATOR);
        return domBeans;
      }));
    }
    else if (!info1.isStereotypeJavaBean()
            || AnnotationUtil.isAnnotated(psiClass, InfraAutoConfiguredModels.annotations, 8)) {

    }
    else {
      addJavaBeanGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
        SmartList<CommonModelElement> smartList = new SmartList<>();
        List<JamBeanPointer> mappedBeans = JavaClassInfo.from(psiClass).getStereotypeMappedBeans();
        for (JamBeanPointer mappedBean : mappedBeans) {
          JamPsiMemberInfraBean<?> bean = mappedBean.getBean();
          if ((bean instanceof InfraJavaBean) || ((bean instanceof InfraStereotypeElement) && !psiClass.isEquivalentTo(bean.getPsiElement()))) {
            smartList.add(bean);
          }
        }
        CommonInfraModel model = InfraModelService.of().getPsiClassModel(psiClass);
        Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
        ContainerUtil.addAllNotNull(smartList, InfraJamService.of().findStereotypeConfigurationBeans(model, mappedBeans, module));
        ContainerUtil.addAllNotNull(smartList, getImportConfigurations(model, psiClass));
        return smartList;
      }), Icons.Gutter.SpringJavaBean);
      PsiClassType classType = JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory().createType(psiClass);
      result.add(AutowiredAnnotator.getNavigateToAutowiredCandidatesBuilder(psiClass, classType)
              .createGroupLineMarkerInfo(identifier)
      );
    }
  }

  @Override
  protected void annotateMethod(UMethod uMethod, PsiElement identifier,
          Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiMethod method = UElementKt.getAsJavaPsiElement(uMethod, PsiMethod.class);
    if (method != null) {
      PsiClass psiClass = method.getContainingClass();
      if (psiClass != null) {
        if (InfraUtils.isBeanCandidateClassInProject(psiClass)) {
          JavaClassInfo info = JavaClassInfo.from(psiClass);

          Collection<Pair<PsiElement, JavaClassInfo.MethodType>> methodTypes = info.getMethodTypes(method);
          if (!methodTypes.isEmpty()) {
            addMethodTypesGutterIcon(result, method, methodTypes);
          }
        }

      }
    }
  }

  private static void addMethodTypesGutterIcon(
          Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiMethod psiMethod,
          Collection<? extends Pair<PsiElement, JavaClassInfo.MethodType>> targets) {
    String tooltipText = InfraBundle.message("bean.methods.tooltip.navigate.declaration");
    Icon icon = Icons.Gutter.SpringBeanMethod;
    if (targets.size() == 1) {
      JavaClassInfo.MethodType methodType = targets.iterator().next().second;
      tooltipText = InfraBundle.message("bean.method.tooltip.navigate.declaration", methodType.getName());
      if (methodType == JavaClassInfo.MethodType.FACTORY) {
        icon = Icons.Gutter.FactoryMethodBean;
      }
    }

    GutterIconBuilder<PsiElement> builder = GutterIconBuilder.create(icon);
    builder.setTargets(ContainerUtil.mapNotNull(targets, pair -> pair.getFirst()))
            .setCellRenderer(BeansPsiElementCellRenderer::new)
            .setPopupTitle(InfraBundle.message("bean.class.navigate.choose.class.title"))
            .setTooltipText(tooltipText);

    PsiIdentifier identifier = psiMethod.getNameIdentifier();
    if (identifier != null) {
      result.add(builder.createLineMarkerInfo(identifier));
    }

  }

  private static void addSpringBeanGutterIcon(Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement psiIdentifier, NotNullLazyValue<Collection<? extends BeanPointer<?>>> targets) {
    var builder = GutterIconBuilder.create(
            Icons.Gutter.SpringBean,
            NavigationGutterIconBuilderUtil.BEAN_POINTER_CONVERTOR,
            NavigationGutterIconBuilderUtil.AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
    );
    builder.setTargets(targets)
            .setEmptyPopupText(InfraBundle.message("gutter.navigate.no.matching.beans"))
            .setPopupTitle(InfraBundle.message("bean.class.navigate.choose.class.title"))
            .setCellRenderer(BeansPsiElementCellRenderer::new)
            .setTooltipText(InfraBundle.message("bean.class.tooltip.navigate.declaration"));
    result.add(builder.createGroupLineMarkerInfo(psiIdentifier));
  }

  private static List<CommonModelElement> getImportConfigurations(CommonInfraModel model, PsiClass psiClass) {
    Module module = model.getModule();
    if (module == null) {
      return Collections.emptyList();
    }
    else {
      List<CommonModelElement> result = new SmartList<>();
      if (model instanceof InfraModel) {
        InfraModelVisitors.visitRecursionAwareRelatedModels(
                model, InfraModelVisitorContext.context(p -> true,
                        (m, p) -> {
                          if (m instanceof LocalAnnotationModel localAnnotationModel) {
                            PsiClass configClass = localAnnotationModel.getConfig();
                            ContainerUtil.addIfNotNull(result, getImportConfiguration(configClass, psiClass));
                            for (PsiClass superClass : InheritanceUtil.getSuperClasses(configClass)) {
                              if (!"java.lang.Object".equals(superClass.getQualifiedName())) {
                                ContainerUtil.addIfNotNull(result, getImportConfiguration(superClass, psiClass));
                              }
                            }
                          }
                          return true;
                        })
        );
      }

      return result;
    }
  }

  private static CommonModelElement getImportConfiguration(PsiClass candidate, PsiClass importedPsiClass) {
    ContextImport contextImport = ContextImport.from(candidate);
    if (contextImport != null && contextImport.getImportedClasses().contains(importedPsiClass)) {
      return contextImport;
    }
    return null;
  }
}


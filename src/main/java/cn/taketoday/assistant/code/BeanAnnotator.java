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
import com.intellij.semantic.SemService;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.contexts.model.LocalAnnotationModel;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.contexts.model.visitors.CommonSpringModelVisitorContext;
import com.intellij.spring.contexts.model.visitors.SpringModelVisitors;
import com.intellij.spring.gutter.SpringBeansPsiElementCellRenderer;
import com.intellij.spring.gutter.groups.SpringGutterIconBuilder;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.JamSpringBeanPointer;
import com.intellij.spring.model.jam.javaConfig.SpringJavaBean;
import com.intellij.spring.model.jam.javaConfig.SpringOldJavaConfigurationUtil;
import com.intellij.spring.model.jam.stereotype.SpringImport;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;
import com.intellij.spring.model.jam.utils.SpringJamUtils;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.spring.model.xml.DomSpringBeanPointer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.util.CommonUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 20:26
 */
public class BeanAnnotator extends AbstractAnnotator {
  public static final List<String> bootAnnotations = List.of(
          "cn.taketoday.context.annotation.config.EnableAutoConfiguration"
  );

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
    return SpringApiIcons.SpringBean;
  }

  @Override
  protected void annotateClass(Collection<? super RelatedItemLineMarkerInfo<?>> result, UClass uClass, PsiElement identifier) {
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (psiClass == null || !CommonUtils.isBeanCandidateClass(psiClass)) {
      return;
    }
    JavaClassInfo info1 = JavaClassInfo.getSpringJavaClassInfo(psiClass);
    if (info1.isMappedDomBean()) {
      addSpringBeanGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
        List<DomSpringBeanPointer> domBeans = JavaClassInfo.getSpringJavaClassInfo(psiClass).getMappedDomBeans();
        domBeans.sort(SpringBeanPointer.DISPLAY_COMPARATOR);
        return domBeans;
      }));
    }
    else if (!info1.isStereotypeJavaBean() || AnnotationUtil.isAnnotated(psiClass, bootAnnotations, 8)) {

    }
    else {
      addJavaBeanGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
        SmartList<CommonModelElement> smartList = new SmartList<>();
        List<JamSpringBeanPointer> mappedBeans = JavaClassInfo.getSpringJavaClassInfo(psiClass).getStereotypeMappedBeans();
        for (JamSpringBeanPointer mappedBean : mappedBeans) {
          JamPsiMemberSpringBean<?> bean = mappedBean.getSpringBean();
          if ((bean instanceof SpringJavaBean) || ((bean instanceof SpringStereotypeElement) && !psiClass.isEquivalentTo(bean.getPsiElement()))) {
            smartList.add(bean);
          }
        }
        CommonSpringModel model = SpringModelUtils.getInstance().getPsiClassSpringModel(psiClass);
        Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
        ContainerUtil.addAllNotNull(smartList, SpringJamUtils.getInstance().findStereotypeConfigurationBeans(model, mappedBeans, module));
        ContainerUtil.addAllNotNull(smartList, getImportConfigurations(model, psiClass));
        return smartList;
      }), SpringApiIcons.Gutter.SpringJavaBean);
      PsiClassType classType = JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory().createType(psiClass);
      result.add(AutowiredAnnotator.getNavigateToAutowiredCandidatesBuilder(psiClass, classType)
              .createSpringGroupLineMarkerInfo(identifier)
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
        if (CommonUtils.isBeanCandidateClassInProject(psiClass)) {
          JavaClassInfo info = JavaClassInfo.getSpringJavaClassInfo(psiClass);
          List<SpringBeanPointer<?>> externalBeans = SpringOldJavaConfigurationUtil.findExternalBeans(method);

          if (!externalBeans.isEmpty()) {
            addSpringBeanGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
              List<SpringBeanPointer<?>> externalBeans2 = SpringOldJavaConfigurationUtil.findExternalBeans(method);
              externalBeans2.sort(SpringBeanPointer.DISPLAY_COMPARATOR);
              return externalBeans2;
            }));
          }

          Collection<Pair<PsiElement, JavaClassInfo.SpringMethodType>> methodTypes = info.getMethodTypes(method);
          if (!methodTypes.isEmpty()) {
            addMethodTypesGutterIcon(result, method, methodTypes);
          }
        }

      }
    }
  }

  private static void addMethodTypesGutterIcon(
          Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiMethod psiMethod,
          Collection<? extends Pair<PsiElement, JavaClassInfo.SpringMethodType>> targets) {
    String tooltipText = SpringBundle.message("spring.bean.methods.tooltip.navigate.declaration");
    Icon icon = SpringApiIcons.Gutter.SpringBeanMethod;
    if (targets.size() == 1) {
      JavaClassInfo.SpringMethodType methodType = targets.iterator().next().second;
      tooltipText = SpringBundle.message("spring.bean.method.tooltip.navigate.declaration", methodType.getName());
      if (methodType == JavaClassInfo.SpringMethodType.FACTORY) {
        icon = SpringApiIcons.Gutter.FactoryMethodBean;
      }
    }

    SpringGutterIconBuilder<PsiElement> builder = SpringGutterIconBuilder.createBuilder(icon);
    builder.setTargets(ContainerUtil.mapNotNull(targets, (pair) -> {
              return (PsiElement) pair.getFirst();
            }))
            .setCellRenderer(SpringBeansPsiElementCellRenderer::new)
            .setPopupTitle(SpringBundle.message("spring.bean.class.navigate.choose.class.title"))
            .setTooltipText(tooltipText);

    PsiIdentifier identifier = psiMethod.getNameIdentifier();
    if (identifier != null) {
      result.add(builder.createLineMarkerInfo(identifier));
    }

  }

  private static void addSpringBeanGutterIcon(Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement psiIdentifier, NotNullLazyValue<Collection<? extends SpringBeanPointer<?>>> targets) {
    var builder = SpringGutterIconBuilder.createBuilder(
            SpringApiIcons.Gutter.SpringBean,
            NavigationGutterIconBuilderUtil.BEAN_POINTER_CONVERTOR,
            NavigationGutterIconBuilderUtil.AUTOWIRED_BEAN_POINTER_GOTO_PROVIDER
    );
    builder.setTargets(targets)
            .setEmptyPopupText(SpringBundle.message("gutter.navigate.no.matching.beans"))
            .setPopupTitle(SpringBundle.message("spring.bean.class.navigate.choose.class.title"))
            .setCellRenderer(SpringBeansPsiElementCellRenderer::new)
            .setTooltipText(SpringBundle.message("spring.bean.class.tooltip.navigate.declaration"));
    result.add(builder.createSpringGroupLineMarkerInfo(psiIdentifier));
  }

  private static List<CommonModelElement> getImportConfigurations(CommonSpringModel model, PsiClass psiClass) {
    Module module = model.getModule();
    if (module == null) {
      return Collections.emptyList();
    }
    else {
      List<CommonModelElement> result = new SmartList<>();
      if (model instanceof SpringModel) {
        SpringModelVisitors.visitRecursionAwareRelatedModels(
                model, CommonSpringModelVisitorContext.context(p -> true,
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
    SpringImport springImport = SemService.getSemService(candidate.getProject())
            .getSemElement(SpringImport.IMPORT_JAM_KEY, candidate);
    if (springImport != null && springImport.getImportedClasses().contains(importedPsiClass)) {
      return springImport;
    }
    return null;
  }
}


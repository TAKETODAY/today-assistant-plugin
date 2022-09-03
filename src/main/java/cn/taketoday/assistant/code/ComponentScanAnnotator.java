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
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UAnnotationUtils;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Icon;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.beans.stereotype.ComponentScan;
import cn.taketoday.assistant.beans.stereotype.ComponentScans;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.jam.stereotype.AbstractComponentScan;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.assistant.service.InfraJamService;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 20:50
 */
public class ComponentScanAnnotator extends AbstractInfraAnnotator {

  @Override
  public String getId() {
    return "ComponentScanAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("core.component.scan.annotator.name");
  }

  @Override
  public Icon getIcon() {
    return Icons.Gutter.SpringScan;
  }

  @Override
  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UElement uAnnotation = UAnnotationUtils.getUParentForAnnotationIdentifier(psiElement);
    if (uAnnotation instanceof UAnnotation element) {
      annotateComponentScan(element, result);
      annotateComponentScans(element, result);
    }
  }

  private static boolean isComponentScanAnno(PsiAnnotation psiAnnotation) {
    if (!isClassLevelAnnotation(psiAnnotation)) {
      return false;
    }
    if (AnnotationConstant.COMPONENT_SCAN.equals(psiAnnotation.getQualifiedName())) {
      return true;
    }
    PsiClass psiClass = PsiTreeUtil.getParentOfType(psiAnnotation, PsiClass.class);
    return psiClass != null && JamService.getJamService(psiAnnotation.getProject()).getJamElement(AbstractComponentScan.COMPONENT_SCAN_JAM_KEY, psiClass) != null;
  }

  private static void annotateComponentScans(UAnnotation uAnnotation, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiAnnotation psiAnnotation = UElementKt.getAsJavaPsiElement(uAnnotation, PsiAnnotation.class);
    if (psiAnnotation != null) {
      UDeclaration uDeclaration = UastUtils.getParentOfType(uAnnotation, UDeclaration.class);
      PsiClass psiClass = UElementKt.getAsJavaPsiElement(uDeclaration, PsiClass.class);
      if (psiClass != null) {
        PsiElement identifier = UAnnotationKt.getNamePsiElement(uAnnotation);
        if (identifier != null) {
          JamService service = JamService.getJamService(psiClass.getProject());
          ComponentScans componentScans = service.getJamElement(psiClass, ComponentScans.META);
          if (componentScans != null) {
            for (ComponentScan componentScan : componentScans.getComponentScans()) {
              if (psiAnnotation.equals(componentScan.getAnnotation())) {
                annotateSpringBeansPackagesScan(result, identifier, componentScan);
              }
            }
          }

        }
      }
    }
  }

  private static void annotateComponentScan(UAnnotation uAnnotation, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiAnnotation element = UElementKt.getAsJavaPsiElement(uAnnotation, PsiAnnotation.class);
    if (element != null && isComponentScanAnno(element)) {
      UDeclaration uDeclaration = UastUtils.getParentOfType(uAnnotation, UDeclaration.class);
      PsiClass owner = UElementKt.getAsJavaPsiElement(uDeclaration, PsiClass.class);
      PsiElement identifier = UAnnotationKt.getNamePsiElement(uAnnotation);
      if (identifier != null && owner != null) {
        List<? extends InfraBeansPackagesScan> allScans = InfraJamService.of().getBeansPackagesScan(owner);
        List<AbstractComponentScan> scans = allScans.stream().filter(scan -> {
          return (scan instanceof AbstractComponentScan) && element.equals(((AbstractComponentScan) scan).getAnnotation());
        }).map(scan2 -> {
          return (AbstractComponentScan) scan2;
        }).collect(Collectors.toList());
        AbstractComponentScan scan3 = ContainerUtil.getFirstItem(scans);
        PsiAnnotation psiAnnotation = scan3 == null ? null : scan3.getAnnotation();
        if (psiAnnotation != null) {
          annotateSpringBeansPackagesScan(result, identifier, psiAnnotation, scans);
        }
      }
    }
  }

  private static boolean isClassLevelAnnotation(PsiAnnotation psiElement) {
    PsiElement parent = psiElement.getParent();
    if (parent == null) {
      return false;
    }
    return parent.getParent() instanceof PsiClass;
  }

  private static void annotateSpringBeansPackagesScan(
          Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement identifier,
          AbstractComponentScan scan) {
    PsiAnnotation psiAnnotation = scan.getAnnotation();
    if (psiAnnotation == null) {
      return;
    }
    annotateSpringBeansPackagesScan(result, identifier, psiAnnotation, new SmartList<>(scan));
  }

  private static void annotateSpringBeansPackagesScan(
          Collection<? super RelatedItemLineMarkerInfo<?>> result,
          PsiElement identifier, PsiAnnotation psiAnnotation, List<? extends AbstractComponentScan> scans) {
    addJavaBeanGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
      Module module = ModuleUtilCore.findModuleForPsiElement(psiAnnotation);
      if (module == null) {
        return Collections.emptySet();
      }
      Set<CommonInfraBean> scannedElements = scans.stream().flatMap(scan -> {
        return scan.getScannedElements(module).stream();
      }).collect(Collectors.toSet());
      List<BeanPointer<?>> beans = new ArrayList<>(InfraBeanService.of().mapBeans(scannedElements));
      beans.sort(BeanPointer.DISPLAY_COMPARATOR);
      return ContainerUtil.map(beans, BeanPointer.TO_BEAN);
    }), Icons.Gutter.SpringScan);
  }
}


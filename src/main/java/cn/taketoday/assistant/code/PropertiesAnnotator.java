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
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.gutter.BeansPsiElementCellRenderer;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.util.InfraUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 20:26
 */
public final class PropertiesAnnotator extends AbstractInfraAnnotator {

  @Override
  public String getId() {
    return "PropertiesAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("core.properties.annotator.name");
  }

  @Override
  public Icon getIcon() {
    return Icons.Gutter.SpringProperty;
  }

  @Override
  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UClass uClass;
    PsiClass psiClass;
    UElement element = UastUtils.getUParentForIdentifier(psiElement);
    if (!(element instanceof UClass) || (psiClass = UElementKt.getAsJavaPsiElement((uClass = (UClass) element), PsiClass.class)) == null || !InfraUtils.isBeanCandidateClass(psiClass)) {
      return;
    }
    annotateClass(result, uClass);
    for (UMethod method : uClass.getMethods()) {
      annotateMethod(method, result);
    }
  }

  private static void addPropertiesGutterIcon(Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement psiIdentifier, NotNullLazyValue<Collection<? extends DomElement>> targets) {
    var builder = GutterIconBuilder.create(
            Icons.Gutter.SpringProperty,
            NavigationGutterIconBuilder.DEFAULT_DOM_CONVERTOR,
            NavigationGutterIconBuilder.DOM_GOTO_RELATED_ITEM_PROVIDER
    );
    builder.setTargets(targets)
            .setCellRenderer(BeansPsiElementCellRenderer::new)
            .setPopupTitle(InfraBundle.message("bean.property.navigate.choose.class.title"))
            .setTooltipText(InfraBundle.message("bean.property.tooltip.navigate.declaration"));
    result.add(builder.createRelatedMergeableLineMarkerInfo(psiIdentifier));
  }

  @Override
  protected void annotateClass(Collection<? super RelatedItemLineMarkerInfo<?>> result, UClass uClass, PsiElement identifier) {
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (psiClass != null) {
      Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
      if (InfraUtils.isInfraEnabledModule(module)) {
        JavaClassInfo info = JavaClassInfo.from(psiClass);
        if (info.isMappedDomBean() || info.isStereotypeJavaBean()) {
          return;
        }
        annotatePsiClassSpringPropertyValues(
                result, psiClass, identifier,
                InfraModelVisitorUtils.getConfigFiles(InfraModelService.of().getModel(psiClass)));
      }

    }
  }

  @Override
  protected void annotateMethod(UMethod umethod, PsiElement identifier, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiClass psiClass;
    PsiMethod method = UElementKt.getAsJavaPsiElement(umethod, PsiMethod.class);
    if (method == null || (psiClass = method.getContainingClass()) == null) {
      return;
    }
    JavaClassInfo info = JavaClassInfo.from(psiClass);
    if (PropertyUtilBase.isSimplePropertySetter(method) && info.isMappedProperty(method)) {
      addPropertiesGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
        String propertyName = PropertyUtilBase.getPropertyNameBySetter(method);
        return JavaClassInfo.from(psiClass).getMappedProperties(propertyName);
      }));
    }
  }

  private static void annotatePsiClassSpringPropertyValues(
          Collection<? super RelatedItemLineMarkerInfo<?>> result,
          PsiClass psiClass, PsiElement identifier, Set<? extends PsiFile> xmlConfigFiles) {
    if (!DumbService.isDumb(psiClass.getProject()) && !xmlConfigFiles.isEmpty()) {
      List<InfraProperty> values = Collections.synchronizedList(new ArrayList());
      List<VirtualFile> springFiles = ContainerUtil.mapNotNull(xmlConfigFiles, psiFile -> {
        if (psiFile instanceof XmlFile) {
          return psiFile.getVirtualFile();
        }
        return null;
      });
      ReferencesSearch.search(psiClass, GlobalSearchScope.filesScope(psiClass.getProject(), springFiles)).forEach(psiReference -> {
        DomElement domElement;
        InfraProperty value;
        PsiElement element = psiReference.getElement();
        if ((element instanceof XmlElement) && (domElement = DomUtil.getDomElement(element)) != null && !isAnonymousBeanClass(domElement) && (value = domElement.getParentOfType(
                InfraProperty.class, false)) != null) {
          values.add(value);
          return true;
        }
        return true;
      });
      if (!values.isEmpty()) {
        addPropertiesGutterIcon(result, identifier, NotNullLazyValue.lazy(() -> {
          return values;
        }));
      }
    }
  }

  private static boolean isAnonymousBeanClass(DomElement domElement) {
    GenericAttributeValue genericAttributeValue = domElement.getParentOfType(GenericAttributeValue.class, false);
    if (genericAttributeValue != null && "class".equals(genericAttributeValue.getXmlElementName())) {
      return genericAttributeValue.getParent() instanceof InfraBean;
    }
    return false;
  }
}
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
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.contexts.model.LocalModel;
import com.intellij.spring.model.jam.testContexts.ContextConfiguration;
import com.intellij.spring.model.jam.testContexts.SpringContextConfiguration;
import com.intellij.spring.model.jam.testContexts.SpringContextHierarchy;
import com.intellij.spring.model.jam.testContexts.SpringTestContextUtil;
import com.intellij.spring.model.jam.testContexts.SpringTestingImplicitContextsProvider;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.util.CommonUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 17:24
 */
public class TestConfigurationAnnotator extends AbstractInfraAnnotator {

  @Override
  public String getId() {
    return "TestConfigurationAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("core.test.configuration.annotator.name");
  }

  @Override
  public Icon getIcon() {
    return Icons.Today;
  }

  @Override
  protected void annotateClass(Collection<? super RelatedItemLineMarkerInfo<?>> result, @NotNull UClass uClass, @NotNull PsiElement identifier) {
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (psiClass != null) {
      if (CommonUtils.isBeanCandidateClass(psiClass)) {
        if (SpringTestContextUtil.getInstance().isTestContextConfigurationClass(psiClass)) {
          this.addTestConfigurationGutter(result, uClass);
        }
      }
    }
  }

  private void addTestConfigurationGutter(Collection<? super RelatedItemLineMarkerInfo<?>> result, UClass uClass) {
    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (psiClass != null) {
      JamService service = JamService.getJamService(psiClass.getProject());
      ContextConfiguration contextConfiguration = service.getJamElement(ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY, psiClass);
      if (contextConfiguration != null) {
        this.annotateContextConfiguration(result, contextConfiguration, uClass);
      }

      SpringContextHierarchy hierarchy = (SpringContextHierarchy) service.getJamElement(psiClass, new JamMemberMeta[] { SpringContextHierarchy.META });
      if (hierarchy != null) {
        for (SpringContextConfiguration configuration : hierarchy.getContextConfigurations()) {
          this.annotateContextConfiguration(result, configuration, uClass);
        }
      }

    }
  }

  private void annotateContextConfiguration(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result, @NotNull ContextConfiguration contextConfiguration, @NotNull UClass uClass) {

    PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if (psiClass != null) {
      PsiAnnotation annotation = contextConfiguration.getAnnotation();
      if (annotation != null) {
        Set<XmlFile> xmlContexts = new LinkedHashSet();
        Set<PsiClass> javaContexts = new LinkedHashSet();
        SpringTestContextUtil.getInstance().discoverConfigFiles(contextConfiguration, xmlContexts, javaContexts, psiClass);
        Set<PsiElement> toNavigate = new LinkedHashSet();
        toNavigate.addAll(xmlContexts);
        toNavigate.addAll(javaContexts);
        Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
        if (module != null) {
          Set<String> annotationActiveProfiles = ContainerUtil.newLinkedHashSet("_DEFAULT_TEST_PROFILE_NAME_");

          for (SpringTestingImplicitContextsProvider provider : SpringTestingImplicitContextsProvider.EP_NAME.getExtensionList()) {
            for (CommonSpringModel model : provider.getModels(module, contextConfiguration, annotationActiveProfiles)) {
              if (model instanceof LocalModel localModel) {
                toNavigate.add(localModel.getConfig());
              }
            }
          }
        }

        GutterIconBuilder<PsiElement> builder = GutterIconBuilder.create(SpringApiIcons.Gutter.Spring);
        builder.setTargets(toNavigate).setPopupTitle(SpringBundle.message("spring.app.context.to.navigate"))
                .setTooltipText(SpringBundle.message("spring.app.context.navigate.tooltip"));
        PsiElement identifier = UAnnotationKt.getNamePsiElement(UastContextKt.toUElement(annotation, UAnnotation.class));
        if (identifier != null) {
          result.add(builder.createRelatedMergeableLineMarkerInfo(identifier));
        }
      }

    }
  }
}

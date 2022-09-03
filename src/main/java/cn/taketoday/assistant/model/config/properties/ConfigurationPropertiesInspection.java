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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.DataManager;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.application.metadata.InfraConfigKetPathBeanPropertyResolver;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

import static cn.taketoday.assistant.InfraAppBundle.message;

public final class ConfigurationPropertiesInspection extends AbstractInfraLocalInspection {
  private final Pattern CANONICAL_PREFIX_PATTERN = Pattern.compile("[a-z]([a-z]|-[a-z]|\\d|\\.[a-z])*");

  @Nullable
  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    ConfigurationProperties configurationProperties;
    PsiElement nameIdentifier = UElementKt.getSourcePsiElement(uClass.getUastAnchor());
    if (nameIdentifier != null) {
      PsiClass javaPsi = uClass.getJavaPsi();
      if (!isFrameworkEnabled(javaPsi)
              || !InfraUtils.isBeanCandidateClass(javaPsi)
              || (configurationProperties = ConfigurationProperties.CLASS_META.getJamElement(javaPsi)) == null) {
        return null;
      }
      return inspectConfigurationProperties(manager, nameIdentifier, isOnTheFly, configurationProperties);
    }
    return null;
  }

  @Nullable
  public ProblemDescriptor[] checkMethod(UMethod method, InspectionManager manager, boolean isOnTheFly) {
    PsiElement nameIdentifier;
    ConfigurationProperties.Method configurationProperties;
    if ((method.getReturnType() instanceof PsiClassType) && (nameIdentifier = UElementKt.getSourcePsiElement(method.getUastAnchor())) != null) {
      PsiMethod javaPsi = method.getJavaPsi();
      if (!isFrameworkEnabled(javaPsi) || (configurationProperties = ConfigurationProperties.Method.METHOD_META.getJamElement(javaPsi)) == null) {
        return null;
      }
      return inspectConfigurationProperties(manager, nameIdentifier, isOnTheFly, configurationProperties);
    }
    return null;
  }

  private ProblemDescriptor[] inspectConfigurationProperties(InspectionManager manager, PsiElement nameIdentifier, boolean isOnTheFly, ConfigurationProperties configurationProperties) {
    ProblemsHolder holder = new ProblemsHolder(manager, nameIdentifier.getContainingFile(), isOnTheFly);
    JamStringAttributeElement<String> valueOrPrefixAttribute = configurationProperties.getValueOrPrefixAttribute();
    if (valueOrPrefixAttribute.getPsiElement() == null) {
      holder.registerProblem(getProblemElement(configurationProperties),
              message("configuration.properties.prefix.must.be.specified"),
              ProblemHighlightType.WARNING);
      return holder.getResultsArray();
    }
    PsiFile containingFile = nameIdentifier.getContainingFile();
    SearchScope useScope = containingFile.getUseScope();
    GlobalSearchScope useScope2 = (GlobalSearchScope) useScope;
    JamService jamService = JamService.getJamService(holder.getProject());
    String value = configurationProperties.getValueOrPrefix();
    if (value == null || StringsKt.isBlank(value)) {
      holder.registerProblem(getPsiElement(valueOrPrefixAttribute), message("configuration.properties.prefix.must.be.non.empty"), ProblemHighlightType.WARNING
      );
    }
    else {
      if (!this.CANONICAL_PREFIX_PATTERN.matcher(value).matches()) {
        holder.registerProblem(getPsiElement(valueOrPrefixAttribute), message("configuration.properties.prefix.must.be.in.canonical.form"));
      }
      List<ConfigurationProperties> duplicates = findDuplicates(jamService, useScope2, configurationProperties, value);
      if (!duplicates.isEmpty()) {
        holder.registerProblem(getPsiElement(valueOrPrefixAttribute), message("configuration.properties.duplicated.prefix"),
                new ConfigurationPropertiesInspection$inspectConfigurationProperties$1(this, jamService, useScope2, configurationProperties, value));
      }
    }
    checkConfigurationPropertiesRegistered(holder, jamService, useScope2, configurationProperties);
    return holder.getResultsArray();
  }

  public List<ConfigurationProperties> findDuplicates(JamService jamService,
          GlobalSearchScope useScope, ConfigurationProperties configurationProperties, @Nullable String value) {

    List<ConfigurationProperties> allConfigurationProperties = jamService.getJamClassElements(ConfigurationProperties.CLASS_META, InfraClassesConstants.CONFIGURATION_PROPERTIES, useScope);
    List<ConfigurationProperties> methodElements = jamService.getJamMethodElements(ConfigurationProperties.Method.METHOD_META, InfraClassesConstants.CONFIGURATION_PROPERTIES, useScope);

    allConfigurationProperties.addAll(methodElements);
    allConfigurationProperties.remove(configurationProperties);

    List<ConfigurationProperties> ret;
    switch (allConfigurationProperties.size()) {
      case 0:
        return CollectionsKt.emptyList();
      case 1:
        ret = new SmartList<>();
        break;
      default:
        ret = new ArrayList<>();
        break;
    }

    for (ConfigurationProperties it : allConfigurationProperties) {
      if (Objects.equals(value, it.getValueOrPrefix())) {
        ret.add(it);
      }
    }
    return ret;
  }

  private void checkConfigurationPropertiesRegistered(ProblemsHolder holder, JamService jamService, GlobalSearchScope useScope, ConfigurationProperties configurationProperties) {
    Module module;
    boolean z;
    String message = null;
    PsiElement identifier;
    if (!(configurationProperties instanceof ConfigurationProperties.Method) && (module = ModuleUtilCore.findModuleForFile(holder.getFile())) != null) {
      PsiElement annotation = configurationProperties.getAnnotation();
      Intrinsics.checkNotNull(annotation);
      UElement uElement = UastContextKt.toUElement(annotation, UAnnotation.class);
      Intrinsics.checkNotNull(uElement);
      UClass containingUClass = UastUtils.getContainingUClass(uElement);

      PsiClass containingClass = containingUClass.getJavaPsi();
      if (InfraUtils.isStereotypeComponentOrMeta(containingClass)) {
        PsiMethod bindingConstructor = InfraConfigKetPathBeanPropertyResolver.getBindingConstructor(containingClass, module, null);
        if (bindingConstructor != null) {
          UElement uastAnchor = containingUClass.getUastAnchor();
          if (uastAnchor == null || (identifier = uastAnchor.getSourcePsi()) == null) {
            return;
          }
          holder.registerProblem(identifier, message("configuration.properties.constructor.binding.component"));
          return;
        }
        return;
      }
      List<EnableConfigurationProperties> enableConfigurationProperties =
              jamService.getJamClassElements(EnableConfigurationProperties.CLASS_META,
                      InfraClassesConstants.ENABLE_CONFIGURATION_PROPERTIES, useScope);

      if (enableConfigurationProperties != null && !enableConfigurationProperties.isEmpty()) {
        for (EnableConfigurationProperties properties : enableConfigurationProperties) {
          if (properties.getValue().contains(containingClass)) {
            return;
          }
        }
      }

      ModelSearchParameters.BeanClass params = ModelSearchParameters.byClass(containingClass);
      for (CommonInfraModel infraModel : InfraManager.from(containingClass.getProject()).getAllModels(module)) {
        if (InfraModelSearchers.doesBeanExist(infraModel, params)) {
          return;
        }
      }
      message = message("configuration.properties.not.registered", "@" + StringUtil.getShortName(InfraClassesConstants.ENABLE_CONFIGURATION_PROPERTIES),
              "@" + StringUtil.getShortName(InfraClassesConstants.CONFIGURATION_PROPERTIES_SCAN));
    }
    String description = message;
    holder.registerProblem(getProblemElement(configurationProperties), description);

  }

  private boolean isFrameworkEnabled(PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module != null) {
      return InfraUtils.hasFacet(module)
              && InfraLibraryUtil.hasFrameworkLibrary(module);
    }
    return false;
  }

  public PsiElement getPsiElement(JamStringAttributeElement<String> jamStringAttributeElement) {
    return UElementKt.getSourcePsiElement(UastContextKt.toUElement(jamStringAttributeElement.getPsiElement()));
  }

  private PsiElement getProblemElement(ConfigurationProperties configurationProperties) {
    PsiElement annotation = configurationProperties.getAnnotation();
    UAnnotation annotationElement = UastContextKt.toUElement(annotation, UAnnotation.class);
    return UAnnotationKt.getNamePsiElement(annotationElement);
  }
}

final class ConfigurationPropertiesInspection$inspectConfigurationProperties$1 implements LocalQuickFix {
  final ConfigurationPropertiesInspection this$0;
  final JamService jamService;
  final GlobalSearchScope useScope;
  final ConfigurationProperties configurationProperties;
  final String value;

  ConfigurationPropertiesInspection$inspectConfigurationProperties$1(
          ConfigurationPropertiesInspection inspection, JamService jamService,
          GlobalSearchScope searchScope, ConfigurationProperties configurationProperties, String value) {
    this.this$0 = inspection;
    this.jamService = jamService;
    this.useScope = searchScope;
    this.configurationProperties = configurationProperties;
    this.value = value;
  }

  public boolean startInWriteAction() {
    return false;
  }

  public String getFamilyName() {
    return InfraAppBundle.message("configuration.properties.show.duplicates");
  }

  public void applyFix(Project project, ProblemDescriptor descriptor) {
    List<ConfigurationProperties> properties = this$0.findDuplicates(jamService, this.useScope, this.configurationProperties, this.value);
    ArrayList<PsiElement> objects = new ArrayList<>(Math.max(properties.size(), 10));
    for (ConfigurationProperties it : properties) {

      JamStringAttributeElement<String> valueOrPrefixAttribute = it.getValueOrPrefixAttribute();
      PsiElement psiElement = this.this$0.getPsiElement(valueOrPrefixAttribute);
      objects.add(psiElement);
    }
    PsiElement[] findDuplicates = objects.toArray(new PsiElement[0]);
    DataManager dataManager = DataManager.getInstance();

    dataManager.getDataContextFromFocusAsync().onSuccess(context -> {
      JBPopup psiElementPopup;
      if (findDuplicates.length == 0) {
        psiElementPopup = JBPopupFactory.getInstance()
                .createMessage(InfraAppBundle.message("configuration.properties.no.duplicates.found", value));
      }
      else {
        psiElementPopup = NavigationUtil.getPsiElementPopup(findDuplicates,
                InfraAppBundle.message("configuration.properties.show.duplicates.for.prefix", value));
      }
      psiElementPopup.showInBestPositionFor(context);
    });
  }
}
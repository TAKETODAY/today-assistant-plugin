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

package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.codeInsight.daemon.quickFix.CreateFilePathFix;
import com.intellij.codeInsight.daemon.quickFix.NewFileLocation;
import com.intellij.codeInsight.daemon.quickFix.TargetDirectory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileTargetContext;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReferenceHelper;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.highlighting.jam.BeanPointerResolveInspection;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSql;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.InfraTestingSqlConfig;
import cn.taketoday.assistant.model.jam.utils.InfraResourceLocationsUtil;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class ContextConfigurationInspection extends AbstractInfraLocalInspection {

  public ContextConfigurationInspection() {
    super(UClass.class);
  }

  @Nullable
  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement nameIdentifier;
    Intrinsics.checkNotNullParameter(uClass, "uClass");
    Intrinsics.checkNotNullParameter(manager, "manager");
    UElement uastAnchor = uClass.getUastAnchor();
    if (uastAnchor == null || (nameIdentifier = uastAnchor.getSourcePsi()) == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiClass javaPsi = uClass.getJavaPsi();
    if (ModuleUtilCore.findModuleForPsiElement(javaPsi) == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    ProblemsHolder holder = new ProblemsHolder(manager, nameIdentifier.getContainingFile(), isOnTheFly);
    JamService jamService = JamService.getJamService(javaPsi.getProject());
    ContextConfiguration contextConfiguration = jamService.getJamElement(ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY, javaPsi);
    if (contextConfiguration != null) {
      checkContextConfiguration(contextConfiguration, holder);
    }
    TransactionConfiguration transactionConfiguration = TransactionConfiguration.META.getJamElement(javaPsi);
    if (transactionConfiguration != null) {
      BeanPointerResolveInspection.checkBeanPointerResolve(holder, transactionConfiguration.getTransactionManagerAttributeElement());
    }
    InfraTestingSqlConfig sqlConfig = getTestingSqlConfig(javaPsi);
    if (sqlConfig != null) {
      BeanPointerResolveInspection.checkBeanPointerResolve(holder, sqlConfig.getTransactionManagerElement());
      BeanPointerResolveInspection.checkBeanPointerResolve(holder, sqlConfig.getDatasourceAttrElement());
    }
    return holder.getResultsArray();
  }

  private InfraTestingSqlConfig getTestingSqlConfig(PsiClass psiClass) {
    InfraTestingSqlConfig sqlConfig = InfraTestingSqlConfig.META.getJamElement(psiClass);
    if (sqlConfig != null) {
      return sqlConfig;
    }
    InfraTestingSql testingSql = InfraTestingSql.CLASS_META.getJamElement(psiClass);
    if (testingSql == null) {
      return null;
    }
    return testingSql.getSqlConfig();
  }

  private void checkContextConfiguration(ContextConfiguration contextConfiguration, ProblemsHolder holder) {
    if (isLocationsAttributesDefinedInHierarchy(contextConfiguration)) {
      for (JamStringAttributeElement attributeElement : contextConfiguration.getLocationElements()) {
        InfraResourceLocationsUtil infraResourceLocationsUtil = InfraResourceLocationsUtil.INSTANCE;
        Intrinsics.checkNotNullExpressionValue(attributeElement, "attributeElement");
        infraResourceLocationsUtil.checkResourceLocation(holder, attributeElement);
      }
    }
    else if (InfraTestContextUtilImpl.isGenericXmlContextLoader(contextConfiguration)) {
      XmlFile xmlFile = InfraTestContextUtilImpl.getDefaultLocation(contextConfiguration);
      if (xmlFile == null) {
        final PsiElement annotation = contextConfiguration.getAnnotation();
        Intrinsics.checkNotNull(annotation);
        Intrinsics.checkNotNullExpressionValue(annotation, "contextConfiguration.annotation!!");
        PsiFile containingFile = annotation.getContainingFile();
        Intrinsics.checkNotNullExpressionValue(containingFile, "annotation.containingFile");
        PsiDirectory directory = containingFile.getContainingDirectory();
        String newContextFileName = InfraTestContextUtilImpl.getDefaultAppContextName(contextConfiguration);
        LocalQuickFix fix = null;
        if (directory != null) {
          Project project = holder.getProject();
          Intrinsics.checkNotNullExpressionValue(project, "holder.project");
          PsiFile containingFile2 = annotation.getContainingFile();
          Intrinsics.checkNotNullExpressionValue(containingFile2, "annotation.containingFile");
          List<TargetDirectory> directories = findContextXmlTargets(project, containingFile2);
          fix = new CreateFilePathFix(annotation, new NewFileLocation(directories, newContextFileName), () -> {
            Module module = ModuleUtilCore.findModuleForPsiElement(annotation);
            FileTemplate springXmlTemplate = InfraUtils.getXmlTemplate(module);
            Intrinsics.checkNotNullExpressionValue(springXmlTemplate, "CommonUtils.getSpringXmlTemplate(module)");
            return springXmlTemplate.getText();
          });
        }
        holder.registerProblem(annotation, InfraBundle.message("ContextConfigurationInspection.cannot.find.default.app.context", newContextFileName),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fix);
      }
    }
    if (InfraTestContextUtilImpl.isAnnotationConfigLoader(contextConfiguration)) {
      var processor = new CommonProcessors.FindProcessor<ContextConfiguration>() {
        public boolean accept(ContextConfiguration cc) {
          Intrinsics.checkNotNullParameter(cc, "cc");
          return cc.getConfigurationClasses().size() != 0;
        }
      };
      processConfigurationsHierarchy(contextConfiguration, processor);
      if (!processor.isFound() && !hasInnerConfigurations(contextConfiguration)) {
        PsiElement psiElement = contextConfiguration.getAnnotation();
        final PsiClass aClass = contextConfiguration.getPsiElement();
        if (aClass == null) {
          return;
        }
        LocalQuickFix localQuickFix = new LocalQuickFix() {

          public String getFamilyName() {
            return InfraBundle.message("ContextConfigurationInspection.create.nested.configuration.class");
          }

          public void applyFix(Project project2, ProblemDescriptor descriptor) {
            Intrinsics.checkNotNullParameter(project2, "project");
            Intrinsics.checkNotNullParameter(descriptor, "descriptor");
            PsiClass existedClass = aClass.findInnerClassByName(InfraTestContextUtilImpl.CONTEXT_CONFIGURATION_SUFFIX, false);
            if (existedClass != null) {
              PsiModifierList modifierList = existedClass.getModifierList();
              if (modifierList != null) {
                modifierList.addAnnotation(AnnotationConstant.CONFIGURATION);
                return;
              }
              return;
            }
            PsiClass createClass = JavaPsiFacade.getElementFactory(aClass.getProject())
                    .createClass(InfraTestContextUtilImpl.CONTEXT_CONFIGURATION_SUFFIX);
            Intrinsics.checkNotNullExpressionValue(createClass, "JavaPsiFacade.getElement…EXT_CONFIGURATION_SUFFIX)");
            PsiModifierList modifierList2 = createClass.getModifierList();
            if (modifierList2 != null) {
              modifierList2.setModifierProperty("public", true);
              modifierList2.setModifierProperty("static", true);
              modifierList2.addAnnotation(AnnotationConstant.CONFIGURATION);
            }
            aClass.add(createClass);
          }
        };
        String message = InfraBundle.message("ContextConfigurationInspection.cannot.find.default.app.context", aClass.getName() + InfraTestContextUtilImpl.CONTEXT_CONFIGURATION_SUFFIX);
        ProblemHighlightType problemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        LocalQuickFix[] localQuickFixArr = new LocalQuickFix[1];
        localQuickFixArr[0] = holder.isOnTheFly() ? localQuickFix : null;
        holder.registerProblem(psiElement, message, problemHighlightType, localQuickFixArr);
      }
    }
  }

  private List<TargetDirectory> findContextXmlTargets(Project project, PsiFile containingFile) {
    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile != null) {
      PsiFileReferenceHelper helper = PsiFileReferenceHelper.getInstance();
      if (helper.isMine(project, virtualFile)) {
        ArrayList directories = new ArrayList();
        Collection<FileTargetContext> targetContexts = helper.getTargetContexts(project, virtualFile, false);
        for (FileTargetContext tc : targetContexts) {
          Intrinsics.checkNotNullExpressionValue(tc, "tc");
          if (isTargetDirectory(tc)) {
            PsiFileSystemItem fileSystemItem = tc.getFileSystemItem();
            PsiManager manager = fileSystemItem.getManager();
            PsiFileSystemItem fileSystemItem2 = tc.getFileSystemItem();
            PsiDirectory directory = manager.findDirectory(fileSystemItem2.getVirtualFile());
            if (directory != null) {
              directories.add(new TargetDirectory(directory, tc.getPathToCreate()));
            }
          }
        }
        if (!directories.isEmpty()) {
          return directories;
        }
      }
    }
    return CollectionsKt.listOf(new TargetDirectory(containingFile.getContainingDirectory()));
  }

  private boolean isTargetDirectory(FileTargetContext tc) {
    PsiFileSystemItem context = tc.getFileSystemItem();
    Intrinsics.checkNotNullExpressionValue(context, "context");
    if (context.getVirtualFile() != null && context.isDirectory() && context.isValid()) {
      VirtualFile virtualFile = context.getVirtualFile();
      Intrinsics.checkNotNullExpressionValue(virtualFile, "context.virtualFile");
      return virtualFile.isInLocalFileSystem();
    }
    return false;
  }

  private boolean hasInnerConfigurations(ContextConfiguration configuration) {
    PsiElement[] allInnerClasses;
    PsiClass configurationClass = configuration.getPsiElement();
    if (configurationClass != null) {
      for (PsiClass psiElement : configurationClass.getAllInnerClasses()) {
        if (psiElement.hasModifierProperty("static")) {
          Intrinsics.checkNotNullExpressionValue(psiElement, "psiClass");
          if (JamService.getJamService(psiElement.getProject()).getJamElement(Configuration.JAM_KEY, psiElement) != null) {
            return true;
          }
        }
      }
      return false;
    }
    return false;
  }

  public boolean isLocationsAttributesDefined(ContextConfiguration contextConfiguration) {
    return contextConfiguration.hasLocationsAttribute() || contextConfiguration.hasValueAttribute();
  }

  private boolean isLocationsAttributesDefinedInHierarchy(ContextConfiguration contextConfiguration) {
    var processor = new CommonProcessors.FindProcessor<ContextConfiguration>() {
      public boolean accept(ContextConfiguration cc) {
        boolean isLocationsAttributesDefined;
        Intrinsics.checkNotNullParameter(cc, "cc");
        isLocationsAttributesDefined = ContextConfigurationInspection.this.isLocationsAttributesDefined(cc);
        return isLocationsAttributesDefined;
      }
    };
    processConfigurationsHierarchy(contextConfiguration, processor);
    return processor.isFound();
  }

  private boolean processConfigurationsHierarchy(ContextConfiguration contextConfiguration, Processor<ContextConfiguration> processor) {
    if (!processor.process(contextConfiguration)) {
      return false;
    }
    PsiClass psiClass = contextConfiguration.getPsiElement();
    if (psiClass != null) {
      JamService service = JamService.getJamService(psiClass.getProject());
      PsiClass superClass = psiClass.getSuperClass();
      while (true) {
        PsiClass superClass2 = superClass;
        if (superClass2 != null) {
          InfraContextConfiguration configuration = (InfraContextConfiguration) service.getJamElement(
                  superClass2, new JamMemberMeta[] { InfraContextConfiguration.META });
          if (configuration != null && !processor.process(configuration)) {
            return false;
          }
          superClass = superClass2.getSuperClass();
        }
        else {
          return true;
        }
      }
    }
    else {
      return true;
    }
  }
}

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
package cn.taketoday.assistant.model.jam.stereotype;

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.semantic.SemKey;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.context.model.ComponentScanPackagesModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.lang.Nullable;

/**
 * Inheritors should register with {@link #COMPONENT_SCAN_META_KEY} to get builtin support
 * for component-scan-like annotations (e.g. goto annotated elements, highlighting of unresolved package definitions).
 */
public abstract class AbstractComponentScan extends CommonModelElement.PsiBase implements JamElement, InfraBeansPackagesScan {
  protected static final String VALUE_ATTR_NAME = "value";
  protected static final String BASE_PACKAGES_ATTR_NAME = "basePackages";
  protected static final String BASE_PACKAGE_CLASSES_ATTR_NAME = "basePackageClasses";

  public static final SemKey<JamAnnotationMeta> COMPONENT_SCAN_META_KEY = SemKey.createKey("InfraComponentScan");
  public static final SemKey<AbstractComponentScan> COMPONENT_SCAN_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("InfraComponentScan");

  private final PsiClass myPsiElement;
  private final CachedValue<Map<Module, Set<CommonInfraBean>>> myScanned;

  public AbstractComponentScan(PsiClass psiElement) {
    myPsiElement = psiElement;
    myScanned = CachedValuesManager.getManager(psiElement.getProject()).createCachedValue(() -> {
      Map<Module, Set<CommonInfraBean>> map = ConcurrentFactoryMap.createMap(this::getScannedBeans);
      PsiFile file = myPsiElement.getContainingFile();
      return CachedValueProvider.Result.create(map, file == null ? new Object[] { ModificationTracker.EVER_CHANGED } : getScannedElementsDependencies(file));
    }, false);
  }

  @Override
  public final Set<CommonInfraBean> getScannedElements(Module module) {
    if (module.isDisposed())
      return Collections.emptySet();
    return myScanned.getValue().get(module);
  }

  public static Object[] getScannedElementsDependencies(PsiFile containingFile) {
    Object[] dependencies = InfraModificationTrackersManager.from(containingFile.getProject()).getOuterModelsDependencies();
    dependencies = ArrayUtil.append(dependencies, containingFile);
    return dependencies;
  }

  protected Set<CommonInfraBean> getScannedBeans(Module module) {
    final Collection<BeanPointer<?>> scannedComponents =
            ComponentScanPackagesModel.getScannedComponents(getPsiPackages(),
                    module,
                    null,
                    useDefaultFilters(),
                    getExcludeContextFilters(),
                    getIncludeContextFilters());
    return ContainerUtil.map2LinkedSet(scannedComponents, BeanPointer.TO_BEAN);
  }

  protected abstract PsiElementRef<PsiAnnotation> getAnnotationRef();

  @Nullable
  public PsiAnnotation getAnnotation() { return getAnnotationRef().getPsiElement(); }

  @Override
  public PsiClass getPsiElement() {
    return myPsiElement;
  }

  @Override

  public Set<PsiPackage> getPsiPackages() {
    Set<PsiPackage> definedPackages = getDefinedPackages();
    return definedPackages.isEmpty() ? getDefaultPsiPackages() : definedPackages;
  }

  public Set<PsiPackage> getDefinedPackages() {
    Set<PsiPackage> allPackages = new LinkedHashSet<>();

    for (JamStringAttributeMeta.Collection<Collection<PsiPackage>> packageMeta : getPackageJamAttributes()) {
      addPackages(allPackages, packageMeta);
    }
    addBasePackageClasses(allPackages);
    return allPackages;
  }

  private Set<PsiPackage> getDefaultPsiPackages() {
    PsiFile file = myPsiElement.getContainingFile();
    if (file instanceof PsiClassOwner) {
      final String packageName = ((PsiClassOwner) file).getPackageName();
      final PsiManager manager = myPsiElement.getManager();
      final PsiPackage psiPackage = JavaPsiFacade.getInstance(manager.getProject()).findPackage(packageName);
      if (psiPackage != null) {
        return Collections.singleton(psiPackage);
      }
    }
    return Collections.emptySet();
  }

  private void addBasePackageClasses(Set<PsiPackage> importedResources) {
    final List<JamClassAttributeElement> basePackageClasses = getBasePackageClassAttribute();
    for (JamClassAttributeElement bpc : basePackageClasses) {
      PsiClass psiClass = bpc.getValue();
      if (psiClass != null) {
        final PsiDirectory containingDirectory = psiClass.getContainingFile().getContainingDirectory();

        if (containingDirectory != null) {
          final PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(containingDirectory);
          if (psiPackage != null) {
            importedResources.add(psiPackage);
          }
        }
      }
    }
  }

  protected List<JamClassAttributeElement> getBasePackageClassAttribute() {
    final JamClassAttributeMeta.Collection meta = getBasePackageClassMeta();

    return meta != null ? meta.getJam(getAnnotationRef()) : Collections.emptyList();
  }

  @Nullable
  protected abstract JamClassAttributeMeta.Collection getBasePackageClassMeta();

  private void addPackages(Set<PsiPackage> importedResources,
          JamStringAttributeMeta.Collection<Collection<PsiPackage>> attrMeta) {
    for (JamStringAttributeElement<Collection<PsiPackage>> element : attrMeta.getJam(getAnnotationRef())) {
      if (element != null) {
        Collection<PsiPackage> psiPackages = element.getValue();
        if (psiPackages != null) {
          ContainerUtil.addAllNotNull(importedResources, psiPackages);
        }
      }
    }
  }

  protected abstract JamAnnotationMeta getAnnotationMeta();

  public abstract List<JamStringAttributeMeta.Collection<Collection<PsiPackage>>> getPackageJamAttributes();

  public boolean processPsiPackages(Processor<Pair<PsiPackage, ? extends PsiElement>> processor) {
    List<JamStringAttributeMeta.Collection<Collection<PsiPackage>>> packageJamAttributes = getPackageJamAttributes();
    List<JamClassAttributeElement> basePackageClasses = getBasePackageClassAttribute();

    int packageDefiningElementsCount = basePackageClasses.size();
    for (JamStringAttributeMeta.Collection<Collection<PsiPackage>> packageMeta : packageJamAttributes) {
      packageDefiningElementsCount += getAnnotationMeta().getAttribute(myPsiElement, packageMeta).size();
    }

    boolean useCurrentPackageForScan = packageDefiningElementsCount == 0;
    boolean useAnnotationAsElement = packageDefiningElementsCount == 1;
    PsiElement annotationElement = getAnnotationMeta().getAnnotation(myPsiElement);

    for (JamStringAttributeMeta.Collection<Collection<PsiPackage>> packageMeta : packageJamAttributes) {
      for (JamStringAttributeElement<Collection<PsiPackage>> element : getAnnotationMeta().getAttribute(myPsiElement, packageMeta)) {
        if (element != null) {
          Collection<PsiPackage> psiPackages = element.getValue();
          if (psiPackages != null) {
            for (PsiPackage psiPackage : psiPackages) {
              final PsiElement identifyingElement =
                      useAnnotationAsElement && annotationElement != null ? annotationElement : element.getPsiElement();
              if (!processor.process(Pair.create(psiPackage, identifyingElement == null ? psiPackage : identifyingElement))) {
                return false;
              }
            }
          }
        }
      }
    }

    for (JamClassAttributeElement bpc : basePackageClasses) {
      PsiClass psiClass = bpc.getValue();
      if (psiClass != null) {
        final PsiDirectory containingDirectory = psiClass.getContainingFile().getContainingDirectory();

        if (containingDirectory != null) {
          final PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(containingDirectory);
          if (psiPackage != null) {
            if (!processor.process(Pair.create(psiPackage, useAnnotationAsElement ? annotationElement : bpc.getPsiElement()))) {
              return false;
            }
          }
        }
      }
    }

    if (useCurrentPackageForScan) {
      PsiFile file = myPsiElement.getContainingFile();
      if (file instanceof PsiClassOwner) {
        final String packageName = ((PsiClassOwner) file).getPackageName();
        final PsiManager manager = myPsiElement.getManager();
        final PsiPackage psiPackage = JavaPsiFacade.getInstance(manager.getProject()).findPackage(packageName);
        if (psiPackage != null) {
          if (!processor.process(Pair.create(psiPackage, annotationElement))) {
            return false;
          }
        }
      }
    }

    return true;
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return getAnnotation();
  }
}

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

package cn.taketoday.assistant.model.actions.generate;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupFocusDegree;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.highlighting.xml.InfraConstructorArgResolveUtil;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraInjection;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;
import static cn.taketoday.assistant.InfraBundle.messagePointer;

public final class GenerateBeanDependenciesUtil {

  public static boolean acceptBean(InfraBean springBean, boolean isSetterDependency) {
    return !getCandidates(springBean, isSetterDependency).isEmpty();
  }

  public static boolean acceptPsiClass(PsiClass psiClass, boolean isSetterDependency) {
    CommonInfraModel model = InfraModelService.of().getModel(psiClass);
    return InfraModelSearchers.doesBeanExist(model, psiClass) && !getCandidates(model, psiClass, isSetterDependency).isEmpty();
  }

  public static List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> generateDependenciesFor(@Nullable CommonInfraModel model,
          @Nullable PsiClass psiClass, boolean isSetterDependency) {
    List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> createdProperties = new ArrayList<>();
    if (model != null && psiClass != null) {
      List<BeanPointer<?>> list = InfraModelSearchers.findBeans(model, ModelSearchParameters.byClass(psiClass));
      if (list.size() > 0) {
        for (BeanPointer pointer : list) {
          CommonInfraBean springBean = pointer.getBean();
          if ((springBean instanceof InfraBean) && acceptBean((InfraBean) springBean, isSetterDependency)) {
            if (ensureFileWritable((InfraBean) springBean)) {
              return generateDependenciesFor((InfraBean) springBean, isSetterDependency);
            }
            return new ArrayList<>();
          }
        }
      }
      else {
        List<BeanPointer<?>> beans = chooseDependentBeans(getCandidates(model, psiClass, isSetterDependency), psiClass.getProject(), isSetterDependency);
        if (beans.size() > 0) {
          return createBeanAndGenerateDependencies(psiClass, isSetterDependency, beans);
        }
      }
    }
    return createdProperties;
  }

  public static List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> createBeanAndGenerateDependencies(
          PsiClass psiClass, boolean isSetterDependency, List<? extends BeanPointer<?>> beans) {
    Object mo448getSpringBean = beans.get(0).getBean();
    InfraBean bean = null;
    if (mo448getSpringBean instanceof DomInfraBean domSpringBean) {
      bean = createSpringBean(domSpringBean.getParentOfType(Beans.class, false), psiClass);
    }
    if (bean == null) {
      return new ArrayList<>();
    }
    return generateDependencies(bean, beans, isSetterDependency);
  }

  @Nullable
  private static InfraBean createSpringBean(Beans parentBeans, PsiClass psiClass) {
    if (!ensureFileWritable(parentBeans)) {
      return null;
    }
    return WriteCommandAction.writeCommandAction(psiClass.getProject())
            .withName(message("model.actions.generate.beans"))
            .compute(() -> {
              InfraBean springBean = parentBeans.addBean();
              springBean.getClazz().setStringValue(psiClass.getQualifiedName());
              String[] strings = BeanCoreUtils.suggestBeanNames(springBean);
              springBean.getId().setStringValue(strings.length > 0 ? strings[0] : "");
              return springBean;
            });
  }

  public static boolean ensureFileWritable(DomElement domElement) {
    return ensureFileWritable(DomUtil.getFile(domElement).getVirtualFile(), domElement.getManager().getProject());
  }

  public static boolean ensureFileWritable(PsiClass psiClass) {
    return ensureFileWritable(psiClass.getContainingFile().getVirtualFile(), psiClass.getProject());
  }

  public static boolean ensureFileWritable(@Nullable VirtualFile virtualFile, Project project) {
    if (virtualFile != null && !virtualFile.isWritable()) {
      return ReadonlyStatusHandler.ensureFilesWritable(project, virtualFile);
    }
    return true;
  }

  public static List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> generateDependenciesFor(@Nullable InfraBean springBean,
          boolean isSetterDependency) {
    if (springBean == null || PsiTypesUtil.getPsiClass(springBean.getBeanType()) == null) {
      return Collections.emptyList();
    }
    Project project = springBean.getManager().getProject();
    List<BeanPointer<?>> dependencies = chooseDependentBeans(getCandidates(springBean, isSetterDependency), project, isSetterDependency);
    return generateDependencies(springBean, dependencies, isSetterDependency);
  }

  public static List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> generateDependencies(InfraBean springBean,
          List<? extends BeanPointer<?>> dependencies, boolean isSetterDependency) {
    Module module = springBean.getModule();
    return module == null ? Collections.emptyList() : WriteCommandAction.writeCommandAction(module.getProject()).compute(() -> {
      Pair<InfraInjection, InfraGenerateTemplatesHolder> createConstructorArg;
      List<Pair<InfraInjection, InfraGenerateTemplatesHolder>> springInjections = new ArrayList<>();
      CommonInfraModel model = InfraModelService.of().getModelByBean(springBean);
      for (BeanPointer<?> dependency : dependencies) {
        if (isSetterDependency) {
          createConstructorArg = createDependency(springBean, dependency, model);
        }
        else {
          createConstructorArg = createConstructorArg(springBean, dependency, model);
        }
        Pair<InfraInjection, InfraGenerateTemplatesHolder> pair = createConstructorArg;
        if (pair != null) {
          springInjections.add(pair);
        }
      }
      return springInjections;
    });
  }

  public static List<BeanPointer<?>> chooseDependentBeans(Set<InfraBeanClassMember> candidates, Project project, boolean setterDependency) {
    List<BeanPointer<?>> chosenBeans = new ArrayList();
    MemberChooser<InfraBeanClassMember> chooser = new MemberChooser<>(
            candidates.toArray(new InfraBeanClassMember[0]), false, setterDependency, project) {
      protected MemberChooser<InfraBeanClassMember>.ShowContainersAction getShowContainersAction() {
        return new MemberChooser.ShowContainersAction(messagePointer("beans.chooser.show.context.files"), Icons.SpringConfig);
      }

      protected String getAllContainersNodeName() {
        return message("beans.chooser.all.context.files");
      }
    };
    chooser.setTitle(message("bean.dependencies.chooser.title"));
    chooser.setCopyJavadocVisible(false);
    chooser.show();
    if (chooser.getExitCode() == 0) {
      InfraBeanClassMember[] members = chooser.getSelectedElements(
              new InfraBeanClassMember[0]);
      if (members != null) {
        for (InfraBeanClassMember member : members) {
          chosenBeans.add(member.getInfraBean());
        }
      }
    }

    return chosenBeans;
  }

  public static Set<InfraBeanClassMember> getAutowiredBeanCandidates(CommonInfraModel model, Condition<? super BeanPointer<?>> condition) {
    Set<InfraBeanClassMember> beanClassMembers = new HashSet<>();
    Collection<BeanPointer<?>> allBeans = model.getAllCommonBeans();
    for (BeanPointer<?> pointer : allBeans) {
      PsiClass[] dependentBeanClasses = getEffectiveBeanClasses(pointer);
      if (canBeReferenced(pointer, allBeans) && dependentBeanClasses.length > 0 && condition.value(pointer)) {
        beanClassMembers.add(new InfraBeanClassMember(pointer));
      }
    }
    return beanClassMembers;
  }

  private static PsiClass[] getEffectiveBeanClasses(BeanPointer pointer) {
    return ContainerUtil.map2Array(pointer.getEffectiveBeanTypes(), PsiClass.class, PsiTypesUtil::getPsiClass);
  }

  public static Set<InfraBeanClassMember> getCandidates(InfraBean springBean, boolean setterDependency) {
    PsiClass springBeanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (springBeanClass == null) {
      return Collections.emptySet();
    }
    Set<InfraBeanClassMember> beanClassMembers = new HashSet<>();
    CommonInfraModel model = InfraModelService.of().getModel(springBean);
    Collection<BeanPointer<?>> allBeans = model.getAllCommonBeans();
    for (BeanPointer<?> pointer : allBeans) {
      if (!pointer.isReferenceTo(springBean)) {
        PsiClass[] dependentBeanClasses = getEffectiveBeanClasses(pointer);
        if (canBeReferenced(pointer, allBeans) && dependentBeanClasses.length > 0 && !hasDependency(springBean, pointer, setterDependency) && ((setterDependency && !isCompiledElementWithoutSetter(
                springBeanClass, dependentBeanClasses)) || (!setterDependency && !isCompiledElementWithoutProperConstructor(springBean, model, PsiTypesUtil.getPsiClass(springBean.getBeanType()),
                dependentBeanClasses)))) {
          beanClassMembers.add(new InfraBeanClassMember(pointer));
        }
      }
    }
    return beanClassMembers;
  }

  private static boolean canBeReferenced(BeanPointer<?> bean, Collection<? extends BeanPointer<?>> beans) {
    return BeanCoreUtils.getReferencedName(bean, beans) != null;
  }

  public static Set<InfraBeanClassMember> getCandidates(CommonInfraModel processor, PsiClass psiClass, boolean setterDependency) {
    Set<InfraBeanClassMember> beanClassMembers = new HashSet<>();
    Collection<BeanPointer<?>> allBeans = processor.getAllCommonBeans();
    for (BeanPointer<?> bean : allBeans) {
      PsiClass[] dependentBeanClasses = getEffectiveBeanClasses(bean);
      if (canBeReferenced(bean, allBeans) && dependentBeanClasses.length > 0 && ((setterDependency && !isCompiledElementWithoutSetter(psiClass,
              dependentBeanClasses)) || (!setterDependency && !isCompiledElementWithoutProperConstructor(null, processor, psiClass, dependentBeanClasses)))) {
        beanClassMembers.add(new InfraBeanClassMember(bean));
      }
    }
    return beanClassMembers;
  }

  private static boolean isCompiledElementWithoutProperConstructor(@Nullable InfraBean springBean, CommonInfraModel model, PsiClass springBeanClass, PsiClass[] beanClasses) {
    if (!(springBeanClass instanceof PsiCompiledElement) && !(springBeanClass.getOriginalElement() instanceof PsiCompiledElement)) {
      return false;
    }
    if (springBean != null) {
      for (PsiClass beanClass : beanClasses) {
        if (getCompiledElementCandidateConstructor(springBean, springBeanClass, beanClass) != null) {
          return false;
        }
      }
      return true;
    }
    List<BeanPointer<?>> list = BeanCoreUtils.findBeansByClassName(model.getAllCommonBeans(), springBeanClass.getQualifiedName());
    for (PsiClass beanClass2 : beanClasses) {
      for (var pointer : list) {
        CommonInfraBean bean = pointer.getBean();
        if ((bean instanceof InfraBean) && getCompiledElementCandidateConstructor((InfraBean) bean, springBeanClass, beanClass2) != null) {
          return false;
        }
      }
    }
    for (PsiMethod constructor : springBeanClass.getConstructors()) {
      if (constructor.getParameterList().getParametersCount() == 1) {
        PsiType type = constructor.getParameterList().getParameters()[0].getType();
        PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(springBeanClass.getProject()).getElementFactory();
        for (PsiClass beanClass3 : beanClasses) {
          PsiClassType classType = psiElementFactory.createType(beanClass3);
          if (type.isAssignableFrom(classType)) {
            return false;
          }
        }
        continue;
      }
    }
    return true;
  }

  @Nullable
  private static PsiMethod getCompiledElementCandidateConstructor(InfraBean currentBean, PsiClass currentBeanClass, PsiClass candidateParameterClass) {
    PsiType candidatePsiType = JavaPsiFacade.getInstance(currentBeanClass.getProject()).getElementFactory().createType(candidateParameterClass);
    if (currentBean.getConstructorArgs().isEmpty()) {
      return findConstructor(currentBeanClass.getConstructors(), Collections.singletonList(candidatePsiType));
    }
    List<PsiMethod> methods = InfraConstructorArgResolveUtil.findMatchingMethods(currentBean);
    for (PsiMethod method : methods) {
      List<PsiType> psiParameterTypes = getParameterTypes(method);
      psiParameterTypes.add(candidatePsiType);
      PsiMethod existedConstructor = findConstructor(currentBeanClass.getConstructors(), psiParameterTypes);
      if (existedConstructor != null) {
        return existedConstructor;
      }
    }
    return null;
  }

  @Nullable
  public static PsiMethod findConstructor(PsiMethod[] constructors, List<? extends PsiType> psiParameterTypes) {
    for (PsiMethod constructor : constructors) {
      if (constructor.getParameterList().getParametersCount() == psiParameterTypes.size()) {
        boolean isAccepted = true;
        PsiParameter[] parameters = constructor.getParameterList().getParameters();
        int i = 0;
        while (true) {
          if (i < psiParameterTypes.size()) {
            if (psiParameterTypes.get(i).isAssignableFrom(parameters[i].getType())) {
              i++;
            }
            else {
              isAccepted = false;
              break;
            }
          }
          else {
            break;
          }
        }
        if (isAccepted) {
          return constructor;
        }
      }
    }
    return null;
  }

  private static List<PsiType> getParameterTypes(PsiMethod method) {
    List<PsiType> psiParameterTypes = new ArrayList<>();
    PsiParameter[] parameters = method.getParameterList().getParameters();
    for (PsiParameter parameter : parameters) {
      psiParameterTypes.add(parameter.getType());
    }
    return psiParameterTypes;
  }

  private static boolean isCompiledElementWithoutSetter(PsiClass springBeanClass, PsiClass[] beanClasses) {
    if ((springBeanClass instanceof PsiCompiledElement) || (springBeanClass.getOriginalElement() instanceof PsiCompiledElement)) {
      for (PsiClass beanClass : beanClasses) {
        if (getExistedSetter(springBeanClass, beanClass) != null) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private static boolean hasDependency(CommonInfraBean currentBean, BeanPointer<?> candidateBean, boolean isSetterDependency) {
    if (isSetterDependency) {
      return getSetterDependencies(currentBean).contains(candidateBean);
    }
    return getConstructorDependencies(currentBean).contains(candidateBean);
  }

  public static List<BeanPointer<?>> getSetterDependencies(CommonInfraBean springBean) {
    List<BeanPointer<?>> dependencies = new ArrayList<>();
    if (springBean instanceof DomInfraBean) {
      for (InfraPropertyDefinition property : InfraPropertyUtils.getProperties(springBean)) {
        if (property instanceof InfraValueHolder) {
          dependencies.addAll(InfraPropertyUtils.getInfraValueHolderDependencies(property));
        }
      }
    }
    return dependencies;
  }

  public static List<BeanPointer<?>> getConstructorDependencies(CommonInfraBean springBean) {
    if (springBean instanceof InfraBean) {
      List<BeanPointer<?>> dependencies = new ArrayList<>();
      for (ConstructorArg arg : ((InfraBean) springBean).getConstructorArgs()) {
        dependencies.addAll(InfraPropertyUtils.getInfraValueHolderDependencies(arg));
      }
      return dependencies;
    }
    return Collections.emptyList();
  }

  @Nullable
  private static Pair<InfraInjection, InfraGenerateTemplatesHolder> createDependency(InfraBean currentBean, BeanPointer<?> bean,
          CommonInfraModel model) {
    PsiMethod setter;
    InfraGenerateTemplatesHolder templatesHolder = new InfraGenerateTemplatesHolder(
            currentBean.getManager().getProject());
    PsiClass currentBeanClass = PsiTypesUtil.getPsiClass(currentBean.getBeanType());
    PsiClass[] candidateBeanClasses = getEffectiveBeanClasses(bean);
    if (currentBeanClass != null && candidateBeanClasses.length > 0 && (setter = getOrCreateSetter(bean, currentBeanClass, candidateBeanClasses, templatesHolder, model)) != null) {
      InfraProperty property = currentBean.addProperty();
      property.getName().ensureXmlElementExists();
      property.getName().setStringValue(PropertyUtilBase.getPropertyNameBySetter(setter));
      property.getRefAttr().setStringValue(getReferencedName(currentBean, bean));
      return new Pair<>(property, templatesHolder);
    }
    return null;
  }

  @Nullable
  private static Pair<InfraInjection, InfraGenerateTemplatesHolder> createConstructorArg(InfraBean currentBean, BeanPointer<?> bean,
          CommonInfraModel processor) {
    PsiClass psiClass;
    ConstructorArg constructorArg = null;
    InfraGenerateTemplatesHolder holder = new InfraGenerateTemplatesHolder(
            currentBean.getManager().getProject());
    PsiClass currentBeanClass = PsiTypesUtil.getPsiClass(currentBean.getBeanType());
    PsiClass[] candidateBeanClasses = getEffectiveBeanClasses(bean);
    if (currentBeanClass != null && candidateBeanClasses.length > 0) {
      PsiMethod existedConstructor = findExistedConstructor(currentBean, currentBeanClass, candidateBeanClasses);
      if (existedConstructor == null) {
        if (!ensureFileWritable(currentBeanClass)) {
          return null;
        }
        existedConstructor = findProperConstructorAndAddParameter(currentBean, bean, currentBeanClass, candidateBeanClasses[0], holder, processor);
      }
      constructorArg = currentBean.addConstructorArg();
      constructorArg.getRefAttr().setStringValue(getReferencedName(currentBean, bean));
      if (existedConstructor == null && InfraConstructorArgResolveUtil.findMatchingMethods(currentBean).isEmpty()) {
        PsiMethod psiMethod = createConstructor(currentBean);
        if (psiMethod.getParameterList().getParametersCount() == 1) {
          PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
          PsiType type = parameter.getType();
          if ((type instanceof PsiClassType psiClassType) && (psiClass = psiClassType.resolve()) != null) {
            addCreateSetterTemplate(psiMethod, new PsiClass[] { psiClass }, bean, holder, processor);
          }
        }
      }
    }
    return new Pair<>(constructorArg, holder);
  }

  @Nullable
  public static String getReferencedName(InfraBean currentBean, BeanPointer<?> bean) {
    CommonInfraModel model = InfraModelService.of().getModel(currentBean);
    return BeanCoreUtils.getReferencedName(bean, model.getAllCommonBeans());
  }

  @Nullable
  private static PsiMethod getOrCreateSetter(BeanPointer<?> candidateBean, PsiClass currentBeanClass, PsiClass[] candidateBeanClasses,
          InfraGenerateTemplatesHolder templatesHolder, CommonInfraModel model) {
    for (PsiClass candidateBeanClass : candidateBeanClasses) {
      PsiMethod existedSetter = getExistedSetter(currentBeanClass, candidateBeanClass);
      if (existedSetter != null) {
        return existedSetter;
      }
    }
    boolean isWritable = ensureFileWritable(currentBeanClass);
    if (!isWritable) {
      return null;
    }
    PsiMethod setter = createSetter(candidateBean, currentBeanClass, candidateBeanClasses);
    addCreateSetterTemplate(setter, candidateBeanClasses, candidateBean, templatesHolder, model);
    return setter;
  }

  @Nullable
  public static PsiMethod findExistedConstructor(InfraBean currentBean, PsiClass currentBeanClass, PsiClass[] candidateParameterClasses) {
    List<PsiMethod> constructors = InfraConstructorArgResolveUtil.findMatchingMethods(currentBean);
    for (PsiClass candidateBeanClass : candidateParameterClasses) {
      for (PsiMethod constructor : constructors) {
        List<PsiType> psiParameterTypes = getParameterTypes(constructor);
        PsiClassType candidateBeanType = PsiTypesUtil.getClassType(candidateBeanClass);
        psiParameterTypes.add(candidateBeanType);
        PsiMethod existedConstructorWithRequiredParameter = findConstructor(currentBeanClass.getConstructors(), psiParameterTypes);
        if (existedConstructorWithRequiredParameter != null) {
          return existedConstructorWithRequiredParameter;
        }
      }
    }
    return null;
  }

  @Nullable
  private static PsiMethod findProperConstructorAndAddParameter(InfraBean currentBean, BeanPointer<?> bean, PsiClass currentBeanClass, PsiClass candidateBeanClass,
          InfraGenerateTemplatesHolder holder, CommonInfraModel processor) {
    PsiMethod properConstructor = currentBean.getResolvedConstructorArgs().getResolvedMethod();
    if (properConstructor != null) {
      addConstructorParameter(currentBeanClass, candidateBeanClass, properConstructor);
      addCreateSetterTemplate(properConstructor, new PsiClass[] { candidateBeanClass }, bean, holder, properConstructor.getParameterList().getParametersCount() - 1, processor);
      return properConstructor;
    }
    return null;
  }

  private static PsiMethod createConstructor(InfraBean springBean) {
    PsiClass instantiationClass = null;
    PsiMethod instantiationMethod = null;
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    assert beanClass != null;
    try {
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(beanClass.getProject()).getElementFactory();
      if (isInstantiatedByFactory(springBean)) {
        BeanPointer<?> beanPointer = springBean.getFactoryBean().getValue();
        if (beanPointer != null) {
          instantiationClass = beanPointer.getBeanClass();
          String methodName = getInstantiationMethodName(instantiationClass, springBean);
          String methodText = "public " + beanClass.getName() + " " + methodName + "() { return null; }";
          instantiationMethod = elementFactory.createMethodFromText(methodText, null);
        }
      }
      else if (isInstantiatedByFactoryMethod(springBean)) {
        instantiationClass = beanClass;
        String methodName2 = getInstantiationMethodName(instantiationClass, springBean);
        String methodText2 = "public static " + beanClass.getName() + " " + methodName2 + "() { return null; }";
        instantiationMethod = elementFactory.createMethodFromText(methodText2, null);
      }
      else {
        instantiationClass = beanClass;
        instantiationMethod = elementFactory.createConstructor();
      }
      List<PsiParameter> parameters = InfraConstructorArgResolveUtil.suggestParamsForConstructorArgs(springBean);
      assert instantiationMethod != null;
      for (PsiParameter parameter : parameters) {
        instantiationMethod.getParameterList().add(parameter);
      }
      return (PsiMethod) instantiationClass.add(instantiationMethod);
    }
    catch (IncorrectOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getInstantiationMethodName(PsiClass factoryBeanClass, InfraBean springBean) {
    String methodName = springBean.getFactoryMethod().getStringValue();
    if (!StringUtil.isEmptyOrSpaces(methodName)) {
      return methodName;
    }
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    String methodName2 = "create" + beanClass.getName();
    int i = 0;
    while (factoryBeanClass.findMethodsByName(methodName2, true).length > 0) {
      i++;
      methodName2 = "create" + beanClass.getName() + i;
    }
    return methodName2;
  }

  private static boolean isInstantiatedByFactoryMethod(InfraBean springBean) {
    return DomUtil.hasXml(springBean.getFactoryMethod());
  }

  private static boolean isInstantiatedByFactory(InfraBean springBean) {
    return DomUtil.hasXml(springBean.getFactoryBean());
  }

  private static void addConstructorParameter(PsiClass currentBeanClass, PsiClass candidateBeanClass, PsiMethod constructor) {
    PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(currentBeanClass.getProject()).getElementFactory();
    try {
      PsiClassType psiClassType = psiElementFactory.createType(candidateBeanClass);
      SuggestedNameInfo nameInfo = JavaCodeStyleManager.getInstance(currentBeanClass.getProject()).suggestVariableName(VariableKind.PARAMETER, null, null, psiClassType);
      String name = nameInfo.names[0];
      int i = 0;
      while (hasSuchName(constructor.getParameterList().getParameters(), name)) {
        i++;
        name = name + i;
      }
      PsiParameter parameter = psiElementFactory.createParameter(name, psiClassType);
      constructor.getParameterList().add(parameter);
    }
    catch (IncorrectOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean hasSuchName(PsiParameter[] parameters, String name) {
    for (PsiParameter parameter : parameters) {
      if (name.equals(parameter.getName())) {
        return true;
      }
    }
    return false;
  }

  private static PsiMethod createSetter(BeanPointer<?> candidateBean, PsiClass currentBeanClass, PsiClass[] candidateBeanClasses) {
    try {
      PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(currentBeanClass.getProject());
      String beanName = candidateBean.getName();
      String name = (beanName == null || !psiNameHelper.isIdentifier(beanName)) ? candidateBeanClasses[0].getName() : beanName;
      PsiManager psiManager = currentBeanClass.getManager();
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(psiManager.getProject()).getElementFactory();
      String methodText = "public void set" + StringUtil.capitalize(name) + "(" + candidateBeanClasses[0].getQualifiedName() + " " + StringUtil.decapitalize(name) + ") { }";
      PsiMethod method = (PsiMethod) currentBeanClass.add(elementFactory.createMethodFromText(methodText, null));
      reformat(method);
      return method;
    }
    catch (IncorrectOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public static void reformat(PsiMember psiMember) {
    CodeStyleManager formatter = CodeStyleManager.getInstance(psiMember.getProject());
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(psiMember.getProject());
    codeStyleManager.shortenClassReferences(formatter.reformat(psiMember));
  }

  private static void addCreateSetterTemplate(PsiMethod method, PsiClass[] psiClasses, BeanPointer<?> bean,
          InfraGenerateTemplatesHolder templatesHolder, CommonInfraModel model) {
    addCreateSetterTemplate(method, psiClasses, bean, templatesHolder, 0, model);
  }

  private static void addCreateSetterTemplate(PsiMethod method, PsiClass[] psiClasses, BeanPointer<?> bean, InfraGenerateTemplatesHolder templatesHolder, int paramId, CommonInfraModel model) {
    templatesHolder.addTemplateFactory(method.getParameterList(), () -> {
      PsiParameter parameter = method.getParameterList().getParameters()[paramId];
      PsiTypeElement typeElement = parameter.getTypeElement();
      Collection<PsiClass> variants = getSuperTypeVariants(psiClasses);
      Expression interfaces = GenerateAutowiredDependenciesUtil.getSuperTypesExpression(typeElement.getType().getCanonicalText(), variants);
      Expression ids = getSuggestNamesExpression(method, getSuggestedNames(bean, method, paramId), paramId, model);
      TemplateBuilderImpl builder = new TemplateBuilderImpl(method.getParameterList());
      if (variants.size() > 1) {
        builder.replaceElement(typeElement, "type", interfaces, true);
      }
      builder.replaceElement(parameter.getNameIdentifier(), "names", ids, true);
      return builder.buildInlineTemplate();
    });
  }

  public static Collection<PsiClass> getSuperTypeVariants(PsiClass[] psiClasses) {
    PsiClass[] supers;
    Collection<PsiClass> variants = new LinkedHashSet<>();
    for (PsiClass beanClass : psiClasses) {
      variants.add(beanClass);
      ContainerUtil.addAll(variants, beanClass.getInterfaces());
      for (PsiClass psiClass : beanClass.getSupers()) {
        if (!Object.class.getName().equals(psiClass.getQualifiedName())) {
          variants.add(psiClass);
        }
      }
    }
    return variants;
  }

  private static Expression getSuggestNamesExpression(PsiMethod method, Collection<String> suggestedNames, int paramId, CommonInfraModel model) {
    PsiParameter parameter = method.getParameterList().getParameters()[paramId];
    return new Expression() {

      public Result calculateResult(ExpressionContext context) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        PsiIdentifier psiIdentifier = parameter.getNameIdentifier();
        return new TextResult(psiIdentifier != null ? psiIdentifier.getText() : "foo");
      }

      public LookupElement[] calculateLookupItems(ExpressionContext context) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
        LinkedHashSet<LookupElement> items = new LinkedHashSet<>();
        for (String name : suggestedNames) {
          items.add(LookupElementBuilder.create(name));
        }
        return items.toArray(LookupElement.EMPTY_ARRAY);
      }

      public LookupFocusDegree getLookupFocusDegree() {
        return LookupFocusDegree.UNFOCUSED;
      }
    };
  }

  private static Collection<String> getSuggestedNames(BeanPointer beanPointer, PsiMethod method, int paramId) {
    PsiNameHelper psiNameHelper = PsiNameHelper.getInstance(method.getProject());
    Set<String> names = new HashSet<>();
    for (String name : InfraBeanUtils.of().findBeanNames(beanPointer.getBean())) {
      if (psiNameHelper.isIdentifier(name)) {
        names.add(name);
      }
    }
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(method.getProject());
    PsiParameter[] parameters = method.getParameterList().getParameters();
    if (paramId < parameters.length) {
      SuggestedNameInfo info = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, null, null, parameters[paramId].getType());
      ContainerUtil.addAll(names, info.names);
    }
    return names;
  }

  @Nullable
  public static PsiMethod getExistedSetter(PsiClass currentBeanClass, PsiClass setterPsiClass) {
    PsiClassType psiClassType = JavaPsiFacade.getInstance(setterPsiClass.getProject()).getElementFactory().createType(setterPsiClass);
    for (PsiMethod psiMethod : currentBeanClass.getAllMethods()) {
      if (PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
        PsiType type = psiMethod.getParameterList().getParameters()[0].getType();
        if (type.isAssignableFrom(psiClassType)) {
          return psiMethod;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Module getSpringModule(PsiClass psiClass) {
    PsiFile psiFile = psiClass.getContainingFile();
    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    ProjectFileIndex index = ProjectRootManagerEx.getInstanceEx(psiClass.getProject()).getFileIndex();
    if (index.isLibraryClassFile(virtualFile) || index.isInLibrarySource(virtualFile)) {
      List<OrderEntry> orderEntries = index.getOrderEntriesForFile(virtualFile);
      for (OrderEntry orderEntry : orderEntries) {
        Module module = orderEntry.getOwnerModule();
        if (InfraUtils.hasFacet(module)) {
          return module;
        }
      }
    }
    Module module2 = index.getModuleForFile(virtualFile);
    if (!InfraUtils.hasFacet(module2)) {
      return null;
    }
    return module2;
  }
}

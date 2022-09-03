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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.InfraConstructorArgUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.CNamespaceDomElement;
import cn.taketoday.assistant.model.xml.beans.CNamespaceRefValue;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class InfraConstructorArgResolveUtil extends InfraConstructorArgUtils {

  public static List<PsiMethod> findMatchingMethods(InfraBean infraBean) {
    CommonInfraModel model = InfraModelService.of().getModel(infraBean);
    return findMatchingMethods(infraBean, model);
  }

  private static List<PsiMethod> findMatchingMethods(InfraBean infraBean, CommonInfraModel infraModel) {
    List<PsiMethod> methods = infraBean.getInstantiationMethods();
    if (methods.isEmpty()) {
      return methods;
    }
    Set<ConstructorArgDefinition> allArgs = new HashSet<>();
    Set<ConstructorArg> args = infraBean.getAllConstructorArgs();
    allArgs.addAll(args);
    allArgs.addAll(infraBean.getCNamespaceConstructorArgDefinitions());
    boolean constructorAutowire = AutowireUtil.isConstructorAutowire(infraBean);
    Map<Integer, ConstructorArg> indexedArgs = getIndexedConstructorArgs(args);
    List<PsiMethod> accepted = new ArrayList<>(methods.size());
    for (PsiMethod method : methods) {
      PsiParameter[] parameters = method.getParameterList().getParameters();
      if (acceptMethodByAutowire(constructorAutowire, allArgs, parameters) && acceptMethodByParameterTypes(indexedArgs, constructorAutowire, infraModel, allArgs, parameters)) {
        accepted.add(method);
      }
    }
    return accepted;
  }

  public static boolean acceptMethodByAutowire(boolean constructorAutowire, Set<ConstructorArgDefinition> args, PsiParameter[] parameters) {
    if (constructorAutowire || parameters.length == args.size()) {
      return !constructorAutowire || parameters.length >= args.size();
    }
    return false;
  }

  private static boolean acceptMethodByParameterTypes(Map<Integer, ConstructorArg> indexedArgs, boolean constructorAutowire, CommonInfraModel model, Set<? extends ConstructorArgDefinition> args,
          PsiParameter[] parameters) {
    SmartList smartList = new SmartList();
    for (int i = 0; i < parameters.length; i++) {
      PsiParameter parameter = parameters[i];
      if (!acceptParameter(parameter, args, indexedArgs, i, smartList)
              && (!constructorAutowire || AutowireUtil.autowireByType(model, parameter.getType(), parameter.getName(), false)
              .isEmpty())) {
        return false;
      }
    }
    return true;
  }

  public static boolean acceptParameter(PsiParameter parameter,
          Collection<? extends ConstructorArgDefinition> allConstructorArgs,
          Map<Integer, ConstructorArg> indexedArgs, int i, Collection<? super ConstructorArgDefinition> usedArgs) {
    if (AutowireUtil.isValueAnnoInjection(parameter)) {
      return true;
    }
    PsiType psiType = parameter.getType();
    if (indexedArgs.get(i) != null) {
      return indexedArgs.get(i).isAssignable(psiType);
    }
    for (ConstructorArgDefinition arg : allConstructorArgs) {
      if (!usedArgs.contains(arg)) {
        if ((arg instanceof CNamespaceDomElement) && acceptParameter(parameter, i, (CNamespaceDomElement) arg)) {
          usedArgs.add(arg);
          return true;
        }
        else if ((arg instanceof ConstructorArg) && !indexedArgs.containsValue(arg)) {
          boolean assignable = ((ConstructorArg) arg).isAssignable(psiType);
          if (assignable) {
            usedArgs.add(arg);
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean acceptParameter(PsiParameter parameter, int i, CNamespaceDomElement cNamespaceDomElement) {
    String parameterName = parameter.getName();
    if (parameterName.equals(cNamespaceDomElement.getAttributeName())) {
      return true;
    }
    Integer index = cNamespaceDomElement.getIndex();
    return index != null && i == index;
  }

  public static Map<Integer, ConstructorArg> getIndexedConstructorArgs(Collection<? extends ConstructorArg> list) {
    Map<Integer, ConstructorArg> indexed = new HashMap<>();
    for (ConstructorArg constructorArg : list) {
      Integer value = constructorArg.getIndex().getValue();
      if (value != null) {
        indexed.put(value, constructorArg);
      }
    }
    return indexed;
  }

  @Override
  @Nullable
  public PsiMethod getInfraBeanConstructor(InfraBean infraBean, CommonInfraModel infraModel) {
    if (infraBean == null || infraModel == null || isInstantiatedByFactory(infraBean)) {
      return null;
    }
    List<PsiMethod> psiMethods = findMatchingMethods(infraBean, infraModel);
    PsiMethod resolvedConstructor = null;
    for (PsiMethod psiMethod : psiMethods) {
      if (resolvedConstructor == null || resolvedConstructor.getParameterList().getParametersCount() < psiMethod.getParameterList().getParametersCount()) {
        resolvedConstructor = psiMethod;
      }
    }
    return resolvedConstructor;
  }

  public static boolean isInstantiatedByFactory(InfraBean infraBean) {
    return DomUtil.hasXml(infraBean.getFactoryMethod());
  }

  public static boolean hasEmptyConstructor(InfraBean infraBean) {
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType(false));
    if (beanClass != null) {
      PsiMethod[] constructors = beanClass.getConstructors();
      if (constructors.length == 0) {
        return true;
      }
      for (PsiMethod constructor : constructors) {
        if (constructor.getParameterList().getParametersCount() == 0) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  public static String suggestParamsForConstructorArgsAsString(InfraBean infraBean) {
    List<String> params = new ArrayList<>();
    for (PsiParameter psiParameter : suggestParamsForConstructorArgs(infraBean)) {
      params.add(psiParameter.getText());
    }
    return StringUtil.join(params, ",");
  }

  public static List<PsiParameter> suggestParamsForConstructorArgs(InfraBean infraBean) {
    List<PsiParameter> methodParameters = new LinkedList<>();
    Project project = infraBean.getManager().getProject();
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    for (Pair<String, PsiType> param : suggestConstructorParamsForBean(infraBean)) {
      methodParameters.add(elementFactory.createParameter(param.first, param.second));
    }
    return methodParameters;
  }

  public static List<Pair<String, PsiType>> suggestConstructorParamsForBean(InfraBean infraBean) {
    List<Pair<String, PsiType>> result = new LinkedList<>();
    Project project = infraBean.getManager().getProject();
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    PsiClassType defaultParamType = getDefaultParamType(project, elementFactory);
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
    List<String> existedNames = new ArrayList<>();
    List<CNamespaceDomElement> cNamespaceDomElements = infraBean.getCNamespaceConstructorArgDefinitions();
    if (cNamespaceDomElements.size() > 0) {
      for (CNamespaceDomElement namespaceDomElement : cNamespaceDomElements) {
        String name = namespaceDomElement.getAttributeName();
        if (!StringUtil.isEmptyOrSpaces(name)) {
          result.add(Pair.create(StringUtil.sanitizeJavaIdentifier(name), getConstructorArgDefinitionType(defaultParamType, namespaceDomElement)));
        }
      }
    }
    else {
      for (ConstructorArg arg : sortConstructorArgsByIndex(infraBean.getConstructorArgs())) {
        if (arg != null) {
          PsiType type = getConstructorArgDefinitionType(defaultParamType, arg);
          result.add(Pair.create(getConstructorArgParamName(codeStyleManager, existedNames, arg, type), type));
        }
      }
    }
    return result;
  }

  private static String getConstructorArgParamName(JavaCodeStyleManager codeStyleManager, List<? super String> existedNames, ConstructorArg arg, PsiType type) {
    String name = arg.getNameAttr().getStringValue();
    if (StringUtil.isEmptyOrSpaces(name) || !PsiNameHelper.getInstance(arg.getManager().getProject()).isIdentifier(name)) {
      SuggestedNameInfo nameInfo = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, null, null, type);
      name = nameInfo.names.length > 0 ? nameInfo.names[0] : "p";
      int i = 1;
      while (existedNames.contains(name)) {
        i++;
        name = name + i;
      }
    }
    existedNames.add(name);
    return name;
  }

  private static PsiClassType getDefaultParamType(Project project, PsiElementFactory elementFactory) {
    return elementFactory.createTypeByFQClassName("java.lang.String", GlobalSearchScope.allScope(project));
  }

  private static PsiType getConstructorArgDefinitionType(PsiClassType defaultParamType, ConstructorArgDefinition arg) {
    PsiType constructorArgType = getConstructorArgType(arg);
    if (constructorArgType == null || constructorArgType.equals(PsiType.NULL)) {
      constructorArgType = defaultParamType;
    }
    return constructorArgType;
  }

  @Nullable
  private static PsiType getConstructorArgType(ConstructorArgDefinition arg) {
    PsiType[] effectiveBeanType;
    PsiType psiType;
    if (!(arg instanceof ConstructorArg) || (psiType = ((ConstructorArg) arg).getType().getValue()) == null || psiType.getCanonicalText().endsWith(".")) {
      BeanPointer<?> referencedBean = getReferencedBean(arg);
      if (referencedBean != null && (effectiveBeanType = referencedBean.getEffectiveBeanTypes()) != null && effectiveBeanType.length > 0) {
        return effectiveBeanType[0];
      }
      return null;
    }
    return psiType;
  }

  @Nullable
  private static BeanPointer<?> getReferencedBean(ConstructorArgDefinition arg) {
    if (arg instanceof ConstructorArg) {
      return InfraPropertyUtils.findReferencedBean((ConstructorArg) arg);
    }
    if (arg instanceof CNamespaceRefValue) {
      return arg.getRefValue();
    }
    return null;
  }

  private static ConstructorArg[] sortConstructorArgsByIndex(List<? extends ConstructorArg> constructorArgs) {
    ConstructorArg[] args = new ConstructorArg[constructorArgs.size()];
    Map<Integer, ConstructorArg> indexedConstructorArgs = getIndexedConstructorArgs(constructorArgs);
    if (indexedConstructorArgs.size() == 0) {
      return constructorArgs.toArray(new ConstructorArg[0]);
    }
    List<ConstructorArg> indexed = new ArrayList<>();
    for (Integer index : indexedConstructorArgs.keySet()) {
      int i = index;
      if (i >= 0 && i < args.length) {
        ConstructorArg arg = indexedConstructorArgs.get(index);
        args[i] = arg;
        indexed.add(arg);
      }
    }
    for (ConstructorArg constructorArg : constructorArgs) {
      if (!indexed.contains(constructorArg)) {
        for (int i2 = 0; i2 < args.length; i2++) {
          if (args[i2] == null) {
            args[i2] = constructorArg;
          }
        }
      }
    }
    return args;
  }
}

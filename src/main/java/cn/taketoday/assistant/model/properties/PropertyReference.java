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

package cn.taketoday.assistant.model.properties;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.beanProperties.CreateBeanPropertyFixes;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class PropertyReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference, EmptyResolveMessageProvider, LocalQuickFixProvider {
  private final PropertyReferenceSet myReferenceSet;
  private final int myIndex;

  public PropertyReference(PropertyReferenceSet set, TextRange range, int index) {
    super(set.getElement(), range, true);
    this.myReferenceSet = set;
    this.myIndex = index;
  }

  @Nullable
  private PsiClass getPsiClass() {
    if (isFirst()) {
      return this.myReferenceSet.getBeanClass();
    }
    ResolveResult[] results = this.myReferenceSet.getReference(this.myIndex - 1).multiResolve(false);
    if (results.length > 0) {
      PsiMethod method = chooseMethod(ContainerUtil.map2List(results, resolveResult -> (PsiMethod) resolveResult.getElement()));
      PsiType returnType = method.getReturnType();
      if (returnType instanceof PsiClassType psiClass) {
        return psiClass.resolve();
      }
      return null;
    }
    return null;
  }

  private Set<PsiMethod> getSharedProperties(Collection<BeanPointer<?>> descendants) {
    Set<PsiClass> beanClasses = getUniqueBeanClasses(descendants);
    boolean acceptSetters = isLast();
    Set<PsiMethod> maps = new HashSet<>();
    String propertyName = getValue();
    for (PsiClass beanClass : beanClasses) {
      if (acceptSetters) {
        maps.addAll(PropertyUtilBase.getSetters(beanClass, propertyName));
      }
      else {
        maps.addAll(PropertyUtilBase.getGetters(beanClass, propertyName));
      }
    }
    return maps;
  }

  private static PsiMethod chooseMethod(List<PsiMethod> methods) {
    int methodsCount = methods.size();
    if (methodsCount == 1) {
      return methods.get(0);
    }
    PsiMethod chosenMethod = methods.get(0);
    for (int i = 1; i < methodsCount; i++) {
      PsiMethod method = methods.get(i);
      if (InheritanceUtil.isInheritorOrSelf(chosenMethod.getContainingClass(), method.getContainingClass(), true)) {
        chosenMethod = method;
      }
    }
    return chosenMethod;
  }

  private Map<String, Set<PsiMethod>> getAllSharedProperties(Collection<BeanPointer<?>> descendants) {
    Set<PsiClass> beanClasses = getUniqueBeanClasses(descendants);
    boolean acceptGetters = !isLast();
    List<Map<String, PsiMethod>> maps = new ArrayList<>();
    for (PsiClass beanClass : beanClasses) {
      maps.add(PropertyUtilBase.getAllProperties(beanClass, true, acceptGetters));
    }
    return reduce(maps);
  }

  @Nullable
  public PsiMethod resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return (PsiMethod) (resolveResults.length == 1 ? resolveResults[0].getElement() : null);
  }

  public ResolveResult[] multiResolve(boolean incompleteCode) {
    if (isFirst()) {
      InfraModel model = InfraConverterUtil.getInfraModel(this.myReferenceSet.getContext());
      if (model == null) {
        return ResolveResult.EMPTY_ARRAY;
      }
      Set<PsiMethod> methods = getSharedProperties(getInheritorBeansOrSelf(model));
      return PsiElementResolveResult.createResults(methods);
    }
    PsiClass psiClass = getPsiClass();
    if (psiClass != null) {
      String propertyName = getValue();
      PsiMethod method = resolve(psiClass, propertyName);
      if (method != null) {
        return new ResolveResult[] { new PsiElementResolveResult(method) };
      }
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  private List<BeanPointer<?>> getInheritorBeansOrSelf(InfraModel model) {
    BeanPointer<?> pointer = InfraBeanService.of().createBeanPointer(this.myReferenceSet.getBean());
    List<BeanPointer<?>> descendants = InfraModelVisitorUtils.getDescendants(model, pointer);
    return descendants.isEmpty() ? Collections.singletonList(pointer) : descendants;
  }

  @Nullable
  private PsiMethod resolve(PsiClass psiClass, String propertyName) {
    boolean isLast = isLast();
    PsiMethod method = isLast ? PropertyUtilBase.findPropertySetter(psiClass, propertyName, false, true) : PropertyUtilBase.findPropertyGetter(psiClass, propertyName, false, true);
    if (method == null || !method.hasModifierProperty("public")) {
      return null;
    }
    return method;
  }

  private boolean isLast() {
    return this.myReferenceSet.getReferences().size() - 1 == this.myIndex;
  }

  private boolean isFirst() {
    return this.myIndex == 0;
  }

  public Object[] getVariants() {
    Map<String, PsiMethod> properties;
    InfraModel model = InfraConverterUtil.getInfraModel(this.myReferenceSet.getContext());
    if (model == null) {
      return EMPTY_ARRAY;
    }
    CommonInfraBean bean = this.myReferenceSet.getBean();
    if (!isFirst()) {
      PsiClass psiClass = getPsiClass();
      if (psiClass == null) {
        return EMPTY_ARRAY;
      }
      properties = PropertyUtilBase.getAllProperties(psiClass, true, !isLast());
    }
    else {
      Collection<BeanPointer<?>> descendants = getInheritorBeansOrSelf(model);
      if (!descendants.isEmpty()) {
        Map<String, Set<PsiMethod>> sharedProperties = getAllSharedProperties(descendants);
        properties = new HashMap<>();
        for (Map.Entry<String, Set<PsiMethod>> entry : sharedProperties.entrySet()) {
          String propertyName = entry.getKey();
          PsiMethod firstMethod = entry.getValue().iterator().next();
          properties.put(propertyName, firstMethod);
        }
      }
      else {
        PsiClass psiClass2 = getPsiClass();
        if (psiClass2 == null) {
          return EMPTY_ARRAY;
        }
        properties = PropertyUtilBase.getAllProperties(psiClass2, true, !isLast());
      }
    }
    List<String> existingPropertyNames = getExistingPropertyNames(bean);
    Set<LookupElementBuilder> variants = new HashSet<>();
    for (Map.Entry<String, PsiMethod> entry2 : properties.entrySet()) {
      String propertyName2 = entry2.getKey();
      if (!existingPropertyNames.contains(propertyName2)) {
        PsiMethod psiMethod = entry2.getValue();
        PsiType propertyType = PropertyUtilBase.getPropertyType(psiMethod);
        boolean isAutowired = AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.AUTOWIRED, 1);
        assert propertyType != null;
        variants.add(LookupElementBuilder.create(psiMethod, propertyName2)
                .withIcon(Icons.SpringProperty)
                .withTailText(isAutowired ? " (@Autowired)" : "", true)
                .withStrikeoutness(psiMethod.isDeprecated())
                .withTypeText(propertyType.getPresentableText()));
      }
    }

    return variants.toArray(new LookupElementBuilder[0]);
  }

  private static List<String> getExistingPropertyNames(CommonInfraBean bean) {
    return ContainerUtil.map(InfraPropertyUtils.getProperties(bean), InfraPropertyDefinition::getPropertyName);
  }

  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    String name = PropertyUtilBase.getPropertyName(newElementName);
    return super.handleElementRename(name == null ? newElementName : name);
  }

  public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
    String propertyName;
    if ((element instanceof PsiMethod) && (propertyName = PropertyUtilBase.getPropertyName((PsiMember) element)) != null) {
      return super.handleElementRename(propertyName);
    }
    return getElement();
  }

  private static Set<PsiClass> getUniqueBeanClasses(Collection<BeanPointer<?>> beans) {
    if (beans.isEmpty()) {
      return Collections.emptySet();
    }
    Set<PsiClass> classes = new HashSet<>();
    for (BeanPointer bean : beans) {
      PsiClass psiClass = bean.getBeanClass();
      if (psiClass != null) {
        classes.add(psiClass);
      }
    }
    return classes;
  }

  private static <K, V> Map<K, Set<V>> reduce(Collection<Map<K, V>> maps) {
    HashMap<K, Set<V>> intersection = new HashMap<>();
    Iterator<Map<K, V>> i = maps.iterator();
    if (i.hasNext()) {
      Map<K, V> first = i.next();
      for (Map.Entry<K, V> entry : first.entrySet()) {
        Set<V> values = new HashSet<>();
        values.add(entry.getValue());
        intersection.put(entry.getKey(), values);
      }
      while (i.hasNext()) {
        Map<K, V> map = i.next();
        intersection.keySet().retainAll(map.keySet());
        for (Map.Entry<K, Set<V>> entry2 : intersection.entrySet()) {
          entry2.getValue().add(map.get(entry2.getKey()));
        }
      }
    }
    return intersection;
  }

  public String getUnresolvedMessagePattern() {
    return message("model.property.error.message", getValue());
  }

  public LocalQuickFix[] getQuickFixes() {
    PsiClass psiClass;
    String value = getValue();
    if (StringUtil.isNotEmpty(value) && (psiClass = getPsiClass()) != null) {
      InfraPropertyDefinition definition = (InfraPropertyDefinition) this.myReferenceSet.getGenericDomValue().getParent();
      PsiType type = definition.guessTypeByValue();
      return CreateBeanPropertyFixes.createFixes(value, psiClass, type, true);
    }
    return LocalQuickFix.EMPTY_ARRAY;
  }
}

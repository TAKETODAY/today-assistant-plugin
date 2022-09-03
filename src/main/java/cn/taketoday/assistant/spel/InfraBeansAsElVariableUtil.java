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

package cn.taketoday.assistant.spel;

import com.intellij.javaee.el.util.ELImplicitVariable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.lang.Nullable;

public final class InfraBeansAsElVariableUtil {

  public static void addVariables(List<? super PsiVariable> resultVars, Module module) {
    CommonInfraModel combinedModel = InfraManager.from(module.getProject()).getCombinedModel(module);
    addVariables(resultVars, combinedModel);
  }

  public static void addVariables(List<? super PsiVariable> resultVars, CommonInfraModel model) {
    Collection<? extends BeanPointer<?>> list = model.getAllCommonBeans();

    for (BeanPointer<?> pointer : list) {
      CommonInfraBean bean = pointer.getBean();
      if (bean instanceof ContextJavaBean) {
        for (PomNamedTarget target : ((ContextJavaBean) bean).getPomTargets()) {
          PsiElement declarationElement = PomService.convertToPsi((PsiTarget) target);
          String name = target.getName();
          if (StringUtil.isNotEmpty(name)) {
            resultVars.add(createVariable(pointer, name, declarationElement));
          }
        }
      }
      else {
        String beanName = pointer.getName();
        if (!StringUtil.isEmptyOrSpaces(beanName) && pointer.isValid()) {
          PsiElement declarationElement = pointer.getPsiElement();
          if (declarationElement != null) {
            Set<String> beanNames = InfraBeanUtils.of().findBeanNames(bean);

            for (String aliasName : beanNames) {
              if (!StringUtil.isEmptyOrSpaces(aliasName)) {
                resultVars.add(createVariable(pointer, aliasName, declarationElement));
              }
            }
          }
        }
      }
    }

  }

  public static InfraElBeanVariable createVariable(BeanPointer<?> beanPointer, String name) {
    return createVariable(beanPointer, name, beanPointer.getPsiElement());
  }

  private static InfraElBeanVariable createVariable(BeanPointer<?> beanPointer, String beanName, PsiElement declarationElement) {
    PsiElement declaration;
    if (declarationElement instanceof PomTargetPsiElement) {
      declaration = declarationElement.getNavigationElement();
    }
    else {
      declaration = declarationElement;
    }

    return new InfraElBeanVariable(beanPointer, beanName, declaration);
  }

  static class InfraElBeanVariable extends ELImplicitVariable {
    @Nullable
    private final Icon myIcon;

    public InfraElBeanVariable(BeanPointer pointer, String beanName, PsiElement declaration) {
      super(declaration.getContainingFile(), beanName, getBeanType(pointer), declaration, "NESTED");
      this.myIcon = InfraPresentationProvider.getInfraIcon(pointer);
    }

    private static PsiType getBeanType(BeanPointer beanPointer) {
      PsiType[] classes = beanPointer.getEffectiveBeanTypes();
      if (classes.length == 0) {
        PsiManager psiManager = beanPointer.getContainingFile().getManager();
        return PsiType.getJavaLangObject(psiManager, GlobalSearchScope.allScope(psiManager.getProject()));
      }
      else {
        return classes[0];
      }
    }

    @Nullable
    public Icon getIcon(boolean open) {
      return this.myIcon;
    }

    protected boolean isContentChangeAllowed(@Nullable PsiElement declaration) {
      return !(declaration instanceof XmlTag);
    }
  }

}

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

import com.intellij.javaee.el.ELElementProcessor;
import com.intellij.javaee.el.ImplicitVariableWithCustomResolve;
import com.intellij.javaee.el.psi.ELExpression;
import com.intellij.javaee.el.util.ELImplicitVariable;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesElementFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.spring.el.contextProviders.SpringElContextsExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;

/**
 * systemProperties, systemEnvironment
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
final class SystemEnvVariablesExtension extends SpringElContextsExtension {
  private static final String[] ALL_SYSTEM_PROPERTIES = {
          "systemProperties", "systemEnvironment"
  };

  @Override
  public Collection<? extends PsiVariable> getContextVariables(PsiElement contextElement) {
    ArrayList<PsiVariable> variables = new ArrayList<>(ALL_SYSTEM_PROPERTIES.length);
    for (String systemPropertiesName : ALL_SYSTEM_PROPERTIES) {
      variables.add(new SystemPropertiesVariable(contextElement, systemPropertiesName));
    }
    return variables;
  }

  private static final class SystemPropertiesVariable
          extends ELImplicitVariable implements ImplicitVariableWithCustomResolve {

    private SystemPropertiesVariable(PsiElement element, String name) {
      super(element, name, getType(element), element, "NESTED");
    }

    private static PsiType getType(PsiElement element) {
      return JavaPsiFacade.getInstance(element.getProject())
              .getElementFactory()
              .createTypeByFQClassName("java.util.Map", element.getResolveScope());
    }

    @Override
    public boolean process(ELExpression element, ELElementProcessor processor) {
      Iterator<IProperty> var3 = PropertiesElementFactory.getSystemProperties(element.getProject()).getProperties().iterator();
      IProperty property;
      do {
        if (!var3.hasNext()) {
          return true;
        }

        property = var3.next();
      }
      while (processor.processProperty(property));

      return false;
    }

    @Override
    public String getLocationString() {
      return InfraBundle.message("el.location.name");
    }

    @Override
    public Icon getIcon(boolean open) {
      return Icons.Today;
    }

    @Override
    public Icon getIcon(int flags) {
      return Icons.Today;
    }
  }

}

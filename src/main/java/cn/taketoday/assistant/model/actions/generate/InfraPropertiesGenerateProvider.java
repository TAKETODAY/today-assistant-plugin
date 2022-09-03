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

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementNavigationProvider;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.actions.generate.AbstractDomGenerateProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTree;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraPropertiesGenerateProvider extends AbstractDomGenerateProvider<InfraProperty> {

  public InfraPropertiesGenerateProvider() {
    this(message("generate.properties"));
  }

  public InfraPropertiesGenerateProvider(String description) {
    super(description, description, InfraProperty.class);
  }

  protected DomElement getParentDomElement(Project project, Editor editor, PsiFile file) {
    return BeanCoreUtils.getBeanForCurrentCaretPosition(editor, file);
  }

  public InfraProperty generate(@Nullable DomElement parent, Editor editor) {
    PsiMethodMember[] members;
    if (parent instanceof InfraBean infraBean) {
      Collection<PsiMethod> setters = getNonInjectedPropertySetters(infraBean);
      PsiMethodMember[] psiMethodMembers = getPsiMethodMembers(setters);
      Project project = parent.getManager().getProject();
      MemberChooser<PsiMethodMember> chooser = new MemberChooser<>(psiMethodMembers, false, true, project);
      chooser.setTitle(message("bean.properties.chooser.title"));
      chooser.setCopyJavadocVisible(false);
      chooser.show();
      if (chooser.getExitCode() == 0 && (members = chooser.getSelectedElements(new PsiMethodMember[0])) != null && members.length > 0) {
        PsiMethod[] methods = new PsiMethod[members.length];
        for (int i = 0; i < members.length; i++) {
          methods[i] = members[i].getElement();
        }
        doGenerate(editor, infraBean, project, methods);
      }
    }
    return null;
  }

  public static void doGenerate(Editor editor, InfraBean infraBean, Project project, PsiMethod... methods) {
    if (!GenerateBeanDependenciesUtil.ensureFileWritable(infraBean)) {
      return;
    }
    WriteCommandAction.writeCommandAction(project).run(() -> {
      InfraTemplateBuilder builder = new InfraTemplateBuilder(project);
      CommonInfraModel model = InfraModelService.of().getModel(infraBean);
      for (PsiMethod method : methods) {
        createProperty(method, model, builder);
      }
      InfraTemplateBuilder.preparePlace(editor, project, infraBean.addProperty());
      builder.startTemplate(editor, AbstractDomGenerateProvider.createNamespacePrefixMap(infraBean));
    });
  }

  private static PsiMethodMember[] getPsiMethodMembers(Collection<PsiMethod> setters) {
    List<PsiMethodMember> psiMethodMembers = new ArrayList<>();
    for (final PsiMethod psiMethod : setters) {
      psiMethodMembers.add(new PsiMethodMember(psiMethod) {

        public void renderTreeNode(SimpleColoredComponent component, JTree tree) {
          component.append(getText(), getTextAttributes(tree));
          component.append(": ", getTextAttributes(tree));
          component.append(psiMethod.getParameterList().getParameters()[0].getType().getCanonicalText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
          component.setIcon(Icons.SpringProperty);
        }

        public String getText() {
          return PropertyUtilBase.getPropertyNameBySetter(psiMethod);
        }
      });
    }
    return psiMethodMembers.toArray(new PsiMethodMember[0]);
  }

  public static Collection<PsiMethod> getNonInjectedPropertySetters(InfraBean infraBean) {
    Map<MethodSignature, PsiMethod> map = new LinkedHashMap<>();
    PsiClass psiClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (psiClass != null) {
      for (PsiMethod method : psiClass.getAllMethods()) {
        if (PropertyUtilBase.isSimplePropertySetter(method) && method.hasModifierProperty("public") && !method.hasModifierProperty("static") && InfraPropertyUtils.findPropertyByName(infraBean,
                PropertyUtilBase.getPropertyNameBySetter(method)) == null) {
          MethodSignature key = method.getSignature(PsiSubstitutor.UNKNOWN);
          if (!map.containsKey(key)) {
            map.put(key, method);
          }
        }
      }
    }
    return map.values();
  }

  private static void createProperty(PsiMethod method, CommonInfraModel model, InfraTemplateBuilder builder) {
    PsiType type = method.getParameterList().getParameters()[0].getType();
    String name = PropertyUtilBase.getPropertyName(method);
    builder.addTextSegment("<");
    builder.addVariableSegment("NS_PREFIX");
    builder.addTextSegment("property name=\"" + name + "\"");
    builder.createValueAndClose(type, model, "property");
  }

  public boolean isAvailableForElement(DomElement contextElement) {
    InfraBean infraBean = DomUtil.getParentOfType(contextElement, InfraBean.class, false);
    return infraBean != null && getNonInjectedPropertySetters(infraBean).size() > 0;
  }

  protected void doNavigate(DomElementNavigationProvider navigateProvider, DomElement element) {
  }
}

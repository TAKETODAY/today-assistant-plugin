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

import com.intellij.codeInspection.ProblemDescriptorUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Colors;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.xml.beans.CNamespaceValue;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgDefinition;

public class ResolvedConstructorArgsMessageBuilder {
  private static final Color AUTOWIRED_COLOR = new JBColor(Colors.DARK_BLUE, LightColors.BLUE.brighter());
  private static final Color UNKNOWN_INJECTION_COLOR = new JBColor(Colors.DARK_RED, LightColors.RED.brighter());

  private final String myMessage;
  private final PsiMethod[] myConstructors;

  private final ResolvedConstructorArgs myResolvedConstructorArgs;
  private final StringBuilder mySB;

  public ResolvedConstructorArgsMessageBuilder(String message, PsiMethod[] constructors, ResolvedConstructorArgs resolvedArgs) {
    this.mySB = new StringBuilder();
    this.myMessage = message;
    this.myConstructors = constructors;
    this.myResolvedConstructorArgs = resolvedArgs;
  }

  public String getMessage() {
    PsiMethod[] psiMethodArr;
    PsiParameter[] parameters;
    if (this.myConstructors.length == 0) {
      return this.myMessage;
    }
    append("<html>");
    append(this.myMessage);
    append("#treeend");
    for (PsiMethod constructor : this.myConstructors) {
      Map<ConstructorArgDefinition, PsiParameter> args = this.myResolvedConstructorArgs.getResolvedArgs(constructor);
      Map<PsiParameter, Collection<BeanPointer<?>>> params = this.myResolvedConstructorArgs.getAutowiredParams(constructor);
      append("<hr>");
      append("<table>");
      append("<tr><td><b>").append(constructor.getName()).append("(...):</b></td><td></td><td><b>Bean:</b></td></tr>");
      for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
        append("<tr>");
        append("<td>");
        appendParameterText(parameter);
        append("</td><td>&nbsp;&nbsp;&nbsp;</td>");
        append("<td>");
        Collection<BeanPointer<?>> pointers = params == null ? Collections.emptySet() : params.get(parameter);
        if (pointers != null && pointers.size() > 0) {
          appendAutowiredBeanPointersText(pointers);
        }
        else {
          ConstructorArgDefinition key = null;
          if (args != null) {
            Iterator<Map.Entry<ConstructorArgDefinition, PsiParameter>> it = args.entrySet().iterator();
            while (true) {
              if (!it.hasNext()) {
                break;
              }
              Map.Entry<ConstructorArgDefinition, PsiParameter> entry = it.next();
              if (entry.getValue().equals(parameter)) {
                key = entry.getKey();
                break;
              }
            }
          }
          if (key != null) {
            appendConstructorArgText(key);
          }
          else {
            append("<font color='#").append(ColorUtil.toHex(UNKNOWN_INJECTION_COLOR)).append("'>");
            append("<b> ??? </b>");
            append("</font>");
          }
        }
        append("</td>");
        append("</tr>");
      }
      append("</table>");
    }
    append("</html>");
    return this.mySB.toString();
  }

  private void appendAutowiredBeanPointersText(Collection<BeanPointer<?>> pointers) {
    String qualifiedName;
    append("<font color='").append("#").append(ColorUtil.toHex(pointers.size() > 1 ? UNKNOWN_INJECTION_COLOR : AUTOWIRED_COLOR)).append("'>");
    append("Autowired:");
    for (BeanPointer pointer : pointers) {
      append(" ");
      append(pointer.getName());
      PsiClass beanClass = pointer.getBeanClass();
      if (beanClass != null && (qualifiedName = beanClass.getQualifiedName()) != null) {
        append("(");
        append(StringUtil.getShortName(qualifiedName));
        append(")");
        append(";");
      }
    }
    this.mySB.deleteCharAt(this.mySB.length() - 1);
    append("</font>");
  }

  private void appendConstructorArgText(ConstructorArgDefinition key) {
    if (key instanceof CNamespaceValue) {
      XmlElement element = key.getXmlElement();
      if (element != null) {
        appendXmlCode("<bean ... ");
        append(StringUtil.escapeXmlEntities("<b>" + element.getText() + "</b>"));
        appendXmlCode("... />");
        return;
      }
      return;
    }
    XmlTag element2 = key.getXmlTag();
    if (element2 != null) {
      String text = element2.getText();
      String[] strings = StringUtil.splitByLines(text);
      if (strings.length > 1) {
        text = strings[0] + strings[1] + "...";
      }
      appendXmlCode(text);
    }
  }

  private void appendParameterText(PsiParameter parameter) {
    appendXmlCode(StringUtil.getShortName(parameter.getType().getPresentableText()));
    append(" ");
    append(parameter.getName());
  }

  private StringBuilder append(String text) {
    return this.mySB.append(text);
  }

  private StringBuilder appendXmlCode(String text) {
    append(ProblemDescriptorUtil.XML_CODE_MARKER.first);
    append(StringUtil.escapeXmlEntities(text));
    return append(ProblemDescriptorUtil.XML_CODE_MARKER.second);
  }
}

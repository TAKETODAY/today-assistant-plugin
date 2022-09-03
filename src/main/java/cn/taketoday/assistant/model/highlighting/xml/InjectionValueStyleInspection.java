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

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.Properties;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraModelVisitor;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraKey;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

public class InjectionValueStyleInspection extends InfraBeanInspectionBase {

  private static final String KEY = "key";

  private static final String VALUE = "value";

  private static final String VALUE_REF = "value-ref";

  private static final String KEY_REF = "key-ref";

  private static final String REF = "ref";

  @Override
  protected InfraModelVisitor createVisitor(final DomElementAnnotationHolder holder, Beans beans, @Nullable CommonInfraModel model) {
    return new InfraModelVisitor() {
      @Override
      protected boolean visitValueHolder(InfraValueHolder valueHolder) {
        InjectionValueStyleInspection.checkValueHolder(holder, valueHolder);
        return true;
      }
    };
  }

  private static void checkValueHolder(DomElementAnnotationHolder holder, InfraValueHolderDefinition valueHolder) {
    checkValue(valueHolder, holder);
    checkRefBean(valueHolder, holder);
    if (valueHolder instanceof InfraEntry) {
      checkValue(((InfraEntry) valueHolder).getKey(), holder);
      checkRefBean(((InfraEntry) valueHolder).getKey(), holder);
    }
  }

  private static void checkValue(InfraValueHolderDefinition valueHolder, DomElementAnnotationHolder holder) {
    GenericDomValue<?> value;
    String s;
    PsiType type = TypeHolderUtil.getRequiredType(valueHolder);
    if ((type == null || !Properties.class.getName()
            .equals(type.getCanonicalText())) && (value = valueHolder.getValueElement()) != null && !(value instanceof GenericAttributeValue) && (s = value.getRawText()) != null && !isMultiline(s)) {
      if (!(value instanceof InfraValue) || !DomUtil.hasXml(((InfraValue) value).getType())) {
        XmlTag tag = value.getXmlTag();
        if (tag != null && tag.getValue().hasCDATA()) {
          return;
        }
        LocalQuickFix fix = new ValueQuickFix(valueHolder);
        holder.createProblem(value, HighlightSeverity.ERROR, InfraBundle.message("model.inspection.injection.value.style.message"), fix)
                .highlightWholeElement();
      }
    }
  }

  private static boolean isMultiline(String s) {
    return s.trim().indexOf(10) >= 0;
  }

  private static void checkRefBean(InfraValueHolderDefinition valueHolder, DomElementAnnotationHolder holder) {
    if (valueHolder instanceof InfraValueHolder) {
      InfraRef ref = ((InfraValueHolder) valueHolder).getRef();
      GenericAttributeValue<BeanPointer<?>> bean = ref.getBean();
      if (DomUtil.hasXml(bean)) {
        LocalQuickFix fix = new RefQuickFix((InfraValueHolder) valueHolder);
        holder.createProblem(ref, HighlightSeverity.ERROR, InfraBundle.message("model.inspection.injection.value.style.ref.message"), fix)
                .highlightWholeElement();
      }
    }
  }

  public static class ValueQuickFix implements LocalQuickFix {
    private final InfraValueHolderDefinition myValueHolder;

    ValueQuickFix(InfraValueHolderDefinition valueHolder) {
      this.myValueHolder = valueHolder.createStableCopy();
    }

    public String getName() {
      Object[] objArr = new Object[1];
      objArr[0] = this.myValueHolder instanceof InfraKey ? InjectionValueStyleInspection.KEY : "value";
      String message = InfraBundle.message("model.inspection.injection.value.style.value.fix", objArr);
      return message;
    }

    public String getFamilyName() {
      String message = InfraBundle.message("model.inspection.injection.value.style.value.fix.family.name");
      return message;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      XmlElement xmlElement = this.myValueHolder.getXmlElement();
      if (xmlElement == null) {
        return;
      }
      GenericDomValue<?> valueElement = this.myValueHolder.getValueElement();
      String val = valueElement.getRawText();
      if (this.myValueHolder instanceof InfraKey) {
        InfraEntry entry = (InfraEntry) this.myValueHolder.getParent();
        entry.getKeyAttr().setStringValue(val);
        this.myValueHolder.undefine();
        XmlTag tag = entry.getXmlTag();
        tag.collapseIfEmpty();
        return;
      }
      if (this.myValueHolder instanceof InfraValueHolder holder) {
        holder.getValueAttr().undefine();
        holder.getValue().undefine();
      }
      GenericDomValue<?> element = this.myValueHolder.getValueElement();
      element.setStringValue(val);
      XmlTag tag2 = this.myValueHolder.getXmlTag();
      tag2.collapseIfEmpty();
    }
  }

  public static class RefQuickFix implements LocalQuickFix {
    private final InfraValueHolder myValueHolder;

    RefQuickFix(InfraValueHolder valueHolder) {
      this.myValueHolder = valueHolder.createStableCopy();
    }

    public String getName() {
      String attr;
      if (this.myValueHolder instanceof InfraKey) {
        attr = InjectionValueStyleInspection.KEY_REF;
      }
      else if (this.myValueHolder instanceof InfraEntry) {
        attr = InjectionValueStyleInspection.VALUE_REF;
      }
      else {
        attr = InjectionValueStyleInspection.REF;
      }
      String message = InfraBundle.message("model.inspection.injection.value.style.ref.fix", attr);
      return message;
    }

    public String getFamilyName() {
      String message = InfraBundle.message("model.inspection.injection.value.style.ref.fix.family.name");
      return message;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      XmlElement element = this.myValueHolder.getXmlElement();
      if (element == null) {
        return;
      }
      String val = this.myValueHolder.getRef().getBean().getRawText();
      if (this.myValueHolder instanceof InfraKey) {
        InfraEntry entry = (InfraEntry) this.myValueHolder.getParent();
        entry.getKeyRef().setStringValue(val);
        this.myValueHolder.undefine();
        XmlTag tag = entry.getXmlTag();
        assert tag != null;
        tag.collapseIfEmpty();
        return;
      }
      this.myValueHolder.getRefAttr().setStringValue(val);
      this.myValueHolder.getRef().undefine();
      XmlTag tag2 = this.myValueHolder.getXmlTag();
      tag2.collapseIfEmpty();
    }
  }
}

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
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.AddDomElementQuickFix;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.RemoveDomElementQuickFix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraModelVisitor;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.Idref;
import cn.taketoday.assistant.model.xml.beans.InfraElementsHolder;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraMap;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InjectionValueConsistencyInspection extends InfraBeanInspectionBase {

  @Override
  protected InfraModelVisitor createVisitor(DomElementAnnotationHolder holder, Beans beans, @Nullable CommonInfraModel model) {
    return new InfraModelVisitor() {
      @Override
      protected boolean visitProperty(InfraPropertyDefinition property) {
        if (property instanceof InfraValueHolder) {
          checkValueHolder((InfraValueHolder) property, holder, getProperty());
          return true;
        }
        return true;
      }

      @Override
      protected boolean visitConstructorArg(ConstructorArg arg) {
        checkValueHolder(arg, holder, getArg());
        return true;
      }

      @Override
      protected boolean visitMapEntry(InfraEntry entry) {
        checkMapEntry(holder, entry);
        return true;
      }

      @Override
      public boolean visitRef(InfraRef ref) {
        checkRef(ref, holder);
        return true;
      }

      @Override
      public boolean visitIdref(Idref idref) {
        checkIdref(idref, holder);
        return true;
      }
    };
  }

  private static void checkValueHolder(InfraValueHolder valueHolder, DomElementAnnotationHolder holder, String elementName) {
    boolean hasRefAttribute = DomUtil.hasXml(valueHolder.getRefAttr());
    boolean hasValueAttribute = DomUtil.hasXml(valueHolder.getValueAttr());
    Set<DomElement> values = getValues(valueHolder);
    if (!hasRefAttribute && !hasValueAttribute && values.size() == 0) {
      reportNoValue(valueHolder, holder, elementName);
    }
    else if ((hasRefAttribute && hasValueAttribute) || ((hasRefAttribute || hasValueAttribute) && values.size() > 0)) {
      String message = message("bean.property.value.inconsistency.ref.or.value.sub.element.must.defined", elementName);
      if (hasValueAttribute) {
        reportAttribute(valueHolder.getValueAttr(), holder, message);
      }
      if (hasRefAttribute) {
        reportAttribute(valueHolder.getRefAttr(), holder, message);
      }
      reportSubtags(values, holder, message);
    }
    else if (values.size() > 1) {
      reportSubtags(values, holder, message("bean.property.value.inconsistency.more.one.sub.element", elementName));
    }
  }

  private static void checkMapEntry(DomElementAnnotationHolder holder, InfraEntry entry) {
    boolean hasKeyAttr = DomUtil.hasXml(entry.getKeyAttr());
    boolean hasKeyElement = DomUtil.hasXml(entry.getKey());
    boolean hasKeyRef = DomUtil.hasXml(entry.getKeyRef());
    if (!hasKeyAttr && !hasKeyElement && !hasKeyRef) {
      holder.createProblem(entry, message("model.inspection.injection.value.entry.key")).highlightWholeElement();
    }
    else if ((hasKeyAttr && hasKeyElement) || ((hasKeyAttr && hasKeyRef) || (hasKeyElement && hasKeyRef))) {
      String message = message("bean.property.value.inconsistency.key");
      if (hasKeyAttr) {
        reportAttribute(entry.getKeyAttr(), holder, message);
      }
      if (hasKeyElement) {
        reportAttribute(entry.getKey(), holder, message);
      }
      if (hasKeyRef) {
        reportAttribute(entry.getKeyRef(), holder, message);
      }
    }
    checkValueHolder(entry, holder, getEntry());
  }

  private static void checkRef(InfraRef ref, DomElementAnnotationHolder holder) {
    if (!DomUtil.hasXml(ref)) {
      return;
    }
    boolean hasBean = DomUtil.hasXml(ref.getBean());
    boolean hasLocal = DomUtil.hasXml(ref.getLocal());
    boolean hasParent = DomUtil.hasXml(ref.getParentAttr());
    if (!hasBean && !hasLocal && !hasParent) {
      holder.createProblem(ref, HighlightSeverity.ERROR, message("bean.ref.attributes.must.specify"),
              new AddRefFix(ref.getBean()), new AddRefFix(ref.getLocal()), new AddRefFix(ref.getParentAttr())).highlightWholeElement();
    }
    else if ((hasBean && hasLocal) || ((hasBean && hasParent) || (hasLocal && hasParent))) {
      String message = message("bean.ref.attributes.inconsistency");
      reportAttribute(ref.getBean(), holder, message);
      reportAttribute(ref.getLocal(), holder, message);
      reportAttribute(ref.getParentAttr(), holder, message);
    }
  }

  private static void checkIdref(Idref ref, DomElementAnnotationHolder holder) {
    if (!DomUtil.hasXml(ref)) {
      return;
    }
    boolean hasBean = DomUtil.hasXml(ref.getBean());
    boolean hasLocal = DomUtil.hasXml(ref.getLocal());
    if (!hasBean && !hasLocal) {
      holder.createProblem(ref, HighlightSeverity.ERROR, message("bean.idref.attributes.must.specify"),
              new AddRefFix(ref.getBean()), new AddRefFix(ref.getLocal())).highlightWholeElement();
    }
    else if (hasBean && hasLocal) {
      String message = message("bean.idref.attributes.inconsistency");
      reportAttribute(ref.getBean(), holder, message);
      reportAttribute(ref.getLocal(), holder, message);
    }
  }

  private static Set<DomElement> getValues(InfraElementsHolder elementsHolder) {
    Set<DomElement> values = new HashSet<>(DomUtil.getDefinedChildrenOfType(elementsHolder, DomInfraBean.class, true, false));
    addValue(elementsHolder.getIdref(), values);
    addValue(elementsHolder.getList(), values);
    addValue(elementsHolder.getMap(), values);
    addValue(elementsHolder.getNull(), values);
    addValue(elementsHolder.getProps(), values);
    addValue(elementsHolder.getRef(), values);
    addValue(elementsHolder.getSet(), values);
    addValue(elementsHolder.getValue(), values);
    addValue(elementsHolder.getArray(), values);
    return values;
  }

  private static void addValue(DomElement valueElement, Set<DomElement> values) {
    if (DomUtil.hasXml(valueElement)) {
      values.add(valueElement);
    }
  }

  private static void reportSubtags(Set<DomElement> values, DomElementAnnotationHolder holder, @InspectionMessage String message) {
    for (DomElement value : values) {
      holder.createProblem(value, HighlightSeverity.ERROR, message, new RemoveDomElementQuickFix(value)).highlightWholeElement();
    }
  }

  private static void reportAttribute(DomElement element, DomElementAnnotationHolder holder, @InspectionMessage String message) {
    if (DomUtil.hasXml(element)) {
      holder.createProblem(element, HighlightSeverity.ERROR, message, new RemoveDomElementQuickFix(element)).highlightWholeElement();
    }
  }

  private static void reportNoValue(InfraValueHolder injection, DomElementAnnotationHolder holder, String elementName) {
    List<PsiType> types = TypeHolderUtil.getRequiredTypes(injection);
    ArrayList<LocalQuickFix> quickFixes = new ArrayList<>();
    quickFixes.add(new AddDomElementQuickFix<>(injection.getValueAttr()));
    for (PsiType psiClassType : types) {
      if (psiClassType instanceof PsiClassType type) {
        PsiClass psiClass = type.resolve();
        quickFixes.add(0, new AddRefFix(injection.getRefAttr(), psiClass));
        if (psiClass != null) {
          Project project = psiClass.getProject();
          PsiManager psiManager = PsiManager.getInstance(project);
          GlobalSearchScope scope = ProjectScope.getAllScope(project);
          PsiClass listClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(List.class.getName(), scope);
          if (InheritanceUtil.isInheritorOrSelf(psiClass, listClass, true)) {
            quickFixes.add(0, new AddListOrSetFix.List(injection.getList()));
          }
          else {
            PsiClass mapClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(Map.class.getName(), scope);
            if (InheritanceUtil.isInheritorOrSelf(psiClass, mapClass, true)) {
              quickFixes.add(0, new AddMapFix(injection.getMap()));
            }
            else {
              PsiClass setClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(Set.class.getName(), scope);
              if (InheritanceUtil.isInheritorOrSelf(psiClass, setClass, true)) {
                quickFixes.add(0, new AddListOrSetFix.Set(injection.getSet()));
              }
            }
          }
        }
      }
      else if (psiClassType instanceof PsiArrayType) {
        quickFixes.add(0, new AddListOrSetFix.Array(injection.getArray()));
        quickFixes.add(0, new AddListOrSetFix.Set(injection.getSet()));
        quickFixes.add(0, new AddListOrSetFix.List(injection.getList()));
      }
    }
    holder.createProblem(injection, HighlightSeverity.ERROR, message("model.inspection.injection.value.message", elementName),
            quickFixes.toArray(LocalQuickFix.EMPTY_ARRAY)).highlightWholeElement();
  }

  public static class AddRefFix extends AddDomElementQuickFix<GenericDomValue<?>> {
    @Nullable
    private final SmartPsiElementPointer<PsiClass> myPsiClassPointer;

    AddRefFix(GenericDomValue ref) {
      this(ref, null);
    }

    AddRefFix(GenericDomValue ref, @Nullable PsiClass psiClass) {
      super(ref);
      this.myPsiClassPointer = psiClass == null ? null : SmartPointerManager.getInstance(psiClass.getProject()).createSmartPsiElementPointer(psiClass);
    }

    public String getName() {
      String message = message("model.inspection.injection.value.add.ref");
      return message;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      PsiClass psiClass;
      super.applyFix(project, descriptor);
      if (this.myPsiClassPointer != null && (psiClass = this.myPsiClassPointer.getElement()) != null) {
        XmlElement element = this.myElement.getXmlElement();
        assert element != null;
        InfraModel model = InfraManager.from(project).getInfraModelByFile(element.getContainingFile());
        if (model != null) {
          ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes();
          List<BeanPointer<?>> list = InfraModelSearchers.findBeans(model, searchParameters);
          if (list.size() == 1) {
            this.myElement.setStringValue(BeanCoreUtils.getReferencedName(list.get(0), model.getAllCommonBeans()));
          }
        }
      }
    }
  }

  private abstract static class AddListOrSetFix extends AddDomElementQuickFix<ListOrSet> {

    AddListOrSetFix(ListOrSet listOrSet) {
      super(listOrSet);
    }

    public static class List extends AddListOrSetFix {

      List(ListOrSet listOrSet) {
        super(listOrSet);
      }

      public String getName() {
        String message = message("model.inspection.injection.value.add.list");
        return message;
      }
    }

    public static class Set extends AddListOrSetFix {

      Set(ListOrSet listOrSet) {
        super(listOrSet);
      }

      public String getName() {
        String message = message("model.inspection.injection.value.add.set");
        return message;
      }
    }

    public static class Array extends AddListOrSetFix {

      Array(ListOrSet listOrSet) {
        super(listOrSet);
      }

      public String getName() {
        String message = message("model.inspection.injection.value.add.array");
        return message;
      }
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
      InfraValue value = this.myElement.addValue();
      value.setStringValue("x");
      value.setStringValue("");
      PsiNavigateUtil.navigate(value.getXmlTag());
    }
  }

  public static class AddMapFix extends AddDomElementQuickFix<InfraMap> {

    AddMapFix(InfraMap map) {
      super(map);
    }

    public String getName() {
      String message = message("model.inspection.injection.value.add.map");
      return message;
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      this.myElement.addEntry();
    }
  }

  private static String getProperty() {
    return message("bean.property");
  }

  private static String getArg() {
    return message("bean.constructor.arg");
  }

  private static String getEntry() {
    return message("bean.map.entry");
  }
}

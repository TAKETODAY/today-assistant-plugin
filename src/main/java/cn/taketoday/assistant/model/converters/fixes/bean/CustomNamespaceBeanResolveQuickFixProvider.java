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

package cn.taketoday.assistant.model.converters.fixes.bean;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.DomReflectionUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.values.ClassValueConverter;
import com.intellij.util.xml.reflect.DomCollectionChildDescription;
import com.intellij.xml.XmlNamespaceHelper;
import com.intellij.xml.util.XmlUtil;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingConstants;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.BeanTypeProvider;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class CustomNamespaceBeanResolveQuickFixProvider implements BeanResolveQuickFixProvider {
  private static final Condition<QuickFixInfo> DEFINED_NAMESPACE_CONDITION
          = info -> info.namespaceResult == QuickFixInfo.NamespaceResult.DEFINED;

  private static QuickFixInfo getQuickFixInfo(XmlFile contextFile, DomCollectionChildDescription description, DomFileDescription<DomElement> domFileDescription) {
    String namespaceKey = description.getXmlName().getNamespaceKey();
    List<String> namespaces = namespaceKey == null ? Collections.emptyList() : domFileDescription.getAllowedNamespaces(namespaceKey, contextFile);
    String elementName = description.getXmlElementName();
    if (namespaces.isEmpty()) {
      return new QuickFixInfo(description, "<" + elementName + ">", QuickFixInfo.NamespaceResult.NOT_FOUND, null);
    }
    for (String namespace : namespaces) {
      String prefixByURI = XmlUtil.findNamespacePrefixByURI(contextFile, namespace);
      if (prefixByURI != null) {
        String namespacePrefixDisplay = prefixByURI.length() != 0 ? prefixByURI + ":" : "";
        return new QuickFixInfo(description, "<" + namespacePrefixDisplay + elementName + ">", QuickFixInfo.NamespaceResult.DEFINED, null);
      }
    }
    String namespaceURI = ContainerUtil.getFirstItem(namespaces);
    return new QuickFixInfo(description, "<" + elementName + "> (" + namespaceURI + ")", QuickFixInfo.NamespaceResult.UNDEFINED, namespaceURI);
  }

  private static void createCustomNamespaceBean(QuickFixInfo info, Beans beans, String beanId) {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(beans.getXmlElement())) {
      return;
    }
    if (info.namespaceResult == QuickFixInfo.NamespaceResult.UNDEFINED) {
      XmlFile containingFile = (XmlFile) beans.getXmlTag().getContainingFile();
      XmlNamespaceHelper extension = XmlNamespaceHelper.getHelper(containingFile);
      extension.insertNamespaceDeclaration(containingFile, null, Collections.singleton(info.namespaceURI), null, null);
    }
    DomInfraBean domSpringBean = (DomInfraBean) info.description.addValue(beans);
    domSpringBean.getId().ensureXmlElementExists();
    domSpringBean.setName(beanId);
  }

  @Override
  public List<LocalQuickFix> getQuickFixes(ConvertContext context, Beans beans, @Nullable String beanId, List<PsiClassType> requiredClasses) {
    if (requiredClasses.isEmpty()) {
      return Collections.emptyList();
    }
    GenericDomValue<BeanPointer<?>> value = (GenericDomValue<BeanPointer<?>>) context.getInvocationElement();
    String stringValue = beanId != null ? beanId : value.getStringValue();
    if (stringValue == null) {
      return Collections.emptyList();
    }
    String id = stringValue.trim();
    if (StringUtil.isEmptyOrSpaces(id)) {
      return Collections.emptyList();
    }
    Module module = context.getModule();
    if (module == null || module.isDisposed()) {
      return Collections.emptyList();
    }
    DomFileDescription<DomElement> domFileDescription = DomUtil.getFileElement(beans).getFileDescription();
    XmlFile contextFile = context.getFile();
    Map<DomCollectionChildDescription, PsiClass> mappings = getAvailableDomToPsiClassMappings(beans, contextFile);
    List<PsiClass> resolvedRequiredClasses = ContainerUtil.mapNotNull(requiredClasses, type -> {
      PsiClass resolve = type.resolve();
      if (resolve == null || "java.lang.Object".equals(resolve.getQualifiedName())) {
        return null;
      }
      return resolve;
    });

    SmartList<QuickFixInfo> smartList = new SmartList<>();
    for (Map.Entry<DomCollectionChildDescription, PsiClass> mapping : mappings.entrySet()) {
      Iterator<PsiClass> it = resolvedRequiredClasses.iterator();
      while (true) {
        if (it.hasNext()) {
          PsiClass required = it.next();
          if (InheritanceUtil.isInheritorOrSelf(mapping.getValue(), required, true)) {
            QuickFixInfo quickFixInfo = getQuickFixInfo(contextFile, mapping.getKey(), domFileDescription);
            smartList.add(quickFixInfo);
            break;
          }
        }
      }
    }
    List<QuickFixInfo> defined = ContainerUtil.filter(smartList, DEFINED_NAMESPACE_CONDITION);
    SmartList<LocalQuickFix> smartList2 = new SmartList<>();
    for (QuickFixInfo info : defined) {
      smartList2.add(new CreateDefinedQuickFix(info, beans, id));
    }
    smartList.removeAll(defined);
    if (!smartList.isEmpty()) {
      smartList2.add(new CreateFromUndefinedNamespaceQuickFix(smartList, beans, id));
    }
    return smartList2;
  }

  private static Map<DomCollectionChildDescription, PsiClass> getAvailableDomToPsiClassMappings(Beans beans, XmlFile contextFile) {
    return CachedValuesManager.getCachedValue(contextFile, () -> {
      GlobalSearchScope searchScope = ClassValueConverter.getScope(contextFile.getProject(), ModuleUtilCore.findModuleForFile(contextFile), contextFile);
      JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(contextFile.getProject());
      Map<DomCollectionChildDescription, PsiClass> result = new LinkedHashMap<>();
      List<? extends DomCollectionChildDescription> descriptions = beans.getGenericInfo().getCollectionChildrenDescriptions();
      for (DomCollectionChildDescription description : descriptions) {
        Type childDescriptionType = description.getType();
        Class<?> type = ReflectionUtil.getRawType(childDescriptionType);
        if (DomInfraBean.class.isAssignableFrom(type)) {
          Class<DomElement> clazz = (Class) childDescriptionType;
          BeanType beanType = DomReflectionUtil.findAnnotationDFS(clazz, BeanType.class);
          if (beanType != null) {
            PsiClass beanClass = null;
            if (!beanType.value().isEmpty()) {
              String staticBeanClass = beanType.value();
              beanClass = javaPsiFacade.findClass(staticBeanClass, searchScope);
            }
            else {
              Class<? extends BeanTypeProvider> provider = beanType.provider();
              BeanTypeProvider provider1 = InfraUtils.getBeanTypeProvider(provider);
              for (String candidateFqn : provider1.getBeanTypeCandidates()) {
                beanClass = javaPsiFacade.findClass(candidateFqn, searchScope);
                if (beanClass != null) {
                  break;
                }
              }
            }
            if (beanClass != null) {
              result.put(description, beanClass);
            }
          }
        }
      }
      return CachedValueProvider.Result.create(result, contextFile);
    });
  }

  public static final class QuickFixInfo {
    private final DomCollectionChildDescription description;
    private final String displayName;
    private final NamespaceResult namespaceResult;
    private final String namespaceURI;

    public enum NamespaceResult {
      NOT_FOUND,
      UNDEFINED,
      DEFINED
    }

    private QuickFixInfo(DomCollectionChildDescription domCollectionChildDescription, @NlsContexts.ListItem String name, NamespaceResult result, String uri) {
      this.description = domCollectionChildDescription;
      this.displayName = name;
      this.namespaceResult = result;
      this.namespaceURI = uri;
    }
  }

  public static final class CreateFromUndefinedNamespaceQuickFix implements LocalQuickFix {
    private final List<QuickFixInfo> myQuickFixInfos;
    private final Beans myBeans;
    private final String myId;

    private CreateFromUndefinedNamespaceQuickFix(List<QuickFixInfo> infos, Beans beans, String id) {
      this.myQuickFixInfos = infos;
      this.myBeans = beans;
      this.myId = id;
    }

    public String getName() {
      String message = message("custom.namespace.quick.fixes.create.custom.namespace.bean", this.myQuickFixInfos.size());
      return message;
    }

    public String getFamilyName() {
      return message("custom.namespace.quick.fixes.family.name");
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        doApplyFix(ContainerUtil.getFirstItem(this.myQuickFixInfos), project);
        return;
      }
      ListPopup popup = JBPopupFactory.getInstance()
              .createListPopup(new BaseListPopupStep<>(message("custom.namespace.quick.fixes.popup.title"),
                      myQuickFixInfos, Icons.SpringBean) {
                public String getTextFor(QuickFixInfo value) {
                  return value.displayName;
                }

                public boolean isSpeedSearchEnabled() {
                  return true;
                }

                public PopupStep onChosen(QuickFixInfo selectedValue, boolean finalChoice) {
                  if (selectedValue != null) {
                    CreateFromUndefinedNamespaceQuickFix.this.doApplyFix(selectedValue, project);
                  }
                  return FINAL_CHOICE;
                }
              });
      popup.setAdText(message("bean.filter.tooltip"), SwingConstants.LEFT);
      DataManager.getInstance().getDataContextFromFocusAsync()
              .onSuccess(popup::showInBestPositionFor);
    }

    private void doApplyFix(QuickFixInfo selectedValue, Project project) {
      WriteCommandAction.writeCommandAction(project)
              .withName(message("custom.namespace.quick.fixes.create.custom.namespace.bean.with.id", this.myId))
              .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
              .run(() -> {
                createCustomNamespaceBean(selectedValue, this.myBeans, this.myId);
              });
    }
  }

  private static final class CreateDefinedQuickFix implements LocalQuickFix, HighPriorityAction {
    private final QuickFixInfo myQuickFixInfo;
    private final Beans myBeans;
    private final String myId;

    private CreateDefinedQuickFix(QuickFixInfo quickFixInfo, Beans beans, String id) {
      this.myQuickFixInfo = quickFixInfo;
      this.myBeans = beans;
      this.myId = id;
    }

    public String getName() {
      return message("custom.namespace.quick.fixes.create.name", this.myQuickFixInfo.displayName);
    }

    public String getFamilyName() {
      return message("custom.namespace.quick.fixes.family.name");
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      createCustomNamespaceBean(this.myQuickFixInfo, this.myBeans, this.myId);
    }
  }
}

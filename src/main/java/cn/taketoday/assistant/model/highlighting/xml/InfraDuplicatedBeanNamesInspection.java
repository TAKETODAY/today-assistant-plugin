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

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.ide.DataManager;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.NullableFunction;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.StringTokenizer;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Scope;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.xml.util.PsiElementPointer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Alias;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public final class InfraDuplicatedBeanNamesInspection extends InfraBeanInspectionBase {

  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    PsiFile file = domFileElement.getFile();
    InfraModel model = InfraManager.from(file.getProject()).getInfraModelByFile(file);
    if (model == null) {
      return;
    }
    Beans beans = domFileElement.getRootElement();
    checkRootBeans(holder, model, beans);
  }

  private static void checkRootBeans(DomElementAnnotationHolder holder, InfraModel model, Beans beans) {
    checkBeans(holder, model, beans);
    for (Beans beanProfiles : beans.getBeansProfiles()) {
      checkRootBeans(holder, model, beanProfiles);
    }
  }

  private static void checkBeans(DomElementAnnotationHolder holder, InfraModel model, Beans beans) {
    List<CommonInfraBean> allBeans = new ArrayList<>();
    Processor<CommonInfraBean> processor = Processors.cancelableCollectProcessor(allBeans);
    InfraBeanUtils.of().processChildBeans(beans, false, processor);
    InfraProfile profile = beans.getProfile();
    for (CommonInfraBean bean : allBeans) {
      if (bean instanceof DomInfraBean) {
        checkBean((DomInfraBean) bean, holder, model, profile);
      }
    }
    for (Alias alias : beans.getAliases()) {
      checkAlias(alias, holder, model, profile);
    }
  }

  private static void checkBean(DomInfraBean bean, DomElementAnnotationHolder holder, InfraModel infraModel, InfraProfile profile) {
    GenericAttributeValue<String> beanId = bean.getId();
    String id = beanId.getStringValue();
    if (id != null && beanId.getAnnotation(Scope.class) == null) {
      LocalQuickFix[] fixes = getFixes(id, InfraBeanService.of().createBeanPointer(bean), infraModel, profile);
      if (fixes.length > 0) {
        holder.createProblem(beanId, HighlightSeverity.ERROR, InfraBundle.message("bean.duplicate.bean.name"), fixes);
      }
    }
    if (bean instanceof InfraBean infraBean) {
      GenericAttributeValue<List<String>> name = infraBean.getName();
      String value = name.getStringValue();
      if (value != null) {
        StringTokenizer tokenizer = new StringTokenizer(value, InfraUtils.INFRA_DELIMITERS);
        while (tokenizer.hasMoreTokens()) {
          String s = tokenizer.nextToken();
          LocalQuickFix[] fixes2 = getFixes(s, InfraBeanService.of().createBeanPointer(bean), infraModel, profile);
          if (fixes2.length > 0) {
            holder.createProblem(name, HighlightSeverity.ERROR, InfraBundle.message("bean.duplicate.bean.name"),
                    TextRange.from((tokenizer.getCurrentPosition() - s.length()) + 1, s.length()), fixes2);
          }
        }
      }
    }
  }

  private static void checkAlias(Alias alias, DomElementAnnotationHolder holder, InfraModel model, InfraProfile profile) {
    GenericAttributeValue<String> value = alias.getAlias();
    String aliasName = value.getStringValue();
    if (aliasName != null) {
      LocalQuickFix[] fixes = getFixes(aliasName, alias.getAliasedBean().getValue(), model, profile);
      if (fixes.length > 0) {
        holder.createProblem(value, HighlightSeverity.ERROR, InfraBundle.message("bean.duplicate.bean.name"), fixes);
      }
    }
  }

  private static LocalQuickFix[] getFixes(final String name, final PsiElementPointer bean, InfraModel model, InfraProfile profile) {
    if (bean == null) {
      return LocalQuickFix.EMPTY_ARRAY;
    }
    final Collection<BeanPointer<?>> pointers = InfraModelSearchers.findBeans(model, name).stream().filter(pointer -> {
      return !pointer.equals(bean) && isPointerInTheSameProfiles(profile, pointer.getPsiElement());
    }).collect(Collectors.toSet());
    if (pointers.isEmpty()) {
      return LocalQuickFix.EMPTY_ARRAY;
    }
    LocalQuickFix fix = new LocalQuickFix() {

      public String getFamilyName() {
        String message = InfraBundle.message("duplicated.bean.name.inspection.name.view.duplicates");
        return message;
      }

      public boolean startInWriteAction() {
        return false;
      }

      public void applyFix(Project project, ProblemDescriptor descriptor) {
        List<FakePsiElement> duplications = ContainerUtil.mapNotNull(pointers, new MyFunction(bean, name));
        if (duplications.size() > 1 && !ApplicationManager.getApplication().isUnitTestMode()) {
          NavigationUtil.getPsiElementPopup(PsiUtilCore.toPsiElementArray(duplications), InfraBundle.message("duplicated.bean.quick.fix.popup.title"))
                  .showInBestPositionFor(DataManager.getInstance().getDataContext());
        }
        else if (!duplications.isEmpty()) {
          duplications.get(0).navigate(true);
        }
      }
    };
    return new LocalQuickFix[] { fix };
  }

  private static boolean isPointerInTheSameProfiles(InfraProfile profile, @Nullable PsiElement pointerPsiElement) {
    DomElement domElement;
    Beans parentBeans;
    if (pointerPsiElement == null) {
      return false;
    }
    Set<String> profiles = profile.getExpressions();
    if ((pointerPsiElement instanceof PsiMember) && profiles.size() == 1 && InfraProfile.DEFAULT_PROFILE_NAME.equals(profiles.iterator().next())) {
      return true;
    }
    if (pointerPsiElement instanceof PomTargetPsiElement) {
      pointerPsiElement = pointerPsiElement.getNavigationElement();
      if (pointerPsiElement == null) {
        return false;
      }
    }
    XmlTag xmlTag = PsiTreeUtil.getParentOfType(pointerPsiElement, XmlTag.class, false);
    if (xmlTag == null || (domElement = DomUtil.getDomElement(xmlTag)) == null || (parentBeans = DomUtil.getParentOfType(domElement, Beans.class, true)) == null) {
      return false;
    }
    for (String name : parentBeans.getProfile().getExpressions()) {
      if (profiles.contains(name)) {
        return true;
      }
    }
    return false;
  }

  public static class MyFunction implements NullableFunction<PsiElementPointer, FakePsiElement> {
    private final PsiElementPointer myBean;
    private final String myName;

    MyFunction(PsiElementPointer bean, String name) {
      this.myBean = bean;
      this.myName = name;
    }

    public FakePsiElement fun(final PsiElementPointer pointer) {
      final PsiElement psiElement = pointer.getPsiElement();
      if (psiElement == null) {
        return null;
      }
      final PsiFile containingFile = psiElement.getContainingFile();
      if (!psiElement.equals(this.myBean.getPsiElement())) {
        return new FakePsiElement() {

          public PsiElement getNavigationElement() {
            PsiElement psiElement2 = psiElement;
            return psiElement2;
          }

          public String getName() {
            return MyFunction.this.myName;
          }

          public Icon getIcon(boolean open) {
            if (pointer instanceof BeanPointer) {
              return InfraPresentationProvider.getInfraIcon(pointer);
            }
            return null;
          }

          public PsiElement getParent() {
            return containingFile;
          }

          public String getLocationString() {
            return "(" + containingFile.getName() + ")";
          }
        };
      }
      return null;
    }
  }
}

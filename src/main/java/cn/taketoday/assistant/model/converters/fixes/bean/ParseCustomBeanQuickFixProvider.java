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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.custom.ParseCustomBeanIntention;
import cn.taketoday.lang.Nullable;

class ParseCustomBeanQuickFixProvider implements BeanResolveQuickFixProvider {

  @Override
  public List<LocalQuickFix> getQuickFixes(ConvertContext context, Beans beans, @Nullable String beanId, List<PsiClassType> requiredClasses) {
    GenericDomValue<BeanPointer<?>> beanPointer = (GenericDomValue<BeanPointer<?>>) context.getInvocationElement();
    String parseBeanId = beanId != null ? beanId : beanPointer.getStringValue();
    if (StringUtil.isEmptyOrSpaces(parseBeanId)) {
      return Collections.emptyList();
    }
    PsiFile file = context.getFile();
    InfraModel infraModel = InfraManager.from(file.getProject()).getInfraModelByFile(file);
    if (infraModel == null) {
      return Collections.emptyList();
    }
    Collection<XmlTag> tags = InfraModelVisitorUtils.getCustomBeanCandidates(infraModel, parseBeanId);
    if (tags.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new TryParsingCustomBeansFix(tags));
  }

  private static final class TryParsingCustomBeansFix implements LocalQuickFix, IntentionAction {
    private final Collection<XmlTag> myTags;

    private TryParsingCustomBeansFix(Collection<XmlTag> tags) {
      this.myTags = tags;
    }

    public String getText() {
      return getName();
    }

    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      ParseCustomBeanIntention.invokeCustomBeanParsers(project, this.myTags);
    }

    public boolean startInWriteAction() {
      return false;
    }

    public String getFamilyName() {
      return InfraBundle.message("try.parsing.custom.beans");
    }

    public void applyFix(Project project, ProblemDescriptor descriptor) {
      throw new UnsupportedOperationException("Method applyFix is not yet implemented in " + getClass().getName());
    }
  }
}

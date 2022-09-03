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

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.codeInsight.generation.MemberChooserObjectBase;
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;

import java.util.Objects;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.model.BeanPointer;

public final class InfraBeanClassMember extends MemberChooserObjectBase implements ClassMember {
  private final BeanPointer<?> myInfraBean;

  public InfraBeanClassMember(BeanPointer<?> bean) {
    super(InfraPresentationProvider.getBeanName(bean), InfraPresentationProvider.getInfraIcon(bean));
    this.myInfraBean = bean;
  }

  public MemberChooserObject getParentNodeDelegate() {
    return new InfraFileMemberChooserObjectBase(myInfraBean.getContainingFile());
  }

  public BeanPointer<?> getInfraBean() {
    return this.myInfraBean;
  }

  private static final class InfraFileMemberChooserObjectBase extends PsiElementMemberChooserObject {

    InfraFileMemberChooserObjectBase(PsiFile psiFile) {
      super(psiFile, psiFile.getName(), psiFile instanceof XmlFile ? Icons.SpringConfig : Icons.SpringJavaConfig);
    }
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InfraBeanClassMember member = (InfraBeanClassMember) o;
    return Objects.equals(this.myInfraBean, member.myInfraBean);
  }

  public int hashCode() {
    if (this.myInfraBean != null) {
      return this.myInfraBean.hashCode();
    }
    return 0;
  }
}

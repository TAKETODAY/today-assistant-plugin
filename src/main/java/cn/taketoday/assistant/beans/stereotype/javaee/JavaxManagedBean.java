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

package cn.taketoday.assistant.beans.stereotype.javaee;

import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;

import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 15:15
 */
public class JavaxManagedBean extends InfraStereotypeElement {
  public static final JamClassMeta<JavaxManagedBean> META = new JamClassMeta<>(null, JavaxManagedBean.class,
          JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("JavaxManagedBean"));

  static {
    addPomTargetProducer(META);
  }

  public JavaxManagedBean(PsiClass psiClass) {
    super(JavaeeConstant.JAVAX_MANAGED_BEAN, PsiElementRef.real(psiClass));
  }
}

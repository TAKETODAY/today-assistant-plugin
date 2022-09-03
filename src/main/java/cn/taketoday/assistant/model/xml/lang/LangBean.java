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

package cn.taketoday.assistant.model.xml.lang;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiType;
import com.intellij.util.ArrayUtilRt;

import java.util.Set;

import cn.taketoday.assistant.model.scripts.ScriptBeanPsiClassDiscoverer;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.BeanTypeProvider;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.lang.Nullable;

@BeanType(provider = LangBean.LangBeanBeanTypeProvider.class)
public interface LangBean extends DomInfraBean, SimpleScript {

  public static class LangBeanBeanTypeProvider implements BeanTypeProvider<LangBean> {

    @Override
    public String[] getBeanTypeCandidates() {
      return ArrayUtilRt.EMPTY_STRING_ARRAY;
    }

    @Override
    @Nullable
    public String getBeanType(LangBean bean) {
      PsiClass psiClass;
      Set<PsiFileSystemItem> items = bean.getScriptSource().getValue();
      if (items != null && items.size() > 0) {
        for (PsiFileSystemItem psiFileSystemItem : items) {
          if (psiFileSystemItem instanceof PsiFile) {
            for (ScriptBeanPsiClassDiscoverer psiClassDiscoverer : ScriptBeanPsiClassDiscoverer.EP_NAME.getExtensionList()) {
              PsiType inferredScriptReturnType = psiClassDiscoverer.getInferredScriptReturnType((PsiFile) psiFileSystemItem);
              if ((inferredScriptReturnType instanceof PsiClassType psiClassType) && (psiClass = psiClassType.resolve()) != null) {
                return psiClass.getQualifiedName();
              }
            }
          }
          if (psiFileSystemItem instanceof PsiClassOwner) {
            PsiClass[] classes = ((PsiClassOwner) psiFileSystemItem).getClasses();
            if (classes.length == 1) {
              return classes[0].getQualifiedName();
            }
          }
        }
        return null;
      }
      return null;
    }
  }
}

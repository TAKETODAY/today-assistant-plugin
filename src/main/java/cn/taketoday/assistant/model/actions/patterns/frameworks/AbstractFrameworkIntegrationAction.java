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
package cn.taketoday.assistant.model.actions.patterns.frameworks;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlFile;

import java.util.Map;

import cn.taketoday.lang.Nullable;

public abstract class AbstractFrameworkIntegrationAction extends FrameworkIntegrationAction {

  @Override
  protected void generateSpringBeans(final Module module, final Editor editor, final XmlFile xmlFile) {
    FrameworkSupportTemplatesRunner.getInstance().generateSpringBeans(this, module, editor, xmlFile);
  }

  @Override

  public Map<String, String> getPredefinedVars(Module module, XmlFile xmlFile) {
    return FrameworkSupportTemplatesRunner.getInstance().getPredefinedVars(module, xmlFile);
  }

  @Override
  public void addFacet(final Module module) {
    if (module == null)
      return;

    final String facetId = getFacetId();
    if (!StringUtil.isEmptyOrSpaces(facetId)) {

      final FacetManager facetManager = FacetManager.getInstance(module);
      final FacetType<?, ?> type = FacetTypeRegistry.getInstance().findFacetType(facetId);

      if (type != null) {

        if (facetManager.getFacetByType(type.getId()) == null) {
          final ModifiableFacetModel model = facetManager.createModifiableModel();

          final Facet facet = facetManager.addFacet(type, type.getDefaultFacetName(), null);

          model.addFacet(facet);
          model.commit();
        }
      }
    }
  }

  @Nullable
  protected String getFacetId() {
    return null;
  }
}

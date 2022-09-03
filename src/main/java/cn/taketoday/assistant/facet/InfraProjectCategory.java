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

package cn.taketoday.assistant.facet;

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.ide.projectWizard.ModuleTypeCategory;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.util.registry.Registry;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;

public class InfraProjectCategory extends ModuleTypeCategory.Java {
  public static final String LEGACY_MODULE_BUILDER_ID = "LegacyInfra";
  public static final FrameworkRole ROLE = new FrameworkRole(InfraFacet.FACET_TYPE_ID.toString());

  public String getId() {
    return InfraFacet.FACET_TYPE_ID.toString();
  }

  public String getDisplayName() {
    return "Infra (Legacy)";
  }

  public Icon getIcon() {
    return Icons.Today;
  }

  public String getDescription() {
    return null;
  }

  public String[] getPreselectedFrameworkIds() {
    return new String[] { FacetBasedFrameworkSupportProvider.getProviderId(InfraFacet.FACET_TYPE_ID) };
  }

  public FrameworkRole[] getAcceptableFrameworkRoles() {
    return new FrameworkRole[] { ROLE };
  }

  public ModuleBuilder createModuleBuilder() {
    return new JavaModuleBuilder() {

      public String getBuilderId() {
        return InfraProjectCategory.LEGACY_MODULE_BUILDER_ID;
      }

      public boolean isAvailable() {
        return Registry.is("javaee.legacy.project.wizard", false);
      }
    };
  }
}

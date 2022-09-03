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
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.framework.library.LibraryBasedFrameworkSupportProvider;
import com.intellij.framework.library.LibraryBasedFrameworkType;
import com.intellij.ide.util.projectWizard.ModuleBuilder;

public abstract class InfraLibraryFrameworkType extends LibraryBasedFrameworkType {

  protected InfraLibraryFrameworkType(String id, Class<? extends DownloadableLibraryType> libraryTypeClass) {
    super(id, libraryTypeClass);
  }

  public final String getUnderlyingFrameworkTypeId() {
    return FacetBasedFrameworkSupportProvider.getProviderId(InfraFacet.FACET_TYPE_ID);
  }

  public FrameworkSupportInModuleProvider createProvider() {
    return new LibraryBasedFrameworkSupportProvider(this, getLibraryTypeClass()) {

      public boolean isEnabledForModuleBuilder(ModuleBuilder builder) {
        return InfraProjectCategory.LEGACY_MODULE_BUILDER_ID.equals(builder.getBuilderId());
      }
    };
  }
}

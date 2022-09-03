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
package cn.taketoday.assistant.model.actions.patterns.frameworks.ui;

import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.module.Module;

import org.jetbrains.annotations.Nls;

import cn.taketoday.assistant.model.actions.patterns.frameworks.util.StandardBeansDocLinksManager;

public class TemplateInfo {
  private final Template template;
  private final @Nls String name;
  private final String referenceLink;
  private final String myApiLink;
  private final String description;
  private boolean isAccepted;

  public TemplateInfo(Module module, Template template, @Nls String name, String description) {
    this(module, template, name, description, true);
  }

  public TemplateInfo(Module module, Template template, @Nls String name, String description, boolean isAccepted) {
    this.template = template;
    this.name = name;
    StandardBeansDocLinksManager linksManager = StandardBeansDocLinksManager.getInstance();
    referenceLink = linksManager.getReferenceLink(template.getId());
    myApiLink = linksManager.getApiLink(template.getId());
    this.description = description;
    this.isAccepted = isAccepted;
  }

  public Template getTemplate() {
    return template;
  }

  public boolean isAccepted() {
    return isAccepted;
  }

  public void setAccepted(boolean accepted) {
    isAccepted = accepted;
  }

  public @Nls String getName() {
    return name;
  }

  public String getDescription() {
    return description == null ? "" : description;
  }

  public String getReferenceLink() {
    return referenceLink;
  }

  public String getApiLink() {
    return myApiLink;
  }

}

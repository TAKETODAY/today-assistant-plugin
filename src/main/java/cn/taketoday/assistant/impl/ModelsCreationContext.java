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

package cn.taketoday.assistant.impl;

import com.intellij.openapi.module.Module;

public final class ModelsCreationContext {
  private final Module myModule;
  private boolean modelsFromModuleDependencies;
  private boolean modelsFromDependentModules;

  private ModelsCreationContext(Module module) {
    this.modelsFromModuleDependencies = false;
    this.modelsFromDependentModules = false;
    this.myModule = module;
  }

  public static ModelsCreationContext create(Module module) {
    return new ModelsCreationContext(module);
  }

  public static ModelsCreationContext fromEverywhere(Module module) {
    return create(module).loadModelsFromDependentModules().loadModelsFromModuleDependencies();
  }

  public Module getModule() {
    return this.myModule;
  }

  public boolean onlyCurrentModule() {
    return !this.modelsFromDependentModules && !this.modelsFromModuleDependencies;
  }

  public ModelsCreationContext loadModelsFromModuleDependencies() {
    return loadModelsFromModuleDependencies(true);
  }

  public ModelsCreationContext loadModelsFromModuleDependencies(boolean modelsFromModuleDependencies) {
    this.modelsFromModuleDependencies = modelsFromModuleDependencies;
    return this;
  }

  public ModelsCreationContext loadModelsFromDependentModules() {
    return loadModelsFromDependentModules(true);
  }

  public ModelsCreationContext loadModelsFromDependentModules(boolean modelsFromDependentModules) {
    this.modelsFromDependentModules = modelsFromDependentModules;
    return this;
  }

  public boolean isLoadModelsFromModuleDependencies() {
    return this.modelsFromModuleDependencies;
  }

  public boolean isLoadModelsFromDependentModules() {
    return this.modelsFromDependentModules;
  }
}

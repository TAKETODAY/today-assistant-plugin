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
package cn.taketoday.assistant.model.utils.resources;

import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.util.Function;

import java.util.Collection;

import javax.swing.Icon;

import cn.taketoday.lang.Nullable;

public final class InfraResourcesBuilder {
  private final PsiElement myElement;
  private final String myText;
  private boolean myFromRoot;
  private boolean myFromCurrent;
  private int myOffset;
  private boolean mySoft = true;
  private Condition<PsiFileSystemItem> myFilter = Conditions.alwaysTrue();
  private boolean myEndingSlashNotAllowed = true;
  @Nullable
  private Function<PsiFile, Collection<PsiFileSystemItem>> myCustomDefaultPathEvaluator;
  private Module[] myModules = Module.EMPTY_ARRAY;
  @Nullable
  private String myTemplateName;

  private InfraResourcesBuilder(PsiElement element, String text) {
    this(element, text, ElementManipulators.getOffsetInElement(element));
  }

  private InfraResourcesBuilder(PsiElement element,
          String text,
          int offset) {
    myElement = element;
    myText = text;
    myOffset = offset;
  }

  public static InfraResourcesBuilder create(PsiElement element, String s) {
    return new InfraResourcesBuilder(element, s);
  }

  public static InfraResourcesBuilder create(PsiElement element, String s, int offset) {
    return new InfraResourcesBuilder(element, s, offset);
  }

  public static InfraResourcesBuilder create(Module module, String s, int offset) {
    return new InfraResourcesBuilder(new DummyPsiElement(module), s, offset);
  }

  public InfraResourcesBuilder fromRoot(boolean fromRoot) {
    myFromRoot = fromRoot;
    return this;
  }

  public InfraResourcesBuilder fromCurrent(boolean fromCurrent) {
    myFromCurrent = fromCurrent;
    return this;
  }

  public InfraResourcesBuilder offset(int offset) {
    myOffset = offset;
    return this;
  }

  public InfraResourcesBuilder soft(boolean soft) {
    mySoft = soft;
    return this;
  }

  public InfraResourcesBuilder endingSlashNotAllowed(boolean endingSlashNotAllowed) {
    myEndingSlashNotAllowed = endingSlashNotAllowed;
    return this;
  }

  public InfraResourcesBuilder filter(Condition<PsiFileSystemItem> filter) {
    myFilter = filter;
    return this;
  }

  public InfraResourcesBuilder customDefaultPathEvaluator(@Nullable Function<PsiFile, Collection<PsiFileSystemItem>> customDefaultPathEvaluator) {
    myCustomDefaultPathEvaluator = customDefaultPathEvaluator;
    return this;
  }

  public InfraResourcesBuilder modules(Module... modules) {
    myModules = modules;
    return this;
  }

  /**
   * Sets template name to use when creating not existing file.
   *
   * @param templateName File template name.
   * @return This.
   * @see com.intellij.ide.fileTemplates.FileTemplateManager
   */
  public InfraResourcesBuilder newFileTemplateName(String templateName) {
    myTemplateName = templateName;
    return this;
  }

  PsiElement getElement() {
    return myElement;
  }

  String getText() {
    return myText;
  }

  boolean isFromRoot() {
    return myFromRoot;
  }

  boolean isFromCurrent() {
    return myFromCurrent;
  }

  int getOffset() {
    return myOffset;
  }

  boolean isSoft() {
    return mySoft;
  }

  Condition<PsiFileSystemItem> getFilter() {
    return myFilter;
  }

  boolean isEndingSlashNotAllowed() {
    return myEndingSlashNotAllowed;
  }

  @Nullable
  Function<PsiFile, Collection<PsiFileSystemItem>> getCustomDefaultPathEvaluator() {
    return myCustomDefaultPathEvaluator;
  }

  Module[] getModules() {
    return myModules;
  }

  @Nullable
  String getTemplateName() {
    return myTemplateName;
  }

  /**
   * It is a hack to deceive the {@link ResourcesUtil} employing it's power to resolve Spring-syntax references (like classpath-prefixed)
   * even when there is no PsiElement is available.
   * This {@link DummyPsiElement} provides the access to {@link Module} and {@link Project} info which is required for resolve in {@link ResourcesUtil}
   */
  private static final class DummyPsiElement extends RenameableFakePsiElement {
    private final NotNullLazyValue<PsiFile> myFileNotNullLazyValue;
    private final Module myModule;

    private DummyPsiElement(Module module) {
      super(null);
      myModule = module;
      myFileNotNullLazyValue = NotNullLazyValue.lazy(() -> {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText("DummyFile.txt", FileTypes.PLAIN_TEXT, "");
        psiFile.putUserData(ModuleUtilCore.KEY_MODULE, myModule);
        return psiFile;
      });
    }

    @Override
    public Project getProject() {
      return myModule.getProject();
    }

    @Override
    public PsiFile getContainingFile() {
      return myFileNotNullLazyValue.getValue();
    }

    @Override
    public String getName() {
      return null;
    }

    @Nullable
    @Override
    public String getTypeName() {
      return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
      return null;
    }
  }

}

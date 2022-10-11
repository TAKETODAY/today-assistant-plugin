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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.microservices.jvm.config.ConfigKeyPathBeanPropertyResolver;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyLookupElementBuilder;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTypesUtil;

import javax.swing.Icon;

import cn.taketoday.lang.Nullable;

class InfraApplicationMetaConfigKeyImpl implements MetaConfigKey {
  private final SmartPsiElementPointer<PsiElement> myDeclaration;

  private final MetaConfigKey.DeclarationResolveResult myDeclarationResolveResult;
  private final String myName;
  private final DescriptionText myDescriptionText;
  private final String myDefaultValue;
  private final Deprecation myDeprecation;
  @Nullable
  private final PsiType myType;
  private final ItemHint myItemHint;
  private final ItemHint myKeyItemHint;
  private final AccessType myAccessType;
  private final NullableLazyValue<PsiType> myEffectiveValueType;
  @Nullable
  private final PsiClass myMapKeyType;
  private final ConfigKeyPathBeanPropertyResolver myResolver;
  private final MetaConfigKeyPresentation myPresentation;

  InfraApplicationMetaConfigKeyImpl(PsiElement declaration, MetaConfigKey.DeclarationResolveResult declarationResolveResult, String name,
          MetaConfigKey.DescriptionText descriptionText, @Nullable String defaultValue, MetaConfigKey.Deprecation deprecation, @Nullable PsiType type,
          MetaConfigKey.AccessType accessType, MetaConfigKey.ItemHint itemHint, MetaConfigKey.ItemHint keyItemHint, ConfigKeyPathBeanPropertyResolver resolver) {

    this.myPresentation = new MetaConfigKeyPresentation() {

      public Icon getIcon() {
        switch (myAccessType) {
          case INDEXED -> {
            return AllIcons.Nodes.PropertyRead;
          }
          case MAP -> {
            return AllIcons.Nodes.PropertyWrite;
          }
          case ENUM_MAP -> {
            return AllIcons.Nodes.PropertyWriteStatic;
          }
          default -> {
            return AllIcons.Nodes.Property;
          }
        }
      }

      public LookupElementBuilder getLookupElement() {
        return getLookupElement(InfraApplicationMetaConfigKeyImpl.this.getName());
      }

      public LookupElementBuilder getLookupElement(String lookupString) {
        return MetaConfigKeyLookupElementBuilder.create(InfraApplicationMetaConfigKeyImpl.this, lookupString);
      }

      public LookupElement tuneLookupElement(LookupElement lookupElement) {
        if (InfraApplicationMetaConfigKeyImpl.this.getDeprecation() != Deprecation.NOT_DEPRECATED) {
          return PrioritizedLookupElement.withPriority(lookupElement, -100.0d);
        }
        if (InfraApplicationMetaConfigKeyImpl.this.getDeclarationResolveResult() == DeclarationResolveResult.JSON_UNRESOLVED_SOURCE_TYPE) {
          return PrioritizedLookupElement.withPriority(lookupElement, -50.0d);
        }
        if (InfraApplicationMetaConfigKeyImpl.this.getDeclarationResolveResult() == DeclarationResolveResult.ADDITIONAL_JSON) {
          return PrioritizedLookupElement.withPriority(lookupElement, 50.0d);
        }
        return lookupElement;
      }
    };
    this.myDeclaration = SmartPointerManager.getInstance(declaration.getProject()).createSmartPsiElementPointer(declaration);
    this.myDeclarationResolveResult = declarationResolveResult;
    this.myName = name;
    this.myDescriptionText = descriptionText;
    this.myDefaultValue = defaultValue;
    this.myDeprecation = deprecation;
    this.myType = type;
    this.myItemHint = itemHint;
    this.myKeyItemHint = keyItemHint;
    if (accessType == AccessType.MAP) {
      this.myMapKeyType = getMapKeyClass();
      boolean isEnumMapKeyType = this.myMapKeyType != null && this.myMapKeyType.isEnum();
      this.myAccessType = isEnumMapKeyType ? AccessType.ENUM_MAP : accessType;
    }
    else {
      this.myMapKeyType = null;
      this.myAccessType = accessType;
    }
    this.myEffectiveValueType = NullableLazyValue.volatileLazyNullable(() -> {
      if (this.myType == null) {
        return null;
      }
      return this.myAccessType.getEffectiveValueType(this.myType);
    });
    this.myResolver = resolver;
  }

  public MetaConfigKeyManager getManager() {
    return InfraApplicationMetaConfigKeyManager.of();
  }

  @Nullable
  private PsiClass getMapKeyClass() {
    if (this.myType == null) {
      return null;
    }
    PsiType[] parameters = ((PsiClassType) this.myType).getParameters();
    if (parameters.length < 1) {
      return null;
    }
    PsiType parameterType = parameters[0];
    return PsiTypesUtil.getPsiClass(parameterType);
  }

  public PsiElement getDeclaration() {
    PsiElement element = this.myDeclaration.getElement();
    assert element != null : this.myName;
    return element;
  }

  public MetaConfigKey.DeclarationResolveResult getDeclarationResolveResult() {
    return this.myDeclarationResolveResult;
  }

  public String getName() {
    return this.myName;
  }

  public MetaConfigKey.Deprecation getDeprecation() {
    return this.myDeprecation;
  }

  public MetaConfigKey.DescriptionText getDescriptionText() {
    return this.myDescriptionText;
  }

  @Nullable
  public String getDefaultValue() {
    return this.myDefaultValue;
  }

  @Nullable
  public PsiType getEffectiveValueType() {
    return this.myEffectiveValueType.getValue();
  }

  public boolean isAccessType(AccessType... types) {
    for (AccessType type : types) {
      if (type == this.myAccessType) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public PsiClass getMapKeyType() {
    return this.myMapKeyType;
  }

  @Nullable
  public PsiType getType() {
    return this.myType;
  }

  public MetaConfigKey.ItemHint getItemHint() {
    return myItemHint;
  }

  public MetaConfigKey.ItemHint getKeyItemHint() {
    return this.myKeyItemHint;
  }

  public MetaConfigKey.MetaConfigKeyPresentation getPresentation() {
    return this.myPresentation;
  }

  public ConfigKeyPathBeanPropertyResolver getPropertyResolver() {
    return this.myResolver;
  }

  public String toString() {
    return "InfraApplicationMetaConfigKey{myName='" + this.myName + "', myDescriptionText='" + this.myDescriptionText + "', myDefaultValue='" + this.myDefaultValue + "', myDeprecation=" + this.myDeprecation + ", myType=" + this.myType + ", myAccessType=" + this.myAccessType + ", myMapKeyType=" + this.myMapKeyType + ", myItemHint=" + this.myItemHint + ", myKeyItemHint=" + this.myKeyItemHint + "}";
  }
}

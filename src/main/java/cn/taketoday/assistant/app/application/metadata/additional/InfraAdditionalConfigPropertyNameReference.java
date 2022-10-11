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

package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;

import java.util.EnumSet;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraVersion;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.app.application.metadata.InfraValueProvider;
import cn.taketoday.lang.Nullable;

class InfraAdditionalConfigPropertyNameReference extends PsiReferenceBase<PsiElement> {

  private static final TailType CLOSE_PROPERTY_NAME_TAIL = new TailType() {
    public int processTail(Editor editor, int tailOffset) {
      return insertChar(editor, insertChar(editor, insertChar(editor, tailOffset, '\"'), ':'), ' ', false);
    }
  };
  private static final TailType STRING_LITERAL_TAIL = new TailType() {
    public int processTail(Editor editor, int tailOffset) {
      return moveCaret(editor, insertChar(editor, insertChar(editor, CLOSE_PROPERTY_NAME_TAIL.processTail(editor, tailOffset), '\"'), '\"'), -1);
    }
  };
  private static final TailType ARRAY_TAIL = new TailType() {
    public int processTail(Editor editor, int tailOffset) {
      return moveCaret(editor, insertChar(editor,
                      insertChar(editor, insertChar(editor, insertChar(editor, CLOSE_PROPERTY_NAME_TAIL.processTail(editor, tailOffset), '['), '{'), '}'), ']'),
              -2);
    }
  };
  private static final TailType OBJECT_TAIL = new TailType() {
    public int processTail(Editor editor, int tailOffset) {
      return moveCaret(editor, insertChar(editor, insertChar(editor, CLOSE_PROPERTY_NAME_TAIL.processTail(editor, tailOffset), '{'), '}'), -1);
    }
  };
  private static final Function<Variant, LookupElement> VARIANT_LOOKUP_ELEMENT_FUNCTION = variant -> {
    LookupElementBuilder builder = LookupElementBuilder.create(variant.name);
    return switch (variant.variantType) {
      case STRING_LITERAL -> TailTypeDecorator.withTail(builder, STRING_LITERAL_TAIL);
      case ARRAY -> TailTypeDecorator.withTail(builder, ARRAY_TAIL);
      case OBJECT -> TailTypeDecorator.withTail(builder, OBJECT_TAIL);
      case DEFAULT -> TailTypeDecorator.withTail(builder, CLOSE_PROPERTY_NAME_TAIL);
    };
  };
  private final GroupContext groupContext;

  private enum VariantType {
    DEFAULT,
    STRING_LITERAL,
    ARRAY,
    OBJECT
  }

  InfraAdditionalConfigPropertyNameReference(PsiElement element, GroupContext groupContext) {
    super(element);
    this.groupContext = groupContext;
  }

  @Nullable
  public PsiElement resolve() {
    return getElement();
  }

  public Object[] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    switch (groupContext) {
      case FAKE_TOP_LEVEL:
        return createVariants(EnumSet.of(Variant.TOP_LEVEL_GROUPS, Variant.TOP_LEVEL_PROPERTIES, Variant.TOP_LEVEL_HINTS));
      case GROUPS:
        return createVariants(EnumSet.of(Variant.TYPE, Variant.SOURCE_TYPE, Variant.NAME, Variant.DESCRIPTION, Variant.SOURCE_METHOD));
      case PROPERTIES:
        return createVariants(EnumSet.of(Variant.TYPE, Variant.SOURCE_TYPE, Variant.NAME, Variant.DESCRIPTION, Variant.DEFAULT_VALUE, Variant.DEPRECATION));
      case DEPRECATION:
        return createVariants(EnumSet.of(Variant.REASON, Variant.LEVEL, Variant.REPLACEMENT));
      case HINTS:
        return createVariants(EnumSet.of(Variant.NAME, Variant.VALUES, Variant.PROVIDERS));
      case HINTS_VALUES:
        return createVariants(EnumSet.of(Variant.VALUE, Variant.DESCRIPTION));
      case HINTS_PROVIDERS:
        return createVariants(EnumSet.of(Variant.NAME, Variant.PARAMETERS));
      case HINTS_PARAMETERS:
        InfraValueProvider valueProvider = findSpringBootValueProvider();
        if (valueProvider == null) {
          return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }
        EnumSet<Variant> myVariantsFromParameters = EnumSet.noneOf(Variant.class);
        for (InfraValueProvider.Parameter parameter : valueProvider.getParameters()) {
          Variant variant = Variant.findByName(parameter.getName());
          if (variant != null && InfraLibraryUtil.isAtLeastVersion(module, parameter.getMinimumVersion())) {
            myVariantsFromParameters.add(variant);
          }
        }
        return createVariants(myVariantsFromParameters);
      default:
        throw new IllegalStateException(this.groupContext.name());
    }
  }

  @Nullable
  private InfraValueProvider findSpringBootValueProvider() {
    JsonObject superParent;
    JsonProperty nameProperty;
    JsonStringLiteral nameLiteral;
    JsonObject parametersObject = PsiTreeUtil.getParentOfType(getElement(), JsonObject.class);
    if (parametersObject == null || (superParent = PsiTreeUtil.getParentOfType(parametersObject, JsonObject.class)) == null || (nameProperty = superParent.findProperty(
            InfraMetadataConstant.NAME)) == null || (nameLiteral = ObjectUtils.tryCast(nameProperty.getValue(), JsonStringLiteral.class)) == null) {
      return null;
    }
    for (PsiReference psiReference : nameLiteral.getReferences()) {
      if (psiReference instanceof InfraAdditionalConfigValueProviderReference configValueProviderReference) {
        return configValueProviderReference.getValueProvider();
      }
    }
    return null;
  }

  private LookupElement[] createVariants(EnumSet<Variant> variants) {
    JsonObject jsonObject = PsiTreeUtil.getParentOfType(getElement(), JsonObject.class);
    if (jsonObject == null) {
      return LookupElement.EMPTY_ARRAY;
    }
    Set<String> existingProperties = ContainerUtil.map2Set(jsonObject.getPropertyList(), JsonProperty::getName);
    EnumSet<Variant> filteredVariants = EnumSet.noneOf(Variant.class);
    for (Variant variant : variants) {
      if (!existingProperties.contains(variant.name)) {
        filteredVariants.add(variant);
      }
    }
    return ContainerUtil.map2Array(filteredVariants, LookupElement.class, VARIANT_LOOKUP_ELEMENT_FUNCTION);
  }

  private enum Variant {

    TOP_LEVEL_GROUPS(InfraMetadataConstant.GROUPS, VariantType.ARRAY),
    TOP_LEVEL_PROPERTIES(InfraMetadataConstant.PROPERTIES, VariantType.ARRAY),
    TOP_LEVEL_HINTS(InfraMetadataConstant.HINTS, VariantType.ARRAY),
    NAME(InfraMetadataConstant.NAME, VariantType.STRING_LITERAL),
    TYPE(InfraMetadataConstant.TYPE, VariantType.STRING_LITERAL),
    SOURCE_TYPE(InfraMetadataConstant.SOURCE_TYPE, VariantType.STRING_LITERAL),
    SOURCE_METHOD(InfraMetadataConstant.SOURCE_METHOD, VariantType.STRING_LITERAL),
    DESCRIPTION(InfraMetadataConstant.DESCRIPTION, VariantType.STRING_LITERAL),
    DEFAULT_VALUE(InfraMetadataConstant.DEFAULT_VALUE, VariantType.DEFAULT),
    DEPRECATED(InfraMetadataConstant.DEPRECATED, VariantType.DEFAULT),
    DEPRECATION(InfraMetadataConstant.DEPRECATION, VariantType.OBJECT),
    REASON(InfraMetadataConstant.REASON, VariantType.STRING_LITERAL),
    REPLACEMENT(InfraMetadataConstant.REPLACEMENT, VariantType.STRING_LITERAL),
    LEVEL(InfraMetadataConstant.LEVEL, VariantType.STRING_LITERAL),
    VALUES(InfraMetadataConstant.VALUES, VariantType.ARRAY),
    PROVIDERS(InfraMetadataConstant.PROVIDERS, VariantType.ARRAY),
    VALUE("value", VariantType.DEFAULT),
    PARAMETERS(InfraMetadataConstant.PARAMETERS, VariantType.OBJECT),
    TARGET(InfraMetadataConstant.TARGET, VariantType.STRING_LITERAL),
    CONCRETE(InfraMetadataConstant.CONCRETE, VariantType.DEFAULT),
    GROUP(InfraMetadataConstant.GROUP, VariantType.DEFAULT);

    private final String name;
    private final VariantType variantType;

    Variant(String name, VariantType variantType) {
      this.name = name;
      this.variantType = variantType;
    }

    @Nullable
    static Variant findByName(String name) {
      for (Variant variant : values()) {
        if (variant.name.equals(name)) {
          return variant;
        }
      }
      return null;
    }
  }

  enum GroupContext {
    FAKE_TOP_LEVEL("topLevel", InfraVersion.V_4_0),
    GROUPS(InfraMetadataConstant.GROUPS, InfraVersion.V_4_0),
    PROPERTIES(InfraMetadataConstant.PROPERTIES, InfraVersion.V_4_0),
    DEPRECATION(InfraMetadataConstant.DEPRECATION, InfraVersion.V_4_0),
    HINTS(InfraMetadataConstant.HINTS, InfraVersion.V_4_0),
    HINTS_VALUES(InfraMetadataConstant.VALUES, InfraVersion.V_4_0),
    HINTS_PROVIDERS(InfraMetadataConstant.PROVIDERS, InfraVersion.V_4_0),
    HINTS_PARAMETERS(InfraMetadataConstant.PARAMETERS, InfraVersion.V_4_0);

    private final String myPropertyName;
    private final InfraVersion myMinimumVersion;

    GroupContext(String propertyName, InfraVersion minimumVersion) {
      this.myPropertyName = propertyName;
      this.myMinimumVersion = minimumVersion;
    }

    InfraVersion getMinimumVersion() {
      return this.myMinimumVersion;
    }

    @Nullable
    static GroupContext forProperty(String propertyName) {
      for (GroupContext groupContext : values()) {
        if (groupContext.myPropertyName.equals(propertyName)) {
          return groupContext;
        }
      }
      return null;
    }
  }
}

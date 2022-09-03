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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonElementVisitor;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.json.psi.impl.JsonRecursiveElementVisitor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.util.NotNullProducer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.app.application.metadata.InfraValueProvider;

public class InfraAdditionalConfigInspection extends LocalInspectionTool {

  @Override
  public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
    if (!InfraAdditionalConfigUtils.isAdditionalMetadataFile(file)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    JsonFile jsonFile = (JsonFile) file;
    JsonValue topValue = jsonFile.getTopLevelValue();
    if (topValue == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
    PropertyVisitor visitor = new PropertyVisitor(holder, true);
    topValue.acceptChildren(visitor);
    visitTopLevelValues(topValue, Conditions.alwaysTrue(), () -> new NameAttributeVisitor(holder));
    visitTopLevelValues(topValue, jsonProperty -> jsonProperty.getName().equals(InfraMetadataConstant.HINTS), () -> new HintsVisitor(holder));
    return holder.getResultsArray();
  }

  private static void visitTopLevelValues(JsonValue topValue, Condition<JsonProperty> runVisitorCondition, NotNullProducer<JsonElementVisitor> visitorProducer) {
    if (!(topValue instanceof JsonObject jsonObject)) {
      return;
    }
    for (JsonProperty property : jsonObject.getPropertyList()) {
      JsonValue propertyValue = property.getValue();
      if ((propertyValue instanceof JsonArray) && runVisitorCondition.value(property)) {
        JsonElementVisitor elementVisitor = visitorProducer.produce();
        propertyValue.acceptChildren(elementVisitor);
      }
    }
  }

  private static final class PropertyVisitor extends JsonElementVisitor {
    private final ProblemsHolder myHolder;
    private final boolean myIsAtLeast13;

    private PropertyVisitor(ProblemsHolder holder, boolean isAtLeast13) {
      this.myHolder = holder;
      this.myIsAtLeast13 = isAtLeast13;
    }

    public void visitObject(JsonObject jsonObject) {
      jsonObject.acceptChildren(this);
    }

    public void visitArray(JsonArray jsonArray) {
      jsonArray.acceptChildren(this);
    }

    public void visitProperty(JsonProperty jsonProperty) {
      boolean z;
      JsonValue value = jsonProperty.getValue();
      if (value != null) {
        String propertyName = jsonProperty.getName();
        for (PsiReference psiPolyVariantReference : value.getReferences()) {
          if (!psiPolyVariantReference.isSoft()) {
            if (psiPolyVariantReference instanceof PsiPolyVariantReference psiPolyVariantReference1) {
              z = psiPolyVariantReference1.multiResolve(false).length == 0;
            }
            else {
              z = psiPolyVariantReference.resolve() == null;
            }
            boolean unresolved = z;
            if (unresolved) {
              this.myHolder.registerProblem(psiPolyVariantReference, ProblemsHolder.unresolvedReferenceMessage(psiPolyVariantReference),
                      getUnresolvedReferenceProblemHighlightType(propertyName, psiPolyVariantReference));
            }
          }
        }
        if ((value instanceof JsonStringLiteral jsonStringLiteral)
                && (propertyName.equals(InfraMetadataConstant.DESCRIPTION)
                || propertyName.equals(InfraMetadataConstant.REASON))) {
          String text = jsonStringLiteral.getValue();
          if (!StringUtil.endsWithChar(text, '.')) {
            this.myHolder.registerProblem(value, InfraAppBundle.message("additional.config.text.should.end.with.dot"), ProblemHighlightType.WEAK_WARNING);
          }
        }
        if (this.myIsAtLeast13 && propertyName.equals(InfraMetadataConstant.DEPRECATED)) {
          this.myHolder.registerProblem(jsonProperty, InfraAppBundle.message("additional.config.deprecated.property"), ProblemHighlightType.LIKE_DEPRECATED);
        }
        value.accept(this);
      }
    }

    private static ProblemHighlightType getUnresolvedReferenceProblemHighlightType(String propertyName, PsiReference reference) {
      if (reference instanceof InfraAdditionalConfigMetaConfigKeyReference) {
        return ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
      }
      else if (!(reference instanceof JavaClassReference)) {
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
      }
      else if (propertyName.equals(InfraMetadataConstant.TARGET)) {
        return ProblemHighlightType.WEAK_WARNING;
      }
      else {
        return ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
      }
    }
  }

  private static final class NameAttributeVisitor extends JsonElementVisitor {
    private final ProblemsHolder myHolder;
    private final Map<String, JsonValue> myExistingNames = new HashMap<>();

    private NameAttributeVisitor(ProblemsHolder holder) {
      this.myHolder = holder;
    }

    public void visitObject(JsonObject jsonObject) {
      if (jsonObject.findProperty(InfraMetadataConstant.NAME) == null) {
        this.myHolder.registerProblem(jsonObject, InfraAppBundle.message("additional.config.missing.required.property", InfraMetadataConstant.NAME));
      }
      jsonObject.acceptChildren(this);
    }

    public void visitProperty(JsonProperty jsonProperty) {
      JsonStringLiteral jsonValue;
      if (!jsonProperty.getName().equals(InfraMetadataConstant.NAME)
              || (jsonValue = ObjectUtils.tryCast(jsonProperty.getValue(), JsonStringLiteral.class)) == null) {
        return;
      }
      String nameValue = jsonValue.getValue();
      if (!this.myExistingNames.containsKey(nameValue)) {
        this.myExistingNames.put(nameValue, jsonValue);
        return;
      }
      String message = InfraAppBundle.message("application.config.duplicate.entry", nameValue);
      registerDuplicate(jsonValue, message);
      JsonValue existingValue = this.myExistingNames.get(nameValue);
      registerDuplicate(existingValue, message);
    }

    private void registerDuplicate(JsonValue valueElement, @InspectionMessage String message) {
      this.myHolder.registerProblem(valueElement, ElementManipulators.getValueTextRange(valueElement), message);
    }
  }

  private static final class HintsVisitor extends JsonRecursiveElementVisitor {
    private final ProblemsHolder myHolder;

    private HintsVisitor(ProblemsHolder holder) {
      this.myHolder = holder;
    }

    public void visitProperty(JsonProperty jsonProperty) {
      InfraValueProvider.Parameter[] parameters;
      if (jsonProperty.getName().equals(InfraMetadataConstant.PROVIDERS)) {
        JsonValue providersValue = jsonProperty.getValue();
        JsonArray providersArray = ObjectUtils.tryCast(providersValue, JsonArray.class);
        if (providersArray == null) {
          return;
        }
        for (JsonValue value : providersArray.getValueList()) {
          JsonObject provider = ObjectUtils.tryCast(value, JsonObject.class);
          if (provider != null) {
            JsonProperty providerName = provider.findProperty(InfraMetadataConstant.NAME);
            if (providerName == null) {
              this.myHolder.registerProblem(provider, InfraAppBundle.message("additional.config.missing.required.property", InfraMetadataConstant.NAME));
            }
            else {
              JsonValue providerNameValue = providerName.getValue();
              if (providerNameValue instanceof JsonStringLiteral) {
                InfraValueProvider valueProvider = null;
                PsiReference[] references = providerNameValue.getReferences();
                int length = references.length;
                int i = 0;
                while (true) {
                  if (i >= length) {
                    break;
                  }
                  PsiReference psiReference = references[i];
                  if (!(psiReference instanceof InfraAdditionalConfigValueProviderReference configValueProviderReference)) {
                    i++;
                  }
                  else {
                    valueProvider = configValueProviderReference.getValueProvider();
                    break;
                  }
                }
                if (valueProvider != null && valueProvider.hasRequiredParameters()) {
                  JsonProperty parameters2 = provider.findProperty(InfraMetadataConstant.PARAMETERS);
                  JsonObject parametersValue = parameters2 == null ? null : ObjectUtils.tryCast(parameters2.getValue(), JsonObject.class);
                  SmartList<String> smartList = new SmartList<>();
                  for (InfraValueProvider.Parameter parameter : valueProvider.getParameters()) {
                    if (parameter.isRequired() && (parametersValue == null || parametersValue.findProperty(parameter.getName()) == null)) {
                      smartList.add(parameter.getName());
                    }
                  }
                  if (!smartList.isEmpty()) {
                    this.myHolder.registerProblem(providerNameValue, ElementManipulators.getValueTextRange(providerNameValue),
                            InfraAppBundle.message("additional.config.missing.required.parameter", smartList.size(), StringUtil.join(smartList, ", ")));
                  }
                }
              }
            }
          }
        }
      }
      if (jsonProperty.getName().equals(InfraMetadataConstant.VALUES)) {
        JsonValue providersValue2 = jsonProperty.getValue();
        JsonArray providersArray2 = ObjectUtils.tryCast(providersValue2, JsonArray.class);
        if (providersArray2 == null) {
          return;
        }
        for (JsonValue value2 : providersArray2.getValueList()) {
          JsonObject provider2 = ObjectUtils.tryCast(value2, JsonObject.class);
          if (provider2 != null) {
            JsonProperty valueProperty = provider2.findProperty("value");
            if (valueProperty == null) {
              this.myHolder.registerProblem(provider2, InfraAppBundle.message("additional.config.missing.required.property", "value"));
            }
          }
        }
      }
    }
  }
}

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

package cn.taketoday.assistant.model.config.autoconfigure.conditions.jam;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.jam.JamAttributeElement;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamService;
import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.lang.Nullable;

public class ConditionalOnProperty extends JamBaseElement<PsiModifierListOwner> implements ConditionalOnJamElement {
  private static final JamStringAttributeMeta.Collection<MetaConfigKey> VALUE_META;
  private static final JamStringAttributeMeta.Collection<MetaConfigKey> NAME_META;
  private static final JamStringAttributeMeta.Single<String> PREFIX_META;
  private static final JamStringAttributeMeta.Single<String> HAVING_VALUE_META;
  private static final JamBooleanAttributeMeta MATCH_IF_MISSING_META;
  private static final JamBooleanAttributeMeta RELAXED_NAMES_META;
  private static final JamAnnotationMeta ANNOTATION_META;
  private static final SemKey<ConditionalOnProperty> SEM_KEY;
  public static final JamClassMeta<ConditionalOnProperty> CLASS_META;
  public static final JamMethodMeta<ConditionalOnProperty> METHOD_META;

  static {
    VALUE_META = JamAttributeMeta.collectionString("value", new NameJamConverter());
    NAME_META = JamAttributeMeta.collectionString(InfraMetadataConstant.NAME, new NameJamConverter());
    PREFIX_META = JamAttributeMeta.singleString("prefix", new PrefixJamConverter());
    HAVING_VALUE_META = JamAttributeMeta.singleString("havingValue", new HavingValueJamConverter());
    MATCH_IF_MISSING_META = JamAttributeMeta.singleBoolean("matchIfMissing", false);
    RELAXED_NAMES_META = JamAttributeMeta.singleBoolean("relaxedNames", true);
    ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_PROPERTY).addAttribute(VALUE_META).addAttribute(NAME_META).addAttribute(PREFIX_META)
            .addAttribute(HAVING_VALUE_META).addAttribute(MATCH_IF_MISSING_META).addAttribute(RELAXED_NAMES_META);
    SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnProperty");
    CLASS_META = new JamClassMeta<>(null, ConditionalOnProperty.class, SEM_KEY).addAnnotation(ANNOTATION_META);
    METHOD_META = new JamMethodMeta<>(null, ConditionalOnProperty.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  }

  public ConditionalOnProperty(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override

  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    ConditionalOnPropertyEvaluator evaluator = new ConditionalOnPropertyEvaluator(this, context);
    return evaluator.matches();
  }

  private List<JamStringAttributeElement<MetaConfigKey>> getNameOrValue() {
    List<JamStringAttributeElement<MetaConfigKey>> name = ANNOTATION_META.getAttribute(getPsiElement(), NAME_META);
    return !name.isEmpty() ? name : ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META);
  }

  private String getPrefix() {
    String prefix = (ANNOTATION_META.getAttribute(getPsiElement(), PREFIX_META)).getValue();
    if (StringUtil.isEmpty(prefix)) {
      return "";
    }
    if (!StringUtil.endsWithChar(prefix, '.')) {
      String str = prefix + ".";
      return str;
    }
    return prefix;
  }

  public List<String> getMetaConfigKeyNames() {
    SmartList smartList = new SmartList();
    String prefix = getPrefix();
    processNameOrValueAttributes(jamAttributeElement -> {
      smartList.add(prefix + jamAttributeElement.getStringValue());
      return true;
    });
    return smartList;
  }

  public List<MetaConfigKey> getResolvedMetaConfigKeys(Module module) {
    SmartList smartList = new SmartList();
    NameJamConverter converter = new NameJamConverter(module);
    processNameOrValueAttributes(jamAttributeElement -> {
      ContainerUtil.addIfNotNull(smartList, converter.fromString(jamAttributeElement.getStringValue(), jamAttributeElement));
      return true;
    });
    return smartList;
  }

  private boolean processNameOrValueAttributes(Processor<JamStringAttributeElement<MetaConfigKey>> processor) {
    List<JamStringAttributeElement<MetaConfigKey>> attribute = getNameOrValue();
    for (JamStringAttributeElement<MetaConfigKey> element : attribute) {
      if (!processor.process(element)) {
        return false;
      }
    }
    return true;
  }

  public String getHavingValue() {
    return (String) ((JamStringAttributeElement) ANNOTATION_META.getAttribute(getPsiElement(), HAVING_VALUE_META)).getValue();
  }

  public boolean isMatchIfMissing() {
    return ANNOTATION_META.getAttribute(getPsiElement(), MATCH_IF_MISSING_META).getValue().booleanValue();
  }

  public boolean isRelaxedNames() {
    return ANNOTATION_META.getAttribute(getPsiElement(), RELAXED_NAMES_META).getValue().booleanValue();
  }

  @Nullable
  private static ConditionalOnProperty getCurrentConditionalOnProperty(JamAttributeElement<?> context) {
    PsiElement jamOwner;
    PsiAnnotation annotation = context.getParentAnnotationElement().getPsiElement();
    if (annotation == null || (jamOwner = PsiTreeUtil.getParentOfType(context.getPsiElement(), PsiMethod.class, PsiClass.class)) == null) {
      return null;
    }
    ConditionalOnProperty conditionalOnProperty = JamService.getJamService(annotation.getProject()).getJamElement(SEM_KEY, jamOwner);
    return conditionalOnProperty;
  }

  private static boolean isValidKey(@Nullable MetaConfigKey key) {
    return key != null && key.isAccessType(MetaConfigKey.AccessType.NORMAL);
  }

  private static final class NameJamConverter extends JamSimpleReferenceConverter<MetaConfigKey> {
    private final Module myModule;

    private NameJamConverter() {
      this.myModule = null;
    }

    private NameJamConverter(Module module) {
      this.myModule = module;
    }

    @Nullable
    public MetaConfigKey fromString(@Nullable String s, JamStringAttributeElement<MetaConfigKey> context) {
      String prefix = getPrefixValueForName(context);
      if (prefix == null) {
        return null;
      }
      PsiAnnotationMemberValue psiElement = context.getPsiElement();
      Module module = ObjectUtils.chooseNotNull(this.myModule, ModuleUtilCore.findModuleForPsiElement(psiElement));
      MetaConfigKey key = InfraApplicationMetaConfigKeyManager.getInstance().findCanonicalApplicationMetaConfigKey(module, prefix + s);
      if (!isValidKey(key)) {
        return null;
      }
      return key;
    }

    @Nullable
    public PsiElement getPsiElementFor(MetaConfigKey target) {
      return target.getDeclaration();
    }

    public LookupElement[] getLookupVariants(JamStringAttributeElement<MetaConfigKey> context) {
      String prefix = getPrefixValueForName(context);
      if (prefix == null) {
        return LookupElement.EMPTY_ARRAY;
      }
      SmartList smartList = new SmartList();
      for (MetaConfigKey key : getVariants(context)) {
        String actualKey = StringUtil.substringAfter(key.getName(), prefix);
        LookupElement element = key.getPresentation().tuneLookupElement(key.getPresentation().getLookupElement(actualKey));
        smartList.add(element);
      }
      return (LookupElement[]) smartList.toArray(LookupElement.EMPTY_ARRAY);
    }

    public Collection<MetaConfigKey> getVariants(JamStringAttributeElement<MetaConfigKey> context) {
      Module module;
      String prefix = StringUtil.notNullize(getPrefixValueForName(context));
      PsiAnnotationMemberValue psiElement = context.getPsiElement();
      if (psiElement != null && (module = ModuleUtilCore.findModuleForPsiElement(psiElement)) != null) {
        List<? extends MetaConfigKey> allKeys = InfraApplicationMetaConfigKeyManager.getInstance().getAllMetaConfigKeys(module);
        MetaConfigKeyManager.ConfigKeyNameBinder binder = InfraApplicationMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(module);
        SmartList smartList = new SmartList();
        for (MetaConfigKey key : allKeys) {
          if (isValidKey(key) && (prefix.isEmpty() || binder.matchesPrefix(key, prefix))) {
            smartList.add(key);
          }
        }
        return smartList;
      }
      return Collections.emptyList();
    }

    @Nullable
    private static String getPrefixValueForName(JamStringAttributeElement<MetaConfigKey> context) {
      ConditionalOnProperty conditionalOnProperty = getCurrentConditionalOnProperty(context);
      if (conditionalOnProperty == null) {
        return null;
      }
      return conditionalOnProperty.getPrefix();
    }
  }

  private static class PrefixJamConverter extends JamSimpleReferenceConverter<String> {

    @Nullable
    public Object m123fromString(@Nullable String str, JamStringAttributeElement jamStringAttributeElement) {
      return fromString(str, (JamStringAttributeElement<String>) jamStringAttributeElement);
    }

    @Nullable
    public String fromString(@Nullable String s, JamStringAttributeElement<String> context) {
      return s;
    }

    public Collection<String> getVariants(JamStringAttributeElement<String> context) {
      int lastDot;
      List<? extends MetaConfigKey> keys = InfraApplicationMetaConfigKeyManager.getInstance().getAllMetaConfigKeys(context.getPsiElement());
      Set<String> prefixes = new LinkedHashSet<>();
      for (MetaConfigKey key : keys) {
        if (isValidKey(key) && (lastDot = key.getName().lastIndexOf(46)) != -1) {
          String keyPrefix = key.getName().substring(0, lastDot);
          prefixes.add(keyPrefix);
        }
      }
      return prefixes;
    }

    public LookupElement createLookupElementFor(String target) {
      return LookupElementBuilder.create(target)
              .withIcon(Icons.Today);
    }
  }

  private static class HavingValueJamConverter extends JamConverter<String> {
    public String fromString(@Nullable String s, JamStringAttributeElement<String> context) {
      return StringUtil.notNullize(s);
    }

    public PsiReference[] createReferences(JamStringAttributeElement<String> context, PsiLanguageInjectionHost injectionHost) {
      ConditionalOnProperty conditionalOnProperty = getCurrentConditionalOnProperty(context);
      if (conditionalOnProperty == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      List<JamStringAttributeElement<MetaConfigKey>> nameAttributes = conditionalOnProperty.getNameOrValue();
      if (nameAttributes.size() != 1) {
        return PsiReference.EMPTY_ARRAY;
      }
      JamStringAttributeElement<MetaConfigKey> firstNameElement = ContainerUtil.getFirstItem(nameAttributes);
      MetaConfigKey configKey = firstNameElement.getValue();
      if (configKey == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      Module module = ModuleUtilCore.findModuleForPsiElement(injectionHost);
      List<TextRange> valueTextRanges = Collections.singletonList(ElementManipulators.getValueTextRange(injectionHost));
      PsiReference[] valueReferences = InfraHintReferencesProvider.getInstance().getValueReferences(module, configKey, null, injectionHost, valueTextRanges, new ProcessingContext());
      return valueReferences;
    }
  }
}

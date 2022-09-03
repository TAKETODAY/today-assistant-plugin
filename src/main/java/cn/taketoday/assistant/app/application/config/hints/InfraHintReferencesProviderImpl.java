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

package cn.taketoday.assistant.app.application.config.hints;

import com.intellij.microservices.jvm.config.ConfigKeyPathReferenceBase;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.hints.BooleanHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.ClassHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.DoubleHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.EncodingHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.EnumHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.FloatHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.microservices.jvm.config.hints.HintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.HintReferenceProviderBase;
import com.intellij.microservices.jvm.config.hints.IntegerHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.LocaleHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.LongHintReferenceProvider;
import com.intellij.microservices.jvm.config.hints.StaticValuesHintReferenceProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.paths.GlobalPathReferenceProvider;
import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.xml.util.documentation.MimeTypeDictionary;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.app.application.metadata.InfraValueProvider;
import cn.taketoday.assistant.model.converters.PatternFileReferenceSet;
import cn.taketoday.assistant.profiles.InfraProfilePsiReference;
import cn.taketoday.assistant.profiles.InfraProfilesFactory;
import cn.taketoday.lang.Nullable;

public final class InfraHintReferencesProviderImpl extends InfraHintReferencesProvider implements Disposable {
  private static final String STATIC = "static";
  private static final String ENUM_VALUES = "enumValues";
  private static final String REGEX = "regex";
  private static final String RESOURCE = "resource";
  private static final String PATH_PATTERN = "pathPattern";
  private static final String CONTENT_TYPE = "contentType";
  private static final String INFRA_PROFILE_EXPRESSION = "infra-profile-expression";
  private static final String INFRA_SPI_CLASSES = "infraSpiClasses";
  private static final String CONFIG = "config";
  private static final String CLASS_REFERENCE_VALUE_SEPARATOR = ";";

  private final HintReferenceProvider infraProfileNameHintReferenceProvider = new HintReferenceProviderBase() {

    protected boolean canProcess(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
      return ModuleUtilCore.findModuleForPsiElement(element) != null;
    }

    protected PsiReference createReference(PsiElement element, TextRange textRange, ProcessingContext context) {
      String elementText = element.getText();
      String tokenText = textRange.substring(elementText);
      if (StringUtil.startsWithChar(tokenText, '!')) {
        textRange = textRange.shiftRight(1).grown(-1);
      }
      Module module = ModuleUtilCore.findModuleForPsiElement(element);
      return new InfraProfilePsiReference(element, textRange, module, false);
    }
  };
  private final HintReferenceProvider myDummyProvider = new HintReferenceProvider() {

    public PsiReference[] getReferences(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
      return PsiReference.EMPTY_ARRAY;
    }
  };
  private final StaticValuesHintReferenceProvider myLocaleProvider = new LocaleHintReferenceProvider();
  private final StaticValuesHintReferenceProvider myContentTypeProvider = new StaticValuesHintReferenceProvider(true, true, null, MimeTypeDictionary.HTML_CONTENT_TYPES);
  private final EncodingHintReferenceProvider myEncodingProvider = new EncodingHintReferenceProvider();
  private final Map<String, HintReferenceProvider> myExplicitKeyProviders = new HashMap<>();
  private final Map<String, String> myExplicitMappings = new HashMap<>();
  private final Map<String, HintReferenceProvider> myExplicitProviders = new HashMap<>();
  private final Map<String, HintReferenceProvider> myBuiltinProviders = new HashMap<>();
  private final Set<String> myOverrideKeys = new HashSet<>();
  private final Set<String> myForcedKeys = new HashSet<>();
  private final Map<String, HintReferenceProvider> myByTypeProviders = ContainerUtil.<String, HintReferenceProvider>immutableMapBuilder()
          .put("java.lang.Boolean", new BooleanHintReferenceProvider())
          .put("java.lang.Integer", new IntegerHintReferenceProvider())
          .put("java.lang.Long", new LongHintReferenceProvider())
          .put("java.lang.Float", new FloatHintReferenceProvider())
          .put("java.lang.Double", new DoubleHintReferenceProvider())
          .put("java.nio.charset.Charset", this.myEncodingProvider)
          .put("cn.taketoday.util.MimeType", this.myContentTypeProvider)
          .put("cn.taketoday.http.MediaType", this.myContentTypeProvider)
          .put("cn.taketoday.core.io.Resource", new ResourceReferenceProvider())
          .put("java.util.Locale", this.myLocaleProvider)
          .put("java.time.Duration", new DurationReferenceProvider())
          .put("cn.taketoday.util.unit.DataSize", new DataSizeReferenceProvider())
          .build();

  private static final String INFRA_PROFILE_NAME = InfraValueProvider.PROFILE_NAME.getId();

  private static final ExtensionPointName<InfraCustomHintReferenceProvider> CUSTOM_HINT_REFERENCE_PROVIDER_EP_NAME = new ExtensionPointName<>(
          "cn.taketoday.assistant.app.customHintReferenceProvider");

  private enum ItemHintType {
    VALUE,
    KEY
  }

  private InfraHintReferencesProviderImpl() {
    this.myBuiltinProviders.put(INFRA_PROFILE_NAME, this.infraProfileNameHintReferenceProvider);
    this.myBuiltinProviders.put(INFRA_PROFILE_EXPRESSION, new HintReferenceProvider() {

      public PsiReference[] getReferences(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
          return PsiReference.EMPTY_ARRAY;
        }
        SmartList<PsiReference> smartList = new SmartList<>();
        String elementText = element.getText();
        int offset = ElementManipulators.getOffsetInElement(element);
        for (TextRange textRange : textRanges) {
          String profile = textRange.substring(elementText);
          try {
            InfraProfilesFactory.getInstance().parseProfileExpressions(new SmartList<>(profile));
            PsiReference[] references = InfraProfilesFactory.getInstance().getProfilesReferences(module, element, profile, textRange.getStartOffset() - offset, "()&|", false);
            ContainerUtil.addAll(smartList, references);
          }
          catch (InfraProfilesFactory.MalformedProfileExpressionException e) {
            InfraProfilePsiReference delegate = new InfraProfilePsiReference(element, textRange, module, false);
            smartList.add(new MalformedProfileExpressionReference(element, textRange, e.getMessage(), delegate));
          }
        }
        return smartList.toArray(PsiReference.EMPTY_ARRAY);
      }
    });
    this.myBuiltinProviders.put(RESOURCE, new ResourceReferenceProvider());
    this.myBuiltinProviders.put(PATH_PATTERN, new HintReferenceProvider() {

      public PsiReference[] getReferences(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
        String valueText = element.getText();
        PsiReference[] allReferences = PsiReference.EMPTY_ARRAY;
        for (TextRange range : textRanges) {
          FileReference[] references = new PatternFileReferenceSet(range.substring(valueText), element, range.getStartOffset(), true).getAllReferences();
          allReferences = ArrayUtil.mergeArrays(allReferences, references);
        }
        return allReferences;
      }
    });
    this.myBuiltinProviders.put(CONTENT_TYPE, this.myContentTypeProvider);
    this.myBuiltinProviders.put(CONFIG, new ConfigReferenceProvider());
    loadExplicitMappings();
    CUSTOM_HINT_REFERENCE_PROVIDER_EP_NAME.addChangeListener(() -> {
      this.myOverrideKeys.clear();
      this.myExplicitProviders.clear();
      this.myExplicitMappings.clear();
      loadExplicitMappings();
    }, this);
  }

  private void loadExplicitMappings() {
    URL url = InfraHintReferencesProviderImpl.class.getResource("infra-hint-registry.xml");
    InfraHintRegistryInfo info = XmlSerializer.deserialize(url,
            InfraHintRegistryInfo.class);
    Map<String, InfraCustomHintReferenceProvider> customProviders = new HashMap<>();
    for (InfraCustomHintReferenceProvider customProvider : CUSTOM_HINT_REFERENCE_PROVIDER_EP_NAME.getExtensions()) {
      customProviders.put(customProvider.getId(), customProvider);
    }
    for (InfraHintRegistryKey key : Objects.requireNonNull(info).myKeys) {
      String providerId = key.myProvider;
      if (key.myOverride) {
        this.myOverrideKeys.add(key.myId);
      }
      Map<String, HintReferenceProvider> explicitProviders = key.myKeyHint ? this.myExplicitKeyProviders : this.myExplicitProviders;
      if (providerId.equals(STATIC) || providerId.equals(ENUM_VALUES)) {
        explicitProviders.put(key.myId, createStaticOrEnumReferenceProvider(key));
      }
      else if (providerId.equals(InfraValueProvider.BEAN_REFERENCE.getId())) {
        explicitProviders.put(key.myId, new BeanReferenceProvider(key.myValue));
      }
      else if (providerId.equals(InfraValueProvider.CLASS_REFERENCE.getId())) {
        explicitProviders.put(key.myId, createClassReferenceProvider(key.myValue, key.myAllowOtherValues, true));
      }
      else if (providerId.equals(REGEX)) {
        explicitProviders.put(key.myId, new RegExReferenceProvider(key.myValue));
      }
      else if (providerId.equals(RESOURCE) || providerId.equals(PATH_PATTERN) || providerId.equals(INFRA_PROFILE_NAME) || providerId.equals(INFRA_PROFILE_EXPRESSION) || providerId.equals(
              CONTENT_TYPE)) {
        if (key.myKeyHint) {
          throw new IllegalArgumentException(providerId + " used for key hint");
        }
        this.myExplicitMappings.put(key.myId, providerId);
      }
      else if (providerId.equals(CONFIG)) {
        if (key.myKeyHint) {
          throw new IllegalArgumentException(providerId + " used for key hint");
        }
        this.myForcedKeys.add(key.myId);
        this.myExplicitMappings.put(key.myId, providerId);
      }
      else if (providerId.equals(INFRA_SPI_CLASSES)) {
        List<String> spiKeys = StringUtil.split(key.myValue, CLASS_REFERENCE_VALUE_SEPARATOR);
        String spiKey = spiKeys.get(0);
        String importKey = spiKeys.size() == 2 ? spiKeys.get(1) : null;
        explicitProviders.put(key.myId, new InfraSpiClassesProvider(spiKey, importKey));
      }
      else {
        InfraCustomHintReferenceProvider customProvider2 = customProviders.get(providerId);
        if (customProvider2 != null) {
          explicitProviders.put(key.myId, customProvider2);
        }
        else if (key.myOptional) {
          if (key.myOverride) {
            this.myOverrideKeys.remove(key.myId);
          }
        }
        else {
          throw new IllegalArgumentException(providerId);
        }
      }
    }
  }

  @Override
  public PsiReference[] getKeyReferences(MetaConfigKey configKey, PsiElement keyPsiElement, TextRange textRange, ProcessingContext context) {
    if (configKey.getKeyItemHint() != MetaConfigKey.ItemHint.NONE) {
      context.put(HINT_REFERENCES_CONFIG_KEY, configKey);
      return getItemHintReferences(configKey, ItemHintType.KEY, keyPsiElement, Collections.singletonList(textRange), context);
    }
    HintReferenceProvider explicitProvider = this.myExplicitKeyProviders.get(configKey.getName());
    if (explicitProvider != null) {
      return explicitProvider.getReferences(keyPsiElement, Collections.singletonList(textRange), context);
    }
    return PsiReference.EMPTY_ARRAY;
  }

  @Override
  public PsiReference[] getValueReferences(Module module, MetaConfigKey configKey, @Nullable PsiElement keyPsiElement, PsiElement valuePsiElement, List<TextRange> valueTextRanges,
          ProcessingContext context) {
    context.put(HINT_REFERENCES_CONFIG_KEY, configKey);
    if (configKey.getItemHint() != MetaConfigKey.ItemHint.NONE) {
      return getItemHintReferences(configKey, ItemHintType.VALUE, valuePsiElement, valueTextRanges, context);
    }
    else {
      HintReferenceProvider byTypeOrExplicitProvider = getByTypeOrExplicitProvider(configKey);
      if (byTypeOrExplicitProvider != this.myDummyProvider) {
        return byTypeOrExplicitProvider.getReferences(valuePsiElement, valueTextRanges, context);
      }
      else if (keyPsiElement == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      else if (!MetaConfigKey.MAP_OR_INDEXED_WITHOUT_KEY_HINTS_CONDITION.value(configKey)) {
        return getEraseOtherReferences(valuePsiElement);
      }
      else {
        HintReferenceProvider pojoReferenceProvider = getPojoValueProvider(keyPsiElement);
        if (pojoReferenceProvider != null) {
          return pojoReferenceProvider.getReferences(valuePsiElement, valueTextRanges, context);
        }
        return getEraseOtherReferences(valuePsiElement);
      }
    }
  }

  private static PsiReference[] getEraseOtherReferences(PsiElement valuePsiElement) {
    if (WebReference.isWebReferenceWorthy(valuePsiElement) && valuePsiElement.textContains(':')) {
      String valueText = ElementManipulators.getValueText(valuePsiElement);
      if (GlobalPathReferenceProvider.isWebReferenceUrl(valueText)) {
        return PsiReference.EMPTY_ARRAY;
      }
    }
    return new PsiReference[] { PsiReferenceBase.createSelfReference(valuePsiElement, valuePsiElement) };
  }

  @Nullable
  private HintReferenceProvider getPojoValueProvider(PsiElement keyPsiElement) {
    PsiReference psiReference = ArrayUtil.getLastElement(keyPsiElement.getReferences());
    if (!(psiReference instanceof ConfigKeyPathReferenceBase configKeyPathReferenceBase)) {
      return null;
    }
    return getByTypeProvider(configKeyPathReferenceBase.getValueElementType());
  }

  private HintReferenceProvider getByTypeOrExplicitProvider(MetaConfigKey configKey) {
    HintReferenceProvider explicitValuesProvider = getExplicitProvider(configKey);
    if (explicitValuesProvider != null) {
      return explicitValuesProvider;
    }
    HintReferenceProvider byTypeProvider = getByTypeProvider(configKey.getEffectiveValueElementType());
    if (byTypeProvider != null) {
      return byTypeProvider;
    }
    return this.myDummyProvider;
  }

  private PsiReference[] getItemHintReferences(MetaConfigKey configKey, ItemHintType itemHintType, PsiElement element, List<TextRange> valueTextRanges, ProcessingContext context) {
    List<HintReferenceProvider> providers = getItemHintProviders(element, configKey, itemHintType, context);
    if (providers.isEmpty()) {
      return PsiReference.EMPTY_ARRAY;
    }
    PsiReference[] allReferences = PsiReference.EMPTY_ARRAY;
    for (HintReferenceProvider provider : providers) {
      PsiReference[] references = provider.getReferences(element, valueTextRanges, context);
      allReferences = ArrayUtil.mergeArrays(allReferences, references);
    }
    return allReferences;
  }

  private List<HintReferenceProvider> getItemHintProviders(PsiElement element, MetaConfigKey configKey, ItemHintType itemHintType, ProcessingContext context) {
    MetaConfigKey.ItemHint keyOrValueHint = itemHintType == ItemHintType.VALUE ? configKey.getItemHint() : configKey.getKeyItemHint();
    List<MetaConfigKey.ValueProvider> valueProviders = keyOrValueHint.getValueProviders();
    SmartList<HintReferenceProvider> smartList = new SmartList<>();
    if (this.myForcedKeys.contains(configKey.getName())) {
      ContainerUtil.addIfNotNull(smartList, getExplicitProvider(configKey));
    }
    List<MetaConfigKey.ValueHint> valueHints = keyOrValueHint.getValueHints();
    if (!valueHints.isEmpty() && smartList.isEmpty()) {
      boolean hasValueProviders = !valueProviders.isEmpty();
      if (StringUtil.isEmpty(context.get(InfraHintReferencesProvider.HINT_REFERENCES_MAP_KEY_PREFIX))) {
        smartList.add(new ValueHintReferenceProvider(configKey, valueHints, hasValueProviders));
      }
    }
    if (valueProviders.isEmpty()) {
      return smartList;
    }
    InfraValueProvider valueProvider = null;
    Map<String, String> parameters = null;

    for (MetaConfigKey.ValueProvider provider : valueProviders) {
      valueProvider = InfraValueProvider.findById(provider.getName());
      if (valueProvider != null) {
        parameters = provider.getParameters();
        break;
      }
    }

    if (valueProvider == null) {
      return smartList;
    }
    if (itemHintType == ItemHintType.VALUE) {
      if (valueProvider == InfraValueProvider.CLASS_REFERENCE) {
        String target = parameters.get(InfraMetadataConstant.TARGET);
        String concreteParameter = parameters.get(InfraMetadataConstant.CONCRETE);
        Boolean concrete = concreteParameter == null ? Boolean.TRUE : Boolean.valueOf(concreteParameter);
        smartList.add(createClassReferenceProvider(target, false, concrete));
      }
      else if (valueProvider == InfraValueProvider.PROFILE_NAME) {
        smartList.add(this.infraProfileNameHintReferenceProvider);
      }
      else if (valueProvider == InfraValueProvider.BEAN_REFERENCE) {
        String target2 = parameters.get(InfraMetadataConstant.TARGET);
        smartList.add(new BeanReferenceProvider(target2));
      }
      else if (valueProvider == InfraValueProvider.HANDLE_AS && parameters.containsKey(InfraMetadataConstant.TARGET)) {
        String target3 = parameters.get(InfraMetadataConstant.TARGET);
        PsiType targetType = JavaPsiFacade.getElementFactory(element.getProject()).createTypeFromText(target3.replace('$', '.'), null);
        MetaConfigKey.AccessType targetAccessType = MetaConfigKey.AccessType.forPsiType(targetType);
        PsiType effectiveTargetType = targetAccessType.getEffectiveValueType(targetType);
        if ((targetAccessType == MetaConfigKey.AccessType.MAP || targetAccessType == MetaConfigKey.AccessType.ENUM_MAP) && effectiveTargetType != null) {
          MetaConfigKey.AccessType valueAccessType = MetaConfigKey.AccessType.forPsiType(effectiveTargetType);
          effectiveTargetType = valueAccessType.getEffectiveValueType(effectiveTargetType);
        }
        if (effectiveTargetType != null) {
          targetType = effectiveTargetType;
        }
        ContainerUtil.addIfNotNull(smartList, getByTypeProvider(targetType));
      }
    }
    if (valueProvider == InfraValueProvider.LOGGER_NAME) {
      String group = parameters.get(InfraMetadataConstant.GROUP);
      boolean resolveGroups = group == null || Boolean.parseBoolean(group);
      smartList.add(new LoggerNameReferenceProvider(resolveGroups));
    }
    return smartList;
  }

  @Nullable
  private HintReferenceProvider getByTypeProvider(@Nullable PsiType valueType) {
    PsiClass typeClass;
    if (valueType == null || (typeClass = PsiTypesUtil.getPsiClass(valueType)) == null) {
      return null;
    }
    if (typeClass.isEnum()) {
      return new EnumHintReferenceProvider(typeClass);
    }
    String typeFqn = typeClass.getQualifiedName();
    if (typeFqn == null) {
      return null;
    }
    if ("java.lang.Class".equals(typeFqn)) {
      PsiType[] parameters = ((PsiClassType) valueType).getParameters();
      if (parameters.length != 1) {
        return createClassReferenceProvider(null, false, Boolean.TRUE);
      }
      PsiType psiWildcardType = parameters[0];
      if (!(psiWildcardType instanceof PsiWildcardType parameter)) {
        return null;
      }
      return createClassReferenceProvider(parameter.getExtendsBound().getCanonicalText(), false, Boolean.TRUE);
    }
    return this.myByTypeProviders.get(typeFqn);
  }

  @Nullable
  private HintReferenceProvider getExplicitProvider(MetaConfigKey configKey) {
    String configKeyName = configKey.getName();
    if (!this.myOverrideKeys.contains(configKeyName)) {
      return null;
    }
    HintReferenceProvider byKey = this.myExplicitProviders.get(configKeyName);
    if (byKey != null) {
      return byKey;
    }
    String explicitProviderId = this.myExplicitMappings.get(configKeyName);
    if (explicitProviderId == null) {
      return null;
    }
    HintReferenceProvider provider = this.myBuiltinProviders.get(explicitProviderId);
    if (provider == null) {
      throw new IllegalStateException("provider id '" + explicitProviderId + "' not found, configured for " + configKey);
    }
    return provider;
  }

  public void dispose() {
  }

  private static HintReferenceProvider createStaticOrEnumReferenceProvider(InfraHintRegistryKey key) {
    String[] values = ContainerUtil.map2Array(StringUtil.split(key.myValue, ","), String.class, StringUtil::trim);
    return new StaticValuesHintReferenceProvider(key.myAllowOtherValues, key.myProvider.equals(ENUM_VALUES), null, values);
  }

  private static HintReferenceProvider createClassReferenceProvider(String baseClass, boolean allowOtherValues, Boolean concrete) {
    if (baseClass != null && baseClass.contains(CLASS_REFERENCE_VALUE_SEPARATOR)) {
      return new ClassHintReferenceProvider(StringUtil.split(baseClass, CLASS_REFERENCE_VALUE_SEPARATOR), allowOtherValues, concrete);
    }
    return new ClassHintReferenceProvider(baseClass, allowOtherValues, concrete);
  }

  private static final class MalformedProfileExpressionReference extends HintReferenceBase {
    private final String myMessage;
    private final PsiReference myDelegate;

    private MalformedProfileExpressionReference(PsiElement element, TextRange rangeInElement, String message, PsiReference delegate) {
      super(element, rangeInElement);
      this.myMessage = message;
      this.myDelegate = delegate;
    }

    @Nullable
    protected PsiElement doResolve() {
      return null;
    }

    public Object[] getVariants() {
      return this.myDelegate.getVariants();
    }

    public String getUnresolvedMessagePattern() {
      return this.myMessage;
    }
  }
}

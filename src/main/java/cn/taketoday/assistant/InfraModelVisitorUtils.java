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

package cn.taketoday.assistant;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.Processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.context.model.CacheableCommonInfraModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;

import static cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext.context;
import static cn.taketoday.assistant.context.model.visitors.InfraModelVisitors.visitRecursionAwareRelatedModels;
import static cn.taketoday.assistant.context.model.visitors.InfraModelVisitors.visitRelatedModels;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 13:15
 */
public abstract class InfraModelVisitorUtils {

  private static final InfraModelVisitorContext.Exec<BeanPointer<?>> DOM_BEANS = (m, p) -> {
    if (m instanceof LocalXmlModel) {
      for (BeanPointer<?> pointer : ((LocalXmlModel) m).getLocalBeans()) {
        if (!p.process(pointer))
          return false;
      }
    }
    return true;
  };

  private static final InfraModelVisitorContext.Exec<BeanPointer<?>>
          PLACEHOLDERS = (m, p) -> {
    if (m instanceof LocalXmlModel) {
      for (BeanPointer<?> pointer : ((LocalXmlModel) m).getPlaceholderConfigurerBeans()) {
        if (!p.process(pointer))
          return false;
      }
    }
    return true;
  };
  private static final InfraModelVisitorContext.Exec<InfraBeansPackagesScan>
          SCANS = (m, p) -> {
    if (m instanceof LocalModel) {
      for (InfraBeansPackagesScan scan : ((LocalModel<?>) m).getPackagesScans()) {
        if (!p.process(scan))
          return false;
      }
    }
    return true;
  };
  private static final InfraModelVisitorContext.Exec<BeanPointer<?>>
          ANNO_CONFIGS = (m, p) -> {
    if (m instanceof LocalXmlModel) {
      for (BeanPointer<?> pointer : ((LocalXmlModel) m).getAnnotationConfigAppContexts()) {
        if (!p.process(pointer))
          return false;
      }
    }
    return true;
  };
  private static final InfraModelVisitorContext.Exec<String> PROFILES = (m, p) -> {
    if (m instanceof CacheableCommonInfraModel) {
      for (String s : ((CacheableCommonInfraModel) m).getProfiles()) {
        if (!p.process(s))
          return false;
      }
    }
    return true;
  };

  private static final InfraModelVisitorContext.Exec<LocalXmlModel>
          LOCAL_XML_MODELS = (m, p) -> {
    if (m instanceof LocalXmlModel) {
      return p.process((LocalXmlModel) m);
    }
    return true;
  };

  public static Set<String> getProfiles(CommonInfraModel model) {
    CommonProcessors.CollectProcessor<String> processor = new CommonProcessors.CollectProcessor<>();
    visitRelatedModels(model, context(processor, PROFILES));
    return new HashSet<>(processor.getResults());
  }

  public static Collection<BeanPointer<?>> getAllDomBeans(CommonInfraModel model) {
    CommonProcessors.CollectProcessor<BeanPointer<?>> processor = new CommonProcessors.CollectProcessor<>();
    visitRelatedModels(model, context(processor, DOM_BEANS));
    return processor.getResults();
  }

  public static Set<LocalXmlModel> getLocalXmlModels(CommonInfraModel model) {
    CommonProcessors.CollectProcessor<LocalXmlModel> processor = new CommonProcessors.CollectProcessor<>();

    visitRelatedModels(model, context(processor, LOCAL_XML_MODELS));

    return new HashSet<>(processor.getResults());
  }

  public static Set<BeanPointer<?>> getPlaceholderConfigurers(CommonInfraModel model) {
    Set<BeanPointer<?>> placeholders = new HashSet<>();
    visitRelatedModels(model, context(Processors.cancelableCollectProcessor(placeholders), PLACEHOLDERS));
    return placeholders;
  }

  public static List<InfraBeansPackagesScan> getComponentScans(CommonInfraModel model) {
    List<InfraBeansPackagesScan> scans = new ArrayList<>();
    visitRecursionAwareRelatedModels(model, context(Processors.cancelableCollectProcessor(scans), SCANS));
    return scans;
  }

  public static boolean hasComponentScans(CommonInfraModel model) {
    CommonProcessors.FindFirstProcessor<InfraBeansPackagesScan> processor = new CommonProcessors.FindFirstProcessor<>();
    visitRecursionAwareRelatedModels(model, context(processor, SCANS));
    return processor.isFound();
  }

  public static List<BeanPointer<?>> getAnnotationConfigApplicationContexts(CommonInfraModel model) {
    List<BeanPointer<?>> pointers = new LinkedList<>();
    visitRecursionAwareRelatedModels(model, context(Processors.cancelableCollectProcessor(pointers), ANNO_CONFIGS));
    return pointers;
  }

  public static Collection<XmlTag> getCustomBeanCandidates(CommonInfraModel model, String id) {
    Set<XmlTag> tags = new HashSet<>();
    visitRecursionAwareRelatedModels(model, context(Processors.cancelableCollectProcessor(tags), (m, p) -> {
      if (m instanceof LocalXmlModel) {
        for (XmlTag xmlTag : ((LocalXmlModel) m).getCustomBeans(id)) {
          if (!p.process(xmlTag))
            return false;
        }
      }
      return true;
    }));
    return tags;
  }

  public static List<BeanPointer<?>> getDescendants(CommonInfraModel model, BeanPointer<?> context) {
    List<BeanPointer<?>> pointers = new LinkedList<>();

    visitRecursionAwareRelatedModels(model, context(Processors.cancelableCollectProcessor(pointers), (m, p) -> {
      if (m instanceof LocalXmlModel) {
        for (BeanPointer<?> pointer : ((LocalXmlModel) m).getDescendantBeans(context)) {
          if (!p.process(pointer))
            return false;
        }
      }
      return true;
    }));

    return pointers;
  }

  public static Set<String> getAllBeanNames(CommonInfraModel model, BeanPointer<?> pointer) {
    String name = pointer.getName();
    if (StringUtil.isEmptyOrSpaces(name))
      return Collections.emptySet();

    Set<String> results = new HashSet<>();

    visitRelatedModels(model, context(Processors.cancelableCollectProcessor(results), (m, p) -> {
      if (m instanceof CacheableCommonInfraModel) {
        for (String s : ((CacheableCommonInfraModel) m).getAllBeanNames(pointer)) {
          if (!p.process(s))
            return false;
        }
      }
      return true;
    }));
    return results.size() > 0 ? new HashSet<>(results) : Collections.singleton(name);
  }

  public static Set<BeanPointer<?>> findQualifiedBeans(CommonInfraModel model, InfraQualifier qualifier) {
    Set<BeanPointer<?>> pointers = new LinkedHashSet<>();
    visitRecursionAwareRelatedModels(model, context(Processors.cancelableCollectProcessor(pointers), (m, p) -> {
      if (m instanceof CacheableCommonInfraModel) {
        for (BeanPointer<?> pointer : ((CacheableCommonInfraModel) m).findQualified(qualifier)) {
          if (!p.process(pointer))
            return false;
        }
      }
      return true;
    }));

    return pointers;
  }

  /**
   * Collects config files from the given model and all related models.
   *
   * @param model the model to traverse.
   * @return config files from the given model and all related models.
   */

  public static Set<PsiFile> getConfigFiles(CommonInfraModel model) {
    CommonProcessors.CollectProcessor<PsiFile> processor = new CommonProcessors.CollectProcessor<>(new HashSet<>());
    processConfigFiles(model, processor);
    return new HashSet<>(processor.getResults());
  }

  /**
   * Checks whether the given config file belongs to the model or one of the related models or not.
   *
   * @param model the model to traverse.
   * @param configFile the configuration file to search.
   * @return {@code true} if the given model or one of the related models uses the given config file, otherwise {@code false}.
   */
  public static boolean hasConfigFile(CommonInfraModel model, PsiFile configFile) {
    VirtualFile virtualFile = configFile.getVirtualFile();
    CommonProcessors.FindProcessor<PsiFile> findProcessor = new CommonProcessors.FindFirstProcessor<>() {
      @Override
      protected boolean accept(PsiFile file) {
        // TODO [konstantin.aleev] comparing virtual files is a workaround for KT-35188
        return Comparing.equal(virtualFile, file.getVirtualFile()) ||
                configFile.getManager().areElementsEquivalent(configFile, file);
      }
    };
    processConfigFiles(model, findProcessor);
    return findProcessor.isFound();
  }

  public static void processConfigFiles(CommonInfraModel model, Processor<? super PsiFile> processor) {
    visitRecursionAwareRelatedModels(model, context(processor, (m, p) -> {
      if (m instanceof LocalModel) {
        PsiFile file = ((LocalModel<?>) m).getConfig().getContainingFile();
        return file == null || p.process(file);
      }
      return true;
    }), false);
  }
}

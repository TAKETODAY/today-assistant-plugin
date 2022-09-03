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

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.inheritance.ImplicitSubclassInspection;
import com.intellij.util.ArrayUtil;

import cn.taketoday.assistant.code.cache.highlighting.CacheAnnotationsOnInterfaceInspection;
import cn.taketoday.assistant.code.cache.highlighting.CacheNamesInspection;
import cn.taketoday.assistant.code.cache.highlighting.CacheableAndCachePutInspection;
import cn.taketoday.assistant.code.cache.highlighting.CacheableComponentsInspection;
import cn.taketoday.assistant.code.event.highlighting.EventListenerInspection;
import cn.taketoday.assistant.model.highlighting.autowire.InfraConstructorAutowiringInspection;
import cn.taketoday.assistant.model.highlighting.autowire.InfraInjectionPointsAutowiringInspection;
import cn.taketoday.assistant.model.highlighting.autowire.InfraUastAutowiredMembersInspection;
import cn.taketoday.assistant.model.highlighting.autowire.JavaStaticMembersAutowiringInspection;
import cn.taketoday.assistant.model.highlighting.autowire.xml.InfraXmlAutowiringInspection;
import cn.taketoday.assistant.model.highlighting.autowire.xml.XmlAutowireExplicitlyInspection;
import cn.taketoday.assistant.model.highlighting.jam.ComponentScanInspection;
import cn.taketoday.assistant.model.highlighting.jam.InfraAsyncMethodInspection;
import cn.taketoday.assistant.model.highlighting.jam.InfraContextJavaBeanUnresolvedMethodsInspection;
import cn.taketoday.assistant.model.highlighting.jam.InfraScheduledMethodInspection;
import cn.taketoday.assistant.model.highlighting.jam.RequiredAnnotationInspection;
import cn.taketoday.assistant.model.highlighting.xml.BeanAttributesInspection;
import cn.taketoday.assistant.model.highlighting.xml.BeanInstantiationInspection;
import cn.taketoday.assistant.model.highlighting.xml.FactoryMethodInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraAbstractBeanReferencesInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraBeanNameConventionInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraConstructorArgInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraContextComponentScanInconsistencyInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraDuplicatedBeanNamesInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraIncorrectResourceTypeInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraInjectionValueTypeInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraLookupMethodInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraPlaceholdersInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraPublicFactoryMethodInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraRequiredBeanTypeInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraScopesInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraUnparsedCustomBeanInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraUtilSchemaInspection;
import cn.taketoday.assistant.model.highlighting.xml.InfraXmlModelInspection;
import cn.taketoday.assistant.model.highlighting.xml.InjectionValueConsistencyInspection;
import cn.taketoday.assistant.model.highlighting.xml.InjectionValueStyleInspection;
import cn.taketoday.assistant.model.highlighting.xml.RequiredPropertyInspection;
import cn.taketoday.assistant.model.jam.lookup.LookupInjectionInspection;
import cn.taketoday.assistant.model.jam.testContexts.ContextConfigurationInspection;
import cn.taketoday.assistant.model.jam.testContexts.TestingDirtiesContextInspection;
import cn.taketoday.assistant.model.jam.testContexts.TestingTransactionalInspection;
import cn.taketoday.assistant.model.jam.transaction.TransactionalComponentInspection;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/26 20:40
 */
public class InfraInspectionsRegistryImpl extends InfraInspectionsRegistry {

  public Class<? extends LocalInspectionTool>[] getSpringInspectionClasses() {
    Class[] allInspectionClasses = getInspectionClasses();
    InfraInspectionsRegistry.Contributor[] var2 = Contributor.EP_NAME.getExtensions();

    for (Contributor contributor : var2) {
      allInspectionClasses = ArrayUtil.mergeArrays(allInspectionClasses, contributor.getInspectionClasses());
    }

    return allInspectionClasses;
  }

  public Class<? extends LocalInspectionTool>[] getTestSpringInspectionClasses() {
    return getInspectionClasses();
  }

  public Class<? extends LocalInspectionTool> getTestSpringModelInspectionClass() {
    return InfraXmlModelInspection.class;
  }

  private static Class[] getInspectionClasses() {
    return new Class[] {
            InfraXmlModelInspection.class,
            InfraScopesInspection.class,
            InfraPlaceholdersInspection.class,
            InfraBeanNameConventionInspection.class,
            InfraInjectionValueTypeInspection.class,
            InfraXmlAutowiringInspection.class,
            InfraConstructorArgInspection.class,
            FactoryMethodInspection.class,
            InfraPublicFactoryMethodInspection.class,
            InfraLookupMethodInspection.class,
            InjectionValueStyleInspection.class,
            InjectionValueConsistencyInspection.class,
            InfraAbstractBeanReferencesInspection.class,
            XmlAutowireExplicitlyInspection.class,
            InfraDuplicatedBeanNamesInspection.class,
            InfraUtilSchemaInspection.class,
            BeanInstantiationInspection.class,
            InfraInjectionPointsAutowiringInspection.class,
            InfraConstructorAutowiringInspection.class,
            InfraUastAutowiredMembersInspection.class,
            JavaStaticMembersAutowiringInspection.class,
            RequiredAnnotationInspection.class,
            RequiredPropertyInspection.class,
            InfraUnparsedCustomBeanInspection.class,
            InfraRequiredBeanTypeInspection.class,
            InfraIncorrectResourceTypeInspection.class,
            ContextConfigurationInspection.class,
            InfraContextComponentScanInconsistencyInspection.class,
            TransactionalComponentInspection.class,
            InfraContextJavaBeanUnresolvedMethodsInspection.class,
            BeanAttributesInspection.class,
            ComponentScanInspection.class,
            CacheableComponentsInspection.class,
            CacheNamesInspection.class,
            CacheAnnotationsOnInterfaceInspection.class,
            CacheableAndCachePutInspection.class,
            TestingDirtiesContextInspection.class,
            TestingTransactionalInspection.class,
            InfraScheduledMethodInspection.class,
            InfraAsyncMethodInspection.class,
            ImplicitSubclassInspection.class,
            EventListenerInspection.class,
            LookupInjectionInspection.class
    };
  }
}

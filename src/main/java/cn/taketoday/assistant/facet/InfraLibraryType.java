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

package cn.taketoday.assistant.facet;

import com.intellij.framework.library.DownloadableLibraryType;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/22 15:23
 */
public class InfraLibraryType extends DownloadableLibraryType {

  protected InfraLibraryType() {
    super(InfraBundle.messagePointer("infra"), "today-infrastructure", "today-infrastructure",
            InfraLibraryType.class.getResource("/versions/infra.xml"));
  }

  @Override
  public Icon getLibraryTypeIcon() {
    return Icons.Today;
  }

  @Override
  protected String[] getDetectionClassNames() {
    return new String[] {
            "cn.taketoday.lang.Version",
            "cn.taketoday.web.socket.Message",
            "cn.taketoday.transaction.TransactionManager",
            "cn.taketoday.test.context.TestContext",
            "cn.taketoday.retry.RetryOperations",
            "cn.taketoday.cache.RedissonCache",
            "cn.taketoday.orm.hibernate5.HibernateOperations",
            "cn.taketoday.orm.mybatis.SqlSessionFactoryBean",
            "cn.taketoday.context.ApplicationContext",
            "cn.taketoday.aop.Advisor",
            "cn.taketoday.jdbc.RepositoryManager",
            "cn.taketoday.cache.jcache.JCacheCache",
            "cn.taketoday.web.RequestContext",
            "cn.taketoday.instrument.InstrumentationSavingAgent",
            "cn.taketoday.framework.Application",
            "cn.taketoday.transaction.aspectj.AnnotationTransactionAspect",
            "cn.taketoday.context.index.processor.CandidateComponentsIndexer",
            "cn.taketoday.beans.BeanMetadata"
    };
  }

}

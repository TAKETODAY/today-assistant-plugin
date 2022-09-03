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

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 11:45
 */
public interface JavaeeConstant {

  String JAVAX_INJECT = "javax.inject.Inject"; //  support for JSR-330
  String JAKARTA_INJECT = "jakarta.inject.Inject"; //

  String JAVAX_NAMED = "javax.inject.Named"; // support for JSR-330
  String JAKARTA_NAMED = "jakarta.inject.Named"; //

  String JAVAX_INJECT_QUALIFIER = "javax.inject.Qualifier"; // support for JSR-330
  // javax annotations
  String JAVAX_MANAGED_BEAN = "javax.annotation.ManagedBean";
  String JAKARTA_MANAGED_BEAN = "jakarta.annotation.ManagedBean";

  String JAVAX_RESOURCE = "javax.annotation.Resource";
  String JAKARTA_RESOURCE = "jakarta.annotation.Resource";

  String JAVAX_TRANSACTIONAL = "javax.transaction.Transactional";
  String JAKARTA_TRANSACTIONAL = "javax.transaction.Transactional";

  String JAVAX_SERVLET_WEB_SERVLET = "javax.servlet.annotation.WebServlet";
  String JAKARTA_SERVLET_WEB_SERVLET = "jakarta.servlet.annotation.WebServlet";
  String JAVAX_RS_PATH = "javax.ws.rs.Path";
  String JAKARTA_RS_PATH = "jakarta.ws.rs.Path";
  String[] PRIORITY_ANNOTATIONS = { "javax.annotation.Priority", "jakarta.annotation.Priority" };

  String JAVAX_VALIDATOR_FACTORY = "javax.validation.ValidatorFactory";
  String JAKARTA_VALIDATOR_FACTORY = "jakarta.validation.ValidatorFactory";

  //JSR-303
  String JAVAX_CONSTRAINT_VALIDATOR = "javax.validation.ConstraintValidator";
  String JAKARTA_CONSTRAINT_VALIDATOR = "jakarta.validation.ConstraintValidator";
}

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
package cn.taketoday.assistant.model.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Determines {@link DomInfraBean} bean name.
 * <p/>
 * This can be done either:
 * <ol>
 * <li>statically via {@link #value()}</li>
 * <li>at runtime via {@link #provider()}</li>
 * </ol>
 * Setting both attributes is not allowed.
 *
 * @see DomInfraBeanImpl#getBeanName()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BeanName {

  /**
   * Static bean name.
   *
   * @return Bean name.
   */
  String value() default "";

  /**
   * Dynamic bean name.
   *
   * @return Provider class.
   */
  Class<? extends BeanNameProvider> provider() default BeanNameProvider.class;

  /**
   * Whether the provided bean name is to be used for display purposes only.
   * <p/>
   * Usually used with "container" or "marker" beans having infrastructure role using "virtual name".
   *
   * @return {@code true} if name is not actual bean identifier.
   */
  boolean displayOnly() default false;
}

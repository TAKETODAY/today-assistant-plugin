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
package cn.taketoday.assistant.model.utils

import cn.taketoday.assistant.model.BeanPointer
import cn.taketoday.assistant.model.CommonInfraBean
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.Processor
import com.intellij.util.SmartList
import com.intellij.util.containers.FactoryMap

/**
 * Processor which collects [BeanPointer]s ignoring beans from "ComponentScan" if they were explicitly redefined
 * in Java or XML configuration.
 */
class ExplicitRedefinitionAwareBeansCollector<T : CommonInfraBean> : Processor<BeanPointer<T>> {

    private val beansByNameMap = FactoryMap.createMap<String, MutableList<BeanPointer<T>>>(
        { SmartList() },
        { linkedMapOf() }
    )

    val result: Set<BeanPointer<T>>
        get() = beansByNameMap.values.flatMapTo(linkedSetOf<BeanPointer<T>>()) { mixedBeans ->
            mixedBeans.takeIf { it.size <= 1 } ?: mixedBeans.groupBy {
                it.beanClass?.qualifiedName ?: ""
            }.values.flatMap { mixedBeansOfOneType ->
                mixedBeansOfOneType.takeIf { it.size <= 1 } ?: mixedBeansOfOneType.filter { isExplicitlyDefined(it) }
                    .takeIf { it.isNotEmpty() } ?: mixedBeansOfOneType
            }
        }

    override fun process(pointer: BeanPointer<T>): Boolean {
        ProgressManager.checkCanceled()
        beansByNameMap.getValue(pointer.name ?: "").add(pointer)
        return true
    }

    private fun isExplicitlyDefined(pointer: BeanPointer<T>) = pointer.psiElement?.isPhysical ?: false
}
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

package cn.taketoday.assistant.context.model.visitors;

import com.intellij.util.Processor;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;

public final class InfraModelVisitorContext<P> {
  private final Set<CommonInfraModel> visited = new HashSet();
  private final Exec<P> exec;
  private final VisitorAwareProcessor<P> vp;

  public interface Exec<P> {
    boolean run(CommonInfraModel infraModel, Processor<? super P> processor);
  }

  private InfraModelVisitorContext(Processor<? super P> p, Exec<P> e) {
    this.exec = e;
    this.vp = new VisitorAwareProcessor<>(this, p);
  }

  public boolean visit(CommonInfraModel model) {
    if (hasBeenVisited(model)) {
      return true;
    }
    this.visited.add(model);
    return this.exec.run(model, this.vp);
  }

  public boolean hasBeenVisited(CommonInfraModel model) {
    return this.visited.contains(model);
  }

  private static class VisitorAwareProcessor<P> implements Processor<P> {
    private final InfraModelVisitorContext<P> visitor;
    private final Processor<? super P> delegate;

    VisitorAwareProcessor(InfraModelVisitorContext<P> visitor, Processor<? super P> delegate) {
      this.visitor = visitor;
      this.delegate = delegate;
    }

    public boolean process(P p) {
      return this.delegate.process(p);
    }
  }

  public static <P> InfraModelVisitorContext<P> context(Processor<? super P> p, Exec<P> exec) {
    if (p instanceof VisitorAwareProcessor) {
      return ((VisitorAwareProcessor) p).visitor;
    }
    return new InfraModelVisitorContext<>(p, exec);
  }
}

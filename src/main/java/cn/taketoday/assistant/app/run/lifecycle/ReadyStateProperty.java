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

package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

class ReadyStateProperty extends AbstractProperty<Boolean> {
  private static final Logger LOG = Logger.getInstance(ReadyStateProperty.class);

  private static final long STATE_CHECKING_RETRY_INTERVAL = 500;
  private static final long STATE_CHECKING_DELAY_INTERVAL = 1000;
  private static final int STATE_CHECKING_RETRY_COUNT = 20;

  private final Object lock = new Object();
  private final Property<String> serviceUrl;
  private final LifecycleErrorHandler errorHandler;
  private final Property<InfraModuleDescriptor> moduleDescriptor;

  private Runnable checker;

  private long start;
  private long retryCount;

  private volatile boolean disposed;

  private static final long STATE_CHECKING_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
  private static final long SERVICE_URL_RETRIEVING_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

  ReadyStateProperty(Property<InfraModuleDescriptor> moduleDescriptor,
          Property<String> serviceUrl, LifecycleErrorHandler errorHandler, Disposable parent) {
    super(Boolean.FALSE);
    this.disposed = false;
    this.serviceUrl = serviceUrl;
    this.errorHandler = errorHandler;
    this.moduleDescriptor = moduleDescriptor;
    this.serviceUrl.addPropertyListener(new PropertyListener() {

      @Override
      public void propertyChanged() { }

      @Override
      public void computationFinished() {
        if (StringUtil.isNotEmpty(serviceUrl.getValue())) {
          startChecking();
        }
      }

      @Override
      public void computationFailed(Exception e) {
        Throwable cause = e.getCause();
        if ((e instanceof LifecycleException) && cause != null) {
          synchronized(lock) {
            retryCount++;
            if (retryCount < STATE_CHECKING_RETRY_COUNT
                    && System.currentTimeMillis() - start <= SERVICE_URL_RETRIEVING_TIMEOUT) {
              var scheduler = JobScheduler.getScheduler();
              scheduler.schedule(serviceUrl::compute, STATE_CHECKING_RETRY_INTERVAL, TimeUnit.MILLISECONDS);
              return;
            }
            errorHandler.handleLifecycleError(
                    message("infra.application.endpoints.error.failed.to.retrieve.jmx.service.url"));
          }
        }
        setValue(null);
        Exception toReport = (!(e instanceof LifecycleException) || !(cause instanceof Exception)) ? e : (Exception) cause;

        dispatchComputationFailed(toReport);
        dispatchComputationFinished();
      }
    });

    if (parent != null) {
      Disposer.register(parent, this);
    }
  }

  @Override
  public void dispose() {
    this.disposed = true;
  }

  @Override
  public void compute() {
    synchronized(this.lock) {
      this.start = System.currentTimeMillis();
      this.retryCount = 0L;
    }
    JobScheduler.getScheduler()
            .schedule(serviceUrl::compute, STATE_CHECKING_DELAY_INTERVAL, TimeUnit.MILLISECONDS);
  }

  private void startChecking() {
    synchronized(this.lock) {
      if (this.checker != null) {
        return;
      }
      setValue(Boolean.FALSE);
      this.checker = new Runnable() {
        @Override
        public void run() {
          if (disposed || Boolean.TRUE.equals(getValue())) {
            return;
          }
          boolean timeoutExceeded = false;
          synchronized(lock) {
            if (System.currentTimeMillis() - start > STATE_CHECKING_TIMEOUT) {
              setValue(null);
              checker = null;
              timeoutExceeded = true;
            }
          }
          if (timeoutExceeded) {
            Exception e = new LifecycleException(message(
                    "infra.application.endpoints.application.ready.check.timeout.exceeded"), null);
            dispatchComputationFailed(e);
            dispatchComputationFinished();
            return;
          }
          var moduleDescriptor = Objects.requireNonNull(ReadyStateProperty.this.moduleDescriptor.getValue());
          try {
            var connector = new InfraApplicationConnector(serviceUrl.getValue(), moduleDescriptor);
            if (connector.isReady()) {
              setValue(Boolean.TRUE);
              dispatchPropertyChanged();
              dispatchComputationFinished();
              connector.close();
              return;
            }
            connector.close();
            JobScheduler.getScheduler().schedule(this, STATE_CHECKING_RETRY_INTERVAL, TimeUnit.MILLISECONDS);
          }
          catch (Exception e2) {
            boolean retryCountExceeded = false;
            synchronized(lock) {
              retryCount++;
              if (retryCount >= STATE_CHECKING_RETRY_COUNT) {
                setValue(null);
                checker = null;
                retryCountExceeded = true;
              }
              if (retryCountExceeded) {
                LOG.debug(e2);
                dispatchComputationFailed(e2);
                dispatchComputationFinished();
              }
            }
          }
        }
      };
      ApplicationManager.getApplication().executeOnPooledThread(this.checker);
    }
  }

}

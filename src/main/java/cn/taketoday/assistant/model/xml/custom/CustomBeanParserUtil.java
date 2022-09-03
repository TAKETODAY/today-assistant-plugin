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

package cn.taketoday.assistant.model.xml.custom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.parsing.SourceExtractor;
import cn.taketoday.beans.factory.xml.DocumentLoader;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.Resource;

public final class CustomBeanParserUtil {
  static final String COPY_KEY = "CustomBeanParser.COPY_KEY";
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  static void parseCustomBean(String tagText, int timeout) {
    List result;
    try {
      result = getAdditionalBeans(tagText, timeout);
    }
    catch (Throwable var7) {
      CustomBeanParser.printException(var7);
      return;
    }

    if (result == null) {
      System.out.print("timeout\n\n##$%^$&%@^#%$#%^&$^&%*&^(*(^&*(&^*&%*&%&*^\n");
    }
    else {
      System.out.print("result\n");
      System.out.print(result.get(0));

      for (int i = 1; i < result.size(); ++i) {
        List<String> s2 = (List) result.get(i);
        System.out.print("\ninfo\n");
        for (String s1 : s2) {
          System.out.print(s1 + "\n");
        }

        System.out.print("info_end");
      }

      System.out.print("\n\n##$%^$&%@^#%$#%^&$^&%*&^(*(^&*(&^*&%*&%&*^\n");
    }
  }

  public static List<String> getAdditionalBeans(String text, int timeout) throws Throwable {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    MyBeanDefinitionsRegistry registry = new MyBeanDefinitionsRegistry();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
    reader.setValidationMode(0);
    reader.setNamespaceAware(true);
    reader.setDocumentLoader(new DocumentLoader() {
      public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
        factory.setNamespaceAware(namespaceAware);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        if (entityResolver != null) {
          builder.setEntityResolver(entityResolver);
        }

        if (errorHandler != null) {
          builder.setErrorHandler(errorHandler);
        }

        Document document = builder.parse(inputSource);
        this.process(document.getDocumentElement(), EMPTY_INT_ARRAY);
        return document;
      }

      private void process(Element element, int[] path) {
        try {
          element.setUserData(COPY_KEY, path, null);
        }
        catch (Throwable var7) {
          throw new RuntimeException("class " + element.getClass()
                  .getName() + " doesn't conform to the interface " + Element.class.getName() + " specification:\n     " + var7 + "\nCheck your classpath for outdated XML APIs (Xerces, etc.)");
        }

        int index = 0;
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); ++i) {
          Node node = nodes.item(i);
          if (node instanceof Element) {
            this.process((Element) node, append(path, index));
            ++index;
          }
        }

      }
    });
    reader.setProblemReporter(new LenientProblemReporter());
    reader.setSourceExtractor(new SourceExtractor() {
      public Object extractSource(Object sourceCandidate, Resource definingResource) {
        return sourceCandidate instanceof Element ? ((Element) sourceCandidate).getUserData(COPY_KEY) : null;
      }
    });
    ByteArrayResource resource = new ByteArrayResource(text.getBytes());
    SemaphoreCopy reads = new SemaphoreCopy();
    reads.down();
    Throwable[] exception = new Throwable[1];
    Thread thread = new Thread("custom bean parse") {
      public void run() {
        try {
          reader.loadBeanDefinitions(resource);
        }
        catch (Throwable var5) {
          exception[0] = var5;
        }
        finally {
          reads.up();
        }

      }
    };
    thread.start();
    if (!reads.waitFor(timeout)) {
      return null;
    }
    else {
      Throwable throwable = exception[0];
      if (throwable == null) {
        return registry.getResult();
      }
      else {
        while (throwable instanceof BeanDefinitionStoreException) {
          Throwable cause = ((BeanDefinitionStoreException) throwable).getRootCause();
          if (cause == null) {
            break;
          }

          throwable = cause;
        }

        throw throwable;
      }
    }
  }

  public static int[] append(int[] array, int value) {
    array = realloc(array, array.length + 1);
    array[array.length - 1] = value;
    return array;
  }

  public static int[] realloc(int[] array, int newSize) {
    if (newSize == 0) {
      return EMPTY_INT_ARRAY;
    }
    else {
      int oldSize = array.length;
      if (oldSize == newSize) {
        return array;
      }
      else {
        int[] result = new int[newSize];
        System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
        return result;
      }
    }
  }

  private static class SemaphoreCopy {
    private int mySemaphore;

    private SemaphoreCopy() {
      this.mySemaphore = 0;
    }

    public synchronized void down() {
      ++this.mySemaphore;
    }

    public synchronized void up() {
      --this.mySemaphore;
      if (this.mySemaphore == 0) {
        this.notifyAll();
      }

    }

    public synchronized boolean waitFor(long timeout) {
      try {
        if (this.mySemaphore == 0) {
          return true;
        }
        else {
          long startTime = System.currentTimeMillis();

          long elapsed;
          for (long waitTime = timeout; this.mySemaphore > 0; waitTime = timeout - elapsed) {
            this.wait(waitTime);
            elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= timeout) {
              break;
            }
          }

          return this.mySemaphore == 0;
        }
      }
      catch (InterruptedException var9) {
        throw new RuntimeException(var9);
      }
    }
  }
}

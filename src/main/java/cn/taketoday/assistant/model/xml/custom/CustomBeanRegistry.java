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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.jsp.JspSpiUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.util.IncludedXmlTag;

import org.jdom.Element;
import org.jdom.IllegalNameException;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.model.xml.impl.CustomBeanWrapperImpl;
import cn.taketoday.assistant.schemas.InfraSchemaProvider;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

@State(name = "CustomBeanRegistry", storages = { @Storage("customInfraBeans.xml") })
public final class CustomBeanRegistry implements PersistentStateComponent<CustomBeanRegistry.State> {
  private static final int CURRENT_VERSION = 1;
  private static final Logger LOG = Logger.getInstance(CustomBeanRegistry.class);

  private static final String CUSTOM_BEAN_PARSER = "cn.taketoday.assistant.model.xml.custom.CustomBeanParser";
  private Map<String, List<CustomBeanInfo>> myText2Infos = new HashMap();
  private Map<MyQName, CustomBeanInfo> myPolicies = new HashMap();

  private static final String FAKE_ID = "IntelliJIDEARulezzz";

  public static final String CUSTOM_SPRING_BEANS_PARSING_TIMEOUT = "custom.infra.beans.parsing.timeout";

  public static class State {
    public int version = CURRENT_VERSION;
    public Map<String, List<CustomBeanInfo>> map = new HashMap();
    public Map<MyQName, CustomBeanInfo> policies = new HashMap();
  }

  private static boolean isDebug() {
    return false;
  }

  private static int getTimeout() {
    try {
      return Integer.parseInt(System.getProperty(CUSTOM_SPRING_BEANS_PARSING_TIMEOUT));
    }
    catch (NumberFormatException e) {
      return isDebug() ? 10000000 : 10000;
    }
  }

  public State getState() {
    State bean = new State();
    bean.map = new HashMap();
    for (String s : this.myText2Infos.keySet()) {
      List<CustomBeanInfo> infos = this.myText2Infos.get(s);
      if (infos != null && !infos.isEmpty()) {
        bean.map.put(s, infos);
      }
    }
    bean.policies = this.myPolicies;
    return bean;
  }

  public void loadState(State state) {
    if (state.version == CURRENT_VERSION) {
      this.myText2Infos = state.map;
      this.myPolicies = state.policies;
    }
  }

  public ParseResult parseBeans(Collection<XmlTag> tags) {
    ParseResult result = ParseResult.EMPTY_PARSE_RESULT;
    for (XmlTag tag : tags) {
      if (tag.isValid()) {
        result = result.merge(parseBean(tag));
      }
    }
    return result;
  }

  public ParseResult parseBean(XmlTag tag) {
    String text = getIdealBeanText(tag);
    try {
      Module module = ModuleUtilCore.findModuleForPsiElement(tag);
      if (module == null) {
        return ParseResult.EMPTY_PARSE_RESULT;
      }
      ParseResult result = getCustomBeans(createTag(text, tag.getProject()), module);
      List<CustomBeanInfo> infos = result.beans == null ? Collections.emptyList() : result.beans;
      this.myText2Infos.put(text, infos);
      return result;
    }
    catch (IncorrectOperationException e) {
      return new ParseResult(e);
    }
  }

  public static CustomBeanRegistry getInstance(Project project) {
    return project.getService(CustomBeanRegistry.class);
  }

  @Nullable
  public List<CustomBeanInfo> getParseResult(XmlTag tag) {
    CustomBeanInfo policy = this.myPolicies.get(new MyQName(tag.getNamespace(), tag.getLocalName()));
    if (policy != null) {
      CustomBeanInfo info = new CustomBeanInfo(policy);
      info.beanName = tag.getAttributeValue(policy.idAttribute);
      return Collections.singletonList(info);
    }
    return this.myText2Infos.get(getIdealBeanText(tag));
  }

  private static String getIdealBeanText(XmlTag tag) {
    Set<String> usedNamespaces = collectReferencedNamespaces(tag);
    String text = tag instanceof IncludedXmlTag ? ((IncludedXmlTag) tag).getOriginal().getText() : tag.getText();
    if (text == null) {
      throw new AssertionError("Null tag text: " + tag + "; " + tag.getClass());
    }
    try {
      XmlTag copy = createTag(text, tag.getProject());
      Map<XmlTag, XmlTag> original2Copy = calcMapping(tag, copy);
      for (XmlTag parent = tag; parent != null; parent = parent.getParentTag()) {
        XmlAttribute[] attributes = parent.getAttributes();
        int length = attributes.length;
        for (XmlAttribute attribute : attributes) {
          if (attribute.isNamespaceDeclaration()) {
            String prefix = "xmlns".equals(attribute.getName()) ? "" : attribute.getLocalName();
            String ns = copy.getNamespaceByPrefix(prefix);
            if (StringUtil.isEmpty(ns) && usedNamespaces.contains(attribute.getDisplayValue())) {
              copy.add(attribute);
            }
          }
        }
      }
      computeDefaultValues(tag, original2Copy);
      text = copy.getText();
      try {
        Element element = JDOMUtil.load(text);
        return JDOMUtil.write(element);
      }
      catch (IllegalNameException | IOException | JDOMException e) {
        return text;
      }
    }
    catch (IncorrectOperationException e3) {
      LOG.error(e3);
      return text;
    }
  }

  private static Map<XmlTag, XmlTag> calcMapping(XmlTag tag, XmlTag copy) {
    Map<XmlTag, XmlTag> original2Copy = new HashMap<>();
    tag.accept(new PsiRecursiveElementWalkingVisitor() {

      public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element instanceof XmlTag) {
          TextRange range = element.getTextRange().shiftRight(-tag.getTextRange().getStartOffset());
          original2Copy.put((XmlTag) element, PsiTreeUtil.findElementOfClassAtRange(copy.getContainingFile(), range.getStartOffset(), range.getEndOffset(), XmlTag.class));
        }
      }
    });
    return original2Copy;
  }

  private static void computeDefaultValues(XmlTag root, Map<XmlTag, XmlTag> original2Copy) {
    root.accept(new PsiRecursiveElementWalkingVisitor() {

      public void visitElement(PsiElement element) {
        XmlElementDescriptor descriptor;
        super.visitElement(element);
        if (element instanceof XmlTag each) {
          XmlTag copy = original2Copy.get(each);
          if (copy != null && (descriptor = each.getDescriptor()) != null) {
            XmlAttributeDescriptor[] attributesDescriptors = descriptor.getAttributesDescriptors(each);
            for (XmlAttributeDescriptor attributeDescriptor : attributesDescriptors) {
              String defValue = attributeDescriptor.getDefaultValue();
              String attrName = attributeDescriptor.getName(each);
              if (StringUtil.isNotEmpty(defValue) && each.getAttribute(attrName) == null) {
                copy.setAttribute(attrName, defValue);
              }
            }
            String defValue2 = descriptor.getDefaultValue();
            if (StringUtil.isNotEmpty(defValue2) && StringUtil.isEmpty(each.getValue().getTrimmedText())) {
              copy.getValue().setText(defValue2);
            }
          }
        }
      }
    });
  }

  private static Set<String> collectReferencedNamespaces(XmlTag tag) {
    Set<String> usedNamespaces = new HashSet<>();
    tag.accept(new XmlElementVisitor() {
      public void visitXmlTag(XmlTag tag2) {
        usedNamespaces.add(tag2.getNamespace());
        XmlAttribute[] attributes = tag2.getAttributes();
        for (XmlAttribute attribute : attributes) {
          visitXmlAttribute(attribute);
        }
        XmlTag[] subTags = tag2.getSubTags();
        for (XmlTag xmlTag : subTags) {
          visitXmlTag(xmlTag);
        }
      }

      public void visitXmlAttribute(XmlAttribute attribute) {
        usedNamespaces.add(attribute.getNamespace());
      }
    });
    return usedNamespaces;
  }

  private static XmlTag createTag(String text, Project project) throws IncorrectOperationException {
    return XmlElementFactory.getInstance(project).createTagFromText(text);
  }

  public static XmlTag getActualSourceTag(CustomBeanInfo info, XmlTag tag) {
    List<Integer> path = info.path;
    for (Integer index : path) {
      XmlTag parent = tag;
      assert parent != null;
      XmlTag[] subTags = parent.getSubTags();
      int i = index;
      tag = subTags[i];
      if (tag == null) {
        LOG.error("parent: " + parent.getText() + "\nindex: " + i + "\nsubTags: " + Arrays.toString(subTags));
      }
    }
    return tag;
  }

  public void addBeanPolicy(String namespace, String localName, CustomBeanInfo info) {
    this.myPolicies.put(new MyQName(namespace, localName), info);
  }

  public static class MyQName {
    public String namespace;
    public String localName;

    public MyQName() { }

    public MyQName(String namespace, String localName) {
      this.namespace = namespace;
      this.localName = localName;
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MyQName myQName)) {
        return false;
      }
      return this.localName.equals(myQName.localName) && this.namespace.equals(myQName.namespace);
    }

    public int hashCode() {
      int result = this.namespace.hashCode();
      return (31 * result) + this.localName.hashCode();
    }
  }

  private static void computeUrls(Module module, PathsList list) {
    // FIXME customNs
    File springPluginClassesLocation = new File(PathUtil.getJarPathForClass(CustomBeanWrapperImpl.class));
    if (springPluginClassesLocation.isFile()) {
      File customNsLocation = new File(springPluginClassesLocation.getParent(), "customNs");
      list.add(new File(customNsLocation, "customNs.jar").getAbsolutePath());
    }
    else {
      list.add(new File(springPluginClassesLocation.getParent(), "intellij.spring.customNs").getAbsolutePath());
    }
    Objects.requireNonNull(list);
    JspSpiUtil.processClassPathItems(null, module, list::add);
    list.addVirtualFiles(OrderEnumerator.orderEntries(module).recursively().sources().usingCache().getRoots());
  }

  private static ParseResult getCustomBeans(XmlTag tag, Module module) {
    Map<String, String> handlersToRun = findHandlersToRun(module, tag);
    String namespace = tag.getNamespace();
    if (!handlersToRun.containsKey(namespace)) {
      return new ParseResult(message("parse.no.namespace.handler", namespace));
    }
    JavaParameters javaParameters = new JavaParameters();
    javaParameters.setJdk(ModuleRootManager.getInstance(module).getSdk());
    javaParameters.setMainClass(CUSTOM_BEAN_PARSER);
    javaParameters.setUseClasspathJar(true);
    if (isDebug()) {
      javaParameters.getVMParametersList().addParametersString("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5239");
    }
    computeUrls(module, javaParameters.getClassPath());
    javaParameters.setUseDynamicClasspath(true);
    try {
      OSProcessHandler handler = new OSProcessHandler(javaParameters.toCommandLine());
      try {
        PrintWriter writer = new PrintWriter(handler.getProcessInput());
        try {
          handler.startNotify();
          int timeout = Math.max(getTimeout(), tag.getTextLength() * 150);
          writer.println(timeout);
          ParseResult result = invokeParser(writer, handler, tag, timeout);
          if (result.getStackTrace() != null && tag.getAttributeValue("id") == null) {
            try {
              tag.setAttribute("id", FAKE_ID);
            }
            catch (IncorrectOperationException e) {
              LOG.error(e);
            }
            ParseResult result1 = invokeParser(writer, handler, tag, timeout);
            List<CustomBeanInfo> list = result1.getBeans();
            if (list != null) {
              for (CustomBeanInfo info : list) {
                if (FAKE_ID.equals(info.beanName) && info.path.isEmpty()) {
                  info.beanName = null;
                  info.idAttribute = "id";
                }
              }
              result = result1;
            }
          }
          List<CustomBeanInfo> infos = result.getBeans();
          if (infos != null) {
            guessIdAttributeNames(writer, handler, tag, infos, timeout);
          }
          ParseResult parseResult = result;
          writer.close();
          handler.getProcess().destroy();
          handler.waitFor();
          return parseResult;
        }
        catch (Throwable th) {
          try {
            writer.close();
          }
          catch (Throwable th2) {
            th.addSuppressed(th2);
          }
          throw th;
        }
      }
      catch (Throwable th3) {
        handler.getProcess().destroy();
        handler.waitFor();
        throw th3;
      }
    }
    catch (ExecutionException e2) {
      return new ParseResult(e2);
    }
  }

  private static ParseResult invokeParser(PrintWriter writer, OSProcessHandler handler, XmlTag tag, int timeout) {
    Ref<ParseResult> result = Ref.create(null);
    Semaphore semaphore = new Semaphore();
    semaphore.down();
    StringBuilder other = new StringBuilder();
    handler.addProcessListener(new ProcessAdapter() {
      final StringBuilder sb = new StringBuilder();

      public void onTextAvailable(ProcessEvent event, Key outputType) {
        int j;
        List<Integer> map;
        try {
          if (outputType != ProcessOutputTypes.STDOUT) {
            other.append(event.getText());
            return;
          }
          this.sb.append(event.getText().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n"));
          int i = this.sb.indexOf(CustomBeanParser.MAGIC);
          if (i < 0) {
            return;
          }
          String input = this.sb.substring(0, i);
          this.sb.delete(0, i + CustomBeanParser.MAGIC.length());
          String[] lines = input.split("\n");
          int k = 0;
          while (k < lines.length && !"exception".equals(lines[k]) && !"timeout".equals(lines[k]) && !"result".equals(lines[k])) {
            k += CURRENT_VERSION;
          }
          if (k >= lines.length) {
            setResult(new ParseResult(message("internal.error.parsing.bean", input)));
            return;
          }
          String first = lines[k];
          if ("exception".equals(first)) {
            setResult(new ParseResult(new ParseResult.StackTrace(CustomBeanParser.decode(lines[k + CURRENT_VERSION]))));
          }
          else if ("timeout".equals(first)) {
            setResult(new ParseResult(message("timeout.parsing.bean")));
          }
          else if ("result".equals(first)) {
            SmartList smartList = new SmartList();
            String nextLine = lines[k + CURRENT_VERSION];
            boolean hasInfras = "has_infrastructures".equals(nextLine);
            for (int j2 = k + 2; j2 < lines.length; j2 = j + CURRENT_VERSION) {
              int i2 = j2;
              j = j2 + CURRENT_VERSION;
              CustomBeanInfo info = new CustomBeanInfo();
              while (!"info_end".equals(lines[j])) {
                int i3 = j;
                int j3 = j + CURRENT_VERSION;
                String prop = lines[i3];
                j = j3 + CURRENT_VERSION;
                String propValue = CustomBeanParser.decode(lines[j3]);
                if ("beanName".equals(prop)) {
                  info.beanName = propValue;
                }
                else if ("beanClassName".equals(prop)) {
                  info.beanClassName = propValue;
                }
                else if ("constructorArgumentCount".equals(prop)) {
                  info.constructorArgumentCount = Integer.parseInt(propValue);
                }
                else if ("factoryMethodName".equals(prop)) {
                  info.factoryMethodName = propValue;
                }
                else if ("factoryBeanName".equals(prop)) {
                  info.factoryBeanName = propValue;
                }
                else {
                  String separated = propValue.substring(CURRENT_VERSION);
                  if (StringUtil.isEmpty(separated)) {
                    map = Collections.emptyList();
                  }
                  else {
                    map = ContainerUtil.map(separated.split(";"), Integer::parseInt);
                  }
                  info.path = map;
                }
              }
              smartList.add(info);
            }
            setResult(new ParseResult(smartList, hasInfras));
          }
        }
        catch (Throwable e) {
          setResult(new ParseResult(e));
        }
      }

      private void setResult(ParseResult value) {
        result.set(value);
        handler.removeProcessListener(this);
        semaphore.up();
      }

      public void processTerminated(ProcessEvent event) {
        if (other.length() == 0 || this.sb.length() == 0) {
          setResult(new ParseResult(message("process.unexpectedly.terminated", "")));
          return;
        }
        String output = ":\n\nSTDOUT:\n" + this.sb + "\n\nOTHER:\n" + other;
        setResult(new ParseResult(message("process.unexpectedly.terminated", output)));
      }
    });
    writer.println("input");
    writer.println(CustomBeanParser.encode(tag.getText()));
    writer.flush();
    boolean inTime = semaphore.waitFor(timeout);
    ParseResult parseResult = result.get();
    if (parseResult == null) {
      if (inTime) {
        return new ParseResult(new ParseResult.StackTrace(other.toString()));
      }
      return new ParseResult(message("timeout.parsing.bean"));
    }
    return parseResult;
  }

  private static void guessIdAttributeNames(PrintWriter writer, OSProcessHandler reader, XmlTag tag, List<CustomBeanInfo> list, int timeout)
          throws IncorrectOperationException {
    String[] fakeNames = new String[list.size()];
    String[] idAttrs = new String[list.size()];
    boolean hasFakeIds = false;

    for (int i = 0; i < list.size(); ++i) {
      CustomBeanInfo info = list.get(i);
      if (info.idAttribute == null) {
        XmlTag sourceTag = getActualSourceTag(info, tag);
        String id = info.beanName;
        XmlAttribute idAttr = id == null ? null : ContainerUtil.find(sourceTag.getAttributes(), (xmlAttribute) -> {
          return !xmlAttribute.isNamespaceDeclaration() && id.equals(xmlAttribute.getDisplayValue());
        });
        if (idAttr != null) {
          String fakeName = FAKE_ID + i;
          fakeNames[i] = fakeName;
          idAttr.setValue(fakeName);
          idAttrs[i] = idAttr.getLocalName();
          hasFakeIds = true;
        }
      }
    }

    if (hasFakeIds) {
      List<CustomBeanInfo> withFakes = invokeParser(writer, reader, tag, timeout).getBeans();
      if (withFakes != null && withFakes.size() == list.size()) {
        for (int i = 0; i < fakeNames.length; ++i) {
          String name = fakeNames[i];
          if (name != null && name.equals(withFakes.get(i).beanName)) {
            list.get(i).idAttribute = idAttrs[i];
          }
        }
      }
    }

  }

  private static Map<String, String> findHandlersToRun(Module module, XmlTag tag) {
    Map<String, String> handlers = InfraSchemaProvider.getHandlers(module);
    if (handlers.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<String> referencedNamespaces = collectReferencedNamespaces(tag);
    HashMap<String, String> handlersToRun = new HashMap<>(referencedNamespaces.size());
    for (String namespace : handlers.keySet()) {
      if (referencedNamespaces.contains(namespace)) {
        handlersToRun.put(namespace, handlers.get(namespace));
      }
    }
    return handlersToRun;
  }

  public static final class ParseResult {
    static final ParseResult EMPTY_PARSE_RESULT = new ParseResult(Collections.emptyList(), false);
    @Nullable
    List<CustomBeanInfo> beans;
    boolean hasInfrastructures;
    @Nullable
    String errorMessage;
    @Nullable
    String stackTrace;

    private ParseResult(@Nullable List<CustomBeanInfo> beans, boolean hasInfrastructures) {
      this.beans = beans;
      this.hasInfrastructures = hasInfrastructures;
    }

    private static String getStackTrace(Throwable e) {
      return StringUtil.getThrowableText(e);
    }

    private ParseResult(Throwable t) {
      this(new StackTrace(getStackTrace(t)));
    }

    private ParseResult(@Nullable String errorMessage) {
      this.errorMessage = errorMessage;
    }

    private ParseResult(StackTrace resultStackTrace) {
      int i;
      this.stackTrace = StringUtil.convertLineSeparators(resultStackTrace.stackTraceText);
      int i2 = this.stackTrace.indexOf(CUSTOM_BEAN_PARSER);
      if (i2 >= 0 && (i = this.stackTrace.lastIndexOf(10, i2)) >= 0) {
        this.stackTrace = this.stackTrace.substring(0, i);
      }
    }

    @Nullable
    public String getErrorMessage() {
      return this.errorMessage;
    }

    @Nullable
    public String getStackTrace() {
      return this.stackTrace;
    }

    @Nullable
    public List<CustomBeanInfo> getBeans() {
      return this.beans;
    }

    public boolean hasInfrastructureBeans() {
      return this.hasInfrastructures;
    }

    public boolean hasErrors() {
      return this.stackTrace != null || this.errorMessage != null;
    }

    public ParseResult merge(ParseResult with) {
      ParseResult result = new ParseResult(null, this.hasInfrastructures || with.hasInfrastructures);
      result.stackTrace = this.stackTrace == null ? with.stackTrace : this.stackTrace;
      result.errorMessage = this.errorMessage == null ? with.errorMessage : this.errorMessage;
      result.beans = this.beans == null ? with.beans : with.beans == null ? this.beans : ContainerUtil.concat(this.beans, with.beans);
      return result;
    }

    public static class StackTrace {
      final String stackTraceText;

      StackTrace(String stackTraceText) {
        this.stackTraceText = stackTraceText;
      }
    }
  }
}

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

package cn.taketoday.assistant.schemas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.DefaultXmlExtension;
import com.intellij.xml.XmlSchemaProvider;
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;
import com.intellij.xml.index.XmlNamespaceIndex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraSchemaProvider extends XmlSchemaProvider {
  private static final Logger LOG = Logger.getInstance(InfraSchemaProvider.class);
  private static final Key<CachedValue<Map<String, VirtualFile>>> SCHEMAS_BUNDLE_KEY = Key.create("infra schemas");
  private static final CachedValueProvider.Result<Map<String, VirtualFile>> EMPTY_MAP_RESULT;
  private static final Set<String> SKIP_NAMESPACE_URLS;

  public XmlFile getSchema(String url, @Nullable Module module, PsiFile baseFile) {
    if (SKIP_NAMESPACE_URLS.contains(url)) {
      return null;
    }
    else {
      if (module == null) {
        PsiDirectory directory = baseFile.getParent();
        if (directory != null) {
          module = ModuleUtilCore.findModuleForPsiElement(directory);
        }
      }

      if (module == null) {
        return null;
      }
      else {
        Map<String, VirtualFile> schemas = getSchemas(module);
        Project project = module.getProject();
        VirtualFile file = schemas.get(url);
        if (file == null) {
          return null;
        }
        else {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
          return !(psiFile instanceof XmlFile) ? null : (XmlFile) psiFile;
        }
      }
    }
  }

  public boolean isAvailable(XmlFile file) {
    boolean isInfra = InfraDomUtils.isInfraXml(file);
    if (isInfra) {
      return true;
    }
    else {
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile == null) {
        return false;
      }
      else {
        String extension = virtualFile.getExtension();
        return "xsd".equals(extension);
      }
    }
  }

  public Set<String> getAvailableNamespaces(XmlFile file, String tagName) {
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null) {
      return Collections.emptySet();
    }
    else {
      Map<String, VirtualFile> map = getSchemas(module);
      HashSet<String> strings = new HashSet<>(map.size());

      for (VirtualFile virtualFile : map.values()) {
        String namespace = getNamespace(virtualFile, file.getProject());
        ContainerUtil.addIfNotNull(strings, namespace);
      }

      Set<String> filtered = DefaultXmlExtension.filterNamespaces(strings, tagName, file);
      if (InfraDomUtils.isInfraXml(file)) {

        filtered.add(InfraConstant.C_NAMESPACE);
        filtered.add(InfraConstant.P_NAMESPACE);
      }

      return filtered;
    }
  }

  @Nullable
  private static String getNamespace(VirtualFile virtualFile, Project project) {
    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
    if (psiFile instanceof XmlFile) {
      XmlDocument document = ((XmlFile) psiFile).getDocument();
      if (document != null) {
        PsiMetaData metaData = document.getMetaData();
        if (metaData instanceof XmlNSDescriptorImpl) {
          return ((XmlNSDescriptorImpl) metaData).getDefaultNamespace();
        }
      }
    }

    return null;
  }

  public String getDefaultPrefix(String namespace, XmlFile context) {

    if (!InfraDomUtils.isInfraXml(context)) {
      return null;
    }
    else {
      String[] strings = namespace.split("/");
      return strings[strings.length - 1];
    }
  }

  public Set<String> getLocations(String namespace, XmlFile context) {
    Module module = ModuleUtilCore.findModuleForPsiElement(context);
    if (module == null) {
      return null;
    }
    else {
      Map<String, VirtualFile> schemas = getSchemas(module);
      String best = null;
      Iterator<Map.Entry<String, VirtualFile>> var6 = schemas.entrySet().iterator();

      while (true) {
        String location;
        do {
          String s;
          do {
            do {
              Map.Entry entry;
              do {
                if (!var6.hasNext()) {
                  return best == null ? Collections.emptySet() : Collections.singleton(best);
                }

                entry = var6.next();
                location = (String) entry.getKey();
              }
              while (!location.endsWith(".xsd"));

              s = getNamespace((VirtualFile) entry.getValue(), context.getProject());
            }
            while (s == null);
          }
          while (!s.equals(namespace));

          if (!Character.isDigit(location.charAt(location.length() - ".xsd".length() - 1))) {
            return Collections.singleton(location);
          }
        }
        while (best != null && location.compareTo(best) <= 0);

        best = location;
      }
    }
  }

  private static Map<String, VirtualFile> getSchemas(Module module) {

    Project project = module.getProject();
    Map<String, VirtualFile> bundle = CachedValuesManager.getManager(project).getCachedValue(module, SCHEMAS_BUNDLE_KEY, () -> computeSchemas(module), false);

    return bundle == null ? Collections.emptyMap() : bundle;
  }

  public static Map<String, String> getHandlers(Module module) {
    return computeHandlers(module);
  }

  private static CachedValueProvider.Result<Map<String, VirtualFile>> computeSchemas(Module module) {
    List<PsiFile> infraSchemaFiles = InfraUtils.findConfigFilesInMetaInf(module, true, "spring.schemas", PsiFile.class);
    if (infraSchemaFiles.isEmpty()) {
      return EMPTY_MAP_RESULT;
    }
    else {
      Map<String, VirtualFile> map = new HashMap<>();
      List<Object> dependencies = new ArrayList<>();
      dependencies.add(ProjectRootManager.getInstance(module.getProject()));
      Iterator<PsiFile> var4 = infraSchemaFiles.iterator();
      label171:
      while (var4.hasNext()) {
        PsiFile schemaPsiFile = var4.next();
        dependencies.add(schemaPsiFile);
        PsiDirectory parent = schemaPsiFile.getContainingDirectory().getParent();

        assert parent != null;

        String root = parent.getVirtualFile().getUrl();
        if (!root.endsWith("/")) {
          root = root + "/";
        }

        InputStream inputStream = null;

        CachedValueProvider.Result var10;
        try {
          VirtualFile schemasFile = schemaPsiFile.getVirtualFile();
          inputStream = schemasFile.getInputStream();
          PropertyResourceBundle bundle = new PropertyResourceBundle(inputStream);
          Enumeration<String> keys = bundle.getKeys();

          while (true) {
            if (!keys.hasMoreElements()) {
              continue label171;
            }

            String key = keys.nextElement();
            String location = (String) bundle.handleGetObject(key);
            String schemaUrl = root + location;
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(schemaUrl);
            if (file != null) {
              map.put(key, file);
              if (key.endsWith(".xsd")) {
                int lastCharPos = key.length() - ".xsd".length() - 1;
                char lastChar = key.charAt(lastCharPos);
                if (!Character.isDigit(lastChar)) {
                  String namespace = XmlNamespaceIndex.getNamespace(file, module.getProject());
                  if (namespace != null) {
                    map.put(namespace, file);
                  }
                }
              }
            }
          }
        }
        catch (IOException var27) {
          LOG.error(var27);
          var10 = EMPTY_MAP_RESULT;
        }
        finally {
          if (inputStream != null) {
            try {
              inputStream.close();
            }
            catch (IOException var26) {
              LOG.error(var26);
            }
          }

        }

        return var10;
      }

      return new CachedValueProvider.Result<>(map, dependencies.toArray());
    }
  }

  private static Map<String, String> computeHandlers(Module module) {

    List<PsiFile> handlerFiles = InfraUtils.findConfigFilesInMetaInf(module, true, "spring.handlers", PsiFile.class);
    if (handlerFiles.isEmpty()) {
      return Collections.emptyMap();
    }
    else {
      Map<String, String> map = new HashMap<>();
      Iterator<PsiFile> var3 = handlerFiles.iterator();

      label154:
      while (var3.hasNext()) {
        PsiFile handlerFile = var3.next();
        VirtualFile handlersFile = handlerFile.getVirtualFile();

        assert handlersFile != null;

        InputStream inputStream = null;

        Map var8;
        try {
          inputStream = handlersFile.getInputStream();
          PropertyResourceBundle bundle = new PropertyResourceBundle(inputStream);
          Enumeration<String> keys = bundle.getKeys();

          while (true) {
            if (!keys.hasMoreElements()) {
              continue label154;
            }

            String key = keys.nextElement();
            map.put(key, (String) bundle.handleGetObject(key));
          }
        }
        catch (IOException var18) {
          LOG.error(var18);
          var8 = Collections.emptyMap();
        }
        finally {
          if (inputStream != null) {
            try {
              inputStream.close();
            }
            catch (IOException var17) {
              LOG.error(var17);
            }
          }

        }

        return var8;
      }

      return map;
    }
  }

  static {
    EMPTY_MAP_RESULT = new CachedValueProvider.Result<>(Collections.emptyMap(), PsiModificationTracker.MODIFICATION_COUNT);
    SKIP_NAMESPACE_URLS = Set.of(
            "http://www.w3.org/2001/XMLSchema-instance",
            "http://www.w3.org/XML/1998/namespace",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/1999/XMLSchema",
            "http://www.w3.org/2000/10/XMLSchema"
    );
  }
}

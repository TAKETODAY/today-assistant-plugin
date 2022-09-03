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

package cn.taketoday.assistant.factories;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializer;

import org.jetbrains.annotations.TestOnly;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.factories.resolvers.BeanReferenceFactoryBeanTypeResolver;
import cn.taketoday.assistant.factories.resolvers.FactoryPropertiesDependentTypeResolver;
import cn.taketoday.assistant.factories.resolvers.JndiObjectFactoryBeanTypeResolver;
import cn.taketoday.assistant.factories.resolvers.MethodInvokingFactoryBeanTypeResolver;
import cn.taketoday.assistant.factories.resolvers.MyBatisPlaceMapperTypeResolver;
import cn.taketoday.assistant.factories.resolvers.ProxyFactoryBeanTypeResolver;
import cn.taketoday.assistant.factories.resolvers.ScopedProxyFactoryBeanTypeResolver;
import cn.taketoday.assistant.factories.resolvers.SingleObjectTypeResolver;
import cn.taketoday.assistant.factories.resolvers.InfraEjbTypeResolver;
import cn.taketoday.assistant.factories.resolvers.TransactionProxyFactoryBeanTypeResolver;
import cn.taketoday.assistant.factories.resolvers.UtilConstantTypeResolver;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.PsiTypeUtil;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 18:04
 */
public class FactoryBeansManager {
  public static final String FACTORY_BEAN_SUFFIX = "FactoryBean";

  private static final Key<CachedValue<Set<PsiType>>> CACHED_OBJECT_TYPE = Key.create("CACHED_OBJECT_TYPE");
  private final Map<String, ObjectTypeResolver> factories = new HashMap<>();

  private final ObjectTypeResolver[] typeResolvers = {
          new TransactionProxyFactoryBeanTypeResolver(), new JndiObjectFactoryBeanTypeResolver(),
          new InfraEjbTypeResolver(), new ProxyFactoryBeanTypeResolver(),
          new ScopedProxyFactoryBeanTypeResolver(), new BeanReferenceFactoryBeanTypeResolver(),
          new UtilConstantTypeResolver(), new MethodInvokingFactoryBeanTypeResolver(), new MyBatisPlaceMapperTypeResolver()
  };

  private static final String FACTORIES_RESOURCE_XML = "/factories.xml";
  private static final String PROPERTY_NAME_DELIMITER = ",";
  private static final int BUNDLED_FACTORIES_VERSION = 0;

  public static FactoryBeansManager of() {
    return ApplicationManager.getApplication().getService(FactoryBeansManager.class);
  }

  public FactoryBeansManager() {
    URL resource = FactoryBeansManager.class.getResource(FACTORIES_RESOURCE_XML);
    assert resource != null;
    FactoriesBean factoriesBean = XmlSerializer.deserialize(resource, FactoriesBean.class);
    assert factoriesBean.getFactories() != null;
    for (FactoryBeanInfo factoryBeanInfo : factoriesBean.getFactories()) {
      String factory = factoryBeanInfo.getFactory();
      if (factory != null && factory.trim().length() > 0) {
        this.factories.put(factory, this.getObjectTypeResolver(factoryBeanInfo));
      }
    }
  }

  public static int getIndexingVersion() {
    return BUNDLED_FACTORIES_VERSION;
  }

  @Nullable
  private ObjectTypeResolver getObjectTypeResolver(FactoryBeanInfo factoryBeanInfo) {
    String type = factoryBeanInfo.getObjectType();
    if (!StringUtil.isEmptyOrSpaces(type)) {
      return new SingleObjectTypeResolver(type);
    }
    else {
      String delimitedNames = factoryBeanInfo.getPropertyNames();
      if (!StringUtil.isEmptyOrSpaces(delimitedNames)) {
        return new FactoryPropertiesDependentTypeResolver(StringUtil.split(delimitedNames, PROPERTY_NAME_DELIMITER));
      }
      else {
        String factoryClass = factoryBeanInfo.getFactory();
        for (ObjectTypeResolver customResolver : typeResolvers) {
          if (customResolver.accept(factoryClass)) {
            return customResolver;
          }
        }

        return null;
      }
    }
  }

  /**
   * FactoryBean can produce PsiArrayType, primitive types, etc.
   */
  public PsiType[] getObjectTypes(PsiType factoryType, @Nullable CommonInfraBean context) {

    Set<PsiType> types = new HashSet<>();
    PsiClass factoryClass = factoryType instanceof PsiClassType ? ((PsiClassType) factoryType).resolve() : null;
    Set<PsiType> objectTypes = factoryClass != null ? this.getObjectTypes(factoryClass, context) : Collections.emptySet();
    if (objectTypes.isEmpty()) {
      Set<PsiType> psiTypes = guessObjectTypes(factoryClass);
      if (!psiTypes.isEmpty()) {
        ContainerUtil.addAllNotNull(types, psiTypes);
      }
      else {
        PsiType typeParameter = PsiUtil.substituteTypeParameter(factoryType, InfraConstant.FACTORY_BEAN, 0, false);
        if (typeParameter != null && !"java.lang.Object".equals(typeParameter.getCanonicalText())) {
          types.add(typeParameter);
        }
      }
    }
    else {
      types.addAll(objectTypes);
    }

    return types.toArray(PsiType.EMPTY_ARRAY);
  }

  @Nullable
  private static PsiType createTypeByTypeName(@Nullable PsiClass factoryClass, @Nullable String typeName) {
    if (factoryClass != null && !StringUtil.isEmptyOrSpaces(typeName)) {
      PsiClass psiClass = JavaPsiFacade.getInstance(factoryClass.getProject()).findClass(typeName, factoryClass.getResolveScope());
      if (psiClass != null) {
        return PsiTypesUtil.getClassType(psiClass);
      }
      else {
        try {
          return JavaPsiFacade.getElementFactory(factoryClass.getProject()).createTypeFromText(typeName, factoryClass);
        }
        catch (IncorrectOperationException var4) {
          return null;
        }
      }
    }
    else {
      return null;
    }
  }

  public Set<PsiClass> getKnownBeanFactories(Project project) {

    Set<PsiClass> factories = new HashSet<>();
    ContainerUtil.addAllNotNull(factories, this.getFactoryBeansFromLibs(project));
    ContainerUtil.addAllNotNull(factories, this.getUserDefinedFactoryBeans(project));
    return Collections.unmodifiableSet(factories);
  }

  public Set<PsiClass> getFactoryBeansFromLibs(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project,
            () -> CachedValueProvider.Result.create(this.getFactoryBeanInheritors(project, ProjectScope.getLibrariesScope(project)),
                    ProjectRootManager.getInstance(project)));
  }

  public Set<PsiClass> getFactoryBeanInheritors(Project project, GlobalSearchScope scope) {

    PsiClass beanFactoryClass = JavaPsiFacade.getInstance(project)
            .findClass(InfraConstant.FACTORY_BEAN, ProjectScope.getLibrariesScope(project));
    Set<PsiClass> factories = new HashSet<>();
    if (beanFactoryClass != null) {
      factories.addAll(ClassInheritorsSearch.search(beanFactoryClass, scope, true, true, false).findAll());
    }

    return factories;
  }

  public Set<PsiClass> getUserDefinedFactoryBeans(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      Set<PsiClass> factories = this.getFactoryBeanInheritors(project, ProjectScope.getContentScope(project));
      return CachedValueProvider.Result.create(factories, PsiModificationTracker.MODIFICATION_COUNT, ProjectRootManager.getInstance(project));
    });
  }

  public boolean isKnownBeanFactory(@Nullable String factoryClass) {
    return this.factories.containsKey(factoryClass);
  }

  private Set<PsiType> getObjectTypes(PsiClass factoryClass, @Nullable CommonInfraBean context) {

    ObjectTypeResolver objectTypeResolver = this.getObjectTypeResolver(factoryClass);

    return objectTypeResolver != null ? objectTypeResolver.getObjectType(context) : Collections.emptySet();
  }

  @Nullable
  private ObjectTypeResolver getObjectTypeResolver(PsiClass factoryClass) {
    return CachedValuesManager.getCachedValue(
            factoryClass, () -> new CachedValueProvider.Result<>(this.doGetObjectTypeResolver(factoryClass), factoryClass));
  }

  @Nullable
  private ObjectTypeResolver doGetObjectTypeResolver(PsiClass factoryClass) {

    String qualifiedName = factoryClass.getQualifiedName();
    ObjectTypeResolver typeResolver = this.factories.get(qualifiedName);
    if (typeResolver != null) {
      return typeResolver;
    }
    else {
      GlobalSearchScope resolveScope = factoryClass.getResolveScope();
      JavaPsiFacade facade = JavaPsiFacade.getInstance(factoryClass.getProject());

      for (String factoryClassName : this.factories.keySet()) {
        PsiClass psiClass = facade.findClass(factoryClassName, resolveScope);
        if (psiClass != null && factoryClass.isInheritor(psiClass, false)) {
          ObjectTypeResolver resolver = this.factories.get(factoryClassName);
          if (resolver != null) {
            return resolver;
          }
        }
      }

      return null;
    }
  }

  private static Set<PsiType> guessObjectTypes(@Nullable PsiClass factoryClass) {
    return factoryClass == null ? Collections.emptySet() : CachedValuesManager.getManager(factoryClass.getProject()).getCachedValue(factoryClass, CACHED_OBJECT_TYPE, () -> {
      Set<PsiType> types = doGuessObjectType(factoryClass);
      return new CachedValueProvider.Result<>(types, getDependencies(factoryClass, types));
    }, false);
  }

  private static Object[] getDependencies(PsiClass factoryClass, Set<PsiType> types) {

    Set<Object> deps = new HashSet();
    deps.add(factoryClass);

    for (PsiType type : types) {
      if (!(type instanceof PsiClassType)) {
        return new Object[] { PsiModificationTracker.MODIFICATION_COUNT };
      }

      ContainerUtil.addIfNotNull(deps, ((PsiClassType) type).resolve());
    }

    return ArrayUtil.toObjectArray(deps);
  }

  @Nullable
  private static PsiMethod getProductTypeMethod(PsiClass factoryClass) {
    PsiMethod[] var1 = factoryClass.findMethodsByName("getObjectType", true);

    for (PsiMethod psiMethod : var1) {
      if (psiMethod.getParameterList().getParameters().length == 0) {
        return psiMethod;
      }
    }

    return null;
  }

  private static Set<PsiType> doGuessObjectType(PsiClass factoryClass) {
    PsiMethod method = getProductTypeMethod(factoryClass);
    if (method == null) {
      return Collections.emptySet();
    }
    else {
      PsiType psiType;
      if (method instanceof PsiCompiledElement) {
        VirtualFile file = method.getContainingFile().getVirtualFile();
        if (file != null) {
          FactoryBeanObjectTypeReader reader = new FactoryBeanObjectTypeReader();

          try {
            (new ClassReader(file.contentsToByteArray())).accept(reader, 2);
          }
          catch (ArrayIndexOutOfBoundsException | IOException ignored) {
          }

          String qName = reader.getResultQName();
          psiType = createTypeByTypeName(factoryClass, qName);
          return psiType != null ? Collections.singleton(psiType) : Collections.emptySet();

        }
      }

      PsiCodeBlock body = method.getBody();
      if (body != null) {
        PsiStatement[] statements = body.getStatements();
        if (statements.length == 1 && statements[0] instanceof PsiReturnStatement) {
          PsiExpression value = ((PsiReturnStatement) statements[0]).getReturnValue();
          if (value != null) {
            if (value instanceof PsiClassObjectAccessExpression) {
              return Collections.singleton(((PsiClassObjectAccessExpression) value).getOperand().getType());

            }

            psiType = value.getType();
            if (psiType instanceof PsiClassType) {
              PsiType classType = PsiTypeUtil.getInstance(factoryClass.getProject()).findType(Class.class);
              if (classType != null && psiType.isAssignableFrom(classType)) {
                return getClassTypes((PsiClassType) psiType);
              }
            }
            else if (psiType != null && !psiType.equals(PsiType.NULL)) {
              return Collections.singleton(psiType);
            }
          }
        }
      }

      PsiType factoryBeanGenericType = PsiUtil.substituteTypeParameter(PsiTypesUtil.getClassType(factoryClass), InfraConstant.FACTORY_BEAN, 0, false);
      if (factoryBeanGenericType != null) {
        return Collections.singleton(factoryBeanGenericType);
      }
      else {
        return Collections.emptySet();
      }
    }
  }

  private static Set<PsiType> getClassTypes(PsiClassType psiType) {
    List<PsiType> types = InfraUtils.resolveGenerics(psiType);
    if (types.size() == 1) {
      PsiType type = types.get(0);
      if (type instanceof PsiClassType) {
        PsiClass aClass = ((PsiClassType) type).resolve();
        if (aClass instanceof PsiTypeParameter) {
          Set<PsiType> classNames = new HashSet();
          Collections.addAll(classNames, aClass.getExtendsListTypes());
          return classNames;
        }

        return Collections.singleton(type);
      }
    }

    return Collections.emptySet();
  }

  @TestOnly
  public void registerFactory(String className, ObjectTypeResolver resolver) {
    this.factories.put(className, resolver);
  }

  @TestOnly
  public void unregisterFactory(String className) {
    this.factories.remove(className);
  }

  public boolean isFactoryBeanClass(PsiClass psiClass) {
    return CachedValuesManager.getCachedValue(psiClass, () -> {
      boolean isBeanFactory = InheritanceUtil.isInheritor(psiClass, InfraConstant.FACTORY_BEAN);
      return CachedValueProvider.Result.createSingleDependency(isBeanFactory, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public boolean isValidFactoryMethod(PsiMethod psiMethod, boolean isBeansXmlFactoryBean) {
    if (!psiMethod.isConstructor() && psiMethod.getReturnType() != null) {
      boolean isStatic = psiMethod.hasModifierProperty("static");
      return (isBeansXmlFactoryBean && !isStatic || !isBeansXmlFactoryBean && isStatic) && hasFactoryReturnType(psiMethod);
    }
    else {
      return false;
    }
  }

  public static boolean hasFactoryReturnType(PsiMethod psiMethod) {
    PsiType returnType = psiMethod.getReturnType();
    return returnType instanceof PsiPrimitiveType || returnType instanceof PsiClassType || returnType instanceof PsiArrayType;
  }

  private static class FactoryBeanObjectTypeReader extends ClassVisitor {
    private String myResultQName;

    FactoryBeanObjectTypeReader() {
      super(589824);
    }

    public String getResultQName() {
      return this.myResultQName;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      return !"getObjectType".equals(name) || signature != null && !signature.startsWith("()") ? super.visitMethod(access, name, desc, signature, exceptions) : new MethodVisitor(589824) {
        private String qname;
        private int number;

        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
          if ((this.number == 0 || this.number == 7) && opcode == 178 || this.number == 5 && opcode == 179) {
            ++this.number;
          }

        }

        public void visitJumpInsn(int opcode, Label label) {
          if (this.number == 1 && opcode == 199 || this.number == 6 && opcode == 167) {
            ++this.number;
          }

        }

        public void visitLdcInsn(Object cst) {
          if (this.number == 2 && cst instanceof String) {
            ++this.number;
            this.qname = (String) cst;
          }
          else if (this.number == 0 && cst instanceof Type) {
            ++this.number;
            this.qname = ((Type) cst).getClassName();
          }

        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
          if (this.number == 3 && opcode == 184 && "class$".equals(name)) {
            ++this.number;
          }
        }

        public void visitInsn(int opcode) {
          if (this.number == 4 && opcode == 89) {
            ++this.number;
          }

          if ((this.number == 8 || this.number == 1) && opcode == 176) {
            if (FactoryBeanObjectTypeReader.this.myResultQName == null) {
              FactoryBeanObjectTypeReader.this.myResultQName = this.qname;
            }

            ++this.number;
          }

        }
      };
    }
  }
}

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

package cn.taketoday.assistant.web.mvc.config.anno;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastCallKind;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import one.util.streamex.StreamEx;

public class CodeConfigurationPropertiesParser {
  private final PsiClass myClass;

  public CodeConfigurationPropertiesParser(PsiClass aClass) {
    this.myClass = aClass;
  }

  @Nullable
  public String getString(String methodName, boolean checkBases) {
    UExpression returnUEspression = ContainerUtil.getOnlyItem(getReturnedUExpression(methodName, checkBases));
    if (returnUEspression != null) {
      return UastUtils.evaluateString(returnUEspression);
    }
    PsiMethod compiledMethod = getCompiledMethod(methodName, checkBases);
    if (compiledMethod != null) {
      return getStringCompiled(compiledMethod);
    }
    return null;
  }

  @Nullable
  private static String getStringCompiled(PsiMethod psiMethod) {
    VirtualFile file = psiMethod.getContainingFile().getVirtualFile();
    if (file == null) {
      return null;
    }
    StringConstantVisitor stringConstantVisitor = new StringConstantVisitor(psiMethod);
    try {
      new ClassReader(file.contentsToByteArray()).accept(stringConstantVisitor, 2);
    }
    catch (IOException e) {
    }
    return stringConstantVisitor.getResult();
  }

  public String[] getStringArray(String methodName, boolean checkBases) {
    UExpression value = ContainerUtil.getOnlyItem(getReturnedUExpression(methodName, checkBases));
    if (value == null) {
      return ArrayUtilRt.EMPTY_STRING_ARRAY;
    }
    return StreamEx.of(getArrayInitializerExpression(value)).map(UastUtils::evaluateString).nonNull().toArray(ArrayUtilRt.EMPTY_STRING_ARRAY);
  }

  public List<PsiClass> getPsiClasses(String methodName, boolean checkBases) {
    UExpression value = ContainerUtil.getOnlyItem(getReturnedUExpression(methodName, checkBases));
    if (value == null) {
      return Collections.emptyList();
    }
    return StreamEx.of(getArrayInitializerExpression(value))
            .map(CodeConfigurationPropertiesParser::evaluateToPsiClass)
            .nonNull()
            .toList();
  }

  @Nullable
  private static PsiClass evaluateToPsiClass(UExpression expression) {
    if (expression instanceof UQualifiedReferenceExpression) {
      expression = ((UQualifiedReferenceExpression) expression).getReceiver();
    }
    if (expression instanceof UClassLiteralExpression) {
      return PsiTypesUtil.getPsiClass(((UClassLiteralExpression) expression).getType());
    }
    return null;
  }

  @Nullable
  private PsiMethod getCompiledMethod(String methodName, boolean checkBases) {
    for (PsiMethod psiMethod : this.myClass.findMethodsByName(methodName, checkBases)) {
      if (psiMethod instanceof PsiCompiledElement) {
        return psiMethod;
      }
    }
    return null;
  }

  private List<UExpression> getReturnedUExpression(String methodName, boolean checkBases) {
    PsiMethod[] methodsByName = this.myClass.findMethodsByName(methodName, checkBases);
    for (PsiMethod psiMethod : methodsByName) {
      List<UExpression> expressions = InfraUtils.getReturnedUExpression(psiMethod);
      if (!expressions.isEmpty()) {
        return expressions;
      }
    }
    return Collections.emptyList();
  }

  private static List<UExpression> getArrayInitializerExpression(UExpression expression) {
    if (expression instanceof UReferenceExpression) {
      UElement uElement = UastContextKt.toUElement(((UReferenceExpression) expression).resolve());
      if (uElement instanceof UField field) {
        expression = field.getUastInitializer();
      }
    }
    if ((expression instanceof UCallExpression) && (((UCallExpression) expression)
            .getKind().equals(UastCallKind.NEW_ARRAY_WITH_INITIALIZER)
            || "arrayOf".equals(((UCallExpression) expression).getMethodName()))) {
      return ((UCallExpression) expression).getValueArguments();
    }
    return Collections.emptyList();
  }

  private static final class StringConstantVisitor extends ClassVisitor {
    private final PsiMethod myPsiMethod;
    private String myResult;

    private StringConstantVisitor(PsiMethod psiMethod) {
      super(589824);
      this.myPsiMethod = psiMethod;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (this.myPsiMethod.getName().equals(name)) {
        return new MethodVisitor(589824) {
          public void visitLdcInsn(Object cst) {
            if (cst instanceof String) {
              StringConstantVisitor.this.myResult = (String) cst;
            }
          }
        };
      }
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private String getResult() {
      return this.myResult;
    }
  }
}

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

package cn.taketoday.assistant.web.mvc.providers;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.microservices.jvm.oas.FormatAndType;
import com.intellij.microservices.jvm.oas.JvmSwaggerUtilsKt;
import com.intellij.microservices.oas.OasEndpointPath;
import com.intellij.microservices.oas.OasHttpMethod;
import com.intellij.microservices.oas.OasOperation;
import com.intellij.microservices.oas.OasParameter;
import com.intellij.microservices.oas.OasParameterIn;
import com.intellij.microservices.oas.OasProperty;
import com.intellij.microservices.oas.OasRequestBody;
import com.intellij.microservices.oas.OasResponse;
import com.intellij.microservices.oas.OasSchema;
import com.intellij.microservices.oas.OasSchemaFormat;
import com.intellij.microservices.oas.OasSchemaType;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlQueryParameter;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UParameterEx;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.jam.CustomRequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.HttpStatus;
import cn.taketoday.assistant.web.mvc.model.jam.InfraRequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.MVCPathVariable;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.WebMVCCookieValue;
import cn.taketoday.assistant.web.mvc.model.jam.WebMVCMatrixVariable;
import cn.taketoday.assistant.web.mvc.model.jam.WebMVCRequestHeader;
import cn.taketoday.assistant.web.mvc.model.jam.WebMVCRequestParam;
import cn.taketoday.lang.Nullable;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.SequenceScope;
import kotlin.sequences.SequencesKt;
import kotlin.text.StringsKt;

public final class WebMvcOasConverters {
  private static final String WILDCARD_CONTENT_TYPE = "*/*";
  private static final String MULTIPART_FILE = "cn.taketoday.web.multipart.MultipartFile";
  private static final OasResponse OK_RESPONSE_STATUS = new OasResponse("200", "OK", null);
  private static final OasResponse INTERNAL_SERVER_ERROR_STATUS = new OasResponse("500", "Internal Server Error", null);
  private static final Set<String> HTTP_METHOD_NAMES;

  static {
    OasHttpMethod[] values = OasHttpMethod.values();
    Collection collection = new ArrayList(values.length);
    for (OasHttpMethod oasHttpMethod : values) {
      collection.add(oasHttpMethod.getMethodName());
    }
    HTTP_METHOD_NAMES = CollectionsKt.toSet(collection);
  }

  @Nullable
  public static Iterable<OasEndpointPath> getMvcHandlerOasModel(UrlMappingElement endpoint, @Nullable UrlTargetInfo info) {
    UMethod uMethod;
    if (info == null || (uMethod = UastContextKt.toUElement(endpoint.getNavigationTarget(), UMethod.class)) == null) {
      return null;
    }
    List controllerHeaders = buildOasControllerHeaderParams(uMethod);
    return SequencesKt.asIterable(SequencesKt.sequence(new Function2<SequenceScope<? super OasEndpointPath>, Continuation<? super Unit>, OasEndpointPath>() {
      @Override
      public OasEndpointPath invoke(SequenceScope<? super OasEndpointPath> sequenceScope, Continuation<? super Unit> continuation) {
        OasEndpointPath.Builder pathBuilder = new OasEndpointPath.Builder(info.getPath());
        Set<String> it = info.getMethods();
        Set<String> urlTargetMethods = !it.isEmpty() ? it : null;
        if (urlTargetMethods == null) {
          urlTargetMethods = HTTP_METHOD_NAMES;
        }
        ArrayList<OasOperation> builders = new ArrayList<>(Math.max(urlTargetMethods.size(), 10));
        for (String method : urlTargetMethods) {
          builders.add(new OasOperation.Builder(method)
                  .build(builder -> {
                    OasRequestBody requestBody;
                    OasResponse mappingResponse;
                    List buildOasPathParams;
                    List buildOasQueryParams;
                    List buildOasCookieParams;
                    List buildOasHeaderParams;
                    builder.setOperationId(uMethod.getName());
                    builder.setDeprecated(info.isDeprecated());
                    Locale locale = Locale.ENGLISH;
                    String upperCase = method.toUpperCase(locale);
                    builder.setSummary(upperCase + " " + info.getPath().getPresentation(UrlPath.FULL_PATH_VARIABLE_PRESENTATION));
                    requestBody = getRequestBody(uMethod);
                    builder.setRequestBody(requestBody);
                    mappingResponse = getMappingResponse(uMethod);
                    builder.setResponses(CollectionsKt.listOf(mappingResponse));
                    List smartList = new SmartList();
                    buildOasPathParams = buildOasPathParams(info, uMethod);
                    addParameters(smartList, buildOasPathParams);
                    buildOasQueryParams = buildOasQueryParams(info, uMethod);
                    addParameters(smartList, buildOasQueryParams);
                    buildOasCookieParams = buildOasCookieParams(uMethod);
                    addParameters(smartList, buildOasCookieParams);
                    buildOasHeaderParams = buildOasHeaderParams(uMethod);
                    addParameters(smartList, buildOasHeaderParams);
                    addParameters(smartList, controllerHeaders);
                    builder.setParameters(smartList);
                    return Unit.INSTANCE;
                  }));
        }
        pathBuilder.setOperations(builders);
        return pathBuilder.build(null);
      }
    }));
  }

  public static void addParameters(List<OasParameter> list, List<OasParameter.Builder> list2) {
    for (OasParameter.Builder builder : list2) {
      list.add(builder.build(null));
    }
  }

  public static List<OasParameter.Builder> buildOasPathParams(UrlTargetInfo urlTargetInfo, UMethod uMethod) {
    List<UParameter> parameters = uMethod.getUastParameters();
    boolean hasMatrixVariables = false;
    if (!parameters.isEmpty()) {
      for (UParameter uParameter : parameters) {
        if (uParameter.findAnnotation(InfraMvcConstant.MATRIX_VARIABLE) != null) {
          hasMatrixVariables = true;
          break;
        }
      }
    }

    if (hasMatrixVariables) {
      return CollectionsKt.emptyList();
    }
    else {
      SmartList pathParams = new SmartList();
      Function1 pathVariableProcessor = (new Function1() {
        public Object invoke(Object var1) {
          this.invoke((UrlPath.PathSegment.Variable) var1);
          return Unit.INSTANCE;
        }

        public void invoke(UrlPath.PathSegment.Variable variable) {
          String pathParamName = variable.getVariableName();
          if (pathParamName != null) {
            OasParameter.Builder pathParam = new OasParameter.Builder(pathParamName, OasParameterIn.PATH);

            for (UParameter parameter : uMethod.getUastParameters()) {
              UParameter var8 = parameter;
              if (!(parameter instanceof UParameterEx)) {
                var8 = null;
              }

              UParameterEx paramEx = (UParameterEx) var8;
              if (paramEx != null) {
                MVCPathVariable pathVariable = MVCPathVariable.META.getJamElement(paramEx.getJavaPsi());
                if (pathVariable == null && Intrinsics.areEqual(pathParamName, parameter.getName()) || pathVariable != null
                        && Intrinsics.areEqual(pathParamName, pathVariable.getName())) {
                  pathParam.setRequired(pathVariable == null || pathVariable.isRequired());
                  setParameterSchema(parameter, pathParam);
                  break;
                }
              }
            }

            pathParams.add(pathParam);
          }
        }

      });
      processUrlPathSegments(urlTargetInfo.getPath().getSegments(), pathVariableProcessor);
      return pathParams.isEmpty() ? CollectionsKt.emptyList() : (List) pathParams;
    }
  }

  private static void processUrlPathSegments(List segments, Function1 pathVariableProcessor) {

    for (Object o : segments) {
      UrlPath.PathSegment segment = (UrlPath.PathSegment) o;
      if (segment instanceof UrlPath.PathSegment.Variable) {
        pathVariableProcessor.invoke(segment);
      }
      else if (segment instanceof UrlPath.PathSegment.Composite) {
        processUrlPathSegments(((UrlPath.PathSegment.Composite) segment).getSegments(), pathVariableProcessor);
      }
    }
  }

  public static List<OasParameter.Builder> buildOasQueryParams(UrlTargetInfo urlTargetInfo, UMethod uMethod) {
    WebMVCCookieValue matrixVariable;
    List<OasParameter.Builder> smartList = new SmartList<>();
    for (UrlQueryParameter param : urlTargetInfo.getQueryParameters()) {
      OasParameter.Builder queryParam = new OasParameter.Builder(param.getName(), OasParameterIn.QUERY);
      for (UParameter parameter : uMethod.getUastParameters()) {

        if (parameter instanceof UParameterEx paramEx) {
          WebMVCRequestParam mvcRequestParam;
          if ((mvcRequestParam = WebMVCRequestParam.META.getJamElement(paramEx.getJavaPsi())) != null) {

            String parameterName = mvcRequestParam.getName();
            if ((parameterName != null && Intrinsics.areEqual(parameterName, param.getName()))
                    || (parameterName == null && Intrinsics.areEqual(parameter.getName(), param.getName()))) {
              queryParam.setRequired(mvcRequestParam.isRequired());
              setParameterSchema(parameter, queryParam);
              break;
            }
          }
        }
      }
      smartList.add(queryParam);
    }
    for (UParameter uParameter : uMethod.getUastParameters()) {
      PsiElement javaPsi = uParameter.getJavaPsi();
      if (!(javaPsi instanceof PsiParameter)) {
        javaPsi = null;
      }
      PsiParameter psiModifierListOwner = (PsiParameter) javaPsi;
      if (psiModifierListOwner != null && (matrixVariable = WebMVCMatrixVariable.META.getJamElement(psiModifierListOwner)) != null) {
        String name = matrixVariable.getName();
        if (name == null) {
          name = uParameter.getName();
        }
        OasParameter.Builder queryParam2 = new OasParameter.Builder(name, OasParameterIn.QUERY);
        queryParam2.setRequired(matrixVariable.isRequired());
        setParameterSchema(uParameter, queryParam2);
        smartList.add(queryParam2);
      }
    }
    return smartList.isEmpty() ? CollectionsKt.emptyList() : smartList;
  }

  public static List<OasParameter.Builder> buildOasCookieParams(UMethod uMethod) {
    WebMVCCookieValue mvcCookieValue;
    List<OasParameter.Builder> smartList = new SmartList<>();
    for (UParameter parameter : uMethod.getUastParameters()) {

      if (parameter instanceof UParameterEx paramEx) {
        if ((mvcCookieValue = WebMVCCookieValue.META.getJamElement(paramEx.getJavaPsi())) != null) {
          String name = mvcCookieValue.getName();
          if (name == null) {
            name = paramEx.getName();
          }
          String parameterName = name;
          if (parameterName.length() != 0) {
            OasParameter.Builder cookieParam = new OasParameter.Builder(parameterName, OasParameterIn.COOKIE);
            cookieParam.setRequired(mvcCookieValue.isRequired());
            setParameterSchema(paramEx, cookieParam);
            smartList.add(cookieParam);
          }
        }
      }

    }
    return smartList.isEmpty() ? CollectionsKt.emptyList() : smartList;
  }

  public static List<OasParameter.Builder> buildOasHeaderParams(UMethod uMethod) {
    WebMVCRequestHeader mvcRequestHeader;
    List<OasParameter.Builder> smartList = new SmartList<>();
    for (UParameter parameter : uMethod.getUastParameters()) {
      if (parameter instanceof UParameterEx paramEx) {
        if ((mvcRequestHeader = WebMVCRequestHeader.META.getJamElement(paramEx.getJavaPsi())) != null) {
          String name = mvcRequestHeader.getName();
          if (name == null) {
            name = paramEx.getName();
          }
          String parameterName = name;
          if (parameterName.length() != 0) {
            OasParameter.Builder headerParam = new OasParameter.Builder(parameterName, OasParameterIn.HEADER);
            headerParam.setRequired(mvcRequestHeader.isRequired());
            setParameterSchema(paramEx, headerParam);
            smartList.add(headerParam);
          }
        }

      }
    }
    for (WebRequestHeader webRequestHeader : getRequestMappingHeaders(uMethod)) {
      String headerName = webRequestHeader.component1();
      List headerValues = webRequestHeader.component2();
      OasParameter.Builder headerParam2 = new OasParameter.Builder(headerName, OasParameterIn.HEADER);
      headerParam2.setRequired(false);
      headerParam2.setSchema(
              new OasSchema(OasSchemaType.STRING, null, null, null, headerValues, null, null));
      smartList.add(headerParam2);
    }
    return smartList.isEmpty() ? CollectionsKt.emptyList() : smartList;
  }

  private static List<WebRequestHeader> getRequestMappingHeaders(UMethod uMethod) {
    List<String> headers = null;
    InfraRequestMapping.MethodMapping methodMapping = InfraRequestMapping.MethodMapping.META.getJamElement(uMethod.getJavaPsi());
    if (methodMapping != null) {
      headers = methodMapping.getHeaders();
    }

    if (headers == null) {
      CustomRequestMapping.MethodMapping element = CustomRequestMapping.MethodMapping.META.getJamElement(uMethod.getJavaPsi());
      if (element != null) {
        headers = element.getHeaders();
      }
    }

    if (headers == null) {
      headers = CollectionsKt.emptyList();
    }

    return parseHeaders(headers);
  }

  private static List<WebRequestHeader> parseHeaders(List<String> list) {
    ArrayList<WebRequestHeader> ret = new ArrayList<>(Math.max(list.size(), 10));
    for (String headerString : list) {
      List<String> split$default = StringsKt.split(headerString, new char[] { '=' }, false, 0);
      String header = split$default.get(0);
      String values = split$default.get(1);
      String obj = StringsKt.trim(header).toString();
      List<String> split = StringsKt.split(values, new char[] { ',' }, false, 0);
      ArrayList<String> arrayList = new ArrayList<>(Math.max(split.size(), 10));
      for (String it : split) {
        arrayList.add(StringsKt.trim(it).toString());
      }
      ret.add(new WebRequestHeader(obj, arrayList));
    }
    return ret;
  }

  private static List<OasParameter.Builder> buildOasControllerHeaderParams(UMethod uMethod) {
    UElement $this$getParentOfType$iv = uMethod;
    boolean strict$iv = true;
    UClass var10000 = UastUtils.getParentOfType($this$getParentOfType$iv, UClass.class, strict$iv);
    if (var10000 != null) {
      PsiClass psiClass = var10000.getJavaPsi();
      if (psiClass != null) {
        List<String> controllerHeaders;
        label30:
        {
          InfraRequestMapping.ClassMapping mapping = InfraRequestMapping.ClassMapping.META.getJamElement(psiClass);
          if (mapping != null) {
            controllerHeaders = mapping.getHeaders();
            if (controllerHeaders != null) {
              break label30;
            }
          }

          controllerHeaders = CollectionsKt.emptyList();
        }

        SmartList<OasParameter.Builder> headerParams = new SmartList<>();

        for (WebRequestHeader header : parseHeaders(controllerHeaders)) {
          String headerName = header.component1();
          List headerValues = header.component2();
          OasParameter.Builder headerParam = new OasParameter.Builder(headerName, OasParameterIn.HEADER);
          headerParam.setRequired(false);
          headerParam.setSchema(
                  new OasSchema(OasSchemaType.STRING, null, null, null, headerValues, null, null));
          headerParams.add(headerParam);
        }

        return headerParams.isEmpty() ? CollectionsKt.emptyList() : headerParams;
      }
    }

    return CollectionsKt.emptyList();
  }

  public static void setParameterSchema(UParameter parameter, OasParameter.Builder builder) {
    FormatAndType it = provideParameterFormatAndType(parameter.getType());
    if (it != null) {
      builder.setSchema(new OasSchema(it.getType(), it.getFormat(), getSchemaDefaultValue(parameter), null, null, null, null));
    }
  }

  private static String getSchemaDefaultValue(UParameter parameter) {
    PsiElement javaPsi = parameter.getJavaPsi();
    if (!(javaPsi instanceof PsiParameter)) {
      javaPsi = null;
    }
    PsiParameter psiParameter = (PsiParameter) javaPsi;
    if (psiParameter != null) {
      if (AnnotationUtil.isAnnotated(psiParameter, InfraMvcConstant.COOKIE_VALUE, 0)) {
        WebMVCCookieValue jamElement = WebMVCCookieValue.META.getJamElement(psiParameter);
        if (jamElement == null) {
          return null;
        }
        return jamElement.getDefaultValue();
      }
      else if (AnnotationUtil.isAnnotated(psiParameter, InfraMvcConstant.REQUEST_HEADER, 0)) {
        WebMVCRequestHeader jamElement2 = WebMVCRequestHeader.META.getJamElement(psiParameter);
        if (jamElement2 == null) {
          return null;
        }
        return jamElement2.getDefaultValue();
      }
      else if (AnnotationUtil.isAnnotated(psiParameter, InfraMvcConstant.REQUEST_PARAM, 0)) {
        WebMVCRequestParam jamElement3 = WebMVCRequestParam.META.getJamElement(psiParameter);
        if (jamElement3 == null) {
          return null;
        }
        return jamElement3.getDefaultValue();
      }
      else {
        return null;
      }
    }
    return null;
  }

  public static OasResponse getMappingResponse(UMethod uMethod) {
    List<String> responseContentTypes;
    label47:
    {
      CustomRequestMapping.MethodMapping var10000 = CustomRequestMapping.MethodMapping.META.getJamElement(uMethod.getJavaPsi());
      if (var10000 != null) {
        responseContentTypes = var10000.getProduces();
        if (responseContentTypes != null) {
          responseContentTypes = !responseContentTypes.isEmpty() ? responseContentTypes : null;
          if (responseContentTypes != null) {
            break label47;
          }
        }
      }

      responseContentTypes = CollectionsKt.listOf("*/*");
    }

    OasSchema responseSchema;
    label40:
    {
      PsiType var25 = uMethod.getReturnType();
      if (var25 != null) {
        FormatAndType format = provideParameterFormatAndType(var25);
        if (format != null) {
          responseSchema = new OasSchema(format.getType(), format.getFormat(), null, null, null, null, null);
          break label40;
        }
      }

      responseSchema = null;
    }

    Map responseContent;
    if (responseSchema != null) {
      LinkedHashMap result$iv = new LinkedHashMap(responseContentTypes.size());
      for (String it : responseContentTypes) {
        result$iv.put(it, responseSchema);
      }
      responseContent = result$iv;
    }
    else {
      responseContent = null;
    }

    return getResponseStatus(uMethod).copy(null, null, responseContent);
  }

  private static OasResponse getResponseStatus(UMethod uMethod) {
    UAnnotation responseStatusAnno;
    Iterator<UAnnotation> it = uMethod.getUAnnotations().iterator();
    while (true) {
      if (!it.hasNext()) {
        responseStatusAnno = null;
        break;
      }
      UAnnotation next = it.next();
      if (Intrinsics.areEqual(next.getQualifiedName(), InfraMvcConstant.RESPONSE_STATUS)) {
        responseStatusAnno = next;
        break;
      }
    }
    if (responseStatusAnno == null) {
      return OK_RESPONSE_STATUS;
    }
    UExpression findDeclaredAttributeValue = responseStatusAnno.findDeclaredAttributeValue(RequestMapping.VALUE_ATTRIBUTE);
    if (findDeclaredAttributeValue == null) {
      findDeclaredAttributeValue = responseStatusAnno.findDeclaredAttributeValue("code");
    }
    if (findDeclaredAttributeValue != null) {
      UExpression uExpression = findDeclaredAttributeValue;
      if (!(uExpression instanceof UReferenceExpression)) {
        uExpression = null;
      }
      UReferenceExpression uReferenceExpression = (UReferenceExpression) uExpression;
      PsiElement resolve = uReferenceExpression != null ? uReferenceExpression.resolve() : null;
      if (!(resolve instanceof PsiEnumConstant)) {
        resolve = null;
      }
      PsiEnumConstant responseEnumConst = (PsiEnumConstant) resolve;
      if (responseEnumConst == null) {
        return INTERNAL_SERVER_ERROR_STATUS;
      }
      String name = responseEnumConst.getName();
      Intrinsics.checkNotNullExpressionValue(name, "responseEnumConst.name");
      HttpStatus httpStatus = HttpStatus.valueOf(name);
      return new OasResponse(String.valueOf(httpStatus.value()), httpStatus.getReasonPhrase(), null);
    }
    return INTERNAL_SERVER_ERROR_STATUS;
  }

  public static OasRequestBody getRequestBody(UMethod uMethod) {
    List<UParameter> parameters = uMethod.getUastParameters();

    UParameter var28 = null;

    outLoop:
    {
      for (UParameter parameter : parameters) {
        List<UAnnotation> annotations = parameter.getUAnnotations();
        if (!annotations.isEmpty()) {
          for (UAnnotation annotation : annotations) {
            if (Intrinsics.areEqual(annotation.getQualifiedName(), InfraMvcConstant.REQUEST_BODY)) {
              var28 = parameter;
              break outLoop;
            }
          }
        }
      }
    }

    if (!(var28 instanceof UParameterEx requestBodyParam)) {
      return getMultipartRequestBody(uMethod);
    }

    boolean requestBodyRequired;
    List<String> contentTypes;
    label79:
    {
      CustomRequestMapping.MethodMapping consumes = CustomRequestMapping.MethodMapping.META.getJamElement(uMethod.getJavaPsi());
      if (consumes != null) {
        contentTypes = consumes.getConsumes();
        if (contentTypes != null) {
          requestBodyRequired = false;
          contentTypes = !contentTypes.isEmpty() ? contentTypes : null;
          if (contentTypes != null) {
            break label79;
          }
        }
      }

      contentTypes = CollectionsKt.listOf("application/json");
    }

    OasSchema oasSchema = getRequestBodyParamSchema(requestBodyParam);
    if (oasSchema == null) {
      return null;
    }
    else {
      LinkedHashMap content = new LinkedHashMap();
      for (String contentType : contentTypes) {
        content.put(contentType, oasSchema);
      }

      label66:
      {
        UAnnotation requestBodyParamAnnotation = requestBodyParam.findAnnotation(InfraMvcConstant.REQUEST_BODY);
        if (requestBodyParamAnnotation != null) {
          UExpression required = requestBodyParamAnnotation.findDeclaredAttributeValue("required");

          if (required != null) {
            if (required.evaluate() instanceof Boolean isRequired && isRequired) {
              requestBodyRequired = true;
              break label66;
            }
          }
        }

        requestBodyRequired = false;
      }

      return new OasRequestBody(content, requestBodyRequired);
    }

  }

  private static OasRequestBody getMultipartRequestBody(UMethod uMethod) {
    List listOf;
    List it;
    UAnnotation reqPartUAnno;
    String name;
    List requiredParams = new ArrayList();
    List properties = new ArrayList();
    for (UParameter uParam : uMethod.getUastParameters()) {
      UParameter uParameter = uParam;
      if (!(uParameter instanceof UParameterEx)) {
        uParameter = null;
      }
      UParameter uParameter2 = uParameter;
      if (uParameter2 != null && (reqPartUAnno = uParameter2.findAnnotation(InfraMvcConstant.REQUEST_PART)) != null) {
        UAnnotation uAnno = uParameter2.findAnnotation(InfraMvcConstant.REQUEST_PART);
        if (uAnno != null) {
          UExpression findDeclaredAttributeValue = uAnno.findDeclaredAttributeValue(RequestMapping.VALUE_ATTRIBUTE);
          if (findDeclaredAttributeValue == null) {
            findDeclaredAttributeValue = uAnno.findDeclaredAttributeValue("name");
          }
          name = findDeclaredAttributeValue != null ? UastUtils.evaluateString(findDeclaredAttributeValue) : null;
        }
        else
          name = uParameter2.getName();
        Intrinsics.checkNotNullExpressionValue(name, "uParamEx.name");
        String paramName = name;
        UExpression findDeclaredAttributeValue2 = reqPartUAnno.findDeclaredAttributeValue("required");
        Object evaluate = findDeclaredAttributeValue2 != null ? findDeclaredAttributeValue2.evaluate() : null;
        if (!(evaluate instanceof Boolean)) {
          evaluate = null;
        }
        Boolean bool = (Boolean) evaluate;
        boolean required = bool == null || bool.booleanValue();
        if (required) {
          requiredParams.add(paramName);
        }
        OasSchema paramSchema = getRequestBodyParamSchema(uParameter2);
        if (paramSchema != null) {
          properties.add(new OasProperty(paramName, paramSchema));
        }
      }
    }
    if (properties.isEmpty()) {
      return null;
    }
    CustomRequestMapping.MethodMapping methodMapping = CustomRequestMapping.MethodMapping.META.getJamElement(uMethod.getJavaPsi());
    if (methodMapping != null && (it = methodMapping.getConsumes()) != null) {
      listOf = !it.isEmpty() ? it : null;
    }
    else
      listOf = CollectionsKt.listOf("application/json");
    List contentTypes = listOf;
    OasSchema oasSchema = new OasSchema(OasSchemaType.OBJECT, null, null, properties, null, requiredParams, null);
    List $this$associateWith$iv = contentTypes;
    Map result$iv = new LinkedHashMap($this$associateWith$iv.size());
    for (Object element$iv$iv : $this$associateWith$iv) {
      result$iv.put(element$iv$iv, oasSchema);
    }
    Map content = result$iv;
    return new OasRequestBody(content, false);
  }

  private static OasSchema getRequestBodyParamSchema(UParameter uParameter) {
    PsiType psiType;
    PsiType type = uParameter.getType();
    FormatAndType it = provideParameterFormatAndType(type);
    if (it != null) {
      OasSchema oasSchema = new OasSchema(it.getType(), it.getFormat(), null, null, null, null, null);
      if (oasSchema.getType() != OasSchemaType.ARRAY) {
        return oasSchema;
      }
      if (type instanceof PsiArrayType arrayType) {
        psiType = arrayType.getComponentType();
      }
      else if (!(type instanceof PsiClassReferenceType)) {
        return null;
      }
      else {
        PsiType[] parameters = ((PsiClassReferenceType) type).getParameters();
        Intrinsics.checkNotNullExpressionValue(parameters, "paramType.parameters");
        psiType = ArraysKt.first(parameters);
      }
      PsiType genericParameterType = psiType;
      Intrinsics.checkNotNullExpressionValue(genericParameterType, "genericParameterType");
      FormatAndType it2 = provideParameterFormatAndType(genericParameterType);
      if (it2 == null) {
        return null;
      }
      OasSchema itemsSchema = new OasSchema(it2.getType(), it2.getFormat(), null, null, null, null, null);
      return oasSchema.copy(null, null, null, null, null, null, itemsSchema);
    }
    return null;
  }

  private static FormatAndType provideParameterFormatAndType(PsiType psiType) {
    return psiType.equalsToText(MULTIPART_FILE) ? new FormatAndType(OasSchemaType.STRING, OasSchemaFormat.BINARY) : JvmSwaggerUtilsKt.getOasParameterFormatAndType(psiType);
  }
}

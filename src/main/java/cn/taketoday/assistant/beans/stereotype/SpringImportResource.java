package cn.taketoday.assistant.beans.stereotype;

import com.intellij.jam.JamCommonModelElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberArchetype;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;
import com.intellij.spring.constants.SpringAnnotationsConstants;
import com.intellij.spring.model.jam.stereotype.ImportResource;
import com.intellij.spring.model.jam.testContexts.converters.ApplicationContextReferenceConverter;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

public class SpringImportResource extends JamCommonModelElement<PsiClass> implements ImportResource {
  private static final SemKey<SpringImportResource> JAM_KEY = IMPORT_RESOURCE_JAM_KEY.subKey("SpringImportResource", new SemKey[0]);
  public static final JamClassMeta<SpringImportResource> META = new JamClassMeta<>((JamMemberArchetype) null, SpringImportResource.class, JAM_KEY);
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(SpringAnnotationsConstants.CONTEXT_IMPORT_RESOURCE);
  private static final JamStringAttributeMeta.Collection<List<XmlFile>> VALUE_ATTR_META = new JamStringAttributeMeta.Collection<>("value", new ApplicationContextReferenceConverter());
  private static final JamStringAttributeMeta.Collection<List<XmlFile>> LOCATION_ATTR_META = new JamStringAttributeMeta.Collection<>("locations", new ApplicationContextReferenceConverter());

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(LOCATION_ATTR_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
  }

  public SpringImportResource(PsiClass psiElement) {
    super(PsiElementRef.real(psiElement));
  }

  @Override
  public List<XmlFile> getImportedResources(Module... contexts) {
    SmartList smartList = new SmartList();
    Processor<Pair<List<XmlFile>, ? extends PsiElement>> collect = pair -> {
      smartList.addAll(pair.first);
      return true;
    };
    processImportedResources(collect, contexts);
    return smartList;
  }

  public boolean processImportedResources(Processor<Pair<List<XmlFile>, ? extends PsiElement>> processor, Module... contexts) {
    PsiAnnotation annotation = ANNO_META.getAnnotation(getPsiElement());
    if (annotation != null) {
      return addFiles(processor, getValueAttrElements(), annotation, contexts) && addFiles(processor, getLocationsAttrElements(), annotation, contexts);
    }
    return true;
  }

  protected List<JamStringAttributeElement<List<XmlFile>>> getValueAttrElements() {
    List<JamStringAttributeElement<List<XmlFile>>> list = (List) ANNO_META.getAttribute(getPsiElement(), VALUE_ATTR_META);
    return list;
  }

  protected List<JamStringAttributeElement<List<XmlFile>>> getLocationsAttrElements() {
    List<JamStringAttributeElement<List<XmlFile>>> list = (List) ANNO_META.getAttribute(getPsiElement(), LOCATION_ATTR_META);
    return list;
  }

  public List<JamStringAttributeElement<List<XmlFile>>> getLocationElements() {
    List<JamStringAttributeElement<List<XmlFile>>> concat = ContainerUtil.concat(getValueAttrElements(), getLocationsAttrElements());
    return concat;
  }

  protected boolean addFiles(Processor<Pair<List<XmlFile>, ? extends PsiElement>> processor, List<JamStringAttributeElement<List<XmlFile>>> valueAttributeElements,
          PsiElement annotationElement, Module[] contexts) {
    boolean useAnnotationAsElement = valueAttributeElements.size() == 1;
    for (JamStringAttributeElement<List<XmlFile>> element : valueAttributeElements) {
      List<XmlFile> value = (List) element.getValue();
      if (!(getPsiElement() instanceof PsiCompiledElement)) {
        if (value == null) {
          continue;
        }
        else {
          if (!processor.process(Pair.create(value, useAnnotationAsElement ? annotationElement : element.getPsiElement()))) {
            return false;
          }
        }
      }
      else {
        String stringValue = element.getStringValue();
        if (StringUtil.isNotEmpty(stringValue)) {
          List<XmlFile> xmlContexts = ApplicationContextReferenceConverter.getApplicationContexts(stringValue, getPsiElement(), contexts);
          if (!processor.process(Pair.create(xmlContexts, useAnnotationAsElement ? annotationElement : element.getPsiElement()))) {
            return false;
          }
        }
        else {
          continue;
        }
      }
    }
    return true;
  }
}

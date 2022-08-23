package cn.taketoday.assistant.beans.stereotype;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import com.intellij.spring.constants.SpringAnnotationsConstants;
import com.intellij.spring.constants.SpringCorePresentationConstants;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringMetaStereotypeComponent;
import com.intellij.util.Function;

import java.util.Collection;

@Presentation(typeName = SpringCorePresentationConstants.SERVICE)
public class Service extends SpringMetaStereotypeComponent {
  public static final SemKey<JamMemberMeta<PsiClass, Service>> META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("ServiceMeta");
  public static final SemKey<Service> JAM_KEY = JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("Service");
  public static final JamClassMeta<Service> META = new JamClassMeta<>(null, Service.class, JAM_KEY);

  private static final Function<Module, Collection<String>> ANNOTATIONS = module -> {
    return getAnnotations(module, SpringAnnotationsConstants.SERVICE);
  };

  public Service(PsiClass psiClass) {
    this(SpringAnnotationsConstants.SERVICE, psiClass);
  }

  public Service(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public static Function<Module, Collection<String>> getServiceAnnotations() {
    return ANNOTATIONS;
  }
}

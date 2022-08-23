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

@Presentation(typeName = SpringCorePresentationConstants.CONTROLLER)
public class Controller extends SpringMetaStereotypeComponent {
  public static final SemKey<JamMemberMeta<PsiClass, Controller>> META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("InfraControllerMeta");
  public static final SemKey<Controller> JAM_KEY = JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("InfraController");
  public static final JamClassMeta<Controller> META = new JamClassMeta<>(null, Controller.class, JAM_KEY);

  private static final Function<Module, Collection<String>> ANNOTATIONS = module -> {
    return getAnnotations(module, SpringAnnotationsConstants.CONTROLLER);
  };

  public Controller(PsiClass psiClass) {
    this(SpringAnnotationsConstants.CONTROLLER, psiClass);
  }

  public Controller(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public static Function<Module, Collection<String>> getControllerAnnotations() {
    return ANNOTATIONS;
  }

}

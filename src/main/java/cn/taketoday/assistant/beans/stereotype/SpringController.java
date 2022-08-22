package cn.taketoday.assistant.beans.stereotype;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberArchetype;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import com.intellij.spring.constants.SpringAnnotationsConstants;
import com.intellij.spring.constants.SpringCorePresentationConstants;
import com.intellij.spring.el.lexer._SpringELLexer;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringMetaStereotypeComponent;
import com.intellij.util.Function;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

@Presentation(typeName = SpringCorePresentationConstants.CONTROLLER)
public class SpringController extends SpringMetaStereotypeComponent {
  public static final SemKey<JamMemberMeta<PsiClass, SpringController>> META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("SpringControllerMeta", new SemKey[0]);
  public static final SemKey<SpringController> JAM_KEY = JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("SpringController", new SemKey[0]);
  public static final JamClassMeta<SpringController> META = new JamClassMeta<>(null, SpringController.class, JAM_KEY);
  private static final Function<Module, Collection<String>> ANNOTATIONS = module -> {
    return getAnnotations(module, SpringAnnotationsConstants.CONTROLLER);
  };

  public SpringController(PsiClass psiClass) {
    this(SpringAnnotationsConstants.CONTROLLER, psiClass);
  }

  public SpringController(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public static Function<Module, Collection<String>> getControllerAnnotations() {
    return ANNOTATIONS;
  }
}

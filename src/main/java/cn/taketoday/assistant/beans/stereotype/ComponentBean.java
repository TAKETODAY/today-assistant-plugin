package cn.taketoday.assistant.beans.stereotype;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.spring.constants.SpringAnnotationsConstants;
import com.intellij.spring.constants.SpringCorePresentationConstants;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;

import org.jetbrains.annotations.NotNull;

@Presentation(typeName = SpringCorePresentationConstants.COMPONENT)
public class ComponentBean extends SpringStereotypeElement {

  public static final JamClassMeta<ComponentBean> META = new JamClassMeta<>(null, ComponentBean.class,
          JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("ComponentBean"));

  static {
    addPomTargetProducer(META);
  }

  public ComponentBean(@NotNull PsiClass psiClass) {
    super(SpringAnnotationsConstants.COMPONENT, PsiElementRef.real(psiClass));
  }
}
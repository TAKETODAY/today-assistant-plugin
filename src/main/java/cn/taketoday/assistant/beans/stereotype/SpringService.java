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

@Presentation(typeName = SpringCorePresentationConstants.SERVICE)
public class SpringService extends SpringMetaStereotypeComponent {
    public static final SemKey<JamMemberMeta<PsiClass, SpringService>> META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("SpringServiceMeta", new SemKey[0]);
    public static final SemKey<SpringService> JAM_KEY = JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("SpringService", new SemKey[0]);
    public static final JamClassMeta<SpringService> META = new JamClassMeta<>((JamMemberArchetype) null, SpringService.class, JAM_KEY);
    private static final Function<Module, Collection<String>> ANNOTATIONS = module -> {
        return getAnnotations(module, SpringAnnotationsConstants.SERVICE);
    };

    private static void $$$reportNull$$$0(int i) {
        String str;
        int i2;
        switch (i) {
            case 0:
            case 1:
            case _SpringELLexer.SELECT:
            default:
                str = "Argument for @NotNull parameter '%s' of %s.%s must not be null";
                break;
            case 3:
                str = "@NotNull method %s.%s must not return null";
                break;
        }
        switch (i) {
            case 0:
            case 1:
            case _SpringELLexer.SELECT:
            default:
                i2 = 3;
                break;
            case 3:
                i2 = 2;
                break;
        }
        Object[] objArr = new Object[i2];
        switch (i) {
            case 0:
            case _SpringELLexer.SELECT:
            default:
                objArr[0] = "psiClass";
                break;
            case 1:
                objArr[0] = "anno";
                break;
            case 3:
                objArr[0] = "com/intellij/spring/model/jam/stereotype/SpringService";
                break;
        }
        switch (i) {
            case 0:
            case 1:
            case _SpringELLexer.SELECT:
            default:
                objArr[1] = "com/intellij/spring/model/jam/stereotype/SpringService";
                break;
            case 3:
                objArr[1] = "getServiceAnnotations";
                break;
        }
        switch (i) {
            case 0:
            case 1:
            case _SpringELLexer.SELECT:
            default:
                objArr[2] = "<init>";
                break;
            case 3:
                break;
        }
        String format = String.format(str, objArr);
        switch (i) {
            case 0:
            case 1:
            case _SpringELLexer.SELECT:
            default:
                throw new IllegalArgumentException(format);
            case 3:
                throw new IllegalStateException(format);
        }
    }

    public SpringService(@NotNull PsiClass psiClass) {
        this(SpringAnnotationsConstants.SERVICE, psiClass);
        if (psiClass == null) {
            $$$reportNull$$$0(0);
        }
    }

    public SpringService(@NotNull String anno, @NotNull PsiClass psiClass) {
        super(anno, psiClass);
        if (anno == null) {
            $$$reportNull$$$0(1);
        }
        if (psiClass == null) {
            $$$reportNull$$$0(2);
        }
    }

    @NotNull
    public static Function<Module, Collection<String>> getServiceAnnotations() {
        Function<Module, Collection<String>> function = ANNOTATIONS;
        if (function == null) {
            $$$reportNull$$$0(3);
        }
        return function;
    }
}

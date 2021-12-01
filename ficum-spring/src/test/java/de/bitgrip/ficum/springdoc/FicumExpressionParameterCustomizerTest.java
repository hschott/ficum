package de.bitgrip.ficum.springdoc;

import de.bitgrip.ficum.annotation.FicumExpression;
import io.swagger.v3.oas.models.parameters.Parameter;
import junit.framework.TestCase;
import org.junit.Assert;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class FicumExpressionParameterCustomizerTest extends TestCase {

    public void testCustomize() {
        Method method = ReflectionUtils.findMethod(Testee.class, "testee", String.class);
        Parameter parameter = new Parameter();
        parameter.setDescription("these are");
        MethodParameter methodParameter = new MethodParameter(method, 0);

        FicumExpressionParameterCustomizer customizer = new FicumExpressionParameterCustomizer();
        customizer.customize(parameter, methodParameter);

        Assert.assertEquals("these are, allowedSelectorNames=awesome, marvelous", parameter.getDescription());
    }

    private interface Testee {
        void testee(@FicumExpression({"awesome", "marvelous"}) String param);
    }
}

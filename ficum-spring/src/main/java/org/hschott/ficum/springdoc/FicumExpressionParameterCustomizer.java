package org.hschott.ficum.springdoc;

import org.hschott.ficum.annotation.FicumExpression;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

@Component
public class FicumExpressionParameterCustomizer implements ParameterCustomizer {
    @Override
    public Parameter customize(Parameter parameterModel, MethodParameter methodParameter) {
        FicumExpression annotation = methodParameter.getParameterAnnotation(FicumExpression.class);
        if (annotation != null) {
            String joinedAllowedValues = String.join(", ", annotation.value());

            parameterModel.description(parameterModel.getDescription() + ", allowedSelectorNames=" + joinedAllowedValues);
        }
        return parameterModel;
    }
}

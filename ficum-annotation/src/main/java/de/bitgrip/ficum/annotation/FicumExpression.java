package de.bitgrip.ficum.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>FicumExpression which marks, that the parameter is a FicumExpression with defined allowedSelectorNames.</p>
 * <p>
 * (w) marcelmuller
 * (c) bitgrip GmbH, 2019
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface FicumExpression {

  /**
   * List of allowedSelectorNames
   * @return
   */
  String[] value();

}

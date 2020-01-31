package org.sonar.check;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to prevent the need to include org.sonarsource.sonarqube:sonar-plugin-api jar.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RuleProperty {

  /**
   * Key.
   */
  public java.lang.String key() default "";

  /**
   * Description.
   */
  public java.lang.String description() default "";

  /**
   * Default value.
   */
  public java.lang.String defaultValue() default "";

 }
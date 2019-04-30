package org.stevenlooman.sw.magik;

import com.google.common.collect.ImmutableList;
import org.stevenlooman.sw.magik.checks.CommentRegularExpressionCheck;
import org.stevenlooman.sw.magik.checks.ExemplarSlotCountCheck;
import org.stevenlooman.sw.magik.checks.LhsRhsComparatorEqualCheck;
import org.stevenlooman.sw.magik.checks.LineLengthCheck;
import org.stevenlooman.sw.magik.checks.MethodComplexityCheck;
import org.stevenlooman.sw.magik.checks.SizeZeroEmptyCheck;
import org.stevenlooman.sw.magik.checks.TrailingWhitespaceCheck;
import org.stevenlooman.sw.magik.checks.UnusedVariableCheck;
import org.stevenlooman.sw.magik.checks.XPathCheck;

import java.util.List;

public final class CheckList {
  public static final String REPOSITORY_KEY = "magik";
  public static final String SONAR_WAY_PROFILE = "Sonar way";
  public static final String PROFILE_DIR = "org/stevenlooman/sw/sonar/l10n/magik/rules";
  public static final String PROFILE_LOCATION = PROFILE_DIR + "/Sonar_way_profile.json";

  private CheckList() {
  }

  /**
   * Get the list of Checks.
   * @return List of with Checks
   */
  public static List<Class> getChecks() {
    return ImmutableList.<Class>of(
        CommentRegularExpressionCheck.class,
        ExemplarSlotCountCheck.class,
        LhsRhsComparatorEqualCheck.class,
        LineLengthCheck.class,
        MethodComplexityCheck.class,
        SizeZeroEmptyCheck.class,
        TrailingWhitespaceCheck.class,
        UnusedVariableCheck.class,
        XPathCheck.class
    );
  }
}

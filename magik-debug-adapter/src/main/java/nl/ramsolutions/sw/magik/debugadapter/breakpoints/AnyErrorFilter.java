package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

/** Stops on every error condition. */
public class AnyErrorFilter implements ExceptionFilter {

  private static final String CONDITION = "_self.taxonomy.includes?(:error)";

  @Override
  public String getId() {
    return CONDITION;
  }

  @Override
  public String getLabel() {
    return "Any error condition";
  }

  @Override
  public String getCondition() {
    return CONDITION;
  }
}

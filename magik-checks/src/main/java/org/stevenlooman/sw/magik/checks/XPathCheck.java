package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.xpath.api.AstNodeXPathQuery;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.TemplatedMagikCheck;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

@TemplatedMagikCheck
@Rule(key = XPathCheck.CHECK_KEY)
public class XPathCheck extends MagikCheck {

  public static final String CHECK_KEY = "XPath";

  private static final String DEFAULT_XPATH_QUERY = "";
  private static final String DEFAULT_MESSAGE = "The XPath expression matches this piece of code";

  @RuleProperty(
      key = "xpath query",
      description = "The XPath query",
      defaultValue = "" + DEFAULT_XPATH_QUERY,
      type = "TEXT")
  public String xpathQuery = DEFAULT_XPATH_QUERY;

  @RuleProperty(
      key = "message",
      description = "The issue message",
      defaultValue = "" + DEFAULT_MESSAGE)
  public String message = DEFAULT_MESSAGE;

  private AstNodeXPathQuery<Object> query = null;

  /**
   * Construct the query.
   * @return Query
   */
  @CheckForNull
  public AstNodeXPathQuery<Object> query() {
    if (query == null && xpathQuery != null && !xpathQuery.isEmpty()) {
      try {
        query = AstNodeXPathQuery.create(xpathQuery);
      } catch (RuntimeException ex) {
        throw new IllegalStateException(
            "Unable to initialize the XPath engine, perhaps because of an invalid query: "
                + xpathQuery,
            ex);
      }
    }
    return query;
  }

  @Override
  public void visitFile(@Nullable AstNode fileNode) {
    AstNodeXPathQuery<Object> xpath = query();
    if (xpath != null && fileNode != null) {
      for (Object object : xpath.selectNodes(fileNode)) {
        if (object instanceof AstNode) {
          AstNode astNode = (AstNode) object;
          addIssue(message, astNode);
        } else if (object instanceof Boolean && (Boolean) object) {
          addFileIssue(message);
        }
      }
    }
  }
}
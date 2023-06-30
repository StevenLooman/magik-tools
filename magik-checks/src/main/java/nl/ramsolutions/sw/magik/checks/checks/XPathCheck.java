package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.xpath.api.AstNodeXPathQuery;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.TemplatedMagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for matching XPath expressions.
 */
@TemplatedMagikCheck
@Rule(key = XPathCheck.CHECK_KEY)
public class XPathCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "XPath";

    private static final String DEFAULT_XPATH_QUERY = "";
    private static final String DEFAULT_MESSAGE = "The XPath expression matches this piece of code";

    /**
     * XPath query.
     */
    @RuleProperty(
        key = "xpath query",
        description = "The XPath query",
        defaultValue = "" + DEFAULT_XPATH_QUERY,
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String xpathQuery = DEFAULT_XPATH_QUERY;

    /**
     * Message to report.
     */
    @RuleProperty(
        key = "message",
        description = "The issue message",
        defaultValue = "" + DEFAULT_MESSAGE,
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String message = DEFAULT_MESSAGE;

    private AstNodeXPathQuery<Object> query;

    /**
     * Construct the query.
     * @return Query
     */
    @CheckForNull
    @SuppressWarnings("checkstyle:IllegalCatch")
    public AstNodeXPathQuery<Object> query() {
        if (this.query == null && this.xpathQuery != null && !this.xpathQuery.isEmpty()) {
            try {
                this.query = AstNodeXPathQuery.create(this.xpathQuery);
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                    "Unable to initialize the XPath engine, perhaps because of an invalid query: "
                    + this.xpathQuery,
                    ex);
            }
        }
        return query;
    }

    @Override
    protected void walkPreMagik(AstNode node) {
        final AstNodeXPathQuery<Object> xpath = query();
        if (xpath != null) {
            for (final Object object : xpath.selectNodes(node)) {
                if (object instanceof final AstNode astNode) {
                    this.addIssue(astNode, message);
                } else if (object instanceof Boolean && (boolean) object) {
                    this.addFileIssue(message);
                }
            }
        }
    }

}

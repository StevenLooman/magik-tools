package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

/** Body scope. */
public class BodyScope extends Scope {

  BodyScope(final Scope parentScope, final AstNode node) {
    super(parentScope, node);
  }
}

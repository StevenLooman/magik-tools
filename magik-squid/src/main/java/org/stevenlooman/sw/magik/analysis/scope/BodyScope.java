package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

public class BodyScope extends Scope {

  BodyScope(Scope parentScope, AstNode node) {
    super(parentScope, node);
  }

}

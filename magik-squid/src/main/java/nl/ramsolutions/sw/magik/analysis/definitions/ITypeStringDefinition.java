package nl.ramsolutions.sw.magik.analysis.definitions;

import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** A resolvable type definition. */
public interface ITypeStringDefinition extends IDefinition {

  public TypeString getTypeString();

  // TODO: Parents.

}

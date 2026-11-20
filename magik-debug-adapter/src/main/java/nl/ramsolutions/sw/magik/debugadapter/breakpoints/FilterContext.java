package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;

/**
 * An exception breakpoint hit whose condition holds.
 *
 * @param threadId Thread which hit the breakpoint.
 * @param slapProtocol Protocol to the agent.
 * @param sourceLookup Finds the source file of a stack frame.
 */
public record FilterContext(long threadId, ISlapProtocol slapProtocol, SourceLookup sourceLookup) {}

package edu.cornell.em577.tamperprooflogging.protocol.exception

/** Runtime exception indicating that a unit of work has been terminated unexpectedly */
class UnexpectedTerminationException(override val message: String) : RuntimeException(message)
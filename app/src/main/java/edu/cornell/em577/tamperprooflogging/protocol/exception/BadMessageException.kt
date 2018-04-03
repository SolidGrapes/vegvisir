package edu.cornell.em577.tamperprooflogging.protocol.exception

/** Runtime exception indicating that a improperly formatted network message was received */
class BadMessageException(override val message: String) : RuntimeException(message)
package edu.cornell.em577.tamperprooflogging.data.exception

/** Runtime exception indicating that a block was unexpectedly not found */
class BlockNotFoundException(override val message: String) : RuntimeException(message)
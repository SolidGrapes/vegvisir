package edu.cornell.em577.tamperprooflogging.data.exception

/** Runtime exception indicating that a signed block was unexpectedly not found */
class SignedBlockNotFoundException(override val message: String) : RuntimeException(message)
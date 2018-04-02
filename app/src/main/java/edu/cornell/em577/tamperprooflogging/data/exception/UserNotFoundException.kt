package edu.cornell.em577.tamperprooflogging.data.exception

/** Runtime exception indicating that a user was unexpectedly not found */
class UserNotFoundException(override val message: String) : RuntimeException(message)
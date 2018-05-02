package edu.cornell.em577.tamperprooflogging.data.exception

/** Runtime exception indicating that the requisite permissions for some action were not found */
class PermissionNotFoundException(override val message: String) : RuntimeException(message)
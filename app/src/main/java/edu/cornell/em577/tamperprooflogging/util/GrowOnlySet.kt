package edu.cornell.em577.tamperprooflogging.util

/** An add-only set abstraction. */
class GrowOnlySet<E> {

    private val addSet = HashSet<E>()

    /** Add an element to the set. */
    fun add(element: E) {
        addSet.add(element)
    }

    /** Return a copy of the set in list form. */
    fun toList(): List<E> {
        return addSet.toList()
    }
}
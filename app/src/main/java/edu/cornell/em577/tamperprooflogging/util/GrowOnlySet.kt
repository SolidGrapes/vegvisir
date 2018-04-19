package edu.cornell.em577.tamperprooflogging.util

class GrowOnlySet<E> {

    private val addSet = HashSet<E>()

    fun add(element: E) {
        addSet.add(element)
    }

    fun toList(): List<E> {
        return addSet.toList()
    }
}
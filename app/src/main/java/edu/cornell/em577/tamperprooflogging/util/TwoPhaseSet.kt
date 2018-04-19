package edu.cornell.em577.tamperprooflogging.util

class TwoPhaseSet<E> {

    private val addSet = HashSet<E>()

    private val removeSet = HashSet<E>()

    fun lookup(element: E): Boolean {
        return addSet.contains(element) && !removeSet.contains(element)
    }

    fun add(elementToAdd: E) {
        addSet.add(elementToAdd)
    }

    fun remove(elementToRemove: E) {
        removeSet.add(elementToRemove)
    }

    fun toList(): List<E> {
        return addSet.minus(removeSet).toList()
    }
}
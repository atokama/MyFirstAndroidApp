package com.example.myfirstandroidapp

class Filter {
    abstract class Node<T>(protected var result: T) {
        var next: Node<T>? = null
        var prev: Node<T>? = null
        var enabled: Boolean = true

        protected abstract fun doProcess(data: T): T

        fun process(data: T): T {
            return if (enabled) {
                result = doProcess(data)
                next?.process(result) ?: result
            } else data
        }
    }


    class Factory<T> {
        fun create(data: T, filter: (T) -> T): Node<T> {
            return F1(data, filter)
        }

        fun create(data: T, filter: (T, T) -> Unit): Node<T> {
            return F2(data, filter)
        }

        private class F1<T>(
            result: T,
            private val func: (T) -> T
        ) : Filter.Node<T>(result) {
            override fun doProcess(data: T): T {
                result = func(data)
                return result
            }
        }

        private class F2<T>(
            result: T,
            private val func: (T, T) -> Unit
        ) : Filter.Node<T>(result) {
            override fun doProcess(data: T): T {
                func(data, result)
                return result
            }
        }

    }


    class Chain<T>(result: T) : Node<T>(result) {
        val filters: MutableList<Node<T>> = mutableListOf()

        fun add(node: Node<T>): Node<T> {
            val last = filters.lastOrNull()
            if (filters.add(node)) {
                if (last != null) {
                    val new = filters.lastOrNull()
                    last.next = new
                    new?.prev = last
                }
            }
            return filters.last()
        }

        class Builder<T>(private val chain: Chain<T>) {

            fun add(data: T, filter: (T) -> T): Node<T> {
                val node = Factory<T>().create(data, filter)
                return chain.add(node)
            }
        }

        override fun doProcess(data: T): T {
            return filters.first().process(data)
        }
    }
}
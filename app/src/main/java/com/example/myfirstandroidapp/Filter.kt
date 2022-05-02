package com.example.myfirstandroidapp

class Filter {
    abstract class Node<T>(private val output: T) {
        var output2: T? = null

        fun getOutput(): T = if (output2 != null) {
            output2!!
        } else output

        fun setOutput(value: T) {
            output2 = value
        }

        private fun resetOutput(): T {
            val tmp = getOutput()
            output2 = null
            return tmp
        }

        var next: Node<T>? = null
        var prev: Node<T>? = null
        var enabled: Boolean = true

        protected abstract fun doProcess(data: T)

        fun process(data: T): T {
            return if (enabled) {
                doProcess(data)
                val output = resetOutput()
                next?.process(output) ?: output
            } else data
        }
    }

    class Factory<T> {
        fun create(data: T, filter: (T, T) -> Unit): Node<T> {
            return F2(data, filter)
        }

        private class F2<T>(
            result: T,
            private inline val func: (T, T) -> Unit
        ) : Filter.Node<T>(result) {
            override fun doProcess(data: T) {
                func(data, getOutput())
            }
        }
    }

    class Chain<T>(result: T) : Node<T>(result) {
        val filters: MutableList<Node<T>> = mutableListOf()

        fun add(node: Node<T>): Node<T> {
            val last = filters.lastOrNull()
            if (filters.add(node)) {
                last?.next = node
                node.prev = last
            }
            return node
        }

        class Builder<T>(private val chain: Chain<T>) {
            fun add(data: T, filter: (T, T) -> Unit): Node<T> {
                val node = Factory<T>().create(data, filter)
                return chain.add(node)
            }
        }

        override fun doProcess(data: T) {
            filters.firstOrNull()?.process(data)
        }
    }
}
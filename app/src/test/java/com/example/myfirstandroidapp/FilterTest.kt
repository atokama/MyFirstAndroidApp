package com.example.myfirstandroidapp

import org.junit.Assert.*
import org.junit.Test

class FilterTest {

    private val chain = Filter.Chain(0)
    private val builder = Filter.Chain.Builder(chain)

    @Test
    fun add() {
        assertEquals(0, chain.filters.size)

        val first = builder.add(0) { n -> n }
        assertEquals(1, chain.filters.size)
        assertEquals(null, first.prev)
        assertEquals(null, first.next)

        val second = builder.add(0) { n -> n }
        assertEquals(2, chain.filters.size)
        assertEquals(null, second.next)
        assertEquals(first.next, second)
        assertEquals(first, second.prev)

        val third = builder.add(0) { n -> n }
        assertEquals(3, chain.filters.size)
        assertEquals(null, third.next)
        assertEquals(second.next, third)
        assertEquals(second, third.prev)
    }

    @Test
    fun process() {
        builder.add(0) { n -> n + 1 }
        builder.add(0) { n -> n + 2 }
        assertEquals(3, chain.process(0))

        // add 1 more filter
        builder.add(0) { n -> n + 3 }
        assertEquals(6, chain.process(0))

        // disable 2 last filters
        val filter = chain.filters.last()
        filter.enabled = false
        filter.prev!!.enabled = false
        assertEquals(1, chain.process(0))

    }


}
package com.example.myfirstandroidapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FilterTest {

    private val chain = Filter.Chain(0)
    private val builder = Filter.Chain.Builder(chain)

    @Test
    fun outputObject() {
        data class Data(val i: Int = 0)

        val chain = Filter.Chain(Data())

        repeat(3) {
            val tmp = Data(it)
            val filter = Filter.Chain.Builder(chain)
                .add(tmp) { data, result -> repeat(data.i) { result.i.inc() } }

            assertSame(tmp, filter.getOutput())
        }

        with(chain.filters) {
            val input = Data(2)
            last().setOutput(input)
            last().prev?.setOutput(input)

            val output = first().process(input)
            assertSame(input, output)
        }
    }

    @Test
    fun add() {
        assertEquals(0, chain.filters.size)
        val first = builder.add(0) { _, _ -> }
        assertEquals(1, chain.filters.size)
        assertEquals(null, first.prev)
        assertEquals(null, first.next)

        val second = builder.add(0) { _, _ -> }
        assertEquals(2, chain.filters.size)
        assertEquals(null, second.next)
        assertEquals(first.next, second)
        assertEquals(first, second.prev)

        val third = builder.add(0) { _, _ -> }
        assertEquals(3, chain.filters.size)
        assertEquals(null, third.next)
        assertEquals(second.next, third)
        assertEquals(second, third.prev)
    }

//    @Test
//    fun process() {
//        builder.add(0) { n, m -> }
//        builder.add(0) { n -> n + 2 }
//        assertEquals(3, chain.process(0))
//
//        // add 1 more filter
//        builder.add(0) { n -> n + 3 }
//        assertEquals(6, chain.process(0))
//
//        // disable 2 last filters
//        val filter = chain.filters.last()
//        filter.enabled = false
//        filter.prev!!.enabled = false
//        assertEquals(1, chain.process(0))
//
//    }


}
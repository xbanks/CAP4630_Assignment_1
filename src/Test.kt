/**
 * Created by Xavier on 9/18/2016.
 */
import java.awt.Point
import java.util.*

data class test( val cost: Int, val path: Int ) {
    override fun hashCode(): Int {
        return cost + path
    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other?.hashCode()
    }
}
class comparetest : Comparator<test> {
    override fun compare(o1: test, o2: test): Int {
        return o1.cost - o2.cost
    }
}

var pqueue = PriorityQueue<test>(comparetest())

fun main(args: Array<String>) {
    val pq = PriorityQueue<test>( comparetest() )
    pq.add( test(100, 20) )
    pq.add( test(11, 20) )
    pq.add( test(10, 20) )
    pq.add( test(12, 20) )
    pq.add( test(1, 20) )

    println( pq.contains(test(21, 0)) )

    while( pq.isNotEmpty() )
    {
        println( pq.poll() )
    }

    println( "hashset" )
    val hs = hashSetOf( test(1,1), test(1,2), test(2,1) )
    for (h in hs)
    {
        println( h )
    }

    println( test(1,2) == test(2,1) )
    println( Point(1,2).hashCode() )
    println( Point(1,2).hashCode() )

    println( Point(1,2) )
}
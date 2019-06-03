package routeguide;

import java.util.concurrent.atomic.AtomicInteger;

/*public class UnsignedInts {
    static final long INT_MASK = 0xffffffffL;

    static int flip(int value) {
        return value & Integer.MAX_VALUE;
    }

    public static long toLong(int value) {
        return value & INT_MASK;
    }
    public static int remainder(int dividend,
                                int divisor){
        return (int) (toLong(dividend) % toLong(divisor));
    }
}*/
public class PositiveAtomicCounter {
    private static final int    MASK = 0x7FFFFFFF;
    private final AtomicInteger atom;

    public PositiveAtomicCounter() {
        atom = new AtomicInteger(0);
    }

    public final int incrementAndGet() {
        return atom.incrementAndGet() & MASK;
    }

    public final int getAndIncrement() {
        return atom.getAndIncrement() & MASK;
    }

    public int get() {
        return atom.get() & MASK;
    }

}

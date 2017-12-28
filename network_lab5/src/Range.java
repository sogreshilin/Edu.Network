import java.util.Iterator;

/**
 * Created by Alexander on 04/11/2017.
 */
public class Range implements Iterable<String> {
    private long begin;
    private long end;

    public Range(long begin, int length) {
        this.begin = begin;
        this.end = begin + length - 1;
    }

    public long getBegin() {
        return begin;
    }

    public int getLength() {
        return (int) (end - begin + 1);
    }

    @Override
    public Iterator<String> iterator() {
        return new RangeIterator(begin, end);
    }

    @Override
    public String toString() {
        return begin + ".." + end;
    }
}

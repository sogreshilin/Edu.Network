import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Alexander on 04/11/2017.
 */
public class RangeIterator implements Iterator<String> {
    final static long LAST_STRING_NUMBER;
    private static final Character[] ALPHABET = new Character[]{'A', 'C', 'G', 'T'};
    private static final int ALPHABET_POWER = ALPHABET.length;
    private final static long[] sumOfAlphabetPowerDegrees = new long[Constants.MAX_LENGTH + 1];

    static {
        long powerDegree = 1;
        sumOfAlphabetPowerDegrees[0] = 1;

        for (int i = 1; i < Constants.MAX_LENGTH + 1; ++i) {
            powerDegree *= ALPHABET_POWER;
            sumOfAlphabetPowerDegrees[i] += sumOfAlphabetPowerDegrees[i - 1] + powerDegree;
        }

        LAST_STRING_NUMBER = sumOfAlphabetPowerDegrees[Constants.MAX_LENGTH] - 1;
    }

    private long current;
    private long last;

    RangeIterator(long begin, long end) {
        this.current = begin;
        this.last = Long.min(end, LAST_STRING_NUMBER);
    }

    @Override
    public boolean hasNext() {
        return current <= last;
    }

    @Override
    public String next() {
        if (current == 0) {
            current++;
            return "";
        }

        int symbolCount = 0;
        for (int i = 0; i < Constants.MAX_LENGTH; ++i) {
            if (current >= sumOfAlphabetPowerDegrees[i] && current < sumOfAlphabetPowerDegrees[i + 1]) {
                symbolCount = i + 1;
                break;
            }
        }

        long value = current - sumOfAlphabetPowerDegrees[symbolCount - 1];

        Character[] array = new Character[symbolCount];
        ArrayList<Integer> list = new ArrayList<>();
        for (int j = symbolCount - 1; j >= 0; --j) {
            array[j] = ALPHABET[(int) value % ALPHABET_POWER];
            value /= ALPHABET_POWER;
        }

        String result = "";
        for (int i = 0; i < symbolCount; ++i) {
            result += array[i];
        }
        current++;
        return result;
    }
}

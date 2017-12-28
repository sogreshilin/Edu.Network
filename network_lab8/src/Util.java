public class Util {
    /**
     * Checks if @param bigger array contains @param smaller array.
     * If it does contain then @return position of first occurrence,
     * otherwise @return -1.
     */
    public static int contains(byte[] bigger, byte[] smaller) {
        for (int i = 0; i < bigger.length - smaller.length + 1; ++i) {
            boolean contains = true;
            for (int j = 0; j < smaller.length; ++j) {
                if (bigger[i + j] != smaller[j]) {
                    contains = false;
                    break;
                }
            }
            if (contains) {
                return i;
            }
        }
        return -1;
    }
}

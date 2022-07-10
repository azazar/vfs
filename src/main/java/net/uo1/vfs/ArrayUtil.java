/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import static java.util.Arrays.copyOfRange;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
class ArrayUtil {

    public static String[] shift(String[] a) {
        return copyOfRange(a, 1, a.length);
    }

    private ArrayUtil() {
    }

}

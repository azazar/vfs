/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.util.Arrays;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
class ArrayUtil {

    public static String[] shift(String[] a) {
        return Arrays.copyOfRange(a, 1, a.length);
    }

    private ArrayUtil() {
    }

}

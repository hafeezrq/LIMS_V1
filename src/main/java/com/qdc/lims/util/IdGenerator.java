package com.qdc.lims.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import java.util.Random;

/**
 * Utility class for generating unique Medical Record Numbers (MRN).
 */
public class IdGenerator {

    // ONLY Numbers (0-9)
    private static final char[] NUMBERS = "0123456789".toCharArray();
    private static final Random RANDOM = new Random();

    /**
     * Generates a random 6-digit Medical Record Number (MRN) formatted as nuumerics-numerics.
     *
     * @return a formatted MRN string (e.g., 852-304)
     */
    public static String generateMrn() {
        // Generate 6 random numbers
        String rawId = NanoIdUtils.randomNanoId(RANDOM, NUMBERS, 6);

        return rawId.substring(0, 3) + "-" + rawId.substring(3, 6);
    }
}
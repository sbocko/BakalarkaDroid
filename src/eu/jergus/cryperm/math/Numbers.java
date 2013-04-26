package eu.jergus.cryperm.math;

import java.math.BigInteger;
import java.util.Random;

/**
 * Various arithmetic functions.
 */
public class Numbers {

	public static final Random RANDOM = new Random();


	/**
	 * Determine if a given number is prime by brute-force (only suitable for
	 * small numbers).
	 */
	public static boolean isPrime(int p) {
		if (p < 0) p = -p;
		if (p < 2) return false;
		for (int i = 2; i*i <= p; ++i) {
			if (p%i == 0) return false;
		}
		return true;
	}


	/**
	 * Generate a random BigInteger between 0 and max-1.
	 */
	public static BigInteger random(BigInteger max) {
		BigInteger res;
		do {
			res = new BigInteger(max.bitLength(), RANDOM);
		} while (res.compareTo(max) >= 0);
		return res;
	}


	/**
	 * Generate a random BigInteger between 0 and max-1 that is relatively prime
	 * to max.
	 */
	public static BigInteger randomRelativelyPrime(BigInteger max) {
		BigInteger res;
		do {
			res = random(max);
		} while (!res.gcd(max).equals(BigInteger.ONE));
		return res;
	}


	/**
	 * Convert int[] to BigInteger[].
	 */
	public static BigInteger[] toBigInteger(int[] array) {
		BigInteger[] res = new BigInteger[array.length];
		for (int i = 0; i < array.length; ++i) {
			res[i] = BigInteger.valueOf(array[i]);
		}
		return res;
	}


	/**
	 * Convert BigInteger[] to int[].
	 */
	public static int[] toInt(BigInteger[] array) {
		int[] res = new int[array.length];
		for (int i = 0; i < array.length; ++i) {
			res[i] = array[i].intValue();
		}
		return res;
	}


	/**
	 * Sum values from two arrays.
	 *
	 * @return a new array containing sums of values from a and b
	 */
	public static int[] sum(int[] a, int[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Trying to sum arrays of different lengths.");
		}
		int[] res = new int[a.length];
		for (int i = 0; i < a.length; ++i) {
			res[i] = a[i] + b[i];
		}
		return res;
	}

}
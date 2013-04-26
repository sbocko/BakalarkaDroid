package eu.jergus.cryperm.math;

import java.math.BigInteger;

/**
 * Polynomial over Z<sub>mod</sub> (mod must be prime, so that every non-zero
 * element has a modular inverse element).
 */
public class Polynomial {

	private int mod;
	private int[] coefficients;

	/**
	 * Generate a random polynomial with the given value at x = 0.
	 */
	public Polynomial(int mod, int order, int zeroValue) {
		this.mod = mod;
		coefficients = new int[order];
		coefficients[0] = zeroValue;
		for (int i = 1; i < order; ++i) {
			coefficients[i] = (int) (Math.random()*mod);
		}
	}


	/**
	 * Calculate value at the given x.
	 */
	public int value(int x) {
		int res = 0;
		int pow = 1;
		for (int coefficient : coefficients) {
			res = (res + coefficient*pow) % mod;
			pow = (pow * x) % mod;
		}
		return res;
	}


	/**
	 * Return an array of values at 1, 2, ..., n.
	 */
	public int[] values(int n) {
		int[] res = new int[n];
		for (int i = 0; i < n; ++i) {
			res[i] = value(i+1);
		}
		return res;
	}


	/**
	 * Given k points [x<sub>1</sub>,y<sub>1</sub>]...[x<sub>k</sub>,y<sub>k</sub>],
	 * determine the value of the (unique) k-th order polynomial passing through
	 * these points, at the given point x.
	 * <p>
	 * Note that this never computes the coefficients of the polynomial, it only
	 * computes the requested value (using the Lagrange interpolating
	 * polynomial).
	 */
	public static int interpolate(int mod, int[][] points, int x) {
		int res = 0;
		for (int i = 0; i < points.length; ++i) {
			int part = points[i][1];
			for (int j = 0; j < points.length; ++j) {
				if (j != i) {
					part *= x - points[j][0];
					part %= mod; if (part < 0) part += mod;
					part *= inverse(mod, points[i][0] - points[j][0]);
					part %= mod;
				}
			}
			res += part;
			res %= mod;
		}
		return res;
	}


	/**
	 * Given k values y<sub>1</sub>, ..., y<sub>k</sub>, determine the value of
	 * the (unique) k-th order polynomial passing through the points
	 * [1,y<sub>1</sub>]...[k,y<sub>k</sub>], at the given point x.
	 *
	 * @see #interpolate(int, int[][], int)
	 */
	public static int interpolate(int mod, int[] values, int x) {
		int[][] points = new int[values.length][2];
		for (int i = 0; i < values.length; ++i) {
			points[i][0] = i+1;
			points[i][1] = values[i];
		}
		return interpolate(mod, points, x);
	}


	/**
	 * Multiplicative inverse of n over Z<sub>mod</sub>.
	 */
	private static int inverse(int mod, int n) {
		n %= mod; if (n < 0) n += mod;
		return BigInteger.valueOf(n).modInverse(BigInteger.valueOf(mod)).intValue();
	}

}
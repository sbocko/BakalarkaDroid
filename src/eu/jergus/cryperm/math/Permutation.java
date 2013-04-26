package eu.jergus.cryperm.math;

import java.math.BigInteger;

/**
 * Represents a permutation of a given size.
 */
public class Permutation {

	private int[] perm;
	private int[] rev;


	/**
	 * Initialize a random permutation with the given length.
	 */
	public Permutation(int length) {
		perm = new int[length];
		for (int i = 0; i < length; ++i) perm[i] = i;
		for (int i = 0; i < length-1; ++i) {
			int j = i + (int)(Math.random()*(length-i));
			int tmp = perm[i];
			perm[i] = perm[j];
			perm[j] = tmp;
		}

		generateRev();
	}


	/**
	 * Initialize this object with the provided permutation.
	 */
	public Permutation(int[] perm) {
		this.perm = perm;

		boolean[] contains = new boolean[perm.length];
		for (int i : perm) {
			if (i < 0 || i >= perm.length || contains[i]) {
				throw new IllegalArgumentException("Illegal permutation.");
			}
			contains[i] = true;
		}

		for (int i = 0; i < perm.length; ++i) {
			if (!contains[i]) {
				throw new IllegalArgumentException("Illegal permutation.");
			}
		}

		generateRev();
	}


	private void generateRev() {
		rev = new int[perm.length];
		for (int i = 0; i < perm.length; ++i) {
			rev[perm[i]] = i;
		}
	}


	/**
	 * Return the element at the specified position in the permutation.
	 */
	public int get(int index) {
		return perm[index];
	}


	/**
	 * Return the element whose position in the permutation is equal to the
	 * specified index.
	 */
	public int rev(int index) {
		return rev[index];
	}


	/**
	 * Get the complete permutation.
	 */
	public int[] get() {
		return perm;
	}


	/**
	 * Apply this permutation to an array.
	 */
	public BigInteger[] apply(BigInteger[] array) {
		if (array.length != perm.length) {
			throw new RuntimeException("Invalid permutation length.");
		}
		BigInteger[] res = new BigInteger[array.length];
		for (int i = 0; i < array.length; ++i) {
			//res[i] = array[i];
			res[i] = array[perm[i]];
		}
		return res;
	}

	/**
	 * Apply this permutation to the rows of a matrix (2D array).
	 */
	public BigInteger[][] apply(BigInteger[][] array) {
		if (array.length != perm.length) {
			throw new RuntimeException("Invalid permutation length.");
		}
		BigInteger[][] res = new BigInteger[array.length][array[0].length];
		for (int i = 0; i < array.length; ++i) {
			for (int j = 0; j < array[0].length; ++j) {
				//res[i][j] = array[i][j];
				res[i][j] = array[perm[i]][j];
			}
		}
		return res;
	}

}
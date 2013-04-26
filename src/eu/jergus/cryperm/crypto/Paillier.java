package eu.jergus.cryperm.crypto;

import java.math.BigInteger;

import eu.jergus.cryperm.math.Numbers;

/**
 * An implementation of the Paillier's cryptosystem.
 */
public class Paillier {

	/**
	 * Index of the public key n in the key array.
	 */
	public static final int N = 0;

	/**
	 * Index of n<sup>2</sup> in the key array.
	 */
	public static final int N2 = 1;

	/**
	 * Index of n+1 in the key array.
	 */
	public static final int N_PLUS_1 = 2;

	/**
	 * Index of the prime factor p in the key array.
	 */
	public static final int P = 3;

	/**
	 * Index of the prime factor q in the key array.
	 */
	public static final int Q = 4;

	/**
	 * Index of &lambda; in the key array.
	 */
	public static final int LAM = 5;

	/**
	 * Index of &lambda;<sup>-1</sup> mod n in the key array.
	 */
	public static final int LAM_INV = 6;

	/**
	 * Index of n<sup>-1</sup> mod &phi;(n) in the key array.
	 */
	public static final int N_INV = 7;


	/**
	 * Public key array length.
	 */
	public static final int PUBLIC_FIELDS = 3;

	/**
	 * Public+private key array length.
	 */
	public static final int FIELDS = 8;


	/**
	 * Generate a random key pair.
	 * <p>
	 * In addition to the public and private key, the returned array contains
	 * various pre-computed values to increase efficiency.
	 */
	public static BigInteger[] generateKey(int size) {
		BigInteger[] key = new BigInteger[FIELDS];

		key[P] = BigInteger.probablePrime(size, Numbers.RANDOM);
		key[Q] = BigInteger.probablePrime(size, Numbers.RANDOM);

		key[N] = key[P].multiply(key[Q]);
		key[N2] = key[N].pow(2);
		key[N_PLUS_1] = key[N].add(BigInteger.ONE);

		BigInteger pMinus1 = key[P].subtract(BigInteger.ONE);
		BigInteger qMinus1 = key[Q].subtract(BigInteger.ONE);
		BigInteger phi = pMinus1.multiply(qMinus1);

		key[LAM] = phi.divide(pMinus1.gcd(qMinus1));
		key[LAM_INV] = key[LAM].modInverse(key[N]);

		key[N_INV] = key[N].modInverse(phi);

		return key;
	}


	/**
	 * Pre-computes additional values from the given public key.
	 */
	public static BigInteger[] keyToArray(BigInteger n) {
		return new BigInteger[] {
			n,
			n.pow(2),
			n.add(BigInteger.ONE)
		};
	}


	/**
	 * Encrypt the specified value using the given public key.
	 */
	public static BigInteger encrypt(int x, BigInteger[] key) {
		return encrypt(BigInteger.valueOf(x), key);
	}

	/**
	 * Encrypt the specified value using the given public key and the specified
	 * randomizing value.
	 */
	public static BigInteger encrypt(int x, BigInteger y, BigInteger[] key) {
		return encrypt(BigInteger.valueOf(x), y, key);
	}


	/**
	 * Encrypt the specified value using the given public key and the specified
	 * randomizing value.
	 */
	public static BigInteger encrypt(int x, int y, BigInteger[] key) {
		return encrypt(BigInteger.valueOf(x), BigInteger.valueOf(y), key);
	}


	/**
	 * Encrypt the specified value using the given public key.
	 */
	public static BigInteger encrypt(BigInteger x, BigInteger[] key) {
		return encrypt(
				x,
				Numbers.randomRelativelyPrime(key[N]),
				key);
	}


	/**
	 * Encrypt the specified value using the given public key and the specified
	 * randomizing value.
	 */
	public static BigInteger encrypt(BigInteger x, BigInteger y, BigInteger[] key) {
		BigInteger a = key[N_PLUS_1].modPow(x, key[N2]);
		BigInteger b = y.modPow(key[N], key[N2]);
		return a.multiply(b).mod(key[N2]);
	}


	/**
	 * Decrypt the given encrypted value. A key array that includes a private
	 * key must be given.
	 */
	public static BigInteger decrypt(BigInteger c, BigInteger[] key) {
		BigInteger cPow = c.modPow(key[LAM], key[N2]);
		BigInteger log = cPow.subtract(BigInteger.ONE).divide(key[N]);
		return log.multiply(key[LAM_INV]).mod(key[N]);
	}


	/**
	 * Decrypt the randomizing value used when encrypting the given ciphertext.
	 * A key array that includes a private key must be given.
	 */
	public static BigInteger decrypt2(BigInteger c, BigInteger[] key) {
		return c.modPow(key[N_INV], key[N]);
	}


	/**
	 * Encrypt an arbitrarily large value, not from [0,n).
	 */
	public static BigInteger encryptBig(BigInteger x, BigInteger[] key) {
		if (x.equals(BigInteger.ZERO)) {
			return encrypt(x, key);
		}

		BigInteger res = BigInteger.ZERO;
		while (!x.equals(BigInteger.ZERO)) {
			res = res.multiply(key[N2]).add(
					encrypt(x.mod(key[N]), key));
			x = x.divide(key[N]);
		}
		return res;
	}

	/**
	 * Decrypt an arbitrarily large value.
	 */
	public static BigInteger decryptBig(BigInteger c, BigInteger[] key) {
		BigInteger res = BigInteger.ZERO;
		while (!c.equals(BigInteger.ZERO)) {
			res = res.multiply(key[N]).add(
					decrypt(c.mod(key[N2]), key));
			c = c.divide(key[N2]);
		}
		return res;
	}
}
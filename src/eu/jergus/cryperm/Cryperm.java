package eu.jergus.cryperm;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.NoSuchElementException;

import eu.jergus.cryperm.crypto.Paillier;
import eu.jergus.cryperm.math.Numbers;
import eu.jergus.cryperm.math.Permutation;
import eu.jergus.cryperm.math.Polynomial;
import eu.jergus.crypto.ParticipantAction;
import eu.jergus.crypto.RemoteParticipant;
import eu.jergus.crypto.Protocol;
import eu.jergus.crypto.exception.DeadObjectException;
import eu.jergus.crypto.exception.ProtocolException;
import eu.jergus.crypto.util.Log;

/**
 * The cryptographic permutation protocol implementation.
 */
public class Cryperm extends Protocol {

	/**
	 * Security parameters.
	 */
	private static final int DEFAULT_KEY_SIZE = 256;
	private static final int DEFAULT_PROOF_ITERATIONS = 16;

	private final int keySize;
	private final int proofIterations;


	/**
	 * Number of participants required to uncover a permutation element.
	 */
	private final int k;

	/**
	 * Permutation size.
	 */
	private final int size;

	/**
	 * Modulus of the field for Shamir's scheme.
	 */
	private int mod;

	/**
	 * List of uncovered elements.
	 */
	private boolean[] uncovered;


	/**
	 * @param localId        ID of the local participant (must be unique)
	 * @param inputStreams   streams to receive data from remote participants
	 * @param outputStreams  streams to send data to remote participants
	 * @param k              minimum number of participants required to uncover a permutation element
	 * @param size           size of the permutation
	 */
 	public Cryperm(
			int localId,
			InputStream[] inputStreams,
			OutputStream[] outputStreams,
			int k,
			int size) {

		this(localId, inputStreams, outputStreams, k, size,
				DEFAULT_KEY_SIZE, DEFAULT_PROOF_ITERATIONS);
	}


	/**
	 * @param localId        ID of the local participant (must be unique)
	 * @param inputStreams   streams to receive data from remote participants
	 * @param outputStreams  streams to send data to remote participants
	 * @param k              minimum number of participants required to uncover a permutation element
	 * @param size           size of the permutation
	 * @param keySize        number of bits in the public key prime factors
	 * @param proofIterations  number of iterations in all the proofs
	 */
	public Cryperm(
			int localId,
			InputStream[] inputStreams,
			OutputStream[] outputStreams,
			int k,
			int size,
			int keySize,
			int proofIterations) {

		super(localId, inputStreams, outputStreams);

		this.k = k;
		this.size = size;
		this.keySize = keySize;
		this.proofIterations = proofIterations;

		uncovered = new boolean[size];
	}


	/**
	 * Starts the protocol.
	 * <p>
	 * This is launched automatically by {@link eu.jergus.crypto.Protocol} when
	 * the object is initialized, in a separate thread.
	 */
	public void run() {
		// compute modulus for Shamir's scheme
		mod = Math.max(n, size);
		while (!Numbers.isPrime(mod)) ++mod;
		Log.log(Log.INFO, "Using modulus "+mod+" for Shamir's scheme.");

		// generate private key
		set("key", Paillier.generateKey(keySize));
		set("n", get("key[0]"));
		broadcast("n");

		runWithEach(new ParticipantAction() {
			public void run(RemoteParticipant p) {
				// wait for n, then compute the remaining key parts
				set("public_keys["+p.getId()+"]", Paillier.keyToArray(p.get("n")));

				// request proof of correctness
				for (int i = 0; i < proofIterations; ++i) {
					BigInteger x = Numbers.random(p.get("n"));
					BigInteger y = Numbers.randomRelativelyPrime(p.get("n"));
					BigInteger c = Paillier.encrypt(x, y, publicKey(p.getId()));
					set("challenge_x["+p.getId()+"]["+i+"]", x);
					set("challenge_y["+p.getId()+"]["+i+"]", y);
					set("challenge["+p.getId()+"]["+i+"]", c);
					p.send("challenge["+p.getId()+"]["+i+"]");
				}

				// verify the proof
				boolean ok = true;
				for (int i = 0; i < proofIterations; ++i) {
					ok &= Arrays.equals(get("challenge_x["+p.getId()+"]", proofIterations),
					                    p.get("response_x["+localId+"]", proofIterations));
					ok &= Arrays.equals(get("challenge_y["+p.getId()+"]", proofIterations),
					    		        p.get("response_y["+localId+"]", proofIterations));
				}
				if (ok) {
					set("is_key_valid["+p.getId()+"]", BigInteger.ONE);
				} else {
					Log.log(Log.WARN, "#"+p.getId()+" failed to prove key correctness.");
					kill();
				}
			}
		});

		// prove public key correctness to everyone
		runWithEach(new ParticipantAction() {
			public void run(RemoteParticipant p) {
				for (int i = 0; i < proofIterations; ++i) {
					BigInteger c = p.get("challenge["+localId+"]["+i+"]");
					BigInteger x = Paillier.decrypt(c, secretKey());
					BigInteger y = Paillier.decrypt2(c, secretKey());
					set("response_x["+p.getId()+"]["+i+"]", x);
					set("response_y["+p.getId()+"]["+i+"]", y);
					p.send("response_x["+p.getId()+"]["+i+"]");
					p.send("response_y["+p.getId()+"]["+i+"]");
				}
			};
		});

		// launch threads to verify everyone's proof when ready
		runWithEach(new ParticipantAction() {
			public void run(RemoteParticipant p) {
				if (verifyProof(p)) {
					set("is_zkp_valid["+p.getId()+"]", BigInteger.ONE);
				} else {
					Log.log(Log.WARN, "#"+p.getId()+" failed to prove permutation correctness.");
					kill();
				}
			}
		});

		// shuffle & mask
		Permutation perm = new Permutation(size);
		int[][] poly = new int[size][];
		for (int i = 0; i < size; ++i) {
			poly[i] = new Polynomial(mod, k, 0).values(n);
		}
		BigInteger[][] y = new BigInteger[size][n];

		BigInteger[][] elements, permuted;

		if (localId % 2 == 0) {
			// 1. receive
			if (localId == 0) {
				elements = generateInitialElements();
			} else {
				elements = participants[localId-1].get("permuted", size, n);
			}
			// 2. shuffle
			permuted = perm.apply(elements);
			// 3. mask & send
			for (int i = 0; i < size; ++i) {
				for (int j = 0; j < n; ++j) {
					y[i][j] = Numbers.randomRelativelyPrime(publicKey(j)[0]);
					set("permuted["+i+"]["+j+"]", mask(permuted[i][j], j, poly[i][j], y[i][j]));
					broadcast("permuted["+i+"]["+j+"]");
				}
			}
		} else {
			// 1. receive & mask
			elements = new BigInteger[size][n];
			permuted = new BigInteger[size][n];
			for (int i = 0; i < size; ++i) {
				for (int j = 0; j < n; ++j) {
					elements[i][j] = participants[localId-1].get("permuted["+i+"]["+j+"]");
					y[perm.rev(i)][j] = Numbers.randomRelativelyPrime(publicKey(j)[0]);
					permuted[i][j] = mask(elements[i][j], j, poly[perm.rev(i)][j], y[perm.rev(i)][j]);
				}
			}
			// 2. shuffle
			permuted = perm.apply(permuted);
			set("permuted", permuted);
			// 3. send
			broadcast("permuted", size, n);
		}

		generateProof(elements, get("permuted", size, n), perm, poly, y);

		// wait for all proofs to finish
		for (int i = 0; i < n; ++i) {
			if (i != localId) {
				get("is_key_valid["+i+"]");
				get("is_zkp_valid["+i+"]");
			}
		}

		if (localId == n-1) {
			set("elements", get("permuted", size, n));
		} else {
			set("elements", participants[n-1].get("permuted", size, n));
		}
		set("permutation_ready", BigInteger.ONE);
		Log.log(Log.INFO, "Permutation ready.");
	}


	/**
	 * Uncovers the next permutation element to the specified participant.
	 */
	public int uncover(int recipientId) throws ProtocolException {
		int elementId = 0;
		synchronized (uncovered) {
			while (elementId < size && uncovered[elementId]) {
				++elementId;
			}
			if (elementId == size) {
				throw new IllegalStateException("No more elements to uncover.");
			}
			uncovered[elementId] = true;
		}
		return uncoverElement(recipientId, elementId);
	}


	/**
	 * Uncovers the specified permutation element to the specified participant.
	 */
	public int uncover(int recipientId, int elementId) throws ProtocolException {
		if (elementId >= size) {
			throw new NoSuchElementException("Element "+elementId+" is not between 0 and size-1.");
		}
		synchronized (uncovered) {
			if (uncovered[elementId]) {
//				throw new IllegalStateException("Element "+elementId+" already uncovered.");
			}
			uncovered[elementId] = true;
		}
		return uncoverElement(recipientId, elementId);
	}


	/**
	 * Internal implementation used by {@link #uncover(int)} and
	 * {@link #uncover(int, int)}.
	 */
	private int uncoverElement(final int recipientId, final int elementId) throws ProtocolException {
		Log.log(Log.INFO, "#"+recipientId+" uncovering element "+elementId);

		// block until permutation is ready
		try {
			get("permutation_ready");
		} catch (DeadObjectException e) {
			throw new ProtocolException("Error while generating permutation.");
		}

		// recipient: get shares and compute the result
		if (recipientId == localId) {
			final int[][] points = new int[k][2];
			final int[] count = {1, 0};  // 0: valid points, 1: failures

			points[0][0] = localId+1;
			points[0][1] = Paillier.decrypt(get("elements["+elementId+"]["+localId+"]"), secretKey()).intValue();

			runWithEach(true, new ParticipantAction() {
				public void run(RemoteParticipant p) {
					boolean ok;
					BigInteger x = null;
					try {
						x = Paillier.decrypt(p.get("secret["+elementId+"][0]"), secretKey());
						BigInteger y = Paillier.decryptBig(p.get("secret["+elementId+"][1]"), secretKey());
						ok = Paillier.encrypt(x, y, publicKey(p.getId())).equals(get("elements["+elementId+"]["+p.getId()+"]"));
						if (!ok) {
							Log.log(Log.WARN, "#"+p.getId()+" failed to prove secret share correctness.");
						}
					} catch (DeadObjectException e) {
						ok = false;
					}
					synchronized (points) {
						if (ok) {
							points[count[0]][0] = p.getId()+1;
							points[count[0]][1] = x.intValue();
							++count[0];
						} else {
							++count[1];
						}
						points.notifyAll();
					}
				}
			});

			while (true) {
				synchronized (points) {
					if (count[0] >= k) {
						return Polynomial.interpolate(mod, points, 0);
					}
					if (count[1] > n-k) {
						throw new ProtocolException("Unable to get "+(k-1)+" secret shares from other participants.");
					}
					try {
						points.wait();
					} catch (InterruptedException e) {
					}
				}
			}

		// all other participants: send necessary data to the recipient
		} else {
			// decrypt
			BigInteger x = Paillier.decrypt(get("elements["+elementId+"]["+localId+"]"), secretKey());
			BigInteger y = Paillier.decrypt2(get("elements["+elementId+"]["+localId+"]"), secretKey());
			// re-encrypt for recipient
			set("secret["+elementId+"][0]", Paillier.encrypt(x, publicKey(recipientId)));
			set("secret["+elementId+"][1]", Paillier.encryptBig(y, publicKey(recipientId)));
			// send to recipient
			participants[recipientId].send("secret["+elementId+"]", 2);

			return -1;
		}
	}


	////////////////////////////////////////////////////////////////////////////
	// HELPER METHODS

	/**
	 * Access the specified participant's public key.
	 *
	 * @param id  participant's ID
	 * @return all public parts of the participant's key
	 */
	private BigInteger[] publicKey(int id) {
		if (id == localId) return get("key", Paillier.PUBLIC_FIELDS);
		return get("public_keys["+id+"]", Paillier.PUBLIC_FIELDS);
	}

	/**
	 * Access the local participant's secret key.
	 *
	 * @return all parts of the local participant's key
	 */
	private BigInteger[] secretKey() {
		return get("key", Paillier.FIELDS);
	}


	/**
	 * Generate initial encrypted element representations, which are then
	 * re-encrypted and shuffled by all players.
	 *
	 * @return the initial permutation matrix
	 */
	private BigInteger[][] generateInitialElements() {
		BigInteger[][] elements = new BigInteger[size][n];
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < n; ++j) {
				elements[i][j] = Paillier.encrypt(i, 1, publicKey(j));
			}
		}
		return elements;
	}


	/**
	 * `Mask' a secret share by adding the encrypted value of the given
	 * polynomial to the original encrypted value.
	 *
	 * @param c     original encrypted value
	 * @param id    ID of the secret share's owner
	 * @param poly  polynomial, value of which is to be added
	 * @return the new encrypted value
	 */
	private BigInteger mask(BigInteger c, int id, int maskingValue, BigInteger y) {
		return c.multiply(Paillier.encrypt(maskingValue, y, publicKey(id)))
		        .mod(publicKey(id)[Paillier.N2]);
	}


	/**
	 * `Mask' a complete permutation matrix using the given permutation, array
	 * of polynomials and a matrix of randomizing values.
	 */
	private BigInteger[][] permuteMatrix(BigInteger[][] matrix, Permutation perm, int[][] poly, BigInteger[][] y) {
		BigInteger[][] res = perm.apply(matrix);

		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < n; ++j) {
				res[i][j] = mask(res[i][j], j, poly[i][j], y[i][j]);
			}
		}

		return res;
	}


	/**
	 * Compute one-way hash function of the given array of matrices (received or
	 * generated as a part of the zero-knowledge permutation correctness proof.
	 */
	private boolean[] hashMatrices(BigInteger[][][] matrices) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(Arrays.deepToString(matrices).getBytes("US-ASCII"));
			boolean[] res = new boolean[proofIterations];
			for (int i = 0; i < proofIterations; ++i) {
				res[i] = ((hash[i/8] >> (i%8)) & 1) == 0;
			}
			return res;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Generate the non-interactive ZKP of permutation matrix correctness.
	 */
	private void generateProof(BigInteger[][] original, BigInteger[][] permuted, Permutation perm, int[][] poly, BigInteger[][] y) {
		BigInteger[][][] matrices = new BigInteger[proofIterations][size][n];
		Permutation[] perms = new Permutation[proofIterations];
		int[][][] polys = new int[proofIterations][size][];
		BigInteger[][][] ys = new BigInteger[proofIterations][size][n];

		for (int it = 0; it < proofIterations; ++it) {
			perms[it] = new Permutation(size);
			for (int i = 0; i < size; ++i) {
				polys[it][i] = new Polynomial(mod, k, 0).values(n);
			}
			for (int i = 0; i < size; ++i) {
				for (int j = 0; j < n; ++j) {
					ys[it][i][j] = Numbers.randomRelativelyPrime(publicKey(j)[0]);
				}
			}

			matrices[it] = permuteMatrix(permuted, perms[it], polys[it], ys[it]);

			set("zkp_matrices["+it+"]", matrices[it]);
			broadcast("zkp_matrices["+it+"]", size, n);
		}

		boolean[] challenges = hashMatrices(matrices);

		for (int it = 0; it < proofIterations; ++it) {
			if (challenges[it]) {
				set("zkp_perms["+it+"]", Numbers.toBigInteger(perms[it].get()));
				for (int i = 0; i < size; ++i) {
					set("zkp_polys["+it+"]["+i+"]", Numbers.toBigInteger(polys[it][i]));
				}
				set("zkp_ys["+it+"]", ys[it]);
			} else {
				set("zkp_perms["+it+"]", perms[it].apply(Numbers.toBigInteger(perm.get())));
				for (int i = 0; i < size; ++i) {
					set("zkp_polys["+it+"]["+i+"]", Numbers.toBigInteger(Numbers.sum(poly[perms[it].get(i)], polys[it][i])));
				}
				for (int i = 0; i < size; ++i) {
					for (int j = 0; j < n; ++j) {
						set("zkp_ys["+it+"]["+i+"]["+j+"]", y[perms[it].get(i)][j].multiply(ys[it][i][j]).mod(publicKey(j)[0]));
					}
				}
			}
			broadcast("zkp_perms["+it+"]", size);
			broadcast("zkp_polys["+it+"]", size, n);
			broadcast("zkp_ys["+it+"]", size, n);
		}
	}


	/**
	 * Verify a permutation correctness proof received from the specified
	 * participant.
	 *
	 * @return true if the proof is correct
	 */
	private boolean verifyProof(RemoteParticipant p) {
		BigInteger[][][] matrices = new BigInteger[proofIterations][][];
		for (int it = 0; it < proofIterations; ++it) {
			matrices[it] = p.get("zkp_matrices["+it+"]", size, n);
		}
		boolean[] challenges = hashMatrices(matrices);

		BigInteger[][] compare1 = p.get("permuted", size, n);
		BigInteger[][] compare0;
		if (p.getId() == 0) {
			compare0 = generateInitialElements();
		} else {
			if (p.getId()-1 == localId) {
				compare0 = get("permuted", size, n);
			} else {
				compare0 = participants[p.getId()-1].get("permuted", size, n);
			}
		}

		for (int it = 0; it < proofIterations; ++it) {
			Permutation perm;
			try {
				perm = new Permutation(Numbers.toInt(p.get("zkp_perms["+it+"]", size)));
			} catch (IllegalArgumentException e) {
				return false;
			}

			int[][] poly = new int[size][];
			for (int i = 0; i < size; ++i) {
				poly[i] = Numbers.toInt(p.get("zkp_polys["+it+"]["+i+"]", n));
				for (int j = 0; j < n; ++j) {
					if (challenges[it] && poly[i][j] > mod-1) return false;
					if (!challenges[it] && poly[i][j] > 2*mod-2) return false;
				}
				if (Polynomial.interpolate(mod, poly[i], 0) != 0) {
					return false;
				}

			}

			BigInteger[][] y = p.get("zkp_ys["+it+"]", size, n);

			BigInteger[][] permuted = permuteMatrix(
					challenges[it] ? compare1 : compare0,
					perm, poly, y);

			if (!Arrays.deepEquals(matrices[it], permuted)) {
				return false;
			}
		}

		return true;
	}

}
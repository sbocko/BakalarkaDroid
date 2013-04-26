package eu.jergus.crypto.util;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import eu.jergus.crypto.exception.DeadObjectException;

/**
 * Provides its subclasses with capabilities to store and query data.
 * <p>
 * Transparently handles synchronization.
 */
public abstract class DataBackedObject {

	/**
	 * Thrown when attempting to assign a new value to an already existing key.
	 */
	protected static class DuplicateKeyException extends RuntimeException {
		private String key;

		private DuplicateKeyException(String key) {
			super("Duplicate key: " + key);
		}

		public String getKey() {
			return key;
		}
	}


	private Map<String, BigInteger> data = new HashMap<String, BigInteger>();

	private boolean dead = false;


	/**
	 * Marks this object as dead.
	 * <p>
	 * This causes all waiting {@link #get} calls to immediately fail. All
	 * subsequent {@link #get} calls also fail, unless the requested key is set.
	 */
	synchronized public void kill() {
		dead = true;
		notifyAll();
	}


	/**
	 * Assign the specified value to the given key.
	 */
	synchronized protected void set(String key, BigInteger value) {
		if (data.containsKey(key)) {
			throw new DuplicateKeyException(key);
		}
		data.put(key, value);
		notifyAll();
	}


	/**
	 * Return the value assigned to the specified key.
	 * <p>
	 * If no such value exists, the thread is blocked until it becomes
	 * available.
	 * <p>
	 * However, if the object is dead, then an exception is thrown instead of
	 * blocking the thread.
	 *
	 * @throws DeadObjectException  if this object is dead
	 */
	synchronized public BigInteger get(String key) throws DeadObjectException {
		while (!data.containsKey(key)) {
			if (dead) throw new DeadObjectException();
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		return data.get(key);
	}


	/**
	 * Set an array of values.
	 */
	protected void set(String key, BigInteger[] array) {
		for (int i = 0; i < array.length; ++i) {
			set(key+"["+i+"]", array[i]);
		}
	}

	/**
	 * Set a 2D array of values.
	 */
	protected void set(String key, BigInteger[][] array) {
		for (int i = 0; i < array.length; ++i) {
			set(key+"["+i+"]", array[i]);
		}
	}

	/**
	 * Get an array of values.
	 */
	public BigInteger[] get(String key, int length) throws DeadObjectException {
		BigInteger[] res = new BigInteger[length];
		for (int i = 0; i < length; ++i) {
			res[i] = get(key+"["+i+"]");
		}
		return res;
	}

	/**
	 * Get a 2D array of values.
	 */
	public BigInteger[][] get(String key, int length, int length2) throws DeadObjectException {
		BigInteger[][] res = new BigInteger[length][];
		for (int i = 0; i < length; ++i) {
			res[i] = get(key+"["+i+"]", length2);
		}
		return res;
	}

}
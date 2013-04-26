package eu.jergus.crypto.exception;

/**
 * Thrown when calling {@link eu.jergus.crypto.util.DataBackedObject#get} on a
 * dead object.
 */
public class DeadObjectException extends RuntimeException {

	public DeadObjectException() {
	}

	public DeadObjectException(String message) {
		super(message);
	}

	public DeadObjectException(Throwable cause) {
		super(cause);
	}

	public DeadObjectException(String message, Throwable cause) {
		super(message, cause);
	}

}
package eu.jergus.crypto.exception;

/**
 * A checked exception thrown when any failure occurs during the protocol run.
 */
public class ProtocolException extends Exception {

	public ProtocolException() {
	}

	public ProtocolException(String message) {
		super(message);
	}

	public ProtocolException(Throwable cause) {
		super(cause);
	}

	public ProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

}
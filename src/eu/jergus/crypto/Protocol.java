package eu.jergus.crypto;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import eu.jergus.crypto.util.DataBackedObject;
import eu.jergus.crypto.util.Log;

/**
 * This class should be extended by all protocol implementations.
 */
public abstract class Protocol extends DataBackedObject implements Runnable {

	/**
	 * Number of participants.
	 */
	protected final int n;

	/**
	 * Index of the local participant.
	 */
	protected final int localId;

	/**
	 * All remote participants (indexes are equal to participants' IDs).
	 * <p>
	 * Note that participants[localId] is null.
	 */
	protected final RemoteParticipant[] participants;


	/**
	 * Initializes the instance variables and creates the {@link #participants}
	 * array.
	 * @param localId        ID of the local participant
	 * @param inputStreams   streams for receiving data from the participants
	 * @param outputStreams  streams for sending data to the participants
	 */
	protected Protocol(int localId, InputStream[] inputStreams, OutputStream[] outputStreams) {
		this.localId = localId;
		n = inputStreams.length;
		participants = new RemoteParticipant[n];

		if (inputStreams.length != outputStreams.length) {
			throw new IllegalArgumentException("InputStream count different from OutputStream count.");
		}

		if (localId < 0 || localId >= n) {
			throw new IllegalArgumentException("localId must be between 0 and n-1");
		}

		for (int i = 0; i < n; ++i) {
			if (i != localId) {
				participants[i] = new RemoteParticipant(i, inputStreams[i], outputStreams[i], this);
			}
		}

		Thread t = new Thread(this);
		t.setUncaughtExceptionHandler(exceptionHandler);
		t.start();
	}


	/**
	 * Performs the given action with all remote participants, in parallel.
	 *
	 * @param daemon  true if the threads should be started as daemon threads
	 *                (ie. not blocking the exit of the application)
	 */
	protected void runWithEach(boolean daemon, final ParticipantAction action) {
		for (final RemoteParticipant p : participants) {
			if (p != null) {
				Thread t = new Thread() {
					public void run() {
						action.run(p);
					};
				};
				t.setDaemon(daemon);
				t.setUncaughtExceptionHandler(exceptionHandler);
				t.start();
			}
		}
	}


	/**
	 * Performs the given action with all remote participants, in parallel.
	 */
	protected void runWithEach(ParticipantAction action) {
		runWithEach(false, action);
	}


	/**
	 * Handles uncaught exceptions from all threads launched by this class.
	 * <p>
	 * By default, this kills the Protocol object if any such exception is
	 * thrown.
	 */
	protected Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread t, Throwable e) {
			kill();
		}
	};


	/**
	 * Adds logging capability to {@link util.DataBackedObject#set}.
	 */
	protected void set(String key, BigInteger value) {
		super.set(key, value);
		Log.log(Log.CALC, key+" = "+Log.bigInteger(value));
	}


	/**
	 * Send the specified values to all remote participants, in parallel.
	 */
	public void broadcast(final String... keys) {
		runWithEach(new ParticipantAction() {
			public void run(RemoteParticipant p) {
				p.send(keys);
			}
		});
	}

	/**
	 * Broadcast an array of values.
	 */
	public void broadcast(final String key, final int length) {
		runWithEach(new ParticipantAction() {
			public void run(RemoteParticipant p) {
				for (int i = 0; i < length; ++i) {
					p.send(key+"["+i+"]");
				}
			}
		});
	}

	/**
	 * Broadcast a 2D array of values.
	 */
	public void broadcast(final String key, final int length, final int length2) {
		runWithEach(new ParticipantAction() {
			public void run(RemoteParticipant p) {
				for (int i = 0; i < length; ++i) {
					p.send(key+"["+i+"]", length2);
				}
			}
		});
	}

}
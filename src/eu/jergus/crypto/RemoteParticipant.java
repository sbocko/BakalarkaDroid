package eu.jergus.crypto;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import eu.jergus.crypto.util.DataBackedObject;
import eu.jergus.crypto.util.Log;

/**
 * Represents one of the remote participants of a protocol. Stores all data
 * received from this participant and handles communication with the
 * participant.
 */
public class RemoteParticipant extends DataBackedObject {

	private int id;
	private InputStream in;
	private OutputStream out;

	/**
	 * The Protocol object that created this Participant object.
	 */
	private Protocol protocol;

	/**
	 * Initialize a remote participant.
	 * <p>
	 * Package-only visibility (all participants are initialized by the Protocol
	 * class).
	 */
	RemoteParticipant(int id, InputStream in, OutputStream out, Protocol protocol) {
		this.id = id;
		this.in = in;
		this.out = new BufferedOutputStream(out, 65532);
		// buffer size aligned to OutputStreamMultiplexer maximum length
		this.protocol = protocol;

		startReceivingThread();
	}

	public int getId() {
		return id;
	}

	/**
	 * Starts a background thread that continually receives data from this
	 * participant's InputStream.
	 */
	private void startReceivingThread() {
		Thread t = new Thread() {
			public void run() {
				try {
					while (true) {
						StringBuilder key = new StringBuilder();
						while (true) {
							int b = in.read();
							if (b == -1) throw new IOException();
							if (b == 0) break;
							key.append((char) b);
						}
						if (key.length() == 0) throw new IOException();

						int len = 0;
						for (int i = 0; i < 4; ++i) {
							int b = in.read();
							if (b == -1) throw new IOException();
							len = (len << 8) + b;
						}

						byte[] b = new byte[len];
						if (in.read(b) != len) throw new IOException();

						BigInteger value = new BigInteger(b);
						try {
							set(key.toString(), value);
							Log.log(Log.RECV, "#"+id+"  "+key+" = "+Log.bigInteger(value));
						} catch (DuplicateKeyException e) {
							Log.log(Log.WARN, "Duplicate key "+key+" received from #"+id);
						}
					}
				} catch (IOException e) {
					Log.log(Log.WARN, "Lost connection to #"+id+".");
					kill();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}


	/**
	 * Sends the specified values from local (protocol) data to this
	 * participant.
	 */
	public void send(String... keys) {
		for (String key : keys) {
			if (!key.matches("^[\\[\\]_0-9a-zA-Z]+$")) {
				throw new IllegalArgumentException("Invalid key.");
			}
			byte[] value = protocol.get(key).toByteArray();

			try {
				synchronized (out) {
					for (int i = 0; i < key.length(); ++i) {
						out.write(key.charAt(i));
					}
					out.write(0);

					out.write(value.length >> 24);
					out.write(value.length >> 16);
					out.write(value.length >> 8);
					out.write(value.length);
					out.write(value);

					out.flush();
				}
				Log.log(Log.SEND, key+" -> #"+id);
			} catch (IOException e) {
				Log.log(Log.WARN, "Failed to send "+key+" to #"+id+".");
				kill();
			}
		}
	}


	/**
	 * Send an array of values to this participant.
	 */
	public void send(String key, int length) {
		for (int i = 0; i < length; ++i) {
			send(key+"["+i+"]");
		}
	}

	/**
	 * Send a 2D array of values to this participant.
	 */
	public void send(String key, int length, int length2) {
		for (int i = 0; i < length; ++i) {
			send(key+"["+i+"]", length2);
		}
	}

}
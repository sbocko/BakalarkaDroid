package eu.jergus.crypto.util.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Combines two or more output streams into a single target output stream.
 */
public class OutputStreamMultiplexer {

	/**
	 * One of the combined partial streams.
	 */
	private class MultiplexOutputStream extends OutputStream {
		private int id;

		private MultiplexOutputStream(int id) {
			this.id = id;
		}

		public void write(int b) throws IOException {
			writeToTarget(id, b);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			writeToTarget(id, b, off, len);
		}
	}


	private OutputStream target;
	private Map<Integer, OutputStream> streams = new HashMap<Integer, OutputStream>();

	private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();

	/**
	 * True when {@link #close} was called.
	 */
	private boolean closed = false;

	/**
	 * True when the stream is actually closed.
	 */
	private boolean streamClosed = false;

	/**
	 * Used to notify any waiting {@link #close} calls that the stream is now
	 * closed.
	 */
	private Object streamClosedMonitor = new Object();

	/**
	 * An exception thrown by the underlining output stream. Rethrown by the
	 * next attempt to write to any of the partial streams.
	 */
	private IOException exception;

	/**
	 * A background thread used for sending the queued data.
	 */
	private Thread sendingThread;

	/**
	 * System time of the next scheduled flush.
	 */
	private long nextFlush = -1;


	/**
	 * Create a new multiplexer which will send combined data from the partial
	 * partial streams to the provided target output stream.
	 * <p>
	 * This automatically launches a background thread which will be used for
	 * sending the data, and another background thread to periodically flush the
	 * target stream (to prevent deadlocks).
	 *
	 * @param tgt  the target output stream
	 */
	public OutputStreamMultiplexer(final OutputStream tgt) {
		this.target = tgt;

		sendingThread = new Thread() {
			public void run() {
				try {
					while (true) {
						if (closed && queue.isEmpty()) break;
						try {
							byte[] b = queue.take();
							synchronized (target) {
								target.write(b);
								nextFlush = System.currentTimeMillis() + 500;
							}
						} catch (InterruptedException e) {}
					}
				} catch (IOException e) {
					exception = e;
				}
				if (closed) {
					try {
						target.close();
					} catch (IOException e) {}
				}
				synchronized (streamClosedMonitor) {
					streamClosed = true;
					streamClosedMonitor.notifyAll();
				}
			}
		};
		sendingThread.start();

		Thread flushingThread = new Thread() {
			public void run() {
				while (true) {
					synchronized (target) {
						if (nextFlush != -1 && System.currentTimeMillis() > nextFlush) {
							try {
								target.flush();
								nextFlush = -1;
							} catch (IOException e) {}
						}
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				}
			};
		};
		flushingThread.setDaemon(true);
		flushingThread.start();
	}


	/**
	 * Returns the specified partial output stream.
	 *
	 * @param id  ID of the partial stream (0 to 255)
	 * @return    the partial stream
	 */
	public OutputStream getOutputStream(int id) {
		if (id < 0 || id >= 256) {
			throw new IllegalArgumentException("Multiplexing stream ID must be between 0 and 255.");
		}

		if (!streams.containsKey(id)) {
			streams.put(id, new MultiplexOutputStream(id));
		}

		return streams.get(id);
	}


	/**
	 * Request to close the target output stream.
	 * <p>
	 * All queued write calls are completed before closing. This method blocks
	 * until all of these calls finish and the stream is actually closed.
	 */
	synchronized public void close() {
		closed = true;
		sendingThread.interrupt();
		synchronized (streamClosedMonitor) {
			if (streamClosed) return;
			try {
				streamClosedMonitor.wait();
			} catch (InterruptedException e) {}
		}
	}


	synchronized private void writeToTarget(int id, int b) throws IOException {
		if (closed) throw new IOException("Stream closed.");
		if (exception != null) throw exception;
		queue.add(new byte[] {(byte) (id&255), 0, 1, (byte) (b&255)});
	}


	synchronized private void writeToTarget(int id, byte[] b, int off, int len) throws IOException {
		if (closed) throw new IOException("Stream closed.");
		if (exception != null) throw exception;
		for (int cur = off; cur < off+len; cur += 65535) {
			int curLen = Math.min(65535, off+len-cur);
			byte[] q = new byte[curLen+3];
			q[0] = (byte) (id & 255);
			q[1] = (byte) ((curLen >> 8) & 255);
			q[2] = (byte) (curLen & 255);
			System.arraycopy(b, cur, q, 3, curLen);
			queue.add(q);
		}
	}

}
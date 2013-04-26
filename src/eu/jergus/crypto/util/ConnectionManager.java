package eu.jergus.crypto.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import eu.jergus.crypto.util.stream.InputStreamDemultiplexer;
import eu.jergus.crypto.util.stream.OutputStreamMultiplexer;


/**
 * Initializes TCP connections to all specified addresses.
 * <p>
 * One index is specified as a `local ID'. No connection is established to the
 * address at this index.
 * <p>
 * A ConnectionManager object must be running at each of the given addresses,
 * and for each of these ConnectionManagers, the specified localId must be
 * different.
 */
public class ConnectionManager {

	/**
	 * Number of participants.
	 */
	private int n;

	/**
	 * Any exception thrown by the connecting threads. This is rethrown by the
	 * connection manager.
	 */
	IOException exception = null;

	Socket[] sockets;

	InputStreamDemultiplexer[] ism;
	OutputStreamMultiplexer[] osm;

	InputStream[][] inputStreams;
	OutputStream[][] outputStreams;


	/**
	 * Initialize connections to the specified addresses.
	 *
	 * @param localId       ID of the local participant (whose address is ignored)
	 * @param addresses     array of participants' addressess (in address:port format)
	 * @throws IOException  any exception thrown while connecting
	 */
	public ConnectionManager(final int localId, final String[] addresses) throws IOException {
		this(localId, addresses, 1);
	}


	/**
	 * Initialize connections to the specified addresses and provide the
	 * specified number of input/output streams for each connection.
	 * <p>
	 * Only one connection to each participant is established. The resulting
	 * input and output stream is then split into the required partial streams
	 * using {@link stream.InputStreamDemultiplexer} and
	 * {@link stream.OutputStreamMultiplexer}.
	 *
	 * @param localId       ID of the local participant (whose address is ignored)
	 * @param addresses     array of participants' addressess (in address:port format)
	 * @param multiplex     number of input/output streams required for each connection
	 * @throws IOException  any exception thrown while connecting
	 */
	public ConnectionManager(final int localId, final String[] addresses, int multiplex) throws IOException {
		n = addresses.length;
		sockets = new Socket[n];

		if (localId < 0 || localId >= n) {
			throw new IllegalArgumentException("localId must be between 0 and addresses.length-1.");
		}
		if (multiplex < 1 || multiplex > 256) {
			throw new IllegalArgumentException("Multiplex ratio must be between 1 and 256.");
		}

		// connect to all lower-ID players
		Thread[] threads = new Thread[localId];
		for (int i = 0; i < localId; ++i) {
			final int cur = i;
			threads[i] = new Thread() {
				public void run() {
					String address = addresses[cur].split(":")[0];
					int port = Integer.parseInt(addresses[cur].split(":")[1]);

					while (true) {
						try {
							sockets[cur] = new Socket(address, port);
							break;
						} catch (IOException e) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException ie) {
							}
						}
					}

					try {
						sockets[cur].getOutputStream().write(localId >> 8);
						sockets[cur].getOutputStream().write(localId);

						Log.print("["+cur+"]");
					} catch (IOException e) {
						exception = e;
					}
				}
			};
			threads[i].start();
		}
		
		

		// wait for connection from all higher-ID players
		int port = Integer.parseInt(addresses[localId].split(":")[1]);

		ServerSocket ss = new ServerSocket(port);
		for (int i = 0; i < n-localId-1; ++i) {
			Socket s = ss.accept();
			int idHi = s.getInputStream().read();
			if (idHi == -1) throw new IOException("Unexpected end of stream.");
			int idLo = s.getInputStream().read();
			if (idLo == -1) throw new IOException("Unexpected end of stream.");
			int id = (idHi << 8) + idLo;

			if (id <= localId || id >= n || sockets[id] != null) {
				throw new IOException("Unexpected connection from ID "+id+".");
			}

			sockets[id] = s;
			Log.print("["+id+"]");
		}

		ss.close();

		// wait for everything to finish
		for (int i = 0; i < localId; ++i) {
			while (true) {
				try {
					threads[i].join();
					break;
				} catch (InterruptedException e) {
				}
			}
		}
		Log.println("");

		if (exception != null) {
			throw exception;
		}

		// prepare streams
		ism = new InputStreamDemultiplexer[n];
		osm = new OutputStreamMultiplexer[n];

		inputStreams = new InputStream[n][multiplex];
		outputStreams = new OutputStream[n][multiplex];

		for (int i = 0; i < n; ++i) {
			if (i != localId) {
				if (multiplex == 1) {
					inputStreams[i][0] = sockets[i].getInputStream();
					outputStreams[i][0] = sockets[i].getOutputStream();
				} else {
					ism[i] = new InputStreamDemultiplexer(sockets[i].getInputStream());
					osm[i] = new OutputStreamMultiplexer(sockets[i].getOutputStream());
					for (int j = 0; j < multiplex; ++j) {
						inputStreams[i][j] = ism[i].getInputStream(j);
						outputStreams[i][j] = osm[i].getOutputStream(j);
					}
				}
			}
		}
	}


	/**
	 * Get the first partial input stream of the specified participant.
	 */
	public InputStream getInputStream(int id) {
		return inputStreams[id][0];
	}

	/**
	 * Get the specified partial input stream of the specified participant.
	 */
	public InputStream getInputStream(int id, int streamId) {
		return inputStreams[id][streamId];
	}

	/**
	 * Get all partial input streams of the specified participant in an array.
	 */
	public InputStream[] getInputStreams(int id) {
		return inputStreams[id];
	}

	/**
	 * Get an array with each participant's partial input stream with the
	 * specified ID.
	 */
	public InputStream[] getPartialInputStreams(int streamId) {
		InputStream[] res = new InputStream[n];
		for (int i = 0; i < n; ++i) {
			res[i] = inputStreams[i][streamId];
		}
		return res;
	}


	/**
	 * Get the first partial output stream of the specified participant.
	 */
	public OutputStream getOutputStream(int id) {
		return outputStreams[id][0];
	}

	/**
	 * Get the specified partial output stream of the specified participant.
	 */
	public OutputStream getOutputStream(int id, int streamId) {
		return outputStreams[id][streamId];
	}

	/**
	 * Get all partial output streams of the specified participant in an array.
	 */
	public OutputStream[] getOutputStreams(int id) {
		return outputStreams[id];
	}

	/**
	 * Get an array with each participant's partial output stream with the
	 * specified ID.
	 */
	public OutputStream[] getPartialOutputStreams(int streamId) {
		OutputStream[] res = new OutputStream[n];
		for (int i = 0; i < n; ++i) {
			res[i] = outputStreams[i][streamId];
		}
		return res;
	}


	/**
	 * Close all streams and sockets opened by this ConnectionManager.
	 */
	public void close() {
		for (InputStreamDemultiplexer i : ism) {
			if (i != null) {
				i.close();
			}
		}
		for (OutputStreamMultiplexer o : osm) {
			if (o != null) {
				o.close();
			}
		}
		for (Socket s : sockets) {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {}
			}
		}
	}

}
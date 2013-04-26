package sk.upjs.bocko.protocol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GameInvitation {
	private final int SERVER_PORT = 4030;
	private final int HOST_PORT = 4031;
	private final String INVITATION_TAG = "inv";
	private final String ACCEPT_TAG = "acc";
	// socket timeout interval in milliseconds
	private final int WAIT_TIME = 30000;
	private List<PlayerIpKeyPair> players = new ArrayList<PlayerIpKeyPair>();
	private RSAcipher rsaCipher;

	public GameInvitation(RSAcipher rsaCipher) {
		this.rsaCipher = rsaCipher;
	}

	// ip adresa v tvare xxx.yyy.zzz.aaa
	@SuppressWarnings("deprecation")
	public PlayerIpKeyPair inviteToGame(String ipAddress)
			throws SocketException {
		// send invitations
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
			final String ip = new String(ipAddress);

			// poslanie pozvanok
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Socket s = new Socket(ip, HOST_PORT);
						System.out.println("posielam pozvanku na " + ip + ":"
								+ HOST_PORT);
						DataOutputStream os = new DataOutputStream(
								s.getOutputStream());
						os.writeBytes(INVITATION_TAG);
						os.flush();
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			});
			t.run();

			// prijatie odpovede
			PlayerIpKeyPair result = new PlayerIpKeyPair();
			String accControl;

			serverSocket.setSoTimeout(WAIT_TIME);
			Socket response = serverSocket.accept();
			// BufferedReader br = new BufferedReader(new InputStreamReader(
			// response.getInputStream()));
			DataInputStream in = new DataInputStream(response.getInputStream());
			// accControl = br.readLine();
			accControl = in.readLine();
			int dataSize = in.readInt();
			byte[] publicKey = new byte[dataSize];
			in.read(publicKey);
			result.setPublicKey(publicKey);
			// result.setPublicKey(br.readLine());
			if (accControl.equals(ACCEPT_TAG)) {
				// zapisanie vlastnej adresy
				if (players.size() == 0) {
					String myIP = response.getLocalAddress().getHostAddress()
							+ ":";
					players.add(new PlayerIpKeyPair(myIP, rsaCipher.getPublic()));
				}

				// spracovanie akceptacie
				System.out.println("Hrac akceptoval ziadost.");
				result.setIpAddress(response.getInetAddress() + ":");
				result.setIpAddress(result.getIpAddress().substring(1));
				players.add(result);
				DataOutputStream os = new DataOutputStream(
						response.getOutputStream());
				// poslem hracovi jeho localID
				os.writeInt(players.size() - 1);
				os.flush();
				response.close();
			} else {
				result = null;
				System.out.println("prislo nieco ine");
			}

			serverSocket.close();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// returns available game server
	public String getInvitation() throws SocketTimeoutException {
		String invitation = null;

		// System.out.println("waiting for invitations");
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(HOST_PORT);
			serverSocket.setSoTimeout(WAIT_TIME);
			Socket response = serverSocket.accept();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					response.getInputStream()));
			invitation = br.readLine();
			if (invitation.equals(INVITATION_TAG)) {
				invitation = response.getInetAddress().toString();
				invitation = invitation.substring(1);
			}
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("-pozvanka od: " + invitation);
		return invitation;
	}

	// accept invitation and send public key
	// returns player id
	public int acceptInvitation(String invitation) {
		System.out.println("odpovedam na ziadost o hru");
		int result = -1;
		try {
			Socket s = new Socket(invitation, SERVER_PORT);
			DataOutputStream os = new DataOutputStream(s.getOutputStream());
			os.writeBytes(ACCEPT_TAG + "\n");
			os.writeInt(rsaCipher.getPublic().length);
			os.write(rsaCipher.getPublic());
			os.flush();

			DataInputStream dis = new DataInputStream(s.getInputStream());
			result = dis.readInt();
			s.close();
			System.out.println("akceptovane...");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<PlayerIpKeyPair> getPlayers() {
		return players;
	}

	// receives player addresses and their public keys
	public void receivePlayerAddresses() throws SocketTimeoutException{
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(HOST_PORT);
			serverSocket.setSoTimeout(WAIT_TIME);
			Socket s = serverSocket.accept();
			DataInputStream in = new DataInputStream(s.getInputStream());
			int numberOfPlayers = in.readInt();
			// System.out.println("CITAM POCET HRACOV: " + numberOfPlayers);
			for (int i = 0; i < numberOfPlayers; i++) {
				int dataSize = in.readInt();
				// System.out.println("CITAM VELKOST IP: " + dataSize);
				byte[] data = new byte[dataSize];
				in.read(data);
				// System.out.println("CITAM IP: " + new String(data));
				String ip = new String(data);
				dataSize = in.readInt();
				// System.out.println("CITAM VELKOST KLUCA: " + dataSize);
				data = new byte[dataSize];
				in.read(data);
				// System.out.println("CITAM KLUC: " + Arrays.toString(data));
				players.add(new PlayerIpKeyPair(ip, data));
				// System.out.println("-hrac: " + players.get(i));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// sends player addresses and their public keys
	public void sendPlayerAddresses() {
		try {
			for (int i = 1; i < players.size(); i++) {
				final int j = i;
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						Scanner scanner = new Scanner(players.get(j)
								.getIpAddress());
						scanner.useDelimiter(":");
						String ip = scanner.next();
						scanner.close();

						try {
							Socket s = new Socket(ip, HOST_PORT);
							DataOutputStream dos = new DataOutputStream(s
									.getOutputStream());
							// send number of players
							dos.writeInt(players.size());
							// System.out.println("ZAPISUJEM POCET HRACOV: " +
							// players.size());
							// String stringToSend = "";
							for (int k = 0; k < players.size(); k++) {
								// stringToSend += players.get(k).getIpAddress()
								// + "\n" + players.get(k).getPublicKey()
								// + "\n";
								// System.out.println("ZAPISUJEM DLZKU IP: " +
								// players.get(k).getIpAddress().length());
								dos.writeInt(players.get(k).getIpAddress()
										.length());
								byte[] data = players.get(k).getIpAddress()
										.getBytes();
								// System.out.println("ZAPISUJEM IP: " +
								// players.get(k).getIpAddress());
								dos.write(data);
								// System.out.println("ZAPISUJEM DLZKU KLUCA: "
								// + players.get(k).getPublicKey().length);
								dos.writeInt(players.get(k).getPublicKey().length);
								// System.out.println("ZAPISUJEM KLUC: " +
								// Arrays.toString(players.get(k).getPublicKey()));
								dos.write(players.get(k).getPublicKey());
							}
							// System.out.println("--sending: " + stringToSend +
							// " ---sent");
							// dos.writeBytes(stringToSend);
							dos.flush();
							s.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				t.run();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public byte[][] getPublicKeys() {
		byte[][] result = new byte[players.size()][];
		for (int i = 0; i < result.length; i++) {
			result[i] = players.get(i).getPublicKey();
		}
		return result;
	}
}
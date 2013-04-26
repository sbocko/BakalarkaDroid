package sk.upjs.bocko.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.jergus.crypto.util.ConnectionManager;

public class MessageSender {
	private final ConnectionManager cm;
	private int playerCount;
	private int localId;
	private AEScipher aesCipher;
	private RSAkeys rsaKeys;

	public MessageSender(ConnectionManager cm, RSAkeys rsaKeys,
			int playerCount, int localId) {
		this.cm = cm;
		this.rsaKeys = rsaKeys;
		this.playerCount = playerCount;
		this.localId = localId;

		SessionKeyGenerator keyGenerator = new SessionKeyGenerator(this,
				localId, playerCount);
		aesCipher = new AEScipher(keyGenerator);
	}

	public MessageSender(ConnectionManager cm, AEScipher aesCipher,
			RSAkeys rsaKeys, int playerCount, int localId) {
		this.cm = cm;
		this.aesCipher = aesCipher;
		this.rsaKeys = rsaKeys;
		this.playerCount = playerCount;
		this.localId = localId;
	}

	private void sendSignedBytes(byte[] data, int playerId) {
		OutputStream out = cm.getOutputStream(playerId);
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeInt(data.length);
			dos.write(data);
			byte[] signature = rsaKeys.sign(new String(data));
			dos.writeInt(signature.length);
			dos.write(signature);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendSignedMessage(String message, int playerId) {
		sendSignedBytes(message.getBytes(), playerId);
	}

	public void sendSignedMessageToAll(String message) {
		byte[] signature = rsaKeys.sign(message);
		for (int i = 0; i < playerCount; i++) {
			if (i != localId) {
				sendInsecuredMessage(message, i);
				sendInsecuredBytes(signature, i);
			}
		}
	}

	private byte[] receiveSignedBytes(int playerId, boolean verify) {
		byte[] message = readInsecuredBytes(playerId);
		byte[] signature = readInsecuredBytes(playerId);
		if (!verify || rsaKeys.verify(playerId, new String(message), signature)) {
			return message;
		}
		return null;
	}

	public String receiveSignedMessage(int playerId, boolean verify) {
		byte[] data = receiveSignedBytes(playerId, verify);
		if (data != null) {
			return new String(data);
		}
		return null;
	}

	public byte[] readBytes(int playerId) {
		try {
			InputStream in = cm.getInputStream(playerId);
			DataInputStream dis = new DataInputStream(in);

			int len = dis.readInt();
			byte[] data = new byte[len];
			if (len > 0) {
				dis.readFully(data);
			}
			return aesCipher.decrypt(data);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void sendMessage(String msgToSend, int playerId) {
		sendInsecuredBytes(aesCipher.encrypt(msgToSend.getBytes()), playerId);
	}

	public void sendMessageToAll(String msgToSend) {
		for (int i = 0; i < playerCount; i++) {
			if (i != localId) {
				sendMessage(msgToSend, i);
			}
		}
	}

	public String receiveMessage(int id) {
		return new String(aesCipher.decrypt(readInsecuredBytes(id)));
	}

	public void sendBytes(byte[] myByteArray, int playerId) {
		byte[] encrypted = aesCipher.encrypt(myByteArray);
		sendBytes(encrypted, 0, encrypted.length, playerId);
	}

	public void sendInsecuredBytes(byte[] myByteArray, int playerId) {
		sendBytes(myByteArray, 0, myByteArray.length, playerId);
	}

	private void sendBytes(byte[] myByteArray, int start, int len, int playerId) {
		try {
			if (len < 0)
				throw new IllegalArgumentException(
						"Negative length not allowed");
			if (start < 0 || start >= myByteArray.length)
				throw new IndexOutOfBoundsException("Out of bounds: " + start);

			OutputStream out = cm.getOutputStream(playerId);
			DataOutputStream dos = new DataOutputStream(out);

			dos.writeInt(len);
			if (len > 0) {
				dos.write(myByteArray, start, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public byte[] readInsecuredBytes(int playerId) {
		try {
			InputStream in = cm.getInputStream(playerId);
			DataInputStream dis = new DataInputStream(in);

			int len = dis.readInt();
			byte[] data = new byte[len];
			if (len > 0) {
				dis.readFully(data);
			}
			return data;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void sendInsecuredMessage(String msgToSend, int playerId) {
		sendInsecuredBytes(msgToSend.getBytes(), playerId);
	}

	public void sendInsecuredMessageToAll(String msgToSend) {
		for (int i = 0; i < playerCount; i++) {
			if (i != localId) {
				sendInsecuredMessage(msgToSend, i);
			}
		}
	}

	public String receiveInsecuredMessage(int id) {
		return new String(readInsecuredBytes(id));
	}

	public AEScipher getAesCipher() {
		return aesCipher;
	}

}

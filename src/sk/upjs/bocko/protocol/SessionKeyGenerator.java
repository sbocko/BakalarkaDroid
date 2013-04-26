package sk.upjs.bocko.protocol;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;

public class SessionKeyGenerator {
	private MessageSender messageSender;
	private int localId;
	private int playerCount;
	private DiffieHellman diffieHellman;

	public SessionKeyGenerator(MessageSender messageSender, int localId,
			int playerCount) {
		this.messageSender = messageSender;
		this.localId = localId;
		this.playerCount = playerCount;
		this.diffieHellman = new DiffieHellman();
	}

	// vygeneruje spolocny kluc pre vsetkych ucastnikov
	public byte[] generateSessionKey(int keySize) {

		// vsetkym dodame rovnake prvocislo a generator
		BigInteger prime;
		BigInteger generator;
		if (localId == 0) {
			// hrac s ID = 0 posle vsetkym prvocislo a generator
			diffieHellman.createKey();
			String primeToSend = diffieHellman.getPrimeP().toString();
			messageSender.sendInsecuredMessageToAll(primeToSend);
			String generatorToSend = diffieHellman.getGeneratorG().toString();
			messageSender.sendInsecuredMessageToAll(generatorToSend);
		} else {
			// ostatni hraci ziskaju prvocislo a generator od hraca s ID = 0
			String primeString = new String(messageSender.readInsecuredBytes(0));
			prime = new BigInteger(primeString);

			String generatorString = new String(
					messageSender.readInsecuredBytes(0));
			generator = new BigInteger(generatorString);
			// System.out.println(generatorString);

			diffieHellman.createSpecificKey(prime, generator);
		}

		int previousId = (localId + playerCount - 1) % playerCount;
		int nextId = (localId + 1) % playerCount;
		Key keyToSend = diffieHellman.getPublicKey();

		KeyAgreement keyAgree;
		KeyFactory factory;

		try {
			factory = KeyFactory.getInstance("DiffieHellman");
			keyAgree = KeyAgreement.getInstance("DH");
			keyAgree.init(diffieHellman.getPrivateKey());

			for (int i = 0; i < playerCount - 2; i++) {
				messageSender
						.sendInsecuredBytes(keyToSend.getEncoded(), nextId);
				byte[] encodedPublicKey = messageSender
						.readInsecuredBytes(previousId);
				Key receivedKey = factory
						.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
				keyToSend = keyAgree.doPhase(receivedKey, false);
			}
			messageSender.sendInsecuredBytes(keyToSend.getEncoded(), nextId);
			byte[] encodedPublicKey = messageSender
					.readInsecuredBytes(previousId);
			Key receivedKey = factory.generatePublic(new X509EncodedKeySpec(
					encodedPublicKey));
			keyToSend = keyAgree.doPhase(receivedKey, true);
			byte[] secretKey = keyAgree.generateSecret();

			// System.out.println(Arrays.toString(secretKey));
			// System.out.println(secretKey.length);
			return Arrays.copyOf(secretKey, keySize / 8);

		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (InvalidKeyException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}
}

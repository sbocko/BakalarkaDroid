package sk.upjs.bocko.protocol;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class RSAkeys {
	private List<Key> publicRsaKeys;
	private RSAcipher rsa;

	public RSAkeys(RSAcipher rsa, byte[][] encodedPublicKeys) {
		this.rsa = rsa;
		try {
			publicRsaKeys = new ArrayList<Key>();
			KeyFactory factory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec pubKeySpec;

			for (int i = 0; i < encodedPublicKeys.length; i++) {
				pubKeySpec = new X509EncodedKeySpec(encodedPublicKeys[i]);
				publicRsaKeys.add(factory.generatePublic(pubKeySpec));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

	public byte[] sign(String message) {
		return rsa.signMessage(message);
	}

	public boolean verify(int id, String msg, byte[] signature) {
		if (id < publicRsaKeys.size()) {
			return rsa.verifySignature(msg, signature,
					(PublicKey) publicRsaKeys.get(id));
		}
		return false;
	}
}

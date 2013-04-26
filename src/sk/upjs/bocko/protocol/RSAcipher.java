package sk.upjs.bocko.protocol;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class RSAcipher {
	private int keySize = 1024;
	private KeyPair keyPair;
	private Signature sig;

	public RSAcipher() {
		try {
			sig = Signature.getInstance("MD5WithRSA");

			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keySize);
			keyPair = kpg.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public byte[] getPublic() {
		return keyPair.getPublic().getEncoded();
	}

	public byte[] signMessage(String data) {
		try {
			sig.initSign(keyPair.getPrivate());
			sig.update(data.getBytes());
			byte[] signatureBytes = sig.sign();
			// System.out.println("Singature:" + new String(signatureBytes));
			sig.initVerify(keyPair.getPublic());
			sig.update(data.getBytes());
			return signatureBytes;
		} catch (SignatureException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean verifySignature(String message, byte[] signature,
			PublicKey publicKey) {
		try {
			sig.initVerify(publicKey);
			sig.update(message.getBytes());
			return sig.verify(signature);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void setKeySize(int keySize){
		this.keySize = keySize;
	}
}

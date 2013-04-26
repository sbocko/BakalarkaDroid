package sk.upjs.bocko.protocol;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AEScipher {
	private static final int DEFAULT_KEY_SIZE = 128;
	private final byte[] key;
	// key size from 128 to 512 bits (multiples of 64)
	private int keySize = 128;

	public AEScipher(SessionKeyGenerator keyGenerator) {
		this.key = keyGenerator.generateSessionKey(keySize);
	}

	public byte[] encrypt(byte[] dataToEncrypt) {
		try {
			Cipher c = Cipher.getInstance("AES");
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			c.init(Cipher.ENCRYPT_MODE, k);
			return c.doFinal(dataToEncrypt);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] decrypt(byte[] dataToDecrypt) {
		try {
			Cipher c = Cipher.getInstance("AES");
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			c.init(Cipher.DECRYPT_MODE, k);
			return c.doFinal(dataToDecrypt);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void setKeySize(int keySize) {
		if (keySize <= 512 && keySize > 0 && keySize % 64 == 0) {
			this.keySize = keySize;
		}
		this.keySize = DEFAULT_KEY_SIZE;
	}
}

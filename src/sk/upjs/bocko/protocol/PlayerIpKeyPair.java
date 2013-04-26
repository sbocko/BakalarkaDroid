package sk.upjs.bocko.protocol;

import java.util.Arrays;

public class PlayerIpKeyPair {
	private String ipAddress;
	private byte[] publicKey;
	
	public PlayerIpKeyPair(String ipAddress, byte[] publicKey) {
		this.ipAddress = ipAddress;
		this.publicKey = publicKey;
	}
	
	public PlayerIpKeyPair() {
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	
	@Override
	public String toString() {
		return this.ipAddress + " " + this.publicKey.length + " " + Arrays.toString(this.publicKey);
	}
}

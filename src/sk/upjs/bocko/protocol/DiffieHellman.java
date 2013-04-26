package sk.upjs.bocko.protocol;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

public class DiffieHellman{
	private KeyPair keyPair;
	private BigInteger prime;
	private BigInteger generator = BigInteger.valueOf(2);
	private final BigInteger GROUP_1 = new BigInteger("00"
			+ "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
			+ "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
			+ "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
			+ "E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF", 16);

	public DiffieHellman() {
		prime = GROUP_1;
	}

	public void createKey(){
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("DiffieHellman");
			kpg.initialize(512);
			KeyPair kp = kpg.generateKeyPair();
			this.keyPair = kp;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public void createSpecificKey(BigInteger p, BigInteger g){
		try {
			DHParameterSpec param = new DHParameterSpec(p, g);
			KeyPairGenerator kpg;
			kpg = KeyPairGenerator.getInstance("DiffieHellman");
			kpg.initialize(param);
			KeyPair kp = kpg.generateKeyPair();
//			System.out.println(kp.getPublic());

			this.keyPair = kp;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
	}

	public void createDefaultKey() {
		try {
			this.createSpecificKey(prime, generator);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void computeSharedKey(PrivateKey privateKey, PublicKey publicKey) {
		try {
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			KeyAgreement KeyAgree = KeyAgreement.getInstance("DH", "BC");
			KeyAgree.init(privateKey);

			KeyAgree.doPhase(publicKey, true);

			MessageDigest hash = MessageDigest.getInstance("SHA1", "BC");
			System.out.println(new String(
					hash.digest(KeyAgree.generateSecret())));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}
	
	public PublicKey getPublicKey(){
		return (DHPublicKey) keyPair.getPublic();
	}
	
	public PrivateKey getPrivateKey(){
		return (DHPrivateKey) keyPair.getPrivate();
	}
	
	public BigInteger getPrimeP(){
		return ((DHPublicKey) getPublicKey()).getParams().getP();
	}
	
	public BigInteger getGeneratorG(){
		return ((DHPublicKey) getPublicKey()).getParams().getG();
	}
	
	public BigInteger getvalueY(){
		return ((DHPublicKey) getPublicKey()).getY();
	}
	
}

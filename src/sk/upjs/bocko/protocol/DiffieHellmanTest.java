package sk.upjs.bocko.protocol;


public class DiffieHellmanTest {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		RSAcipher cipher = new RSAcipher();
		System.out.println(cipher.getPublic().toString());
		System.out.println(cipher.signMessage("Ahoj svet!"));
//		DiffieHellman dh1 = new DiffieHellman();
//		dh1.createDefaultKey();
//		DiffieHellman dh2 = new DiffieHellman();
//		dh2.createDefaultKey();
//		System.out.println("-------------------------------------------------");
//		System.out.println();
//
//		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman");
//		
//		KeyAgreement aKeyAgree = KeyAgreement.getInstance("DH");
//		KeyPair aPair = keyGen.generateKeyPair();
//		aKeyAgree.init(aPair.getPrivate());
//		KeyAgreement bKeyAgree = KeyAgreement.getInstance("DH");
//		KeyPair bPair = keyGen.generateKeyPair();
//		bKeyAgree.init(bPair.getPrivate());
//		KeyAgreement cKeyAgree = KeyAgreement.getInstance("DH");
//		KeyPair cPair = keyGen.generateKeyPair();
//		cKeyAgree.init(cPair.getPrivate());
//		KeyAgreement dKeyAgree = KeyAgreement.getInstance("DH");
//		KeyPair dPair = keyGen.generateKeyPair();
//		dKeyAgree.init(dPair.getPrivate());
//		
//		Key ad = aKeyAgree.doPhase(dPair.getPublic(), false);
//		Key ba = bKeyAgree.doPhase(aPair.getPublic(), false);
//		Key cb = cKeyAgree.doPhase(bPair.getPublic(), false);
//		Key dc = dKeyAgree.doPhase(cPair.getPublic(), false);
//		
//		Key adc = aKeyAgree.doPhase(dc, false);
//		Key bad = bKeyAgree.doPhase(ad, false);
//		Key cba = cKeyAgree.doPhase(ba, false);
//		Key dcb = dKeyAgree.doPhase(cb, false);
//
//		aKeyAgree.doPhase(dcb, true);
//		bKeyAgree.doPhase(adc, true);
//		cKeyAgree.doPhase(bad, true);
//
//		KeyFactory factory = KeyFactory.getInstance("DiffieHellman");
//		dKeyAgree.doPhase(factory.generatePublic(new X509EncodedKeySpec(cba.getEncoded())), true);
//		
//		
//		byte[] aSec = aKeyAgree.generateSecret();
//		byte[] bSec = bKeyAgree.generateSecret();
//		byte[] cSec = cKeyAgree.generateSecret();
//		byte[] dSec = dKeyAgree.generateSecret();
//		
//		System.out.println(Arrays.toString(aSec));
//		System.out.println(Arrays.toString(bSec));
//		System.out.println(Arrays.toString(cSec));
//		System.out.println(Arrays.toString(dSec));
//		
//		dh1.computeSharedKey(dh2.getPrivateKey(), dh1.getPublicKey());
//		dh2.computeSharedKey(dh1.getPrivateKey(), dh2.getPublicKey());
	}

	// public static BigInteger power(BigInteger base, BigInteger exponent,
	// BigInteger prime) {
	//
	// BigInteger result = new BigInteger("1");
	// BigInteger i = new BigInteger("1");
	// while(i.compareTo(exponent) <= 0){
	// result = result.multiply(base);
	// result = result.mod(prime);
	// if(result.signum() == -1){
	// result = result.add(prime);
	// result = result.mod(prime);
	// }
	// i = i.add(new BigInteger("1"));
	// }
	// return result;
	//
	// for (int i = 1; i <= exponent; i++) {
	// result = (result * base) % prime;
	// if (result < 0)
	// result = (result + prime) % prime;
	// }
	// return result;
	// }

}

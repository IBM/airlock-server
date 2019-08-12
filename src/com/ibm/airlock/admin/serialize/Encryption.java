package com.ibm.airlock.admin.serialize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class Encryption
{
	 static final byte[] magic = new byte[] { 0x54, 0x39, 0x71, 0x12 };
	 static final byte[] version = new byte[] { 0x00, 0x01 }; // for future use

	 static final int headerSize = magic.length + version.length;
	 static final int blockSize = 16; // 128 bits
	 static final int dataStart = headerSize + blockSize;

	 public static byte[] getMagic() { return magic; }
	 public static byte[] getLatestVersion() { return version; }

	 byte[] key;

	 public Encryption(byte[] key)
	 {
		 this.key = key;
	 }
	 public Encryption(String key)
	 {
		 this.key = fromB64(key);
	 }

	 public static byte[] fromB64(String in)
	 {
		 return DatatypeConverter.parseBase64Binary(in);
	 }
	 public static String toB64(byte[] in)
	 {
		 return DatatypeConverter.printBase64Binary(in);
	 }
	 public static byte[] fromString(String in)
	 {
		return in.getBytes(StandardCharsets.UTF_8);
	 }
	 public static String toString(byte[] in)
	 {
		 return new String(in, StandardCharsets.UTF_8);
	 }

	 public byte[] encrypt(byte[] plaintext) throws GeneralSecurityException, IOException
	 {
		 Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		 if (cipher.getBlockSize() != blockSize)
			 throw new GeneralSecurityException("unexpected cipher block size");

		byte[] ivBytes = new byte[blockSize];
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.nextBytes(ivBytes);

		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		SecretKeySpec aesKey = new SecretKeySpec(key, "AES");

		cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
		byte[] encrypted = cipher.doFinal(plaintext);

		// prefix the encrypted bytes with the magic, version, and initialization vector
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write( magic );
		out.write( version );
		out.write( ivBytes );
		out.write( encrypted );
		return out.toByteArray();
	 }

	 public byte[] decrypt(byte[] encrypted) throws GeneralSecurityException
	 {
		 if (encrypted.length <= dataStart)
			 throw new GeneralSecurityException("input size is too short");

		 // check the magic
		 if (!goodMagic(encrypted))
			 throw new GeneralSecurityException("missing magic number");

		 // extract initialization vector and encrypted data buffer
		 byte[] ivBytes = Arrays.copyOfRange(encrypted, headerSize, dataStart);
		 byte[] data = Arrays.copyOfRange(encrypted, dataStart, encrypted.length);

		 SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		 IvParameterSpec iv = new IvParameterSpec(ivBytes);

		 Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		 cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
		 return cipher.doFinal(data);
	 }

	 boolean goodMagic(byte[] encrypted)
	 {
		 for (int i = 0; i < magic.length; ++i)
		 {
			 if (encrypted[i] != magic[i])
				 return false;
		 }
		 return true;
	 }

	public static void main(String[] args)
	{
		try {
		       String skey = "Bar12345Bar12345"; // 128 bit key
		       String plaintext = "lalala";
		       System.out.println("original: " + plaintext);

		       byte[] key = Encryption.fromString(skey);
		       byte[] plain = Encryption.fromString(plaintext);

		       Encryption e = new Encryption(key);
		       byte[] encrypted = e.encrypt(plain);

		       String sencrypted = Encryption.toB64(encrypted);
		       System.out.println("encrypted: " + sencrypted);

		       byte[] in = Encryption.fromB64(sencrypted);
		       byte[] decrypted = e.decrypt(in);

		       String out = Encryption.toString(decrypted);
		       System.out.println("decrypted: " + out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
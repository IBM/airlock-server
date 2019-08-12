package com.ibm.airlock.admin.operations;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHash {
	
	private static final String ENCRYPTION_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10;
	private static final String SECURE_NUMBER_ALGORITHM = "SHA1PRNG";
	
	
	public static String generatePasswordHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		char[] chars = password.toCharArray();
		byte[] salt = getSalt();

		PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(ENCRYPTION_ALGORITHM);
		byte[] hash = skf.generateSecret(spec).getEncoded();
		return ITERATIONS + ":" + toHex(salt) + ":" + toHex(hash);
	}

	private static byte[] getSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance(SECURE_NUMBER_ALGORITHM);
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt;
	}

	private static String toHex(byte[] array) throws NoSuchAlgorithmException
	{
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if(paddingLength > 0)
		{
			return String.format("%0"  +paddingLength + "d", 0) + hex;
		}else{
			return hex;
		}
	}
	
	public static boolean validatePassword(String originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String[] parts = storedPassword.split(":");
		int iterations = Integer.parseInt(parts[0]);
		byte[] salt = fromHex(parts[1]);
		byte[] hash = fromHex(parts[2]);

		PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(ENCRYPTION_ALGORITHM);
		byte[] testHash = skf.generateSecret(spec).getEncoded();

		int diff = hash.length ^ testHash.length;
		for(int i = 0; i < hash.length && i < testHash.length; i++)
		{
			diff |= hash[i] ^ testHash[i];
		}
		return diff == 0;
	}
	
	private static byte[] fromHex(String hex) throws NoSuchAlgorithmException
	{
		byte[] bytes = new byte[hex.length() / 2];
		for(int i = 0; i<bytes.length ;i++)
		{
			bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String  originalPassword = "C1wTb0mOOtc=";
		String generatedSecuredPasswordHash = generatePasswordHash(originalPassword);
		System.out.println(generatedSecuredPasswordHash);

		boolean matched = validatePassword(originalPassword, generatedSecuredPasswordHash);
		System.out.println(matched);

		matched = validatePassword(originalPassword+"d", generatedSecuredPasswordHash);
		System.out.println(matched);
	}

}

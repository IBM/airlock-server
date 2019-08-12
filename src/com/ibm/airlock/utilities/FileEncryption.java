package com.ibm.airlock.utilities;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;

//import java.util.Base64;
//import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.serialize.S3DataSerializer;

public class FileEncryption
{
	public static void main(String[] args)
	{
		//test();

       
		if (args.length != 6)
		{
			System.out.println("usage: FileEncryption   [get|put]   [s3_url|default]   s3_region  [bucket|default]   bucket_path   local_path");
			return;
		}

		try {
			run(args);
			System.out.println("done");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	static void run(String[] args) throws Exception
	{
		if (args[1].equals("default"))
			args[1] = ""; // get the S3 URL from environment variable S3_PATH

		if (args[2].equals("default"))
			args[2] = ""; // get bucket from environment variable S3_BUCKET_NAME

		if (args[0].equals("put"))
			put(args);
		else if (args[0].equals("get"))
			get(args);
		else
			throw new Exception("invalid action " + args[0]);
	}


	static void put(String[] args) throws Exception
	{
		String data = Utilities.readString(args[5]);
		S3DataSerializer s3DataSerializer = S3DataSerializer.generateS3DataSerializer(args[1], args[2], args[3], args[4]);
		s3DataSerializer.putEncryptedFile(data, args[4]);
	}
	static void get(String[] args) throws Exception
	{
		S3DataSerializer s3DataSerializer = S3DataSerializer.generateS3DataSerializer(args[1], args[2], args[3], args[4]);
		String data = s3DataSerializer.getEncryptedFileShortName(args[4]);
		Utilities.writeString(data, args[5]);
	}
	
	//---------------------------------------------
	static void test()
	{
        try {
	       String key = "Bar12345Bar12345"; // 128 bit key
	       String initVector = "RandomInitVector"; // 16 bytes IV

	       String plaintext = "lalala";
	       String encrypted = encrypt(key, initVector, plaintext);
	       String decrypted = decrypt(key, initVector, encrypted);
	       System.out.println("encrypted string: " + encrypted);
	       System.out.println("decrypted string: " + decrypted);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
	}

	public static String encrypt(String key, String initVector, String value)
			 throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
	 {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            String b64 = DatatypeConverter.printBase64Binary(encrypted);
            //String b64 = Base64.encodeBase64String(encrypted);
            return b64;
	 }

	 public static String decrypt(String key, String initVector, String encrypted)
			  throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
	  {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] b64 = DatatypeConverter.parseBase64Binary(encrypted);
            //byte[] b64 = Base64.decodeBase64(encrypted);
            byte[] original = cipher.doFinal(b64);

            return new String(original);
	    }
}

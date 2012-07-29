package org.cjsw.wp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.StringTokenizer;

// *********************************************************************
// Oringally converted by the folks at www.orangefood.com
//
// This class implements the popular MD5Crypt function as used by BSD and
// most modern Un*x systems. It was basically converted from the C code
// write by Poul-Henning Kamp. Here is his comment:
//
// "THE BEER-WARE LICENSE" (Revision 42):
// <phk@login.dknet.dk> wrote this file. As long as you retain this notice you
// can do whatever you want with this stuff. If we meet some day, and you think
// this stuff is worth it, you can buy me a beer in return. Poul-Henning Kamp
// *********************************************************************
// found in
// http://www.mirrorservice.org/sites/download.sourceforge.net/pub/sourceforge/j/jy/jymsg9/ymsg_code_source_v0_6.zip
public class MD5Crypt {
	private static final byte[] ITOA64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes();

	private static final String MAGIC = "$1$";

	private static Random random;

	public static String crypt(String strPassword) {
		String salt = "$1$" + generateSalt() + "$";
		return crypt(strPassword, salt);
	}

	/*
	 * Original method by Poul-Henning Kamp
	 */
	public static String crypt(String strPassword, String strSalt) {
		try {
			StringTokenizer st = new StringTokenizer(strSalt, "$");
			st.nextToken();
			byte[] abyPassword = strPassword.getBytes();
			byte[] abySalt = st.nextToken().getBytes();

			MessageDigest _md = MessageDigest.getInstance("MD5");

			_md.update(abyPassword);
			_md.update(MAGIC.getBytes());
			_md.update(abySalt);

			MessageDigest md2 = MessageDigest.getInstance("MD5");
			md2.update(abyPassword);
			md2.update(abySalt);
			md2.update(abyPassword);
			byte[] abyFinal = md2.digest();

			for (int n = abyPassword.length; n > 0; n -= 16) {
				_md.update(abyFinal, 0, n > 16 ? 16 : n);
			}
			abyFinal = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

			// "Something really weird"
			// Not sure why 'j' is here as it is always zero, but it's in Kamp's code too
			for (int j = 0, i = abyPassword.length; i != 0; i >>>= 1) {
				if ((i & 1) == 1)
					_md.update(abyFinal, j, 1);
				else
					_md.update(abyPassword, j, 1);
			}

			// Build the output string
			StringBuffer sbPasswd = new StringBuffer();
			sbPasswd.append(MAGIC);
			sbPasswd.append(new String(abySalt));
			sbPasswd.append('$');

			abyFinal = _md.digest();

			// And now, just to make sure things don't run too fast
			// in C . . . "On a 60 Mhz Pentium this takes 34 msec, so you would
			// need 30 seconds to build a 1000 entry dictionary..."
			for (int n = 0; n < 1000; n++) {
				MessageDigest md3 = MessageDigest.getInstance("MD5");
				// MD5Init(&ctx1);
				if ((n & 1) != 0)
					md3.update(abyPassword);
				else
					md3.update(abyFinal);

				if ((n % 3) != 0)
					md3.update(abySalt);

				if ((n % 7) != 0)
					md3.update(abyPassword);

				if ((n & 1) != 0)
					md3.update(abyFinal);
				else
					md3.update(abyPassword);

				abyFinal = md3.digest();
			}

			// Convert to int's so we can do our bit manipulation
			// it's a bit tricky making the byte act unsigned
			int[] anFinal = new int[] { (abyFinal[0] & 0x7f) | (abyFinal[0] & 0x80), (abyFinal[1] & 0x7f) | (abyFinal[1] & 0x80),
					(abyFinal[2] & 0x7f) | (abyFinal[2] & 0x80), (abyFinal[3] & 0x7f) | (abyFinal[3] & 0x80),
					(abyFinal[4] & 0x7f) | (abyFinal[4] & 0x80), (abyFinal[5] & 0x7f) | (abyFinal[5] & 0x80),
					(abyFinal[6] & 0x7f) | (abyFinal[6] & 0x80), (abyFinal[7] & 0x7f) | (abyFinal[7] & 0x80),
					(abyFinal[8] & 0x7f) | (abyFinal[8] & 0x80), (abyFinal[9] & 0x7f) | (abyFinal[9] & 0x80),
					(abyFinal[10] & 0x7f) | (abyFinal[10] & 0x80), (abyFinal[11] & 0x7f) | (abyFinal[11] & 0x80),
					(abyFinal[12] & 0x7f) | (abyFinal[12] & 0x80), (abyFinal[13] & 0x7f) | (abyFinal[13] & 0x80),
					(abyFinal[14] & 0x7f) | (abyFinal[14] & 0x80), (abyFinal[15] & 0x7f) | (abyFinal[15] & 0x80) };

			to64(sbPasswd, anFinal[0] << 16 | anFinal[6] << 8 | anFinal[12], 4);
			to64(sbPasswd, anFinal[1] << 16 | anFinal[7] << 8 | anFinal[13], 4);
			to64(sbPasswd, anFinal[2] << 16 | anFinal[8] << 8 | anFinal[14], 4);
			to64(sbPasswd, anFinal[3] << 16 | anFinal[9] << 8 | anFinal[15], 4);
			to64(sbPasswd, anFinal[4] << 16 | anFinal[10] << 8 | anFinal[5], 4);
			to64(sbPasswd, anFinal[11], 2);

			return sbPasswd.toString();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	public static String generateSalt() {
		return generateSalt(8);
	}

	public static String generateSalt(int length) {
		if (length < 1)
			length = 1;
		if (length > 8)
			length = 8;
		if (random == null) {
			try {
				random = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				random = new Random();
			}
		}
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			int x = random.nextInt(ITOA64.length);
			result[i] = ITOA64[x];
		}
		return new String(result);
	}

	public static boolean isPasswordValid(String password, String encodedPassword) {
		StringTokenizer tokenizer = new StringTokenizer(encodedPassword, "$");
		String magic = tokenizer.nextToken();
		String iv = tokenizer.nextToken();
		String salt = "$" + magic + "$" + iv + "$";
		String value = crypt(password, salt);
		return value.equals(encodedPassword);
	}

	private static void to64(StringBuffer sb, int n, int nCount) {
		while (--nCount >= 0) {
			sb.append((char) ITOA64[n & 0x3f]);
			n >>= 6;
		}
	}
}

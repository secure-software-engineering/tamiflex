/*******************************************************************************
 * Copyright (c) 2010 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package de.bodden.tamiflex.normalizer;
import java.security.MessageDigest;
 
/**
 * Class to compute the SHA1 hex of a string. 
 */
public class SHAHash {
 
    public static String SHA1(byte[] bytes) {
	    MessageDigest md;
	    try {
			md = MessageDigest.getInstance("SHA-1");
			byte[] sha1hash = new byte[40];
			md.update(bytes, 0, bytes.length);
			sha1hash = md.digest();
			return convertToHex(sha1hash);
		} catch (Exception e) {
			throw new Error(e);
		}
	}

    private static String convertToHex(byte[] data) {
    	StringBuffer buf = new StringBuffer();
    	for (int i = 0; i < data.length; i++) {
    		int halfbyte = (data[i] >>> 4) & 0x0F;
    		int two_halfs = 0;
    		do {
    			if ((0 <= halfbyte) && (halfbyte <= 9))
    				buf.append((char) ('0' + halfbyte));
    			else
    				buf.append((char) ('a' + (halfbyte - 10)));
    			halfbyte = data[i] & 0x0F;
    		} while(two_halfs++ < 1);
    	}
    	return buf.toString();
    }
}

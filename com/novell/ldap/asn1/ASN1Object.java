
package org.ietf.asn1;

import java.io.*;

/**
 * This is the root class for all ASN1 types.
 */
public abstract class ASN1Object implements Serializable {

	protected ASN1Identifier id;

	/**
	 * Encode this ASN1Object directly to a stream.
	 *
	 * @param out The stream into which the encoding will go.
	 */
	abstract public void encode(ASN1Encoder enc, OutputStream out)
		throws IOException;

	/**
	 * Returns the identifier (CLASS, FORM and TAG) for this ASN1Object.
	 */
	public ASN1Identifier getIdentifier()
	{
		return id;
	}

	/**
	 * Sets the identifier (CLASS, FORM and TAG) for this ASN1Object.
    *
	 * This is helpful when creating ASN1Tagged types.
	 */
	protected void setIdentifier(ASN1Identifier id)
	{
		this.id = id;
	}

	/**
	 * Encode this ASN1Object. Return the encoding in a byte array.
	 */
	public byte[] getEncoding(ASN1Encoder enc) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			encode(enc, out);
		}
		catch(IOException ioe) {
		}
		return out.toByteArray();
	}

	public String toString()
	{
		String[] classTypes = {
				"[UNIVERSAL ", "[APPLICATION ", "[", "[PRIVATE " };

		StringBuffer sb = new StringBuffer();
		ASN1Identifier id = getIdentifier(); // could be overridden.

		sb.append(classTypes[id.getASN1Class()]).append(id.getTag()).append("] ");

		return sb.toString();
	}

}

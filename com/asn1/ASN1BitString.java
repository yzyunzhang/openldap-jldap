/* **************************************************************************
 * $Novell$
 *
 * Copyright (C) 1999, 2000 Novell, Inc. All Rights Reserved.
 ***************************************************************************/

package com.novell.asn1;

import java.io.*;

/**
 * Represents the ASN.1 BIT STRING type.
 */
public class ASN1BitString extends ASN1Simple {

   private byte[] content;

   /**
    * ASN.1 BIT STRING tag definition.
    */
   public static final int TAG = 0x03;

   //*************************************************************************
   // Constructors for ASN1BitString
   //*************************************************************************

   /**
    * Constructs an ASN1BitString object using a byte array value.
    */
   public ASN1BitString(byte[] content)
   {
      id = new ASN1Identifier(ASN1Identifier.UNIVERSAL, false, TAG);
      this.content = content;
   }

   /**
    * Constructs an ASN1BitString object by decoding data from an input
    * stream.
    */
   public ASN1BitString(ASN1Decoder dec, InputStream in, int len)
      throws IOException
   {
      id = new ASN1Identifier(ASN1Identifier.UNIVERSAL, false, TAG);
      content = (byte[])dec.decodeBitString(in, len);
   }

   //*************************************************************************
   // ASN1Object implementation
   //*************************************************************************

   /**
    * Encodes the contents of this ASN1BitString directly to an output stream.
    */
   public void encode(ASN1Encoder enc, OutputStream out)
      throws IOException
   {
      enc.encode(this, out);
   }

   //*************************************************************************
   // ASN1BitString specific methods
   //*************************************************************************

   /**
    * Returns the content of this ASN1BitString as a byte[].
    */
   public byte[] getContent()
   {
      return content;
   }

   /**
    * Return a String representation of this ASN1Object.
    */
   public String toString()
   {
      return super.toString() + "BIT STRING: "; // finish this
   }

}


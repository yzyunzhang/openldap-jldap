/* **************************************************************************
 * $Id: NamingContextEntryCountResponse.java,v 1.4 2000/08/08 19:43:55 javed Exp $
 *
 * Copyright (C) 1999, 2000 Novell, Inc. All Rights Reserved.
 * 
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE, MODIFICATION, AND REDISTRIBUTION OF THIS WORK IS SUBJECT
 * TO VERSION 2.0.1 OF THE OPENLDAP PUBLIC LICENSE, A COPY OF WHICH IS
 * AVAILABLE AT HTTP://WWW.OPENLDAP.ORG/LICENSE.HTML OR IN THE FILE "LICENSE"
 * IN THE TOP-LEVEL DIRECTORY OF THE DISTRIBUTION. ANY USE OR EXPLOITATION
 * OF THIS WORK OTHER THAN AS AUTHORIZED IN VERSION 2.0.1 OF THE OPENLDAP
 * PUBLIC LICENSE, OR OTHER PRIOR WRITTEN CONSENT FROM NOVELL, COULD SUBJECT
 * THE PERPETRATOR TO CRIMINAL AND CIVIL LIABILITY. 
 ***************************************************************************/
package com.novell.ldap.ext; 

import org.ietf.ldap.*;
import org.ietf.asn1.*;
import java.io.*;
 
/**
 *
 *      This class is generated by the ExtendedResponseFactory class from
 *  an ExtendedResponse object which has the following OID 
 *  "2.16.840.1.113719.1.27.100.14".
 *
 */
public class NamingContextEntryCountResponse implements ParsedExtendedResponse {
   
   /** The count of the objects returned by the server is saved here
    */
   private int count;
   /**
    *
    * The constructor parses the responseValue which has the following ASN<br><br>
    *  responseValue ::=<br>
    *  &nbsp;&nbsp;&nbsp;&nbsp;     count   INTEGER
    *
    */   
   public NamingContextEntryCountResponse (LDAPExtendedResponse r) 
        	throws IOException {
        
        // parse the contents of the reply
        byte [] returnedValue = r.getValue();
        if (returnedValue == null)
            throw new IOException("No returned value");
        
        // Create a decoder object
        BERDecoder decoder = new BERDecoder();
        if (decoder == null)
            throw new IOException("Decoding error");
            
        ASN1Integer asn1_count = (ASN1Integer)decoder.decode(returnedValue);        
        if (asn1_count == null)
            throw new IOException("Decoding error");     
            
        count = asn1_count.getInt();
   }
   
   /** 
    * 
    * @return the count of the number of objects returned
    */
   public int getCount() {
        return count;
   }
    
}
/* **************************************************************************
 * $Id: NamingContextEntryCountResponse.java,v 1.2 2000/07/27 18:08:13 javed Exp $
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

import com.novell.ldap.*;
import com.novell.ldap.client.protocol.lber.*;
import java.io.IOException;
 
/**
 *  public class NamingContextEntryCountResponse
 *
 *      This class is generated by the ExtendedResponseFactory class from
 *  an ExtendedResponse object which has the following OID 
 *  "2.16.840.1.113719.1.27.100.14".
 *
 */
public class NamingContextEntryCountResponse implements ParsedExtendedResponse {
   
   int count;
   /**
    *  public NamingContextEntryCountResponse()
    *
    * The constructor parses the responseValue which has the following ASN
    *  responseValue ::=
    *          count   INTEGER
    *
    */   
   public NamingContextEntryCountResponse (LDAPExtendedResponse r) 
        	throws IOException {
        
        // parse the contents of the reply
        byte [] returnedValue = r.getValue();
        LberDecoder responseLber;
        if (returnedValue == null)
            throw new IOException("No returned value");
        else
            responseLber = new LberDecoder(returnedValue, 0, returnedValue.length);

        if (responseLber== null)
            throw new IOException("Decoding error");
            
        count = responseLber.parseInt();
   }
   
   public int getCount() {
        return count;
   }
    
}

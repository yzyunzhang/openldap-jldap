/* **************************************************************************
 * $Id: GetReplicationFilterRequest.java,v 1.2 2000/11/01 19:15:48 javed Exp $
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
package com.novell.ldap.extensions; 

import com.novell.ldap.*;
import com.novell.ldap.asn1.*;
import java.io.*;
 
/**
 *
 *  Gets the Replication filter for all replicas on the server.
 *
 * <p>The filter is returned as an array of classnames-attribute names pairs. </p>
 *
 *  <p>To get the filter for all replicas on a specific server, you must 
 *  create an instance of this class and then call the 
 *  extendedOperation method with this object as the required 
 *  LDAPExtendedOperation parameter.</p>
 *
 *  <p>The GetReplicationFilterRequest extension uses the following OID:<br>
 *   &nbsp;&nbsp;&nbsp;2.16.840.1.113719.1.27.100.37</p>
 *
 *  <p>The requestValue has the following format:<br>
 *
 *  requestValue ::=<br>
 *  &nbsp;&nbsp;&nbsp;&nbsp; serverName&nbsp;&nbsp;&nbsp;  LDAPDN</p>    
 */
public class GetReplicationFilterRequest extends LDAPExtendedOperation {
   
/**
 *
 * Constructs an extended operations object which contains the ber encoded
 * replication filter.
 *
 * @param serverDN The server whose replication filter needs to be read
 * <br><br>     
 * @exception LDAPException A general exception which includes an error 
 *                          message and an LDAP error code.
 */   
 public GetReplicationFilterRequest(String serverDN) 
                throws LDAPException {
        
        super(NamingContextConstants.GET_REPLICATION_FILTER_REQ, null);
        
        try {
            
            if (serverDN == null)
                throw new LDAPException("Invalid parameter", LDAPException.PARAM_ERROR);
         
            ByteArrayOutputStream encodedData = new ByteArrayOutputStream();
            LBEREncoder encoder  = new LBEREncoder();
                                                 
            ASN1OctetString asn1_serverDN = new ASN1OctetString(serverDN);
          
            // Add the serverDN to encoded data
            asn1_serverDN.encode(encoder, encodedData);
            setValue(encodedData.toByteArray());
		}
      catch(IOException ioe) {
         throw new LDAPException("Encoding Error",
                                 LDAPException.ENCODING_ERROR);
      }
   }

}

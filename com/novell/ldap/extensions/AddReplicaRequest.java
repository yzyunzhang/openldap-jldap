/* **************************************************************************
 * $Id: AddReplicaRequest.java,v 1.1 2000/07/27 17:50:57 javed Exp $
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
 *  public class AddReplicaRequest
 *
 *      This class inherits from the LDAPExtendedOperation class
 *  and is used to add a replica to the specified directory server.
 *  To add a replica to a particular server create an instance of this 
 *  class and then call the extendedOperation method with this
 *  object as the required LDAPExtendedOperation parameter
 *
 *  The OID used for this extended operation is:
 *      "2.16.840.1.113719.1.27.100.7"
 *
 *  The RequestValue has the folling ASN:
 *
 *  requestValue ::=
 *          flags       INTEGER
 *          replicaType INTEGER
 *          serverName  LDAPDN
 *          dn          LDAPDN
 */
public class AddReplicaRequest extends LDAPExtendedOperation {
   
/**
 *  public AddReplicaRequest()
 *
 *      The constructor takes four parameters:
 *
 *      String dn:          Specify the distinguished name of the replicas
 *                          partition root
 *
 *      String serverDN:    Points to the server on which the replica
 *                          will be added
 *      
 *      int replicaType:    The type of replica that needs to be added. The
 *                          replica types are defined int the 
 *                          NamingContextConstants class
 *
 *      int flags:          Specifies if all servers in the replica ring 
 *                          must be up before proceeding.  Set to 
 *                          LDAP_ENSURE_SERVERS_UP field defined in the 
 *                          NamingContextConstants class .
 */   
 public AddReplicaRequest(String dn, String serverDN, int replicaType, int flags) 
                throws LDAPException {
        
        super(NamingContextConstants.ADD_REPLICA_REQ, null);
        
        try {
            // ber encode the parameters and set the requestValue
            LberEncoder requestlber = new LberEncoder();
            
            if ( (dn == null) || (serverDN == null) )
                throw new LDAPException("Invalid parameter",
				                        LDAPException.PARAM_ERROR);
				                        
		    requestlber.encodeInt(flags);
            requestlber.encodeInt(replicaType);
            requestlber.encodeString(serverDN, true);            
            requestlber.encodeString(dn, true);
                    
            setValue(requestlber.getTrimmedBuf());
            
        }
		catch(IOException ioe) {
			throw new LDAPException("Encoding Error",
				                     LDAPException.ENCODING_ERROR);
		}
   }

}

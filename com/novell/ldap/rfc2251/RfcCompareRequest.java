/* **************************************************************************
 * $OpenLDAP$
 *
 * Copyright (C) 1999 - 2002 Novell, Inc. All Rights Reserved.
 *
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE, MODIFICATION, AND REDISTRIBUTION OF THIS WORK IS SUBJECT
 * TO VERSION 2.0.1 OF THE OPENLDAP PUBLIC LICENSE, A COPY OF WHICH IS
 * AVAILABLE AT HTTP://WWW.OPENLDAP.ORG/LICENSE.HTML OR IN THE FILE "LICENSE"
 * IN THE TOP-LEVEL DIRECTORY OF THE DISTRIBUTION. ANY USE OR EXPLOITATION
 * OF THIS WORK OTHER THAN AS AUTHORIZED IN VERSION 2.0.1 OF THE OPENLDAP
 * PUBLIC LICENSE, OR OTHER PRIOR WRITTEN CONSENT FROM NOVELL, COULD SUBJECT
 * THE PERPETRATOR TO CRIMINAL AND CIVIL LIABILITY.
 ******************************************************************************/
package com.novell.ldap.rfc2251;

import java.util.ArrayList;

import com.novell.ldap.asn1.*;
import com.novell.ldap.*;
import com.novell.ldap.resources.*;

/* 
 *       CompareRequest ::= [APPLICATION 14] SEQUENCE {
 *               entry           LDAPDN,
 *               ava             AttributeValueAssertion }
 */
public class RfcCompareRequest extends ASN1Sequence implements RfcRequest {

	//*************************************************************************
	// Constructor for CompareRequest
	//*************************************************************************

	/**
	 *
	 */
	public RfcCompareRequest(RfcLDAPDN entry, RfcAttributeValueAssertion ava)
	{
		super(2);
		add(entry);
		add(ava);
	}

    /**
    * Constructs a new Compare Request copying from the ArrayList of
    * an existing request.
    */
    /* package */
    RfcCompareRequest(   ArrayList origRequest,
                         String base)
            throws LDAPException
    {
        for(int i=0; i < origRequest.size(); i++) {
            content.add(origRequest.get(i));
        }
        // Replace the base if specified, otherwise keep original base
        if( base != null) {
            content.set(0, new RfcLDAPDN(base));
        }
        return;
    }
	//*************************************************************************
	// Accessors
	//*************************************************************************

	/**
	 * Override getIdentifier to return an application-wide id.
	 */
	public ASN1Identifier getIdentifier()
	{
		return new ASN1Identifier(ASN1Identifier.APPLICATION, true,
			                       RfcProtocolOp.COMPARE_REQUEST);
	}

    public RfcRequest dupRequest(String base, String filter, boolean request)
            throws LDAPException
    {
        return new RfcCompareRequest( content, base);
    }
    public String getRequestDN()
    {
        return ((RfcLDAPDN)getContent().get(0)).getString();
    }
}

/* **************************************************************************
 * $Novell: LDAPModification.java,v 1.2 2000/03/14 18:17:28 smerrill Exp $
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
 
package org.ietf.ldap;
 
/**
 * 4.15 public class LDAPModification
 *
 *  A single change specification for an LDAPAttribute.
 * see sec 4.6 of RFC 2251
 */
public class LDAPModification {

   private int _op;
   private LDAPAttribute _attr;

   /**
    * Add values listed to the given attribute, creating
    * the attribute if necessary;
    */
   public static final int ADD = 0;

   /**
    * Delete values listed from the given attribute,
    * removing the entire attribute if no values are listed, or
    * if all current values of the attribute are listed for
    * deletion;
    */
   public static final int DELETE = 1;

   /**
    * Replace all existing values of the given attribute
    * with the new values listed, creating the attribute if it
    * did not already exist.  A replace with no value will delete
    * the entire attribute if it exists, and is ignored if the
    * attribute does not exist.
    */
   public static final int REPLACE = 2;

   /*
    * 4.15.1 Constructors
    */

   /**
    * Specifies a modification to be made to an attribute.
    *
    * Parameters are:
    *
    *  op             The type of modification to make, which can be
    *                  one of the following:
    *
    *           LDAPModification.ADD     The value should be added to
    *                                    the attribute
    *
    *           LDAPModification.DELETE  The value should be removed
    *                                    from the attribute
    *
    *           LDAPModification.REPLACE The value should replace all
    *                                    existing values of the
    *                                    attribute
    *
    *  attr           The attribute (possibly with values) to be
    *                  modified.
    */
   public LDAPModification(int op, LDAPAttribute attr) {
      _op = op;
      _attr = attr;
   }

   /*
    * 4.15.2 getAttribute
    */

   /**
    * Returns the attribute (possibly with values) to be modified.
    */
   public LDAPAttribute getAttribute() {
      return _attr;
   }

   /*
    * 4.15.3 getOp
    */

   /**
    * Returns the type of modification specified by this object.
    */
   public int getOp() {
      return _op;
   }

}

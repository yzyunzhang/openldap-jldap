/* **************************************************************************
 * $OpenLDAP$
 *
 * Copyright (C) 1999, 2000, 2001 Novell, Inc. All Rights Reserved.
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

package com.novell.ldap;

import com.novell.ldap.client.ArrayList;    // Make sure we get the right one
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.client.*;
import com.novell.ldap.resources.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 *
 *  The enumerable results of a synchronous search operation.
 *
 *  <p>Sample Code: <a href="http://developer.novell.com/ndk/doc/samplecode/
 *jldap_sample/asynchronous/Searchas.java.html">Searchas.java</p>
 *
 * @see LDAPConnection#search
 */
public class LDAPSearchResults implements Enumeration
{

    private Vector entries;             // Search entries
    private int entryCount;             // # Search entries in vector
    private int entryIndex;             // Current position in vector
    private Vector references;          // Search Result References
    private int referenceCount;         // # Search Result Reference in vector
    private int referenceIndex;         // Current position in vector
    private int batchSize;              // Application specified batch size
    private boolean completed = false;  // All entries received
    private int count = 0;              // Number of entries read
    private LDAPControl[] controls = null; // Last set of controls
    private LDAPSearchQueue queue;
    private static Object nameLock = new Object(); // protect resultsNum
    private static int resultsNum = 0;  // used for debug
    private String name;                // used for debug
    private LDAPConnection conn;        // LDAPConnection which started search
    private LDAPSearchConstraints cons; // LDAPSearchConstraints for search
    private ArrayList referralConn = null;// Referral Connections

    /**
     * Constructs a queue object for search results.
     *
     * @param  conn The LDAPConnection which initiated the search
     *<br><br>
     * @param queue The queue for the search results.
     *<br><br>
     * @param cons The LDAPSearchConstraints associated with this search
     */
    /* package */
    LDAPSearchResults(  LDAPConnection conn,
                        LDAPSearchQueue queue,
                        LDAPSearchConstraints cons)
    {
        // setup entry Vector
        this.conn = conn;
        this.cons = cons;
        int batchSize = cons.getBatchSize();
        int vectorIncr = (batchSize == 0) ? 64 : 0;
        entries = new Vector( (batchSize == 0) ? 64 : batchSize, vectorIncr );
        entryCount = 0;
        entryIndex = 0;

        // setup search reference Vector
        references = new Vector( 5, 5);
        referenceCount = 0;
        referenceIndex = 0;

        this.queue = queue;
        this.batchSize = (batchSize == 0) ? Integer.MAX_VALUE : batchSize;

        if( Debug.LDAP_DEBUG ) {
            synchronized(nameLock) {
                name = "LDAPSearchResults(" + ++resultsNum + "): ";
            }
            Debug.trace( Debug.messages, name +
                            " Object created, batch size " + this.batchSize +
                            ", hops " + cons.getHopLimit());
        }

        completed = getBatchOfResults(); // initialize the vector
        return;
    }

    /**
     * Returns a count of the entries in the search result.
     *
     * <p>For a synchronous search with batch size not equal to 0,
     *  this reports the number of results received so far. </p>
     *
     * @return The number of search results received so far.
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Returns the latest server controls returned by the server
     * in the context of this search request, or null
     * if no server controls were returned.
     *
     * @return The server controls returned with the search request, or null
     *         if none were returned.
     */
    public LDAPControl[] getResponseControls()
    {
        return controls;
    }

    /**
     * Specifies whether or not there are more search results in the
     * enumeration.
     *
     * @return True, if there are more search results; false, if there are no
     *         more search results.
     */
    public boolean hasMoreElements()
    {
        boolean ret = false;
        if( (entryIndex < entryCount) || (referenceIndex < referenceCount)) {
            // we have data
            ret = true;
        }
        else
        if(completed == false) { // reload the Vector by getting more results
            resetVectors();
            ret = (entryIndex < entryCount) || (referenceIndex < referenceCount);
        }
        if( Debug.LDAP_DEBUG ) {
            Debug.trace( Debug.messages, name +
                "hasMoreElements: returns " + ret + ", enumeration status " +
                ", entryIdx=" + entryIndex +
                ", entryCnt=" + entryCount +
                ", referIdx=" + referenceIndex +
                ", referCnt=" + referenceCount);
        }
        return ret;
    }

    /*
     * If both of the vectors are empty, get more data for them.
     */
    private void resetVectors()
    {
        // If we're done, no further checking needed
        if( completed) {
            return;
        }
        // Checks if we have run out of references
        if( (referenceIndex != 0) && (referenceIndex >= referenceCount) ) {
            references.setSize(0);
            referenceCount = 0;
            referenceIndex = 0;
        }
        // Checks if we have run out of entries
        if( (entryIndex != 0) && (entryIndex >= entryCount) ) {
            entries.setSize(0);
            entryCount = 0;
            entryIndex = 0;
       }
       // If no data at all, must reload enumeration
       if( (referenceIndex == 0) && (referenceCount == 0) &&
                (entryIndex == 0) && (entryCount == 0)) {
            completed = getBatchOfResults();
       }
       return;
    }
    /**
     * Returns the next result in the enumeration as an LDAPEntry.
     *
     * <p>If automatic referral following is disabled and one or more
     * referrals are among the search results, the next method will throw
     * an LDAPReferralException the last time it is called, after all other
     * results have been returned.</p>
     *
     * @return The next search result as an LDAPEntry.
     *
     * @exception LDAPException A general exception which includes an error
     *                          message and an LDAP error code.
     */
    public LDAPEntry next() throws LDAPException
    {
        if( completed && (entryIndex >= entryCount) &&
                (referenceIndex >= referenceCount) ) {
            throw new NoSuchElementException(
                "LDAPSearchResults.next() no more results");
        }
        // Check if the enumeration is empty and must be reloaded
        resetVectors();

        Object element = null;
        // Check for Search References & deliver to app as they come in
        // We only get here if not following referrals/references
        if( referenceIndex < referenceCount ) {
            String refs[] = (String[])(references.elementAt( referenceIndex++) );
            if( Debug.LDAP_DEBUG ) {
                Debug.trace( Debug.messages, name +
                    "next: throws referral exception");
                for( int i = 0; i < refs.length; i++) {
                    Debug.trace( Debug.messages, name + " \t" + refs[i]);
                }
            }
            LDAPReferralException rex = new LDAPReferralException(
                ExceptionMessages.REFERENCE_NOFOLLOW);
            rex.setReferrals( refs);
            throw rex;
        } else
        if( entryIndex < entryCount ) {
            // Check for Search Entries and the Search Result
            element = entries.elementAt( entryIndex++ );
            if( Debug.LDAP_DEBUG ) {
                Debug.trace( Debug.messages, name +
                    "next: returns " + element.getClass().getName() + "@" +
                    Integer.toHexString(element.hashCode()) +
                    ", elements remaining " + entries.size());
            }
            if(element instanceof LDAPResponse) {         // Search done w/bad status
                if( ((LDAPResponse)element).hasException()) {

                    LDAPResponse lr = (LDAPResponse)element;
                    ReferralInfo ri = lr.getActiveReferral();

                    if( Debug.LDAP_DEBUG ) {
                        Debug.trace( Debug.messages, name +
                            "next: LDAPResponse has embedded exception" +
                            " from following referral - " + (ri != null));
                    }
                    if( ri != null) {
                        // Error attempting to follow a search continuation reference
                        LDAPReferralException rex = new LDAPReferralException(
                            ExceptionMessages.REFERENCE_ERROR,
                            lr.getException());
                        rex.setReferrals(ri.getReferralList());
                        rex.setFailedReferral( ri.getReferralUrl().getUrl());
                        throw rex;
                    }
                }
                // Throw an exception if not success
                ((LDAPResponse)element).chkResultCode();
            } else
            if( element instanceof LDAPException) {
                if( Debug.LDAP_DEBUG ) {
                    Debug.trace( Debug.messages, name +
                        "next: LDAPException "+((LDAPException)element).toString());
                }
                throw (LDAPException)element;
            }
        } else {
            // If not a Search Entry, Search Result, or search continuation
            // we are very confused.
            if( Debug.LDAP_DEBUG ) {
                Debug.trace( Debug.messages, name +
                    "next: No entry found and request incomplete\n" +
                    "\tentryIdx " + entryIndex +
                    ", entryCnt " + entryCount +
                    ", referIdx " + referenceIndex +
                    ", referCnt " + referenceCount );
            }
            // LDAPSearchResults.next(): No entry found & request is not complete
            throw new LDAPException(
                ExceptionMessages.REFERRAL_LOCAL,
                new Object[] { "next" },
                LDAPException.LOCAL_ERROR);
        }
        return (LDAPEntry)element;
    }

    /**
     * Returns the next result in the enumeration as an Object.
     *
     * <p>The nextElement method is the default implementation of the
     * Enumeration.nextElement method. The returned value may be an LDAPEntry
     * or an LDAPReferralException. </p>
     *
     * @return The next element in the enumeration.
     */
    public Object nextElement()
    {
        Object element;
        if( completed && (entryIndex >= entryCount) &&
                (referenceIndex >= referenceCount) ) {
            throw new NoSuchElementException(
                "LDAPSearchResults.nextElement() no more results");
        }
        // Check if need to reload the enumeration
        // We have separate vectors for referrals/references because
        // We return them ahead of any normal data.
        resetVectors(); // Return when we have something

        // Check for Search References, deliver to app as they come in
        // We only get here if not automatically following referrals
        if( referenceIndex < referenceCount ) {
            String refs[] = (String[])(references.elementAt( referenceIndex++) );
            if( Debug.LDAP_DEBUG ) {
                Debug.trace( Debug.messages, name +
                    "next: returns referral exception");
                for( int i = 0; i < refs.length; i++) {
                    Debug.trace( Debug.messages, name + ":\t" + refs[i]);
                }
            }
            // Search result reference received and referral following is off
            element = new LDAPReferralException(
                ExceptionMessages.REFERENCE_NOFOLLOW);
            ((LDAPReferralException)element).setReferrals( refs);
        } else
        if( entryIndex < entryCount ) {
            // Check for Search Entries and the Search Responses
            element = entries.elementAt( entryIndex++ );
            if( Debug.LDAP_DEBUG ) {
                Debug.trace( Debug.messages, name +
                    "nextElement: returns " +
                    element.getClass().getName() + "@" +
                    Integer.toHexString(element.hashCode()) );
            }
            if(element instanceof LDAPResponse) {
                // Search done w/bad status
                LDAPException ex;
                // get Exception object
                ex = ((LDAPResponse)element).getResultException();
                if( Debug.LDAP_DEBUG ) {
                    if( ex == null ) {
                        throw new RuntimeException(
                            name + "nextElement: got success result from queue");
                    }
                }
                element = ex;
            }
        } else {
            if( Debug.LDAP_DEBUG ) {
                Debug.trace( Debug.messages, name +
                    "nextElement: No entry found and request incomplete\n" +
                    "\tentryIdx " + entryIndex +
                    ", entryCnt " + entryCount +
                    ", referIdx " + referenceIndex +
                    ", referCnt " + referenceCount );
            }
            element = new LDAPException(
                ExceptionMessages.REFERRAL_LOCAL,
                new Object[] { "nextElement" },
                LDAPException.LOCAL_ERROR);
        }
        return element;
    }

     /**
      * Sorts all entries in the results using the provided comparison
      * object.
      *
      * <p>If the object has been partially or completely enumerated,
      * only the remaining elements are sorted. Sorting the results requires that
      * they all be present. This implies that LDAPSearchResults.nextElement
      * method will always block until all results have been retrieved,
      * after a sort operation.</p>
      *
      * <p>The LDAPCompareAttrNames class is provided to support the common need
      * to collate by single or multiple attribute values, in ascending or
      * descending order.  Examples: </p>
      *<ul>
      *   <li>res.sort(new LDAPCompareAttrNames("cn"));</li>
      *
      *   <li>res.sort(new LDAPCompareAttrNames("cn", false));</li>
      *
      *   <li>String[] attrNames = { "sn", "givenname" };
      *   res.sort(new LDAPCompareAttrNames(attrNames));</li>
      *</ul>
      *
      *  @param comp     An object that implements the LDAPEntryComparator
      *                  interface to compare two objects of type
      *                  LDAPEntry.
      */
    public void sort(LDAPEntryComparator comp) {
       if (!completed){
         batchSize = Integer.MAX_VALUE;
         completed = getBatchOfResults();
       }

       //ready to sort from index on.
        if (entryIndex < entries.size())  //if not all used up. This replaces 'rangeCheck' in Java Source 1.2.2 of Arrays.sort(...comparator)
           mergeSort((Vector)entries.clone(), entries, entryIndex, entries.size(), comp);
   }

     /**
      * @internal
      *
      * Vector Sort Utility function to aide sort
      * Sorts the source vector into the destination vector according to
      * LDAPEntryComparator.  This sort is a mergesort for large Vectors and an
      * insertion sort for small Vectors.
      *
      * @param source the Vector to be sorted.
      * @param destination the result of Vector. (Clone of source??)
      * @param low index of the first sorted element
      * @param high index of the last sorted element
      * @param comp the LDAPEntryComparator to determine the order of the Vector.
      * @see LDAPCompareAttrNames
      */
    private static void mergeSort(Vector source, Vector destination,
                                int low, int high, LDAPEntryComparator comp){
        int size = low - high;

        //For small sizes simply do an Insertion Sort
        if (size < 7) {
            int j=0;
            for( int i=low; i<high; i++){
                for(j=i; j>low &&
                    !(destination.elementAt(j) instanceof LDAPResponse) &&
                    comp.isGreater((LDAPEntry)destination.elementAt(j-1),
                    (LDAPEntry)destination.elementAt(j)); j--){
                        swap(destination, j, j-1);
                }
            }
            return;
        }

        int middle = (low + high)/2;
        mergeSort(destination, source, low, middle, comp);  //sort left into source
        mergeSort(destination, source, middle, high, comp); //sort right into source

        //Now Merge Left and right into destination
        for(int i=low, j=low, k=middle; j<high; i++){
            if (k>=high || j<middle && (
                (source.elementAt(k) instanceof LDAPResponse) ||
                !comp.isGreater((LDAPEntry)source.elementAt(j),
                                (LDAPEntry)source.elementAt(k))))
                destination.setElementAt(source.elementAt(j++),i );
            else
                destination.setElementAt(source.elementAt(k++),i );
        }
    }



   /**
    * @internal
    *
    * Util function used by MergeSort.  Swaps the elements at index A with B
    * in the Vector
    * @param x  Vector in which elements will be swapped
    * @param a  Index to one of the elements
    * @patam b  Index to the other element
    */
   private static void swap(Vector x, int a, int b) {
   	Object t = x.elementAt(a);
   	   x.setElementAt( x.elementAt(b),a);
   	x.setElementAt(t, b);
   }




    /**
     * @internal
     *
     * Will collect batchSize elements from an LDAPSearchQueue message
     * queue and place them in a Vector. If the last message from the server,
     * the result message, contains an error, it will be stored in the Vector
     * for nextElement to process. (although it does not increment the search
     * result count) All search result entries will be placed in the Vector.
     * If a null is returned from getResponse(), it is likely that the search
     * was abandoned.
     *
     * @return true if all search results have been placed in the vector.
     */
    private boolean getBatchOfResults()
    {
        LDAPMessage msg;

        // <=batchSize so that we can pick up the result-done message
        for(int i=0; i<=batchSize; ) {
            try {
                if((msg = queue.getResponse()) != null) {
                    // Only save controls if there are some
                    LDAPControl[] ctls = msg.getControls();
                    if( ctls != null ) {

                        if( Debug.LDAP_DEBUG) {
                            Debug.trace( Debug.controls, name +
                                "Saving returned controls in " +
                                "LDAPSearchResults local variable.");
                        }
                        controls = ctls;
                    }

                    if(msg instanceof LDAPSearchResult) { // Search Entry
                        Object entry = ((LDAPSearchResult)msg).getEntry();
                        entries.addElement( entry );
                        i++;
                        entryCount++;
                        count++;
                        if( Debug.LDAP_DEBUG ) {
                            Debug.trace( Debug.messages, name +
                                "read LDAPEntry@" +
                                Integer.toHexString(entry.hashCode()) +
                                " from LDAPMessage@" +
                                Integer.toHexString(msg.hashCode()) );
                        }
                    } else
                    if(msg instanceof LDAPSearchResultReference) { // Search Ref
                        String[] refs = ((LDAPSearchResultReference)msg).getReferrals();
                        if( Debug.LDAP_DEBUG ) {
                            Debug.trace( Debug.messages, name + "got " +
                                    refs.length + " references in entry ");
                            for( int k=0; k < refs.length; k++ ) {
                                Debug.trace( Debug.messages,
                                    name + "reference " + k + "\t" + refs[k]);
                            }
                        }

                        if( cons.getReferralFollowing() ) {
                           referralConn = conn.checkForReferral(
                                    queue, cons, msg, refs,
                                    0, true, referralConn);
                        } else {
                            references.addElement( refs );
                            referenceCount++;
                        }
                    } else { // LDAPResponse
                        LDAPResponse resp = (LDAPResponse)msg;
                        int resultCode = resp.getResultCode();
                        // Check for an embedded exception
                        if( resp.hasException()) {
                            // Fake it, results in an exception when msg read
                            resultCode = LDAPException.CONNECT_ERROR;
                            if( Debug.LDAP_DEBUG ) {
                                Debug.trace( Debug.messages, name +
                                    "LDAPResponse with embeddedException");
                            }
                        } else
                        if( Debug.LDAP_DEBUG ) {
                            Debug.trace( Debug.messages, name +
                                "read LDAPResponse@" +
                                Integer.toHexString(resp.hashCode()) +
                                ", result " + resultCode);
                        }

                        if( (resultCode == LDAPException.REFERRAL) &&
                                        cons.getReferralFollowing() ) {
                            // Following referrals
                            if( Debug.LDAP_DEBUG ) {
                                Debug.trace( Debug.messages, name +
                                    "following referrals");
                            }
                            referralConn = conn.checkForReferral(
                                        queue, cons, resp,
                                        resp.getReferrals(), 0,
                                        false, referralConn);
                        } else
                        if(resultCode != LDAPException.SUCCESS) {
                            // Results in an exception when message read
                            entries.addElement(resp);
                            entryCount++;
                            if( Debug.LDAP_DEBUG ) {
                                Debug.trace( Debug.messages, name +
                                    "Add LDAPResponse to entry list, count = " +
                                entries.size());
                            }
                        }
                        if( Debug.LDAP_DEBUG ) {
                            Debug.trace( Debug.messages, name +
                                "checking for done");
                        }
                        // We are done only when we have read all messages
                        // including those received from following referrals
                        int[] msgIDs = queue.getMessageIDs();
                        if( msgIDs.length == 0) {
                            if( Debug.LDAP_DEBUG ) {
                                Debug.trace( Debug.messages, name +
                                    "Search completed, all responses processed");
                            }
                            // Release referral exceptions
                            conn.releaseReferralConnections( referralConn);
                            return true; // search completed
                        } else {
                            if( Debug.LDAP_DEBUG ) {
                                Debug.trace( Debug.messages, name +
                                    "Search not done, " + msgIDs.length +
                                    " Messages still active");
                            }
                        }
                        continue;
                    }
                } else {
                    if( Debug.LDAP_DEBUG) {
                        Debug.trace( Debug.messages, name +
                            "Connection timeout, no results returned");
                    }
                    // We get here if the connection timed out
                    // we have no responses, no message IDs and no exceptions
                    LDAPException e = new LDAPException( null,
                                LDAPException.LDAP_TIMEOUT);
                    entries.addElement(e );
                    break;
                }
            } catch(LDAPException e) {
                if( Debug.LDAP_DEBUG ) {
                    Debug.trace( Debug.messages, name +
                       "Caught exception, add to entry queue: " + e.toString());
                }
                // Hand exception off to user
                entries.addElement( e);
            }
            continue;
        }
        return false; // search not completed
    }

    /**
     * Cancels the search request and clears the message and enumeration.
     */
    /*package*/
    void abandon() {
        if( Debug.LDAP_DEBUG ) {
            Debug.trace( Debug.messages, name + "abandon: Entry");
        }
        // first, remove message ID and timer and any responses in the queue
        queue.getMessageAgent().abandonAll();

        // next, clear out enumeration
        resetVectors();
        completed = true;
    }
}

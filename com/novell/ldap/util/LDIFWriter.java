/* **************************************************************************
 * $OpenLDAP$
 *
 * Copyright (C) 2002 Novell, Inc. All Rights Reserved.
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
package com.novell.ldap.ldif_dsml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPControl;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.ldif_dsml.LDAPRequest;
import com.novell.ldap.ldif_dsml.Base64Encoder;

/**
 * The class that is used to write LDIF content records or LDIF change records
 * to the OutputStream object.
 *
 * <p>The object of the class is used to generate LDIF content record or LDIF
 * change record lines. toRecordLines() methods have different signatures to
 * convert <tt> LDAPEntry</tt> objects, delete dn, modify information, or
 * <tt>LDAPModification</tt> array objects into LDIF record lines.</p>
 */


public class LDIFWriter extends LDIF implements LDAPWriter, LDAPImport {

    private int            recordType;                    // record type
    private String         dn;                            // record dn
    private Base64Encoder  base64Encoder = new Base64Encoder();
    private BufferedWriter bufWriter;
    private LDAPControl[]  currentControls;
    private LDAPEntry      currentEntry = null;
    private LDAPRequest    currentChange = null;


    /**
     * Constructs an LDIFWriter object by calling super constructor, and
     * OutputStreamReader object, and BufferedWriter object.
     *
     * <p>The default version 1 is used for the LDIF file</p>
     *
     * @param out     The OutputStream object
     *
     * @throws IOException
     */
    public LDIFWriter(OutputStream out)
    throws UnsupportedEncodingException, IOException {

        this( out, 1 );
    }

    /**
     * Constructs an LDIFWriter object by calling super constructor, and
     * initializing version, OutputStreamReader object, and BufferedWriter
     * object.
     *
     * @param out     The OutputStream object
     * @param version The version currently used by the LDIF file
     */
    public LDIFWriter(OutputStream out, int version)
    throws UnsupportedEncodingException, IOException {
        super();

        // check LDIF file version
        if ( version != 1 ) {
            throw new RuntimeException(
                        "com.novell.ldap.ldif_dsml.LDIFReader: "
                                 + "Should be version 1");
        }

        super.setVersion( version );
        OutputStreamWriter osw = new OutputStreamWriter(out, "UTF8");
        bufWriter = new BufferedWriter( osw );
        writeVersionLine();
    }

    /**
     * Write the version line of LDIF file into the OutputStream.
     *
     * <p>Two extra lines will be written to separate version line
     * with the rest of lines in LDIF file</p>
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeVersionLine () throws IOException {

        // LDIF file is  currently using 'version 1'
        String versionLine = new String("version: " + getVersion());
        bufWriter.write( versionLine, 0, versionLine.length());
        // write an empty line separate the version line
        // with the rest of the contents in LDIF file
        bufWriter.newLine();
    }

    /**
     * Write an empty line.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeEmptyLine () throws IOException {
        bufWriter.newLine();
    }


    /**
     * Write a comment line into the OutputStream.
     *
     * <p> an '#' char is added to the front of the line to indicate that
     * the line is a coment line. If the line contains more than 78
     * chars, it will be splited into multiple lines that start
     * with '#' chars.</p>
     *
     * @param line The comment line to be written to the OutputStream
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeCommentLine (String line) throws IOException {

        if (line != null && line.length() != 0) {

            if (line.length()<=78) {
                // short line, write it out
                bufWriter.write("# " + line, 0, line.length()+2);
            }
            else {
                // long line, berak it
                StringBuffer longLine = new StringBuffer();
                longLine.append(line);

                while(longLine.length() > 78) {
                    // write "# " and 78 chars
                    bufWriter.write("# " + longLine, 0, 80);
                    // start a new line
                    bufWriter.newLine();
                    // remove the chars already wrote out
                    longLine.delete(0, 78);
                }

                // write the remaining part of the lien out
                bufWriter.write("# " + longLine, 0, longLine.length()+2);
            }
            // start a new line
            bufWriter.newLine();
        }
    }


    /**
     * Write a line into the OutputStream.
     *
     * <p>If the line contains more than 80 chars, it will be splited into
     * multiple lines that start with a space ( ASCII ' ') except the
     * first one.</p>
     *
     * @param line The comment line to be written to the OutputStream
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeLine (String line) throws IOException {

        if ( line != null && line.length() != 0) {

            if (line.length()<=80) {
                // short line, write it out
                bufWriter.write(line, 0, line.length());
            }
            else {
                // long line, berak it
                StringBuffer longLine = new StringBuffer();
                longLine.append(line);
                // write the first 80 chars to outputStream
                bufWriter.write(longLine.toString(), 0, 80);
                // remove the chars that already been written out
                longLine.delete(0, 80);
                bufWriter.newLine();

                while(longLine.length() > 79) {
                    // write continuation line
                    bufWriter.write(" " + longLine, 0, 80);
                    // remove the chars that already been written out
                    longLine.delete(0, 79);
                    // start a new line
                    bufWriter.newLine();
                }

                // write the remaining part of the lien out
                bufWriter.write(" " + longLine, 0, longLine.length()+1);
            }

            // write an empty line
            bufWriter.newLine();
        }
    }


    /**
     * Flush the output stream
     *
     * @throws IOException if an I/O error occurs.
     */
    public void flushStream() throws IOException {
        // flush the stream
        bufWriter.flush();
    }


    /**
     * Flush and close the output stream
     *
     * @throws IOException if an I/O error occurs.
     */
    public void closeStream() throws IOException {
        // flush and then close the stream
        bufWriter.close();
    }


    /**
     * Write a number of content records into LDIF content file
     *
     * @param entries LDAPEntry array object
     * @param ctrls LDAPControl[] object
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeContents(LDAPEntry[] entries, LDAPControl[] ctrls)
    throws IOException  {

        for ( int i = 0; i < entries.length; i++ ) {
            writeContent( entries[i], ctrls );
        }
    }


    /**
     * Write a content record into LDIF content file
     *
     * @param entry LDAPEntry object
     * @param ctrls LDAPControl[] object
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeContent(LDAPEntry entry, LDAPControl[] ctrls)
    throws IOException {

        if( isRequest()) {
            throw new RuntimeException(
                 "com.novell.ldap.ldif_dsml.LDAIWriter: "
                      + "Cannot write change to LDIF content file");
        }

        // write the content line into LDIF file
        writeRecordLines(entry, ctrls);
        // write an empry line to separate records
        bufWriter.newLine();
        // write to putputStream
        flushStream();
    }


    /**
     * Write a number of LDAP change record into LDIF file. The change operation
     * can be LDAPAdd, LDAPDelete, LDAPModDN, or LDAPModify operation.
     *
     * @see LDAPAdd
     * @see LDAPDelete
     * @see LDAPModDN
     * @see LDAPModify
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeChanges(LDAPRequest[] changes) throws IOException  {

        for ( int i = 0; i < changes.length; i++ ) {
            writeChange( changes[i] );
        }
    }


    /**
     * Write a LDAP change record into LDIF file. The change operation may
     * be LDAPAdd, LDAPDelete, LDAPModDN, or LDAPModify.
     *
     * @see LDAPAdd
     * @see LDAPDelete
     * @see LDAPModDN
     * @see LDAPModify
     *
     * @param change LDAPRequest object
     *
     * @throws IOException if an I/O error occurs.
     */
    public void writeChange(LDAPRequest change) throws IOException  {

        this.currentControls = change.getControls();
        LDAPModification[] mods;
        String[] modInfo;

        // write dn field to outputStream
        writeDN(change.getDN());

        if ( change instanceof LDAPAdd) {
            // LDAPAdd request, get LDAPEntry
            this.currentEntry = (LDAPEntry)change.getRequestInfo();
            // write to outputStream
            writeRecordLines(this.currentEntry, this.currentControls);
        }
        else if ( change instanceof LDAPDelete ) {
            // LDAPDelete request, write to outputStream
            writeRecordLines( this.dn, this.currentControls );
        }
        else if ( change instanceof LDAPModDN ) {
            // LDAPModDN request, get moddn info
            modInfo = (String[])change.getRequestInfo();
            // write to outputStream
            writeRecordLines( dn, modInfo, this.currentControls );
        }
        else if ( change instanceof LDAPModify) {
            // LDAPModify request, get LDAPModification array
            mods = (LDAPModification[])change.getRequestInfo();
            // write to outputStream
            writeRecordLines( dn, mods, this.currentControls );
        }
        else {
            throw new RuntimeException("Not supported change type");
        }
        // write an empty line to separate records
        bufWriter.newLine();
    }


    /**
     * Used to generate LDIF content record or LDIF change/add record lines.
     *
     * <p>Turn LDAPEntry object into LDIF record</p>
     *
     * @param entry  LDAPREntry object
     *
     * @throws UnsupportedEncodingException
     */
    public void writeRecordLines( LDAPEntry entry )
    throws IOException, UnsupportedEncodingException {
        writeRecordLines(entry, null);
    }


    /**
     * Used to generate LDIF content record or LDIF change/add record lines.
     *
     * <p>Turn LDAPEntry object and LDAPControl[] object into LDIF record
     * lines</p>
     *
     * @param entry  LDAPREntry object
     * @param ctrls  LDAPControl object
     *
     * @throws UnsupportedEncodingException
     */
    public void writeRecordLines( LDAPEntry entry, LDAPControl[] ctrls )
    throws IOException, UnsupportedEncodingException {

        int      i, len;
        boolean  safeString;
        String   attrName, temp, value;
        LDAPAttribute attr;
        LDAPAttributeSet attrSet;
        Iterator allAttrs;
        Enumeration allValues;

        if ( isRequest() ) {
            // add control lines
            if ( ctrls != null ) {
                writeControls( ctrls );
            }
            // add change type line
            writeLine("changetype: add");
        }
        else {
            // get dn from the entry
            writeDN(entry.getDN());
        }

        // save attribute fields
        attrSet = entry.getAttributeSet();
        allAttrs = attrSet.iterator();

        while(allAttrs.hasNext()) {
           attr = (LDAPAttribute)allAttrs.next();
           attrName = attr.getName();

           allValues = attr.getStringValues();

           if( allValues != null) {
               while(allValues.hasMoreElements()) {
                   value = (String) allValues.nextElement();
                   writeLine(attrName + ": " + value);
               }
           }
        }
    }


    /**
     * Used to generate LDIF change/modify reocrd lines.
     *
     * <p>Turn entry DN, LDAPModification[] object into LDIF LDIF record
     * fields and then turn record fields into LDIF  change/modify record
     * lines</p>
     *
     * @param dn    String object representing entry DN
     * @param mods  LDAPModification array object
     *
     * @see LDAPModification
     * @see LDAPControl
     */
    public void writeRecordLines( String dn, LDAPModification[] mods )
    throws IOException, UnsupportedEncodingException {

        writeRecordLines( dn, mods, null );
    }


    /**
     * Used to generate LDIF change/modify reocrd lines.
     *
     * <p>Turn entry DN, LDAPModification[] object, and LDAPControl[] object
     * into LDIF LDIF record fields and then turn record fields into LDIF
     * change/modify record lines</p>
     *
     * @param dn    String object representing entry DN
     * @param mods  LDAPModification array object
     * @param ctrls LDAPControl array object
     *
     * @see LDAPModification
     * @see LDAPControl
     */
    public void writeRecordLines( String dn, LDAPModification[] mods,
    LDAPControl[] ctrls ) throws IOException, UnsupportedEncodingException {

        int i, modOp, len = mods.length;
        String attrName, attrValue;
        LDAPAttribute attr;

        // save controls if there is any
        if ( ctrls != null ) {
            writeControls( ctrls );
        }

        // save change type
        writeLine("changetype: modify");

        // save attribute names and values
        for ( i = 0; i < len; i++ ) {

            modOp = mods[i].getOp();
            attr =  mods[i].getAttribute();
            attrName = attr.getName();
            attrValue = attr.getStringValue();

            switch ( modOp )  {
                case LDAPModification.ADD:
                    writeLine("add: "+ attrName);
                    break;
                case LDAPModification.DELETE:
                    writeLine("delete: "+ attrName);
                    break;
                case LDAPModification.REPLACE:
                    writeLine("replace: "+ attrName);
                    break;
                default:
            }

            // add attribute names and values to record fields
            writeAttribute(attrName, attrValue);

            // add separators between different modify operations
            writeLine("-");
        }
    }


    /**
     * Used to generate LDIF change/moddn reocrd lines.
     *
     * <p>Turn entry DN and moddn information into LDIF change/modify
     * record lines</p>
     *
     * @param dn      String object representing entry DN
     * @param modInfo String array object contains modify info
     *
     */
    public void writeRecordLines( String dn, String[] modInfo )
    throws IOException, UnsupportedEncodingException {

        writeRecordLines( dn, modInfo, null );
    }


    /**
     * Used to generate LDIF change/moddn reocrd lines.
     *
     * <p>Turn entry DN and moddn information into LDIF change/modify
     * record lines</p>
     *
     * @param dn      String object representing entry DN
     * @param modInfo ModInfo object
     * @param ctrls   LDAPControl array object
     *
     */
    public void writeRecordLines( String dn, String[] modInfo,
    LDAPControl[] ctrls ) throws IOException, UnsupportedEncodingException {

        String tempString;

        if ( ctrls != null ) {
            writeControls( ctrls );
        }

        // save change type
        writeLine("changetype: moddn");

        // save new RDN
        if ( isSafe(modInfo[0]) ) {
            writeLine("newrdn:" + modInfo[0]);
        }
        else {
            // base64 encod newRDN
            tempString = base64Encoder.encoder(modInfo[0]);
            // put newRDN into record fields with a trailing space
            writeLine(new String("newrdn:" + tempString + " "));
        }

        // save deleteOldRDN
        writeLine("deleteoldrdn:" + modInfo[1]);

        // save newSuperior
        if ( ((modInfo[2]).length()) != 0) {

            if ( isSafe(modInfo[2]) ) {
                writeLine("newsuperior:" + modInfo[2]);
            }
            else {
                // base64 encod newRDN
                tempString = base64Encoder.encoder(modInfo[2]);
                // put newSuperior into record fields with a trailing space
                writeLine("newsuperior:" + tempString);
            }
        }
    }

    /**
     * Used to generate LDIF change/delete reocrd lines.
     *
     * <p>Turn entry DN, controls
     * and change type into LDIF change/delete record fields and then turn
     * record fields into LDIF moddn record lines</p>
     *
     * @param dn    String object representing entry DN
     * @param ctrls LDAPControl array object
     *
     * @return String array which contains the LDIF change/delete record lines
     *
     * @see LDAPControl
     */
    public void writeRecordLines( String dn, LDAPControl[] ctrls )
    throws IOException, UnsupportedEncodingException {

        // save controls if there is any
        if ( ctrls != null ) {
            writeControls( ctrls );
        }

        // save change type
        writeLine("changetype: delete");
    }


    public void writeDN(String dn)
    throws IOException, UnsupportedEncodingException {

        Base64Encoder base64Encoder = new Base64Encoder();

        if ( isSafe(dn) ) {
            // safe
            writeLine("dn: " + dn);
        }
        else {
            // not safe
            writeLine("dn:: " + base64Encoder.encoder(dn) + " ");
        }

    }


    /**
     * Convert LDAPControl array object to control fields in LDIF format
     *
     * @param ctrls LDAPControl array object
     *
     */
    public void writeControls(LDAPControl[] ctrls)
    throws IOException, UnsupportedEncodingException {

        boolean criticality;
        byte[]  byteValue = null;
        String  controlOID, controlValue;

        for ( int i = 0; i < ctrls.length; i++ ) {

            controlOID = ctrls[i].getID();
            criticality = ctrls[i].isCritical();
            byteValue = ctrls[i].getValue();

            if ( (byteValue != null) && (byteValue.length > 0) ) {

                // always encode control value(s) ?
                byteValue = base64Encoder.encoder( byteValue );

                controlValue = new String(byteValue, "UTF8");

                // a trailing space is add to the end of base64 encoded value
                writeLine( "control: " + controlOID + " " + criticality
                              + ":: " + controlValue + " " );
            }
            else {
                writeLine("control: " + controlOID + " " + criticality);
            }
        }
    }


    /**
     * Weite attribute name and value into outputStream.
     *
     * <p>Check if attrVal starts with NUL, LF, CR, ' ', ':', or '<'
     * or contains any NUL, LF, or CR and then write it out</p>
     */
    public void writeAttribute(String attrName, String attrVal)
    throws IOException, UnsupportedEncodingException {

        if (attrVal != null) {
            if ( isSafe(attrVal) ) {
                writeLine( attrName + ": " + attrVal );
            }
            else {
                // IF attrSpec contains NON-SAFE-INIT-CHAR or NON-SAFE-CHAR,
                // it has to be base64 encoded
                attrVal = base64Encoder.encoder(attrVal);
                // base64 encoded attribute spec ended with white spavce
                writeLine(attrName + ":: " + attrVal + " " );
            }
        }
    }


    /**
     * Weite attribute name and value into outputStream.
     *
     * <p>Check if attribute value contains NON-SAFE-INIT-CHAR or
     * NON-SAFE-CHAR. if it does, it needs to be base64 encoded and then
     * write it out</p>
     */
    public void writeAttribute(String attrName, byte[] attrVal)
    throws IOException, UnsupportedEncodingException {

        if ((attrVal != null)&&(attrVal.length != 0)) {
            if ( isSafe(attrVal) && isPrintable(attrVal) ) {
                // safe to make a String value
                writeLine( attrName + ": " + new String(attrVal) );
            }
            else {
                // not safe
                attrVal = base64Encoder.encoder(attrVal);
                // base64 encoded attriVal ends with white space
                writeLine(attrName + ":: " + new String(attrVal) + " " );
            }
        }
    }


    /**
     * Check if the input String object is a SAFE-STRING
     *
     * @param boolean object to incidate that the string is safe or not safe
     */
    public boolean isSafe(String value) {

       if (value.length() > 0) {
            if (value.length() > 1){
                // is there any NON-SAFE-INIT-CHAR
                if (      (value.charAt(0) == 0x00)     // NUL
                       || (value.charAt(0) == 0x0A)     // linefeeder
                       || (value.charAt(0) == 0x0D)     // carrage return
                       || (value.charAt(0) == 0x20)     // space(' ')
                       || (value.charAt(0) == 0x3A)     // colon(':')
                       || (value.charAt(0) == 0x3C)) {  // less-than('<')
                    return false;
                }
                // is there any NON-SAFE-CHAR
                for ( int i = 1; i < value.length(); i++ ) {
                    if (      (value.charAt(i) == 0x00)    // NUL
                           || (value.charAt(i) == 0x0A)    // linefeeder
                           || (value.charAt(i) == 0x0D)) { // carrage return
                        return false;
                    }
                }
            }
            else {
                // is there any NON-SAFE-INIT-CHAR
                if (      (value.charAt(0) == 0x00)     // NUL
                       || (value.charAt(0) == 0x0A)     // linefeeder
                       || (value.charAt(0) == 0x0D)     // carrage return
                       || (value.charAt(0) == 0x20)     // space(' ')
                       || (value.charAt(0) == 0x3A)     // colon(':')
                       || (value.charAt(0) == 0x3C)) {  // less-than('<')
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if the input byte array object is safe.
     *
     * <p>Check if the bytes starts with UL, LF, CR, ' ', ':', or '<',
     * or contains any NUL, LF, or CR</p>
     *
     * @param boolean object to incidate that the string is safe or not safe
     */
    public boolean isSafe(byte[] bytes) {

        if (bytes.length > 0) {
            if (bytes.length > 1){
                // is there any NON-SAFE-INIT-CHAR
                if (      (bytes[0] == 0x00)     // NUL
                       || (bytes[0] == 0x0A)     // linefeeder
                       || (bytes[0] == 0x0D)     // carrage return
                       || (bytes[0] == 0x20)     // space(' ')
                       || (bytes[0] == 0x3A)     // colon(':')
                       || (bytes[0] == 0x3C)) {  // less-than('<')
                    return false;
                }
                // is there any NON-SAFE-CHAR
                for ( int i = 1; i < bytes.length; i++ ) {
                    if (      (bytes[0] == 0x00)    // NUL
                           || (bytes[0] == 0x0A)    // linefeeder
                           || (bytes[0] == 0x0D)) { // carrage return
                        return false;
                    }
                }
            }
            else {
                // is there any NON-SAFE-INIT-CHAR
                if (      (bytes[0] == 0x00)     // NUL
                       || (bytes[0] == 0x0A)     // linefeeder
                       || (bytes[0] == 0x0D)     // carrage return
                       || (bytes[0] == 0x20)     // space(' ')
                       || (bytes[0] == 0x3A)     // colon(':')
                       || (bytes[0] == 0x3C)) {  // less-than('<')
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if the input byte array object is safe to make a String.
     *
     * <p>Check if the input byte array contains any un-printable value</p>
     *
     * @param bytes The byte array object to be checked.
     *
     * @return boolean object to incidate that the byte array
     * object is safe or not
     */
    public boolean isPrintable( byte[] bytes ) {

        if (bytes.length > 0) {
            for (int i=0; i<bytes.length; i++) {
                if ( (bytes[i]&0x00ff) < 0x20 || (bytes[i]&0x00ff) > 0x7e ) {
                    return false;
                }
            }
        }
        return true;
    }
}

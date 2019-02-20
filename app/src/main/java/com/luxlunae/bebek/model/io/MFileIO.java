/*
 * Original ADRIFT code Copyright (C) 1997 - 2018 Campbell Wild
 * This port and modifications Copyright (C) 2018 - 2019 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.luxlunae.bebek.model.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.VB;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MVariable;
import com.luxlunae.bebek.model.MWalk;
import com.luxlunae.bebek.model.state.MGameState;
import com.luxlunae.glk.GLKLogger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.Hidden;
import static com.luxlunae.bebek.model.MVariable.VariableType.Numeric;
import static com.luxlunae.bebek.model.io.IOUtils.decompressAndDeobfuscate;
import static com.luxlunae.bebek.model.io.IOUtils.readFileIntoByteArray;
import static com.luxlunae.bebek.model.io.MFile500.load500;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.TextAdventure_TAF;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.XMLModule_AMF;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.AllExceptProperties;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.Properties;
import static com.luxlunae.bebek.model.io.MFileOlder.convertLibraryTaskOverrides;
import static com.luxlunae.bebek.model.io.MFileOlder.loadOlder;
import static com.luxlunae.bebek.model.io.MFileOlder.tweakTasksForV4;

public class MFileIO {

    /**
     * 3rd Apr '11 - Released ADRIFT 5.0.16 Beta.
     */
    private static final byte VERSION_5[] = {
            60, 66, 63, (byte) 201, 106, (byte) 135, (byte) 194, (byte) 207, (byte) 146, 69, 62, 97
    };
    /**
     * 13th Feb ’02 - Released Beta release 0 of ADRIFT 4.00
     * 9th May ’02 - Released ADRIFT 4.00 release 18
     * 7th Oct ’03 - Released ADRIFT 4.00 release 42
     * 5th Oct '04 - Released ADRIFT 4.00 release 45
     * 14th Apr '05 - Released ADRIFT 4.00 release 46
     * 20th Apr '07 - Released ADRIFT 4.00 release 50
     * 20th Apr '08 - Released ADRIFT 4.00 release 51
     * <p>
     * Version 4.00 again was a big improvement over 3.90. This
     * was the first version to become Shareware – previous
     * versions had been Freeware. The main improvements were
     * the ability to play MP3s, much better TAF file compression,
     * images and sounds embedded into TAF files, tasks setting
     * and unsetting other tasks, text variables, referenced text,
     * lockable objects, adventure browser, graphics in main window,
     * advanced command construction, multiple object and character
     * aliases, initial object descriptions, and/or for task
     * restrictions, unlimited room descriptions, text editing
     * window, faster execution, improved battle system, random
     * character movement, object states, and modules.
     */
    private static final byte VERSION_4[] = {
            60, 66, 63, (byte) 201, 106, (byte) 135, (byte) 194, (byte) 207, (byte) 147, 69, 62, 97
    };
    /**
     *  1st Jan ’01 - Released ADRIFT 3.90
     * 27th May ’01 - Released Final release (20) of ADRIFT 3.90
     *
     * Version 3.90 was a big improvement over 3.80. It had an
     * improved layout, graphics and sound, a battle system,
     * Language Resource (ALR) files, feature disabling, system
     * variables, undo facility, score overriding, much improved
     * Player, 8 directional compass, room hiding, size and weight
     * for objects, unlimited actions and restrictions per task,
     * more powerful characters, integer variables, improved map,
     * transcript, pausing and many more small changes.
     */
    private static final byte VERSION_3_9[] = {
            60, 66, 63, (byte) 201, 106, (byte) 135, (byte) 194, (byte) 207, (byte) 148, 69, 55, 97
    };
    /**
     * 3rd Oct ’00
     *
     * Released ADRIFT 3.80
     *
     * Version 3.80 was the first version of ADRIFT to be published
     * in a magazine, and became downloadable from many places on the
     * Internet. Most of the improvements in this version were bug
     * fixes and fine tuning to the whole program, making it a lot
     * more reliable. Tasks were improved significantly.
     */
    private static final byte VERSION_3_8[] = {
            60, 66, 63, (byte) 201, 106, (byte) 135, (byte) 194, (byte) 207, (byte) 148, 69, 54, 97
    };
    static int mCorrectedTasksCount = 0;

    private static boolean loadFile(@NonNull MAdventure adv,
                                    @NonNull String filePath,
                                    FileTypeEnum fileType,
                                    LoadWhatEnum loadWhat, boolean isLibrary) {
        return loadFile(adv, filePath, fileType, loadWhat, isLibrary, null, 0);
    }

    public static boolean loadFile(@NonNull MAdventure adv,
                                   @NonNull String filePath,
                                   FileTypeEnum fileType,
                                   LoadWhatEnum loadWhat, boolean isLibrary,
                                   @Nullable MGameState[] state, long offset) {
        try {
            File f = new File(filePath);
            RandomAccessFile fileStream = new RandomAccessFile(f, "r");
            long szFile = f.length();

            if (fileType == TextAdventure_TAF) {
                fileStream.seek(szFile - 14);
                byte[] data = new byte[12];
                fileStream.read(data);
                byte[] pass = IOUtils.decode(data, (int) szFile - 13, 12);
                String passString = new String(pass, "UTF-8");
                adv.mPassword = (MGlobals.left(passString, 4) +
                        MGlobals.right(passString, 4)).trim();
            }

            if (offset > 0) {
                fileStream.seek(offset);
                offset += 7;  //  for the footer
            } else {
                fileStream.seek(0);
            }

            MGlobals.iLoading++;

            switch (fileType) {
                case Exe:
                    /* TODO */
                    break;

                case Blorb: {
                    if (MBlorb.loadBlorb(fileStream, filePath)) {
                        assert MBlorb.mExecResource != null;

                        // Work out which Adrift version this is
                        byte[] ver = new byte[12];
                        System.arraycopy(MBlorb.mExecResource, 0, ver, 0, 12);
                        String version;
                        if (Arrays.equals(ver, VERSION_5)) {
                            version = "Version 5.00";
                        } else if (Arrays.equals(ver, VERSION_4)) {
                            version = "Version 4.00";
                        } else if (Arrays.equals(ver, VERSION_3_9)) {
                            version = "Version 3.90";
                        } else if (Arrays.equals(ver, VERSION_3_8)) {
                            version = "Version 3.80";
                        } else {
                            version = new String(IOUtils.decode(ver, 1, 12), "UTF-8");
                        }
                        if (!version.startsWith("Version ")) {
                            adv.mView.errMsg("Not an ADRIFT Blorb file");
                            return false;
                        }
                        adv.mVersion = Double.parseDouble(version.replace("Version ", ""));

                        adv.setFilename(f.getName());
                        adv.setFullPath(filePath);

                        boolean deobfuscate = MBlorb.mMetadata == null ||
                                MBlorb.mMetadata.contains("compilerversion");  // Nasty, but works

                        ByteArrayOutputStream advFile = new ByteArrayOutputStream();

                        // Was this a pre-obfuscated size blorb?
                        if (MBlorb.mExecResource.length > 16 &&
                                (MBlorb.mExecResource[12] == 48) &&
                                (MBlorb.mExecResource[13] == 48) &&
                                (MBlorb.mExecResource[14] == 48) &&
                                (MBlorb.mExecResource[15] == 48)) {
                            if (!decompressAndDeobfuscate(MBlorb.mExecResource, advFile,
                                    deobfuscate, 16, MBlorb.mExecResource.length - 30)) {
                                GLKLogger.error("Failed to decompress blorb data.");
                                return false;
                            }
                        } else {
                            if (!decompressAndDeobfuscate(MBlorb.mExecResource, advFile,
                                    deobfuscate, 12, MBlorb.mExecResource.length - 26)) {
                                GLKLogger.error("Failed to decompress blorb data.");
                                return false;
                            }
                        }

                        if (!load500(adv, advFile.toByteArray(), false)) {
                            GLKLogger.error("Failed to load 500 version file.");
                            return false;
                        }

                        if (MBlorb.mMetadata != null) {
                            // adv.BabelTreatyInfo.FromString(MBlorb.mMetadata.OuterXml);
                            GLKLogger.error("TODO: loadBlorb: set babel info");
                        }
                    } else {
                        return false;
                    }
                    break;
                }

                case TextAdventure_TAF: {
                    // Work out which Adrift version this is
                    byte[] ver = new byte[12];
                    fileStream.read(ver);
                    String version;
                    if (Arrays.equals(ver, VERSION_5)) {
                        version = "Version 5.00";
                    } else if (Arrays.equals(ver, VERSION_4)) {
                        version = "Version 4.00";
                    } else if (Arrays.equals(ver, VERSION_3_9)) {
                        version = "Version 3.90";
                    } else if (Arrays.equals(ver, VERSION_3_8)) {
                        version = "Version 3.80";
                    } else {
                        version = new String(IOUtils.decode(ver, 1, 12), "UTF-8");
                    }
                    if (!version.startsWith("Version ")) {
                        adv.mView.errMsg("Not an ADRIFT Text Adventure file");
                        return false;
                    }
                    adv.mVersion = Double.parseDouble(version.replace("Version ", ""));

                    adv.setFilename(f.getName());
                    adv.setFullPath(filePath);

                    GLKLogger.debug("=== Start Load ===");
                    switch (version) {
                        case "Version 3.80":
                        case "Version 3.90":
                        case "Version 4.00":
                            loadLibraries(adv, Properties);
                            if (loadOlder(adv, fileStream, Double.valueOf(version.substring(8)))) {
                                loadLibraries(adv, AllExceptProperties);
                                tweakTasksForV4(adv);
                                convertLibraryTaskOverrides(adv);
                                adv.mVersion = Double.valueOf(version.substring(8));
                                if (adv.mVersion == 4) {
                                    adv.mVersion = 4.000052;
                                }
                            } else {
                                return false;
                            }
                            break;

                        case "Version 5.00":
                            byte sz[] = new byte[4];
                            fileStream.read(sz);
                            String size = new String(sz, "UTF-8");
                            byte chk[] = new byte[8];
                            fileStream.read(chk);
                            String check = new String(chk, "UTF-8");
                            int lenBabel = 0;
                            String babel = "";
                            boolean isObfuscated = true;

                            if (size.equals("0000") || check.equals("<ifindex")) {
                                // 5.0.20 format onwards
                                fileStream.seek(16);  // Set to just after the size chunk
                                try {
                                    // size is hexadecimal string
                                    lenBabel = Integer.parseInt(size, 16);
                                } catch (NumberFormatException e) {
                                    GLKLogger.error("Couldn't convert '" + size + "' to number.");
                                    lenBabel = 0;
                                }
                                if (lenBabel > 0) {
                                    byte[] bbl = new byte[lenBabel];
                                    fileStream.read(bbl);
                                    babel = new String(bbl, "UTF-8");
                                }
                                lenBabel += 4;  // For size header
                            } else {
                                // Pre 5.0.20
                                // THIS COULD BE AN EXTRACTED TAF, THEREFORE NO METADATA!!!
                                // Ok, we have no uncompressed Babel info at the start.
                                // Start over...
                                fileStream.seek(0);
                                fileStream.skipBytes(12);
                                isObfuscated = false;
                            }

                            ByteArrayOutputStream advFile = new ByteArrayOutputStream();
                            if (!readFileIntoByteArray(fileStream, advFile, true,
                                    (int) (szFile - 26 - offset - (long) lenBabel), isObfuscated)) {
                                GLKLogger.error("Failed to decompress data.");
                                return false;
                            }
                            if (!load500(adv, advFile.toByteArray(), false, loadWhat)) {
                                GLKLogger.error("Failed to load 500 version file.");
                                return false;
                            }
                            if (!babel.equals("")) {
                                // TODO
                                GLKLogger.error("TODO: Load500: set babel info");
                            }
                            break;

                        default:
                            adv.mView.errMsg("ADRIFT " + version + " Adventures are not " +
                                    "currently supported by this version of Bebek.");
                            return false;
                    }
                    GLKLogger.debug("=== End Load ===");
                    break;
                }

                case v4Module_AMF: {
                    GLKLogger.error("TODO: Version 4.0 Modules");
                    break;
                }

                case XMLModule_AMF: {
                    // ADRIFT library file
                    ByteArrayOutputStream amfFile = new ByteArrayOutputStream();
                    if (!readFileIntoByteArray(fileStream, amfFile, false,
                            (int) szFile, false)) {
                        GLKLogger.error("Failed to load AMF XML module.");
                        return false;
                    }
                    if (!load500(adv, amfFile.toByteArray(), isLibrary, loadWhat)) {
                        GLKLogger.error("Failed to load AMF XML module.");
                        return false;
                    }
                    break;
                }

                case GameState_TAS: {
                    // ADRIFT state file
                    assert state != null;

                    ByteArrayOutputStream tasFile = new ByteArrayOutputStream();
                    if (!readFileIntoByteArray(fileStream, tasFile, true,
                            (int) szFile, false)) {
                        GLKLogger.error("Failed to decompress save file.");
                        return false;
                    }
                    state[0] = loadState(adv, tasFile.toByteArray());
                    break;
                }
            }

            fileStream.close();

            if (adv.getNotUnderstood().equals("")) {
                adv.setNotUnderstood("Sorry, I didn't understand that command.");
            }

            // Ensure the player is at a valid start location
            if (adv.getPlayer() != null &&
                    (adv.getPlayer().getLocation().getExistsWhere() == Hidden ||
                            adv.getPlayer().getLocation().getKey().equals(""))) {
                // If the player's current location is hidden or not set,
                // move them to the first location in the location hash map.
                if (adv.mLocations.size() > 0) {
                    MCharacter.MCharacterLocation locFirst = adv.getPlayer().getLocation();
                    locFirst.setExistsWhere(AtLocation);
                    locFirst.setKey(adv.mLocations.keySet().iterator().next());
                    adv.getPlayer().moveTo(locFirst);
                }
            }

            MGlobals.iLoading--;
            return true;

        } catch (Exception ex) {
            adv.mView.errMsg("Error loading " + filePath, ex);
            return false;
        }
    }

    @Nullable
    private static String getElementText(@Nullable Node parentNode,
                                         @NonNull String childName) {
        if (parentNode == null) {
            return null;
        }
        NodeList nl = parentNode.getChildNodes();
        for (int i = 0, sz = nl.getLength(); i < sz; i++) {
            Node nod = nl.item(i);
            if (nod.getNodeType() == Node.ELEMENT_NODE &&
                    nod.getNodeName().equals(childName)) {
                return nod.getTextContent();
            }
        }
        return null;
    }

    public static void dumpRawTAF(@NonNull String inPath,
                                  @NonNull String outPath) throws IOException {
        // dumps raw TAF game code for Adrift versions 3.9, 4 or 5
        // for debugging
        // returns true if successfully dumped raw TAF code, false otherwise
        File f = new File(inPath);
        RandomAccessFile fin = new RandomAccessFile(f, "r");
        long szFile = f.length();
        fin.seek(0);

        try {
            // Work out which Adrift version this is
            byte[] bVersion = new byte[12];
            fin.read(bVersion);
            String sVersion;
            if (Arrays.equals(bVersion, VERSION_5)) {
                sVersion = "Version 5.00";
            } else if (Arrays.equals(bVersion, VERSION_4)) {
                sVersion = "Version 4.00";
            } else if (Arrays.equals(bVersion, VERSION_3_9)) {
                sVersion = "Version 3.90";
            } else if (Arrays.equals(bVersion, VERSION_3_8)) {
                sVersion = "Version 3.80";
            } else {
                sVersion = new String(IOUtils.decode(bVersion, 1, 12), "UTF-8");
            }

            if (!sVersion.startsWith("Version ")) {
                throw new IOException("Not an ADRIFT Text Adventure file");
            }

            switch (sVersion) {
                case "Version 3.80":
                case "Version 3.90":
                case "Version 4.00": {
                    byte[] bAdvZLib;
                    byte[] bAdventure;
                    double v = Double.valueOf(sVersion.substring(8));
                    if (v < 4) {
                        fin.seek(0);
                        byte[] bRawData = new byte[(int) fin.length()];
                        fin.read(bRawData);
                        bAdventure = IOUtils.decode(bRawData, 0, bRawData.length);
                    } else {
                        fin.skipBytes(2); // CrLf
                        byte[] bSize = new byte[8];
                        fin.read(bSize);
                        long lSize = Long.valueOf(new String(bSize, "windows-1252"));
                        bAdvZLib = new byte[(int) (lSize - 23)];
                        fin.read(bAdvZLib);
                        byte[] bPass = new byte[12];
                        fin.read(bPass);
                        ByteArrayOutputStream zStream = new ByteArrayOutputStream();
                        if (!IOUtils.decompress(ByteBuffer.wrap(bAdvZLib), zStream)) {
                            throw new IOException("Could not decompress v4 game");
                        }
                        bAdventure = zStream.toByteArray();
                    }

                    // Dump the ADRIFT 3.80 / 3.90 / 4 file
                    FileOutputStream fos = new FileOutputStream(outPath);
                    fos.write(bAdventure);
                    fos.close();
                    return;
                }

                case "Version 5.00": {
                    byte bSize[] = new byte[4];
                    fin.read(bSize);
                    String sSize = new String(bSize, "UTF-8");
                    byte bCheck[] = new byte[8];
                    fin.read(bCheck);
                    String sCheck = new String(bCheck, "UTF-8");
                    int iBabelLength = 0;
                    boolean bObfuscate = true;

                    if (sSize.equals("0000") || sCheck.equals("<ifindex")) {
                        // 5.0.20 format onwards
                        fin.seek(16);  // Set to just after the size chunk
                        try {
                            iBabelLength = Integer.parseInt(sSize, 16);  // hexadecimal
                        } catch (NumberFormatException e) {
                            GLKLogger.error("Couldn't convert '" + sSize + "' to number.");
                            iBabelLength = 0;
                        }
                        if (iBabelLength > 0) {
                            byte[] bBabel = new byte[iBabelLength];
                            fin.read(bBabel);
                        }
                        iBabelLength += 4;  // For size header
                    } else {
                        // Pre 5.0.20
                        fin.seek(0);
                        fin.skipBytes(12);
                        bObfuscate = false;
                    }

                    ByteArrayOutputStream stmFile = new ByteArrayOutputStream();
                    if (!readFileIntoByteArray(fin, stmFile, true,
                            (int) (szFile - 26 - (long) iBabelLength), bObfuscate)) {
                        throw new IOException("Failed to decompress ADRIFT 5 data.");
                    }

                    // Dump the ADRIFT 5 XML file
                    FileOutputStream fos = new FileOutputStream(outPath);
                    fos.write(stmFile.toByteArray());
                    fos.close();
                    return;
                }

                default:
                    throw new IOException("ADRIFT " + sVersion + " Adventures are not currently supported by this version of Bebek.");
            }
        } finally {
            fin.close();
        }
    }

    private static void loadLibraries(@NonNull MAdventure adv, LoadWhatEnum loadWhat) {
        // TODO: Allow the user to load libraries other than
        // StandardLibrary.amf. Original ADRIFT runner had
        // a new line separated library list of format
        // <Library Name>[#[TRUE|FALSE]], where the optional
        // boolean value indicated whether the library should
        // be loaded or not. Libraries would be loaded in order
        // they appeared in the list.
        String[] libraries = new String[1];
        libraries[0] = adv.getLibAdriftPath() + "StandardLibrary.amf";

        for (String lib : libraries) {
            loadFile(adv, lib, XMLModule_AMF, loadWhat, true);
        }
    }

    @Nullable
    private static MGameState loadState(@NonNull MAdventure adv, @NonNull byte[] xmlBytes) {
        try {
            MGameState state = new MGameState();
            Document xmlDocument;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setCoalescing(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            xmlDocument = db.parse(new ByteArrayInputStream(xmlBytes));
            xmlDocument.getDocumentElement().normalize();

            // Try to find the Game root node
            NodeList nll = xmlDocument.getChildNodes();
            Node r = null;
            for (int i = 0; i < nll.getLength(); i++) {
                if (nll.item(i).getNodeName().equals("Game")) {
                    r = nll.item(i);
                    break;
                }
            }
            if (r == null) {
                throw new Exception("Could not find root 'Game' node in XML. Aborting.");
            }
            String s;

            // =========================
            //       LOCATIONS
            // =========================
            NodeList nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodLoc = nl.item(i);
                if (!nodLoc.getNodeName().equals("Location")) {
                    continue;
                }

                MGameState.MLocationState locs = new MGameState.MLocationState();
                String sKey = getElementText(nodLoc, "Key");

                NodeList nl2 = nodLoc.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodProp = nl2.item(j);
                    if (!nodProp.getNodeName().equals("Property")) {
                        continue;
                    }
                    MGameState.MLocationState.MStateProperty props =
                            new MGameState.MLocationState.MStateProperty();
                    String sPropKey = getElementText(nodProp, "Key");
                    props.Value = getElementText(nodProp, "Value");
                    locs.htblProperties.put(sPropKey, props);
                }

                nl2 = nodLoc.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodDisplayed = nl2.item(j);
                    if (!nodDisplayed.getNodeName().equals("Displayed")) {
                        continue;
                    }
                    locs.htblDisplayedDescriptions.put(nodDisplayed.getTextContent(), true);
                }
                state.htblLocationStates.put(sKey, locs);
            }

            // =========================
            //       OBJECTS
            // =========================
            nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodOb = nl.item(i);
                if (!nodOb.getNodeName().equals("Object")) {
                    continue;
                }

                MGameState.MObjectState obs = new MGameState.MObjectState();
                obs.Location = new MObject.MObjectLocation(adv);
                String sKey = getElementText(nodOb, "Key");
                if ((s = getElementText(nodOb, "DynamicExistWhere")) != null) {
                    obs.Location.mDynamicExistWhere =
                            MObject.MObjectLocation.DynamicExistsWhereEnum.valueOf(s);
                } else {
                    obs.Location.mDynamicExistWhere =
                            MObject.MObjectLocation.DynamicExistsWhereEnum.Hidden;
                }
                if ((s = getElementText(nodOb, "StaticExistWhere")) != null) {
                    obs.Location.mStaticExistWhere =
                            MObject.MObjectLocation.StaticExistsWhereEnum.valueOf(s);
                } else {
                    obs.Location.mStaticExistWhere =
                            MObject.MObjectLocation.StaticExistsWhereEnum.NoRooms;
                }
                if ((s = getElementText(nodOb, "LocationKey")) != null) {
                    obs.Location.setKey(s);
                }

                NodeList nl2 = nodOb.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodProp = nl2.item(j);
                    if (!nodProp.getNodeName().equals("Property")) {
                        continue;
                    }
                    MGameState.MObjectState.MStateProperty props =
                            new MGameState.MObjectState.MStateProperty();
                    String sPropKey = getElementText(nodProp, "Key");
                    props.Value = getElementText(nodProp, "Value");
                    obs.htblProperties.put(sPropKey, props);
                }

                nl2 = nodOb.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodDisplayed = nl2.item(j);
                    if (!nodDisplayed.getNodeName().equals("Displayed")) {
                        continue;
                    }
                    obs.htblDisplayedDescriptions.put(nodDisplayed.getTextContent(), true);
                }

                state.htblObjectStates.put(sKey, obs);
            }

            // =========================
            //       TASKS
            // =========================
            nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodTas = nl.item(i);
                if (!nodTas.getNodeName().equals("Task")) {
                    continue;
                }

                MGameState.MTaskState tas = new MGameState.MTaskState();
                String sKey = getElementText(nodTas, "Key");
                tas.Completed = VB.cbool(getElementText(nodTas, "Completed"));
                if ((s = getElementText(nodTas, "Scored")) != null) {
                    tas.Scored = VB.cbool(s);
                }

                NodeList nl2 = nodTas.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodDisplayed = nl2.item(j);
                    if (!nodDisplayed.getNodeName().equals("Displayed")) {
                        continue;
                    }
                    tas.htblDisplayedDescriptions.put(nodDisplayed.getTextContent(), true);
                }

                state.htblTaskStates.put(sKey, tas);
            }

            // =========================
            //       EVENTS
            // =========================
            nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodEv = nl.item(i);
                if (!nodEv.getNodeName().equals("Event")) {
                    continue;
                }

                MGameState.MEventState evs = new MGameState.MEventState();
                String sKey = getElementText(nodEv, "Key");
                evs.Status = MEvent.StatusEnum.valueOf(getElementText(nodEv, "Status"));
                evs.TimerToEndOfEvent = adv.safeInt(getElementText(nodEv, "Timer"));
                if ((s = getElementText(nodEv, "SubEventTime")) != null) {
                    evs.iLastSubEventTime = adv.safeInt(s);
                }
                if ((s = getElementText(nodEv, "SubEventIndex")) != null) {
                    evs.iLastSubEventIndex = adv.safeInt(s);
                }

                NodeList nl2 = nodEv.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodDisplayed = nl2.item(j);
                    if (!nodDisplayed.getNodeName().equals("Displayed")) {
                        continue;
                    }
                    evs.htblDisplayedDescriptions.put(nodDisplayed.getTextContent(), true);
                }

                state.htblEventStates.put(sKey, evs);
            }

            // =========================
            //       CHARACTERS
            // =========================
            nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodCh = nl.item(i);
                if (!nodCh.getNodeName().equals("Character")) {
                    continue;
                }

                MGameState.MCharacterState chs = new MGameState.MCharacterState();
                String sKey = getElementText(nodCh, "Key");
                if (adv.mCharacters.containsKey(sKey)) {
                    chs.Location =
                            new MCharacter.MCharacterLocation(adv, adv.mCharacters.get(sKey));
                    if ((s = getElementText(nodCh, "ExistWhere")) != null) {
                        chs.Location.setExistsWhere(MCharacter.MCharacterLocation.ExistsWhere.valueOf(s));
                    } else {
                        chs.Location.setExistsWhere(Hidden);
                    }
                    if ((s = getElementText(nodCh, "Position")) != null) {
                        chs.Location.setPosition(MCharacter.MCharacterLocation.Position.valueOf(s));
                    } else {
                        chs.Location.setPosition(MCharacter.MCharacterLocation.Position.Standing);
                    }
                    if ((s = getElementText(nodCh, "LocationKey")) != null) {
                        chs.Location.setKey(s);
                    } else {
                        chs.Location.setKey("");
                    }

                    NodeList nl2 = nodCh.getChildNodes();
                    for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                        Node nodW = nl2.item(j);
                        if (!nodW.getNodeName().equals("Walk")) {
                            continue;
                        }
                        MGameState.MCharacterState.MWalkState ws =
                                new MGameState.MCharacterState.MWalkState();
                        ws.Status = MWalk.StatusEnum.valueOf(getElementText(nodW, "Status"));
                        ws.TimerToEndOfWalk = adv.safeInt(getElementText(nodW, "Timer"));
                        chs.lWalks.add(ws);
                    }

                    nl2 = nodCh.getChildNodes();
                    for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                        Node nodProp = nl2.item(j);
                        if (!nodProp.getNodeName().equals("Property")) {
                            continue;
                        }
                        MGameState.MCharacterState.MStateProperty props =
                                new MGameState.MCharacterState.MStateProperty();
                        String sPropKey = getElementText(nodProp, "Key");
                        props.Value = getElementText(nodProp, "Value");
                        chs.htblProperties.put(sPropKey, props);
                    }

                    nl2 = nodCh.getChildNodes();
                    for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                        Node nodSeen = nl2.item(j);
                        if (!nodSeen.getNodeName().equals("Seen")) {
                            continue;
                        }
                        chs.lSeenKeys.add(nodSeen.getTextContent());
                    }

                    nl2 = nodCh.getChildNodes();
                    for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                        Node nodDisplayed = nl2.item(j);
                        if (!nodDisplayed.getNodeName().equals("Displayed")) {
                            continue;
                        }
                        chs.htblDisplayedDescriptions.put(nodDisplayed.getTextContent(), true);
                    }

                    state.htblCharacterStates.put(sKey, chs);
                } else {
                    adv.mView.displayError("Character key " + sKey + " not found in adventure!");
                }
            }

            // =========================
            //       VARIABLES
            // =========================
            nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodVar = nl.item(i);
                if (!nodVar.getNodeName().equals("Variable")) {
                    continue;
                }

                MGameState.MVariableState vars = new MGameState.MVariableState();
                String sKey = getElementText(nodVar, "Key");
                if (adv.mVariables.containsKey(sKey)) {
                    MVariable v = adv.mVariables.get(sKey);
                    vars.Value = new String[v.getLength()];

                    for (int ii = 0; ii < v.getLength(); ii++) {
                        if ((s = getElementText(nodVar, "Value_" + ii)) != null) {
                            vars.Value[ii] = s;
                        } else {
                            if (v.getType() == Numeric) {
                                vars.Value[ii] = "0";
                            } else {
                                vars.Value[ii] = "";
                            }
                        }
                    }
                    if ((s = getElementText(nodVar, "Value")) != null) { // Old style
                        vars.Value[0] = s;
                    }

                    NodeList nl2 = nodVar.getChildNodes();
                    for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                        Node nodDisplayed = nl2.item(j);
                        if (!nodDisplayed.getNodeName().equals("Displayed")) {
                            continue;
                        }
                        vars.htblDisplayedDescriptions.put(nodDisplayed.getTextContent(), true);
                    }

                    state.htblVariableStates.put(sKey, vars);
                } else {
                    adv.mView.displayError("Variable key " + sKey + " not found in adventure!");
                }
            }

            // =========================
            //       GROUPS
            // =========================
            nl = r.getChildNodes();
            for (int i = 0, sz = nl.getLength(); i < sz; i++) {
                Node nodGrp = nl.item(i);
                if (!nodGrp.getNodeName().equals("Group")) {
                    continue;
                }

                MGameState.MGroupState grps = new MGameState.MGroupState();
                String sKey = getElementText(nodGrp, "Key");

                NodeList nl2 = nodGrp.getChildNodes();
                for (int j = 0, sz2 = nl2.getLength(); j < sz2; j++) {
                    Node nodMember = nl2.item(j);
                    if (!nodMember.getNodeName().equals("Member")) {
                        continue;
                    }
                    grps.lstMembers.add(nodMember.getTextContent());
                }

                state.htblGroupStates.put(sKey, grps);
            }

            if ((s = getElementText(r, "Turns")) != null) {
                adv.mTurns = adv.safeInt(s);
            }

            return state;

        } catch (Exception ex) {
            adv.mView.errMsg("Error loading game state", ex);
        }

        return null;
    }

    public static boolean saveState(@NonNull MAdventure adv, @NonNull MGameState state,
                                    @NonNull String filePath) {
        // Write a state to compressed UTF-8 xml file
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder xmlWriter = docFactory.newDocumentBuilder();
            Document xmlDoc = xmlWriter.newDocument();
            Element rootElement = xmlDoc.createElement("Game");
            xmlDoc.appendChild(rootElement);

            // TODO: Ideally this should only save values
            // that are different from the initial TAF state

            // LOCATIONS
            for (String sKey : state.htblLocationStates.keySet()) {
                MGameState.MLocationState locs = state.htblLocationStates.get(sKey);
                Element locElement = xmlDoc.createElement("Location");
                Element locKey = xmlDoc.createElement("Key");
                locKey.setTextContent(sKey);
                locElement.appendChild(locKey);

                for (String sPropKey : locs.htblProperties.keySet()) {
                    MGameState.MLocationState.MStateProperty props =
                            locs.htblProperties.get(sPropKey);
                    Element prop = xmlDoc.createElement("Property");
                    Element key = xmlDoc.createElement("Key");
                    key.setTextContent(sPropKey);
                    Element val = xmlDoc.createElement("Value");
                    val.setTextContent(props.Value);
                    prop.appendChild(key);
                    prop.appendChild(val);
                    locElement.appendChild(prop);
                }

                for (String sDisplayedKey : locs.htblDisplayedDescriptions.keySet()) {
                    Element d = xmlDoc.createElement("Displayed");
                    d.setTextContent(sDisplayedKey);
                    locElement.appendChild(d);
                }

                rootElement.appendChild(locElement);
            }

            // OBJECTS
            for (String sKey : state.htblObjectStates.keySet()) {
                MGameState.MObjectState obs = state.htblObjectStates.get(sKey);
                Element objElement = xmlDoc.createElement("Object");
                Element objKey = xmlDoc.createElement("Key");
                objKey.setTextContent(sKey);
                objElement.appendChild(objKey);

                if (obs.Location.mDynamicExistWhere !=
                        MObject.MObjectLocation.DynamicExistsWhereEnum.Hidden) {
                    Element e = xmlDoc.createElement("DynamicExistWhere");
                    e.setTextContent(obs.Location.mDynamicExistWhere.toString());
                    objElement.appendChild(e);
                }
                if (obs.Location.mStaticExistWhere !=
                        MObject.MObjectLocation.StaticExistsWhereEnum.NoRooms) {
                    Element e = xmlDoc.createElement("StaticExistWhere");
                    e.setTextContent(obs.Location.mStaticExistWhere.toString());
                    objElement.appendChild(e);
                }

                Element lk = xmlDoc.createElement("LocationKey");
                lk.setTextContent(obs.Location.getKey());
                objElement.appendChild(lk);

                for (String sPropKey : obs.htblProperties.keySet()) {
                    MGameState.MObjectState.MStateProperty props =
                            obs.htblProperties.get(sPropKey);
                    Element prop = xmlDoc.createElement("Property");
                    Element key = xmlDoc.createElement("Key");
                    key.setTextContent(sPropKey);
                    Element val = xmlDoc.createElement("Value");
                    val.setTextContent(props.Value);
                    prop.appendChild(key);
                    prop.appendChild(val);
                    objElement.appendChild(prop);
                }

                for (String sDisplayedKey : obs.htblDisplayedDescriptions.keySet()) {
                    Element d = xmlDoc.createElement("Displayed");
                    d.setTextContent(sDisplayedKey);
                    objElement.appendChild(d);
                }

                rootElement.appendChild(objElement);
            }

            // TASKS
            for (String sKey : state.htblTaskStates.keySet()) {
                MGameState.MTaskState tas = state.htblTaskStates.get(sKey);
                Element taskElement = xmlDoc.createElement("Task");
                Element taskKey = xmlDoc.createElement("Key");
                taskKey.setTextContent(sKey);
                taskElement.appendChild(taskKey);
                Element taskCompleted = xmlDoc.createElement("Completed");
                taskCompleted.setTextContent(String.valueOf(tas.Completed));
                taskElement.appendChild(taskCompleted);
                Element taskScored = xmlDoc.createElement("Scored");
                taskScored.setTextContent(String.valueOf(tas.Scored));
                taskElement.appendChild(taskScored);

                for (String sDisplayedKey : tas.htblDisplayedDescriptions.keySet()) {
                    Element d = xmlDoc.createElement("Displayed");
                    d.setTextContent(sDisplayedKey);
                    taskElement.appendChild(d);
                }

                rootElement.appendChild(taskElement);
            }

            // EVENTS
            for (String sKey : state.htblEventStates.keySet()) {
                MGameState.MEventState evs = state.htblEventStates.get(sKey);
                Element evElement = xmlDoc.createElement("Event");
                Element evKey = xmlDoc.createElement("Key");
                evKey.setTextContent(sKey);
                evElement.appendChild(evKey);
                Element evStatus = xmlDoc.createElement("Status");
                evStatus.setTextContent(evs.Status.toString());
                evElement.appendChild(evStatus);
                Element evTimer = xmlDoc.createElement("Timer");
                evTimer.setTextContent(String.valueOf(evs.TimerToEndOfEvent));
                evElement.appendChild(evTimer);
                Element evSEventTime = xmlDoc.createElement("SubEventTime");
                evSEventTime.setTextContent(String.valueOf(evs.iLastSubEventTime));
                evElement.appendChild(evSEventTime);
                Element evSEventIndex = xmlDoc.createElement("SubEventIndex");
                evSEventIndex.setTextContent(String.valueOf(evs.iLastSubEventIndex));
                evElement.appendChild(evSEventIndex);

                for (String sDisplayedKey : evs.htblDisplayedDescriptions.keySet()) {
                    Element d = xmlDoc.createElement("Displayed");
                    d.setTextContent(sDisplayedKey);
                    evElement.appendChild(d);
                }

                rootElement.appendChild(evElement);
            }

            // CHARACTERS
            for (String sKey : state.htblCharacterStates.keySet()) {
                MGameState.MCharacterState chs = state.htblCharacterStates.get(sKey);
                Element chElement = xmlDoc.createElement("Character");
                Element chKey = xmlDoc.createElement("Key");
                chKey.setTextContent(sKey);
                chElement.appendChild(chKey);

                if (chs.Location.getExistsWhere() != Hidden) {
                    Element e = xmlDoc.createElement("ExistWhere");
                    e.setTextContent(chs.Location.getExistsWhere().toString());
                    chElement.appendChild(e);
                }

                if (chs.Location.getPosition() !=
                        MCharacter.MCharacterLocation.Position.Standing) {
                    Element e = xmlDoc.createElement("Position");
                    e.setTextContent(chs.Location.getPosition().toString());
                    chElement.appendChild(e);
                }

                if (!chs.Location.getKey().equals("")) {
                    Element e = xmlDoc.createElement("LocationKey");
                    e.setTextContent(chs.Location.getKey());
                    chElement.appendChild(e);
                }

                for (MGameState.MCharacterState.MWalkState ws : chs.lWalks) {
                    Element e = xmlDoc.createElement("Walk");
                    Element e2 = xmlDoc.createElement("Status");
                    e2.setTextContent(ws.Status.toString());
                    e.appendChild(e2);
                    Element e3 = xmlDoc.createElement("Timer");
                    e3.setTextContent(String.valueOf(ws.TimerToEndOfWalk));
                    e.appendChild(e3);
                    chElement.appendChild(e);
                }

                for (String sSeen : chs.lSeenKeys) {
                    Element e = xmlDoc.createElement("Seen");
                    e.setTextContent(sSeen);
                    chElement.appendChild(e);
                }

                for (String sPropKey : chs.htblProperties.keySet()) {
                    MGameState.MCharacterState.MStateProperty props =
                            chs.htblProperties.get(sPropKey);
                    Element prop = xmlDoc.createElement("Property");
                    Element key = xmlDoc.createElement("Key");
                    key.setTextContent(sPropKey);
                    Element val = xmlDoc.createElement("Value");
                    val.setTextContent(props.Value);
                    prop.appendChild(key);
                    prop.appendChild(val);
                    chElement.appendChild(prop);
                }

                for (String sDisplayedKey : chs.htblDisplayedDescriptions.keySet()) {
                    Element d = xmlDoc.createElement("Displayed");
                    d.setTextContent(sDisplayedKey);
                    chElement.appendChild(d);
                }

                rootElement.appendChild(chElement);
            }

            // VARIABLES
            for (String sKey : state.htblVariableStates.keySet()) {
                MGameState.MVariableState vars = state.htblVariableStates.get(sKey);
                Element varElement = xmlDoc.createElement("Variable");
                Element varKey = xmlDoc.createElement("Key");
                varKey.setTextContent(sKey);
                varElement.appendChild(varKey);

                MVariable v = adv.mVariables.get(sKey);
                for (int i = 0; i < vars.Value.length; i++) {
                    if (v.getType() == Numeric) {
                        if (!vars.Value[i].equals("0")) {
                            Element e = xmlDoc.createElement("Value_" + i);
                            e.setTextContent(vars.Value[i]);
                            varElement.appendChild(e);
                        }
                    } else {
                        if (!vars.Value[i].equals("")) {
                            Element e = xmlDoc.createElement("Value_" + i);
                            e.setTextContent(vars.Value[i]);
                            varElement.appendChild(e);
                        }
                    }
                }

                for (String sDisplayedKey : vars.htblDisplayedDescriptions.keySet()) {
                    Element d = xmlDoc.createElement("Displayed");
                    d.setTextContent(sDisplayedKey);
                    varElement.appendChild(d);
                }

                rootElement.appendChild(varElement);
            }

            // GROUPS
            for (String sKey : state.htblGroupStates.keySet()) {
                MGameState.MGroupState grps = state.htblGroupStates.get(sKey);
                Element grpElement = xmlDoc.createElement("Group");
                Element grpKey = xmlDoc.createElement("Key");
                grpKey.setTextContent(sKey);
                grpElement.appendChild(grpKey);

                for (String sMember : grps.lstMembers) {
                    Element e = xmlDoc.createElement("Member");
                    e.setTextContent(sMember);
                    grpElement.appendChild(e);
                }

                rootElement.appendChild(grpElement);
            }

            Element e = xmlDoc.createElement("Turns");
            e.setTextContent(String.valueOf(adv.mTurns));
            rootElement.appendChild(e);
            DOMSource source = new DOMSource(xmlDoc);

            // write xml to a byte stream
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(byteOutput);
            transformer.transform(source, result);

            // compress byte stream and write to the output file
            ByteArrayOutputStream bData = new ByteArrayOutputStream();
            if (!IOUtils.compress(byteOutput.toByteArray(), bData)) {
                GLKLogger.error("Error compressing save game data");
                return false;
            }
            byte[] bb = bData.toByteArray();
            DataOutputStream stmFile = new DataOutputStream(new FileOutputStream(filePath));
            stmFile.write(bb);
            stmFile.close();
            return true;

        } catch (Exception ex) {
            adv.mView.errMsg("Error saving game state", ex);
            return false;
        }
    }

    @NonNull
    public static String correctBracketSequence(@NonNull String seq) {
        // Corrects the bracket sequences for ORs after ANDs
        if (seq.contains("#A#O#")) {
            for (int i = 10; i >= 0; i--) {
                StringBuilder toFind = new StringBuilder("#A#");
                for (int j = 0; j <= i; j++) {
                    toFind.append("O#");
                }
                StringBuilder toReplace = new StringBuilder("#A(#");
                for (int j = 0; j <= i; j++) {
                    toReplace.append("O#");
                }
                toReplace.append(")");
                while (seq.contains(toFind)) {
                    seq = seq.replace(toFind, toReplace);
                    mCorrectedTasksCount++;
                }
            }
        }
        return seq;
    }

    @NonNull
    public static String fixInitialRefs(@Nullable String cmd) {
        if (cmd == null) {
            return "";
        }
        return cmd.replace("%object%", "%object1%")
                .replace("%character%", "%character1%")
                .replace("%location%", "%location1%")
                .replace("%number%", "%number1%")
                .replace("%text%", "%text1%")
                .replace("%item%", "%item1%")
                .replace("%direction%", "%direction1%");
    }

    public enum LoadItemEnum {
        No,                     // 0
        Yes,                    // 1
        Both                    // 2
    }

    public enum FileTypeEnum {
        TextAdventure_TAF,      // 0
        XMLModule_AMF,          // 1
        v4Module_AMF,           // 2
        GameState_TAS,          // 3
        Blorb,                  // 4
        Exe                     // 5
    }

    public enum LoadWhatEnum {
        All,                    // 0
        Properties,             // 1
        AllExceptProperties     // 2
    }
}
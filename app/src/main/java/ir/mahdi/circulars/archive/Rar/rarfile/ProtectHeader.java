/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 24.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package ir.mahdi.circulars.archive.Rar.rarfile;

import ir.mahdi.circulars.archive.Rar.io.Raw;

public class ProtectHeader extends BlockHeader {

    public static final int protectHeaderSize = 8;

    private byte version;
    private short recSectors;
    private int totalBlocks;
    private byte mark;


    public ProtectHeader(BlockHeader bh, byte[] protectHeader) {
        super(bh);

        int pos = 0;
        version |= protectHeader[pos] & 0xff;

        recSectors = Raw.readShortLittleEndian(protectHeader, pos);
        pos += 2;
        totalBlocks = Raw.readIntLittleEndian(protectHeader, pos);
        pos += 4;
        mark |= protectHeader[pos] & 0xff;
    }


    public byte getMark() {
        return mark;
    }

    public short getRecSectors() {
        return recSectors;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public byte getVersion() {
        return version;
    }
}

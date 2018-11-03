/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 01.06.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
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
package ir.mahdi.circulars.archive.Rar.unpack.decode;

public class Decode {
    private final int[] decodeLen = new int[16];
    private final int[] decodePos = new int[16];
    protected int[] decodeNum = new int[2];
    private int maxNum;

    public int[] getDecodeLen() {
        return decodeLen;
    }

    public int[] getDecodeNum() {
        return decodeNum;
    }

    public int[] getDecodePos() {
        return decodePos;
    }

    public int getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(int maxNum) {
        this.maxNum = maxNum;
    }

}

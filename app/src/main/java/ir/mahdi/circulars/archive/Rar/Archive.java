/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 22.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression
 * algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package ir.mahdi.circulars.archive.Rar;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ir.mahdi.circulars.archive.Rar.exception.RarException;
import ir.mahdi.circulars.archive.Rar.exception.RarException.RarExceptionType;
import ir.mahdi.circulars.archive.Rar.impl.FileVolumeManager;
import ir.mahdi.circulars.archive.Rar.io.IReadOnlyAccess;
import ir.mahdi.circulars.archive.Rar.rarfile.AVHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.BaseBlock;
import ir.mahdi.circulars.archive.Rar.rarfile.BlockHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.CommentHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.EAHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.EndArcHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.FileHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.MacInfoHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.MainHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.MarkHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.ProtectHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.SignHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.SubBlockHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.UnixOwnersHeader;
import ir.mahdi.circulars.archive.Rar.rarfile.UnrarHeadertype;
import ir.mahdi.circulars.archive.Rar.unpack.ComprDataIO;
import ir.mahdi.circulars.archive.Rar.unpack.Unpack;


public class Archive implements Closeable {
    private static Logger logger = Logger.getLogger(Archive.class.getName());
    private final UnrarCallback unrarCallback;
    private final ComprDataIO dataIO;
    private final List<BaseBlock> headers = new ArrayList<BaseBlock>();
    private IReadOnlyAccess rof;
    private MarkHeader markHead = null;

    private MainHeader newMhd = null;

    private Unpack unpack;

    private int currentHeaderIndex;

    private long totalPackedSize = 0L;

    private long totalPackedRead = 0L;

    private VolumeManager volumeManager;
    private Volume volume;

    public Archive(VolumeManager volumeManager) throws RarException,
            IOException {
        this(volumeManager, null);
    }

    public Archive(VolumeManager volumeManager, UnrarCallback unrarCallback)
            throws IOException {
        this.volumeManager = volumeManager;
        this.unrarCallback = unrarCallback;

        setVolume(this.volumeManager.nextArchive(this, null));
        dataIO = new ComprDataIO(this);
    }

    public Archive(File firstVolume) throws RarException, IOException {
        this(new FileVolumeManager(firstVolume), null);
    }

    public Archive(File firstVolume, UnrarCallback unrarCallback)
            throws RarException, IOException {
        this(new FileVolumeManager(firstVolume), unrarCallback);
    }

    // public File getFile() {
    // return file;
    // }
    //
    // void setFile(File file) throws IOException {
    // this.file = file;
    // setFile(new ReadOnlyAccessFile(file), file.length());
    // }

    private void setFile(IReadOnlyAccess file, long length) throws IOException {
        totalPackedSize = 0L;
        totalPackedRead = 0L;
        close();
        rof = file;
        try {
            readHeaders(length);
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "exception in archive constructor maybe file is encrypted "
                            + "or currupt", e);
            // ignore exceptions to allow exraction of working files in
            // corrupt archive
        }
        // Calculate size of packed data
        for (BaseBlock block : headers) {
            if (block.getHeaderType() == UnrarHeadertype.FileHeader) {
                totalPackedSize += ((FileHeader) block).getFullPackSize();
            }
        }
        if (unrarCallback != null) {
            unrarCallback.volumeProgressChanged(totalPackedRead,
                    totalPackedSize);
        }
    }

    public void bytesReadRead(int count) {
        if (count > 0) {
            totalPackedRead += count;
            if (unrarCallback != null) {
                unrarCallback.volumeProgressChanged(totalPackedRead,
                        totalPackedSize);
            }
        }
    }

    public IReadOnlyAccess getRof() {
        return rof;
    }

    public List<FileHeader> getFileHeaders() {
        List<FileHeader> list = new ArrayList<FileHeader>();
        for (BaseBlock block : headers) {
            if (block.getHeaderType().equals(UnrarHeadertype.FileHeader)) {
                list.add((FileHeader) block);
            }
        }
        return list;
    }

    public FileHeader nextFileHeader() {
        int n = headers.size();
        while (currentHeaderIndex < n) {
            BaseBlock block = headers.get(currentHeaderIndex++);
            if (block.getHeaderType() == UnrarHeadertype.FileHeader) {
                return (FileHeader) block;
            }
        }
        return null;
    }

    public UnrarCallback getUnrarCallback() {
        return unrarCallback;
    }

    public boolean isEncrypted() {
        if (newMhd != null) {
            return newMhd.isEncrypted();
        } else {
            throw new NullPointerException("mainheader is null");
        }
    }

    private void readHeaders(long fileLength) throws IOException, RarException {
        markHead = null;
        newMhd = null;
        headers.clear();
        currentHeaderIndex = 0;
        int toRead = 0;

        while (true) {
            int size = 0;
            long newpos = 0;
            byte[] baseBlockBuffer = new byte[BaseBlock.BaseBlockSize];

            long position = rof.getPosition();

            // Weird, but is trying to read beyond the end of the file
            if (position >= fileLength) {
                break;
            }

            // logger.info("\n--------reading header--------");
            size = rof.readFully(baseBlockBuffer, BaseBlock.BaseBlockSize);
            if (size == 0) {
                break;
            }
            BaseBlock block = new BaseBlock(baseBlockBuffer);

            block.setPositionInFile(position);

            switch (block.getHeaderType()) {

                case MarkHeader:
                    markHead = new MarkHeader(block);
                    if (!markHead.isSignature()) {
                        throw new RarException(
                                RarExceptionType.badRarArchive);
                    }
                    headers.add(markHead);
                    // markHead.print();
                    break;

                case MainHeader:
                    toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc
                            : MainHeader.mainHeaderSize;
                    byte[] mainbuff = new byte[toRead];
                    rof.readFully(mainbuff, toRead);
                    MainHeader mainhead = new MainHeader(block, mainbuff);
                    headers.add(mainhead);
                    this.newMhd = mainhead;
                    if (newMhd.isEncrypted()) {
                        throw new RarException(
                                RarExceptionType.rarEncryptedException);
                    }
                    // mainhead.print();
                    break;

                case SignHeader:
                    toRead = SignHeader.signHeaderSize;
                    byte[] signBuff = new byte[toRead];
                    rof.readFully(signBuff, toRead);
                    SignHeader signHead = new SignHeader(block, signBuff);
                    headers.add(signHead);
                    // logger.info("HeaderType: SignHeader");

                    break;

                case AvHeader:
                    toRead = AVHeader.avHeaderSize;
                    byte[] avBuff = new byte[toRead];
                    rof.readFully(avBuff, toRead);
                    AVHeader avHead = new AVHeader(block, avBuff);
                    headers.add(avHead);
                    // logger.info("headertype: AVHeader");
                    break;

                case CommHeader:
                    toRead = CommentHeader.commentHeaderSize;
                    byte[] commBuff = new byte[toRead];
                    rof.readFully(commBuff, toRead);
                    CommentHeader commHead = new CommentHeader(block, commBuff);
                    headers.add(commHead);
                    // logger.info("method: "+commHead.getUnpMethod()+"; 0x"+
                    // Integer.toHexString(commHead.getUnpMethod()));
                    newpos = commHead.getPositionInFile()
                            + commHead.getHeaderSize();
                    rof.setPosition(newpos);

                    break;
                case EndArcHeader:

                    toRead = 0;
                    if (block.hasArchiveDataCRC()) {
                        toRead += EndArcHeader.endArcArchiveDataCrcSize;
                    }
                    if (block.hasVolumeNumber()) {
                        toRead += EndArcHeader.endArcVolumeNumberSize;
                    }
                    EndArcHeader endArcHead;
                    if (toRead > 0) {
                        byte[] endArchBuff = new byte[toRead];
                        rof.readFully(endArchBuff, toRead);
                        endArcHead = new EndArcHeader(block, endArchBuff);
                        // logger.info("HeaderType: endarch\ndatacrc:"+
                        // endArcHead.getArchiveDataCRC());
                    } else {
                        // logger.info("HeaderType: endarch - no Data");
                        endArcHead = new EndArcHeader(block, null);
                    }
                    headers.add(endArcHead);
                    // logger.info("\n--------end header--------");
                    return;

                default:
                    byte[] blockHeaderBuffer = new byte[BlockHeader.blockHeaderSize];
                    rof.readFully(blockHeaderBuffer, BlockHeader.blockHeaderSize);
                    BlockHeader blockHead = new BlockHeader(block,
                            blockHeaderBuffer);

                    switch (blockHead.getHeaderType()) {
                        case NewSubHeader:
                        case FileHeader:
                            toRead = blockHead.getHeaderSize()
                                    - BlockHeader.BaseBlockSize
                                    - BlockHeader.blockHeaderSize;
                            byte[] fileHeaderBuffer = new byte[toRead];
                            rof.readFully(fileHeaderBuffer, toRead);

                            FileHeader fh = new FileHeader(blockHead, fileHeaderBuffer);
                            headers.add(fh);
                            newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                    + fh.getFullPackSize();
                            rof.setPosition(newpos);
                            break;

                        case ProtectHeader:
                            toRead = blockHead.getHeaderSize()
                                    - BlockHeader.BaseBlockSize
                                    - BlockHeader.blockHeaderSize;
                            byte[] protectHeaderBuffer = new byte[toRead];
                            rof.readFully(protectHeaderBuffer, toRead);
                            ProtectHeader ph = new ProtectHeader(blockHead,
                                    protectHeaderBuffer);

                            newpos = ph.getPositionInFile() + ph.getHeaderSize()
                                    + ph.getDataSize();
                            rof.setPosition(newpos);
                            break;

                        case SubHeader: {
                            byte[] subHeadbuffer = new byte[SubBlockHeader.SubBlockHeaderSize];
                            rof.readFully(subHeadbuffer,
                                    SubBlockHeader.SubBlockHeaderSize);
                            SubBlockHeader subHead = new SubBlockHeader(blockHead,
                                    subHeadbuffer);
                            subHead.print();
                            switch (subHead.getSubType()) {
                                case MAC_HEAD: {
                                    byte[] macHeaderbuffer = new byte[MacInfoHeader.MacInfoHeaderSize];
                                    rof.readFully(macHeaderbuffer,
                                            MacInfoHeader.MacInfoHeaderSize);
                                    MacInfoHeader macHeader = new MacInfoHeader(subHead,
                                            macHeaderbuffer);
                                    macHeader.print();
                                    headers.add(macHeader);

                                    break;
                                }
                                // TODO implement other subheaders
                                case BEEA_HEAD:
                                    break;
                                case EA_HEAD: {
                                    byte[] eaHeaderBuffer = new byte[EAHeader.EAHeaderSize];
                                    rof.readFully(eaHeaderBuffer, EAHeader.EAHeaderSize);
                                    EAHeader eaHeader = new EAHeader(subHead,
                                            eaHeaderBuffer);
                                    eaHeader.print();
                                    headers.add(eaHeader);

                                    break;
                                }
                                case NTACL_HEAD:
                                    break;
                                case STREAM_HEAD:
                                    break;
                                case UO_HEAD:
                                    toRead = subHead.getHeaderSize();
                                    toRead -= BaseBlock.BaseBlockSize;
                                    toRead -= BlockHeader.blockHeaderSize;
                                    toRead -= SubBlockHeader.SubBlockHeaderSize;
                                    byte[] uoHeaderBuffer = new byte[toRead];
                                    rof.readFully(uoHeaderBuffer, toRead);
                                    UnixOwnersHeader uoHeader = new UnixOwnersHeader(
                                            subHead, uoHeaderBuffer);
                                    uoHeader.print();
                                    headers.add(uoHeader);
                                    break;
                                default:
                                    break;
                            }

                            break;
                        }
                        default:
                            logger.warning("Unknown Header");
                            throw new RarException(RarExceptionType.notRarArchive);

                    }
            }
            // logger.info("\n--------end header--------");
        }
    }

    public void extractFile(FileHeader hd, OutputStream os) throws RarException {
        if (!headers.contains(hd)) {
            throw new RarException(RarExceptionType.headerNotInArchive);
        }
        try {
            doExtractFile(hd, os);
        } catch (Exception e) {
            if (e instanceof RarException) {
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    public InputStream getInputStream(final FileHeader hd) throws
            IOException {
        final PipedInputStream in = new PipedInputStream(32 * 1024);
        final PipedOutputStream out = new PipedOutputStream(in);

        // creates a new thread that will write data to the pipe. Data will be
        // available in another InputStream, connected to the OutputStream.
        new Thread(new Runnable() {
            public void run() {
                try {
                    extractFile(hd, out);
                } catch (RarException e) {
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
        }).start();

        return in;
    }

    private void doExtractFile(FileHeader hd, OutputStream os)
            throws RarException, IOException {
        dataIO.init(os);
        dataIO.init(hd);
        dataIO.setUnpFileCRC(this.isOldFormat() ? 0 : 0xffFFffFF);
        if (unpack == null) {
            unpack = new Unpack(dataIO);
        }
        if (!hd.isSolid()) {
            unpack.init(null);
        }
        unpack.setDestSize(hd.getFullUnpackSize());
        try {
            unpack.doUnpack(hd.getUnpVersion(), hd.isSolid());
            // Verify file CRC
            hd = dataIO.getSubHeader();
            long actualCRC = hd.isSplitAfter() ? ~dataIO.getPackedCRC()
                    : ~dataIO.getUnpFileCRC();
            int expectedCRC = hd.getFileCRC();
            if (actualCRC != expectedCRC) {
                throw new RarException(RarExceptionType.crcError);
            }
            // if (!hd.isSplitAfter()) {
            // // Verify file CRC
            // if(~dataIO.getUnpFileCRC() != hd.getFileCRC()){
            // throw new RarException(RarExceptionType.crcError);
            // }
            // }
        } catch (Exception e) {
            unpack.cleanUp();
            if (e instanceof RarException) {
                // throw new RarException((RarException)e);
                throw (RarException) e;
            } else {
                throw new RarException(e);
            }
        }
    }

    public MainHeader getMainHeader() {
        return newMhd;
    }

    public boolean isOldFormat() {
        return markHead.isOldFormat();
    }

    public void close() throws IOException {
        if (rof != null) {
            rof.close();
            rof = null;
        }
        if (unpack != null) {
            unpack.cleanUp();
        }
    }

    public VolumeManager getVolumeManager() {
        return volumeManager;
    }

    public void setVolumeManager(VolumeManager volumeManager) {
        this.volumeManager = volumeManager;
    }

    public Volume getVolume() {
        return volume;
    }

    public void setVolume(Volume volume) throws IOException {
        this.volume = volume;
        setFile(volume.getReadOnlyAccess(), volume.getLength());
    }
}

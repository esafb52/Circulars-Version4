package ir.mahdi.circulars.archive.Rar;


public interface UnrarCallback {

    boolean isNextVolumeReady(Volume nextVolume);

    void volumeProgressChanged(long current, long total);
}

package com.indix.gocd.utils.zip;

import java.io.IOException;

public interface IZipArchiveManager {
    void compressDirectory(String dir, String zipFile) throws IOException;
    void extractArchive(String zipFile, String dir) throws IOException;
}

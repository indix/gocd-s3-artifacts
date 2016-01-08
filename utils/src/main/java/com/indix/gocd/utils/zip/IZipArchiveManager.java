package com.indix.gocd.utils.zip;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface IZipArchiveManager {
    void compressDirectory(String dir, String zipFile) throws IOException;

}

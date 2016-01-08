package com.indix.gocd.utils.zip;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipArchiveManager implements IZipArchiveManager {

    public void compressDirectory(String dir, String zipFile) throws IOException {
        ZipOutputStream zos  = null;

        try {
            zos  = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(zipFile))));
            new DirectoryStructureWalker(dir, zos).walk();
        }
        finally {
            if (zos != null) {
                zos.close();
            }
        }
    }

    class DirectoryStructureWalker extends DirectoryWalker {
        private final String configDirectory;
        private final ZipOutputStream zipStream;
        private final ArrayList<String> excludeFiles;

        public DirectoryStructureWalker(String configDirectory, ZipOutputStream zipStream, File ...excludeFiles) {
            this.excludeFiles = new ArrayList<String>();
            for (File excludeFile : excludeFiles) {
                this.excludeFiles.add(excludeFile.getAbsolutePath());
            }

            this.configDirectory = new File(configDirectory).getAbsolutePath();
            this.zipStream = zipStream;
        }

        @Override
        protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
            if (! directory.getAbsolutePath().equals(configDirectory)) {
                ZipEntry e = new ZipEntry(fromRoot(directory) + "/");
                zipStream.putNextEntry(e);
            }
            return true;
        }

        @Override
        protected void handleFile(File file, int depth, Collection results) throws IOException {
            if (excludeFiles.contains(file.getAbsolutePath())) {
                return;
            }
            zipStream.putNextEntry(new ZipEntry(fromRoot(file)));
            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                IOUtils.copy(in, zipStream);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }

        private String fromRoot(File directory) {
            return directory.getAbsolutePath().substring(configDirectory.length() + 1);
        }

        public void walk() throws IOException {
            walk(new File(this.configDirectory), null);
        }
    }
}

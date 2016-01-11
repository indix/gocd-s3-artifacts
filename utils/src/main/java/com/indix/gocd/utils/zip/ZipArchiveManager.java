package com.indix.gocd.utils.zip;

import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipArchiveManager implements IZipArchiveManager {
    private Logger log = Logger.getLoggerFor(ZipArchiveManager.class);

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

    public void extractArchive(String zipFile, String dir) throws IOException {
        unzip(zipFile, dir);
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


    public void unzip(ZipInputStream zipInputStream, File destDir) throws IOException {
        destDir.mkdirs();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            extractTo(zipEntry, zipInputStream, destDir);
            zipEntry = zipInputStream.getNextEntry();
        }
        IOUtils.closeQuietly(zipInputStream);
    }

    protected void unzip(String zip, String destDir) throws IOException {
        unzip(new File(zip), new File(destDir));
    }

    protected void unzip(File zip, File destDir) throws IOException {
        unzip(new ZipInputStream(new BufferedInputStream(new FileInputStream(zip))), destDir);
    }

    private void extractTo(ZipEntry entry, InputStream entryInputStream, File toDir) throws IOException {
        bombIfZipEntryPathContainsDirectoryTraversalCharacters(entry.getName());
        String entryName = nonRootedEntryName(entry);

        File outputFile = new File(toDir, entryName);
        if (isDirectory(entryName)) {
            outputFile.mkdirs();
            return;
        }
        FileOutputStream os = null;
        try {
            outputFile.getParentFile().mkdirs();
            os = new FileOutputStream(outputFile);
            IOUtils.copyLarge(entryInputStream, os);
        } catch (IOException e) {
            log.error(String.format("Failed to unzip file [%s] to directory [%s]", entryName, toDir.getAbsolutePath()), e);
            throw e;
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private void bombIfZipEntryPathContainsDirectoryTraversalCharacters(String filepath) {
        if (filepath.contains("..")) {
            throw new IllegalStateException(String.format("File %s is outside extraction target directory", filepath));
        }
    }

    private String nonRootedEntryName(ZipEntry entry) {
        String entryName = entry.getName();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
        return entryName;
    }

    private boolean isDirectory(String zipName) {
        return zipName.endsWith("/");
    }
}

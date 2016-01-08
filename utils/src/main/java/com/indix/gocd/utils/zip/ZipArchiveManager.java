package com.indix.gocd.utils.zip;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipArchiveManager implements IZipArchiveManager {

    public void compressDirectory(String dir, String zipFile) throws IOException {
        File directory = new File(dir);
        List<String> fileList = getFileList(directory);

        FileOutputStream fos  = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (String filePath : fileList) {
            //System.out.println("Compressing: " + filePath);

            CreateZipEntry(directory, zos, filePath);

            //
            // Read file content and write to zip output stream.
            //
            FileInputStream fis = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            fis.close();
        }

        //
        // Close zip output stream and file output stream. This will
        // complete the compression process.
        //
        zos.close();
        fos.close();
    }

    private void CreateZipEntry(File directory, ZipOutputStream zos, String filePath) throws IOException {
        String name = filePath.substring(directory.getAbsolutePath().length() + 1,
                filePath.length());
        ZipEntry zipEntry = new ZipEntry(name);
        zos.putNextEntry(zipEntry);
    }

    private List<String>  getFileList(File directory) {
        List<String> fileList = new ArrayList<String>();
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                } else {
                    getFileList(file);
                }
            }
        }
        return fileList;
    }
}

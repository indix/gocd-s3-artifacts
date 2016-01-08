package com.indix.gocd.utils.utils.zip;

import com.indix.gocd.utils.zip.IZipArchiveManager;
import com.indix.gocd.utils.zip.ZipArchiveManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZipArchiveManagerTest {

    private IZipArchiveManager sut;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder folderToCompress = new TemporaryFolder();

    @Before
    public void TestSetup()
    {
        this.sut = new ZipArchiveManager();
    }

    @Test
    public void shouldCreateZipFile() {

        try {
            File createdFile= folderToCompress.newFile("myfile.txt");
            String archivePath = tempFolder.getRoot().toString().concat("/compressed.zip");
            sut.compressDirectory(folderToCompress.getRoot().toString(),archivePath);

            File file = new File(archivePath);
            assertTrue(file.exists());
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }

    }
}

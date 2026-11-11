package com.aero.control.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.junit.Test;

public class ProfileBackupRestoreOperationsTest {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void atomicallyReplacesDestinationWithValidProfile() throws Exception {
        File directory = Files.createTempDirectory("aero-profile-test").toFile();
        try {
            File source = new File(directory, "source.xml");
            File destination = new File(directory, "profile.xml");
            write(source, "<map><string name=\"mode\">balanced</string></map>");
            write(destination, "<map><string name=\"mode\">old</string></map>");

            ProfileBackupRestoreOperations.copyFile(source, destination);

            assertEquals(read(source), read(destination));
            assertNoTemporaryFiles(directory);
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    public void preservesExistingProfileWhenStagedXmlIsInvalid() throws Exception {
        File directory = Files.createTempDirectory("aero-profile-test").toFile();
        try {
            File source = new File(directory, "source.xml");
            File destination = new File(directory, "profile.xml");
            String original = "<map><string name=\"mode\">old</string></map>";
            write(source, "<map><boolean name=\"enabled\" value=\"maybe\"/></map>");
            write(destination, original);

            try {
                ProfileBackupRestoreOperations.copyFile(source, destination);
                fail("Expected invalid profile XML to be rejected");
            } catch (IOException expected) {
                assertEquals(original, read(destination));
            }
            assertNoTemporaryFiles(directory);
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    public void rejectsXmlWithExternalEntity() throws Exception {
        File directory = Files.createTempDirectory("aero-profile-test").toFile();
        try {
            File source = new File(directory, "source.xml");
            File destination = new File(directory, "profile.xml");
            String original = "<map><string name=\"mode\">old</string></map>";
            write(source, "<!DOCTYPE map [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                    + "<map><string name=\"mode\">&x;</string></map>");
            write(destination, original);

            try {
                ProfileBackupRestoreOperations.copyFile(source, destination);
                fail("Expected external entity to be rejected");
            } catch (IOException expected) {
                assertEquals(original, read(destination));
            }
            assertNoTemporaryFiles(directory);
        } finally {
            deleteDirectory(directory);
        }
    }

    private static void write(File file, String value) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(UTF_8));
        }
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), UTF_8);
    }

    private static void assertNoTemporaryFiles(File directory) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            fail("Unable to list test directory");
        }
        for (File entry : entries) {
            assertFalse("Unexpected temporary profile file: " + entry.getName(),
                    entry.getName().endsWith(".tmp"));
        }
    }

    private static void deleteDirectory(File directory) {
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                entry.delete();
            }
        }
        directory.delete();
    }
}

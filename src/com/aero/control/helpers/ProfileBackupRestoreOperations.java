package com.aero.control.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/** Handles import and export of profile backups on shared storage. */
public final class ProfileBackupRestoreOperations {
    private final File backupDirectory = new File(
            FilePath.EXTERNAL_PATH + "/com.aero.control/profiles");

    /** Lists valid profile backup filenames available for restore. */
    public String[] listAvailableBackups() throws IOException {
        if (!this.backupDirectory.exists()) {
            return new String[0];
        }
        String[] entries = this.backupDirectory.list();
        if (entries == null) {
            throw new IOException("Unable to list profile backups");
        }
        List<String> backups = new ArrayList<>();
        for (String entry : entries) {
            if (ProfileFileOperations.isValidProfileFilename(entry)
                    && new File(this.backupDirectory, entry).isFile()) {
                backups.add(entry);
            }
        }
        return backups.toArray(new String[backups.size()]);
    }

    public void restoreProfile(String filename) throws IOException {
        File source = backupFile(filename);
        File destination = new File(FilePath.sharedPrefsPath, filename);
        if (!source.isFile()) {
            throw new IOException("Profile backup does not exist");
        }
        copyFile(source, destination);
    }

    public void backupProfile(String profileName) throws IOException {
        if (!ProfileFileOperations.isValidProfileName(profileName)
                || !ProfileFileOperations.isValidProfileFilename(profileName + ".xml")) {
            throw new IOException("Invalid profile name");
        }
        String filename = profileName + ".xml";
        File source = new File(FilePath.sharedPrefsPath, filename);
        if (!source.isFile()) {
            throw new IOException("Saved profile does not exist");
        }
        if (!this.backupDirectory.exists() && !this.backupDirectory.mkdirs()) {
            throw new IOException("Unable to create the profile backup directory");
        }
        if (!this.backupDirectory.isDirectory()) {
            throw new IOException("Profile backup path is not a directory");
        }
        copyFile(source, new File(this.backupDirectory, filename));
    }

    private File backupFile(String filename) throws IOException {
        if (!ProfileFileOperations.isValidProfileFilename(filename)) {
            throw new IOException("Invalid profile backup filename");
        }
        return new File(this.backupDirectory, filename);
    }

    static void copyFile(File source, File destination) throws IOException {
        File absoluteDestination = destination.getAbsoluteFile();
        File parent = absoluteDestination.getParentFile();
        if (parent == null || !parent.isDirectory()) {
            throw new IOException("Profile destination directory does not exist");
        }

        File temporary = File.createTempFile(
                absoluteDestination.getName() + ".", ".tmp", parent);
        boolean replaced = false;
        IOException failure = null;
        try {
            copyContents(source, temporary);
            validateProfileXml(temporary);
            if (absoluteDestination.exists() && !absoluteDestination.isFile()) {
                throw new IOException("Profile destination is not a regular file");
            }
            if (!temporary.renameTo(absoluteDestination)) {
                throw new IOException("Unable to replace profile destination");
            }
            replaced = true;
        } catch (IOException e) {
            failure = e;
        } finally {
            if (!replaced && temporary.exists() && !temporary.delete()) {
                if (failure == null) {
                    failure = new IOException("Unable to remove temporary profile file");
                } else {
                    failure = new IOException(failure.getMessage()
                            + "; unable to remove temporary profile file", failure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void copyContents(File source, File destination) throws IOException {
        try (InputStream input = new BufferedInputStream(new FileInputStream(source));
                FileOutputStream fileOutput = new FileOutputStream(destination);
                OutputStream output = new BufferedOutputStream(fileOutput)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            output.flush();
            fileOutput.getFD().sync();
        }
    }

    private static void validateProfileXml(File profile) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities", false);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            SharedPreferencesXmlValidator validator = new SharedPreferencesXmlValidator();
            reader.setContentHandler(validator);
            reader.setProperty(
                    "http://xml.org/sax/properties/lexical-handler", validator);
            reader.setEntityResolver(new org.xml.sax.EntityResolver() {
                @Override
                public org.xml.sax.InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException {
                    throw new SAXException("External XML entities are not permitted");
                }
            });
            try (InputStream input = new BufferedInputStream(new FileInputStream(profile))) {
                reader.parse(new org.xml.sax.InputSource(input));
            }
        } catch (ParserConfigurationException e) {
            throw new IOException("Unable to configure the profile XML parser", e);
        } catch (SAXException e) {
            throw new IOException("Profile backup is not valid SharedPreferences XML", e);
        }
    }

    private static final class SharedPreferencesXmlValidator extends DefaultHandler
            implements LexicalHandler {
        private final List<String> elements = new ArrayList<>();
        private boolean rootSeen;
        private boolean rootClosed;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            int depth = this.elements.size();
            if (depth == 0) {
                if (this.rootSeen || !"map".equals(qName) || attributes.getLength() != 0) {
                    throw new SAXException("Profile XML must have a single map root");
                }
                this.rootSeen = true;
            } else if (depth == 1) {
                validatePreferenceElement(qName, attributes);
            } else if (depth == 2 && "set".equals(this.elements.get(1))) {
                if (!"string".equals(qName) || attributes.getLength() != 0) {
                    throw new SAXException("Invalid value in profile string set");
                }
            } else {
                throw new SAXException("Unexpected nested element in profile XML");
            }
            this.elements.add(qName);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (this.elements.isEmpty()
                    || !this.elements.get(this.elements.size() - 1).equals(qName)) {
                throw new SAXException("Mismatched element in profile XML");
            }
            this.elements.remove(this.elements.size() - 1);
            if (this.elements.isEmpty()) {
                this.rootClosed = true;
            }
        }

        @Override
        public void endDocument() throws SAXException {
            if (!this.rootSeen || !this.rootClosed || !this.elements.isEmpty()) {
                throw new SAXException("Incomplete profile XML");
            }
        }

        private void validatePreferenceElement(String name, Attributes attributes)
                throws SAXException {
            boolean hasValue = !"string".equals(name) && !"set".equals(name);
            if (!("string".equals(name) || "int".equals(name) || "long".equals(name)
                    || "float".equals(name) || "boolean".equals(name) || "set".equals(name))) {
                throw new SAXException("Unsupported SharedPreferences value type");
            }
            String preferenceName = attributes.getValue("name");
            String value = attributes.getValue("value");
            if (preferenceName == null
                    || (hasValue && value == null)
                    || attributes.getLength() != (hasValue ? 2 : 1)) {
                throw new SAXException("Invalid SharedPreferences value attributes");
            }
            try {
                if ("int".equals(name)) {
                    Integer.parseInt(value);
                } else if ("long".equals(name)) {
                    Long.parseLong(value);
                } else if ("float".equals(name)) {
                    Float.parseFloat(value);
                } else if ("boolean".equals(name)
                        && !"true".equals(value) && !"false".equals(value)) {
                    throw new SAXException("Invalid SharedPreferences boolean value");
                }
            } catch (NumberFormatException e) {
                throw new SAXException("Invalid SharedPreferences numeric value", e);
            }
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            throw new SAXException("Document type declarations are not permitted");
        }

        @Override
        public void endDTD() {
        }

        @Override
        public void startEntity(String name) {
        }

        @Override
        public void endEntity(String name) {
        }

        @Override
        public void startCDATA() {
        }

        @Override
        public void endCDATA() {
        }

        @Override
        public void comment(char[] characters, int start, int length) {
        }
    }
}

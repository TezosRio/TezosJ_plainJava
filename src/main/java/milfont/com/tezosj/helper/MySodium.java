package milfont.com.tezosj.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.ProviderNotFoundException;

import com.goterl.lazycode.lazysodium.Sodium;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.utils.NativeUtils;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class MySodium extends Sodium
{

	private static String fileId = "";
	String path = getLibSodiumFromResources();
	private static File temporaryDir;
    private static final int MIN_PREFIX_LENGTH = 3;
    public static final String NATIVE_FOLDER_PATH_PREFIX = "nativeutils";
	    
    
    public MySodium(String fileId)
	{
		this.fileId = fileId;
        myregisterFromResources();
        onRegistered();
    }
	
    private String getLibSodiumFromResources()
    {
        String path = getPath("windows", "libsodium.dll");
        if (Platform.isLinux() || Platform.isAndroid())
        {
            path = getPath("linux", "libsodium.so");
        }
        else if (Platform.isMac())
        {
            path = getPath("mac", "libsodium.dylib");
        }
        return path;
    }
    
    private String getPath(String folder, String name)
    {
        String separator = "/";
        String resourcePath = folder + separator + name;
        if (!resourcePath.startsWith(separator))
        {
            resourcePath = separator + resourcePath;
        }
        return resourcePath;
    }

    private void myregisterFromResources()
    {
        String path = getLibSodiumFromResources();
        try
        {
            loadLibraryFromJar(path);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static void loadLibraryFromJar(String path) throws IOException {

        if (path == null) {
            throw new IOException("Path cannot be null.");
        }

        String fileName = new File(path).getName();
        if (fileName.length() <= MIN_PREFIX_LENGTH) {
            throw new IOException(
                    "The filename of your native library (" + fileName +
                    ") should be of length longer than " + MIN_PREFIX_LENGTH +
                    " characters."
            );
        }

        // Prepare temporary file
        if (temporaryDir == null) {
            temporaryDir = createTempDirectory(NATIVE_FOLDER_PATH_PREFIX);
            temporaryDir.deleteOnExit();
        }

        // Adds an identifier to the name of the copied libsodium library file to allow multiple instances simultaneously.
        String myFilename = fileName.replace(".dll", "" ) + fileId + ".dll";
        
        if (Platform.isLinux() || Platform.isAndroid())
        {
        	myFilename = fileName.replace(".so", "" ) + fileId + ".so";
        }
        else if (Platform.isMac())
        {
        	myFilename = fileName.replace(".dylib", "" ) + fileId + ".dylib";
        }        
        
        File temp = new File(temporaryDir, myFilename);
        InputStream is = NativeUtils.class.getResourceAsStream(path);
        FileOutputStream out = new FileOutputStream(temp, false);
        try {
            byte [] dest = new byte[4096];
            int amt = is.read(dest);
            while(amt != -1) {
                out.write(dest, 0, amt);
                amt = is.read(dest);
            }
        } catch (IOException e) {
            temp.delete();
            throw e;
        } catch (NullPointerException e) {
            temp.delete();
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }
        finally {
            is.close();
            out.close();
        }

        // Modified to work with JNA
        try {
            Native.register(Sodium.class, temp.getAbsolutePath());
            Native.register(SodiumJava.class, temp.getAbsolutePath());
        } finally {
            if (isPosixCompliant()) {
                // Assume POSIX compliant file system, can be deleted after loading
                temp.delete();
            } else {
                // Assume non-POSIX, and don't delete until last file descriptor closed
                temp.deleteOnExit();
            }
        }
    }

    private static boolean isPosixCompliant() {
        try {
            return FileSystems.getDefault()
                    .supportedFileAttributeViews()
                    .contains("posix");
        } catch (FileSystemNotFoundException
                | ProviderNotFoundException
                | SecurityException e) {
            return false;
        }
    }

    private static File createTempDirectory(String prefix) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());

        if (!generatedDir.mkdir())
            throw new IOException("Failed to create temp directory " + generatedDir.getName());

        return generatedDir;
    }
    
    
    
}

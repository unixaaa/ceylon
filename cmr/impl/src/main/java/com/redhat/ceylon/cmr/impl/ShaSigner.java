package com.redhat.ceylon.cmr.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.common.log.Logger;

/** A container for methods to sign a File using SHA-1.
 *  Moved here from ceylon-compiler. */
public class ShaSigner {
    
    private static class NullLogger implements Logger {
        @Override
        public void error(String str) {}
        @Override
        public void warning(String str) {}
        @Override
        public void info(String str) {}
        @Override
        public void debug(String str) {}
    }
    private static Logger NULL_LOGGER = new NullLogger();

    public static File sign(File file, Logger log, boolean verbose){
        String sha1 = hash(file, "SHA-1", log);
        if(sha1 == null)
            return null;
        
        File sha1File = new File(file.getPath()+".sha1");
        if(verbose){
            log.info("[signing jar "+file.getPath()+" into: "+sha1File.getPath()+"]");
            //Log.printLines(log.noticeWriter, "[signing jar "+file.getPath()+" into: "+sha1File.getPath()+"]");
        }
        writeSha1(sha1File, sha1, log);
        return sha1File;
    }
    
    private static void writeSha1(File sha1Path, String sha1, Logger log) {
        OutputStream os;
        try {
            os = new FileOutputStream(sha1Path);
        } catch (FileNotFoundException e) {
            log.warning("Failed to open archive file "+sha1Path
                    +" for writing, no SHA-1 signature will be made");
            return;
        }
        try {
            os.write(sha1.getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            log.warning("Failed to get an ASCII charset, your JRE does not follow the specs. "
                    +"No SHA-1 signature will be made");
            return;
        } catch (IOException e) {
            log.warning("Failed to write to "+sha1Path
                    +", no SHA-1 signature will be made");
            return;
        }finally{
            try {
                os.flush();
                os.close();
            } catch (IOException e) {
                // don't care
            }
        }
    }

    public static String sha1(File file) {
        return hash(file, "SHA-1", NULL_LOGGER);
    }
    public static String md5(File file) {
        return hash(file, "MD5", NULL_LOGGER);
    }
    
    private static String hash(File file, String hashAlgo, Logger log) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(hashAlgo);
        } catch (NoSuchAlgorithmException e) {
            // can't happen, specs say SHA-1 must be implemented
            log.warning("Failed to get a "+hashAlgo+" message digest, your JRE does not follow the specs. "
                    +"No "+hashAlgo+" signature will be made");
            return null;
        }
        FileInputStream is;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            // can't happen since we just created the file
            log.warning("Failed to open archive file "+file.getPath()
                    +", no "+hashAlgo+" signature will be made");
            return null;
        }
        byte[] buffer = new byte[1024];
     
        int read = 0; 
        try {
            while ((read = is.read(buffer)) != -1) {
              digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            log.warning("Failed to read archive file "+file.getPath()
                    +", no "+hashAlgo+" signature will be made");
            return null;
        }finally{
            try {
                is.close();
            } catch (IOException e) {
                // don't care
            }
        }
        return toHexString(digest.digest());
    }

    final static char[] Hexadecimal = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    private static String toHexString(byte[] bytes){
        char[] chars = new char[bytes.length*2];
        for(int b=0,c=0;b<bytes.length;b++){
            int v = (int)bytes[b] & 0xFF;
            chars[c++] = Hexadecimal[v/16];
            chars[c++] = Hexadecimal[v%16];
        }
        return new String(chars);
    }

    public static void signArtifact(RepositoryManager repoman, ArtifactContext context, File jarFile, Logger log){
        ArtifactContext sha1Context = context.getSha1Context();
        if (sha1Context != null) {
            sha1Context.setForceOperation(true);
            File shaFile = sign(jarFile, log, false);
            if(shaFile != null){
                try{
                    repoman.putArtifact(sha1Context, shaFile);
                }finally{
                    shaFile.delete();
                }
            }
        }
    }

}

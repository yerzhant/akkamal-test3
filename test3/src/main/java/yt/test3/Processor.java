/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yt.test3;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author yerzhan
 */
public class Processor extends DefaultFtplet {

    private static final String HASH_ALG = "MD5";
    private static final String ON_UPLOADED_MESSAGE = "Upload file {}:{}";

    private final Logger log = LoggerFactory.getLogger(Processor.class);

    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return process(request);
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return process(request);
    }

    private FtpletResult process(FtpRequest req) {
        try (FileInputStream is = new FileInputStream(req.getArgument())) {
            log(is, req.getArgument());
        } catch (NoSuchAlgorithmException | IOException ex) {
            log.error("Error", ex);
            return FtpletResult.SKIP;
        }

        return FtpletResult.DEFAULT;
    }
    
    private boolean checkFileName(String name) {
        
    } 

    private void log(FileInputStream is, String fileName) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance(HASH_ALG);

        byte[] buf = new byte[65536];
        for (int len = is.read(buf); len > 0; len = is.read(buf)) {
            md.update(buf, 0, len);
        }
        
        byte[] hash = md.digest();
        
        log.info(ON_UPLOADED_MESSAGE, fileName, Hex.encodeHexString(hash));
    }
}

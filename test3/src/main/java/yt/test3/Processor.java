/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yt.test3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author yerzhan
 */
public class Processor extends DefaultFtplet {

    private static final String HASH_ALG = "MD5";
    private static final String ON_UPLOADED_MESSAGE = "Upload file {}:{}";
    private static final String XPATH_ATTR_TO_SELECT = "name";
    private static final String ENT_L3_WITH_NAME_ATTR_XPATH = "/*/*/ent[@" + XPATH_ATTR_TO_SELECT + "]";

    private final Pattern fileNamePattern = Pattern.compile("^(\\p{Alnum}+)\\p{Alnum}{3,}\\1\\.\\p{Alpha}{3}$");

    private final int PREHASH_BUF_SIZE = 16384;

    private final Map<String, Integer> quantities = new HashMap<>();

    private final Logger log = LoggerFactory.getLogger(Processor.class);

    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return process(session, request);
    }

    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException, IOException {
        return process(session, request);
    }

    private FtpletResult process(FtpSession session, FtpRequest request) throws FtpException, IOException {
        String fileName = session.getFileSystemView().getWorkingDirectory().getAbsolutePath();
        fileName = Server.HOME_DIR + (fileName.equals(File.separator) ? "" : fileName) + File.separator + request.getArgument();

        try {
            log(fileName);

            if (fileNamePattern.matcher(request.getArgument()).matches()) {
                System.out.println("1");
                parse(fileName);
            }
        } catch (NoSuchAlgorithmException | XPathExpressionException ex) {
//            log.error("Error", ex);
//            return FtpletResult.SKIP;
            throw new FtpException(ex);
        }

        return FtpletResult.DEFAULT;
    }

    private void parse(String fileName) throws XPathExpressionException, IOException {
        try (FileInputStream is = new FileInputStream(fileName)) {
            XPathFactory f = XPathFactory.newInstance();
            XPath p = f.newXPath();
            NodeList l = (NodeList) p.evaluate(ENT_L3_WITH_NAME_ATTR_XPATH, new InputSource(is), XPathConstants.NODESET);

            Map<String, Integer> m = new HashMap<>();

            for (int i = 0; i < l.getLength(); i++) {
                System.out.println("2");
                String name = l.item(i).getAttributes().getNamedItem(XPATH_ATTR_TO_SELECT).getNodeValue();
                Integer q = m.get(name);
                m.put(name, q == null ? 1 : q + 1);
            }

            synchronized (quantities) {
                updateQuantities(m);
                logQuantities();
            }
        }
    }

    private void updateQuantities(Map<String, Integer> m) {
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            String name = e.getKey();
            Integer toBeAddedQ = e.getValue();
            Integer currentQ = quantities.get(name);
            quantities.put(name, currentQ == null ? toBeAddedQ : currentQ + toBeAddedQ);
        }
    }

    private void logQuantities() {
        List<Map.Entry<String, Integer>> l = new ArrayList<>(quantities.entrySet());

        Collections.sort(l, new Comparator<Map.Entry<String, Integer>>() {

            @Override
            public int compare(Map.Entry<String, Integer> i1, Map.Entry<String, Integer> i2) {
                return i2.getValue().compareTo(i1.getValue());
            }
        });
        for (Map.Entry<String, Integer> e : l) {
            log.info(e.getKey() + ":" + e.getValue());
        }
    }

    private void log(String fileName) throws NoSuchAlgorithmException, IOException {
        try (FileInputStream is = new FileInputStream(fileName)) {
            MessageDigest md = MessageDigest.getInstance(HASH_ALG);

            byte[] buf = new byte[PREHASH_BUF_SIZE];
            for (int len = is.read(buf); len > 0; len = is.read(buf)) {
                md.update(buf, 0, len);
            }

            byte[] hash = md.digest();

            log.info(ON_UPLOADED_MESSAGE, Paths.get(fileName, "").getFileName(), Hex.encodeHexString(hash));
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yt.test3;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.UserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author yerzhan
 */
public class Server {

    private static final String CONFIG_FILE_NAME = "app.conf";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String STARTUP_MESSAGE = "Starting server on port {}";
    private static final String SHUTDOWN_MESSAGE = "Shutting down...";

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final Properties config = new Properties();
    
    FtpServer server;

    public Server() throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(CONFIG_FILE_NAME);
        config.load(in);
    }

    public void start() throws FtpException {
        FtpServerFactory sf = new FtpServerFactory();
        ListenerFactory lf = new ListenerFactory();

        final int port = Integer.parseInt(config.getProperty(PORT));
        lf.setPort(port);
        sf.addListener("default", lf.createListener());

        server = sf.createServer();
        logger.info(STARTUP_MESSAGE, port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info(SHUTDOWN_MESSAGE);
                server.stop();
            }
        });
        
        setUpAuth(sf);

        server.start();
    }
    
    private void setUpAuth(FtpServerFactory sf) throws FtpException {
        BaseUser user = new BaseUser();
        user.setName(config.getProperty(USERNAME));
        user.setPassword(config.getProperty(PASSWORD));
        
        user.setHomeDirectory(".");
        List<Authority> auths = new ArrayList<>();
        auths.add(new WritePermission());
        user.setAuthorities(auths);
        
        UserManager um = sf.getUserManager();
        um.save(user);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (IOException | FtpException ex) {
            logger.error("Exception: {}", ex.getLocalizedMessage());
        }
    }
}

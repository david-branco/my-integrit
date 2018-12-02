import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Signal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by db on 31-05-2015.
 */
public class Daemon {

    private static final String CLEAN_DB = "/home/db/Programs/myIntegrit/cleandb.txt";
    private static final String RELOAD_DB = "/home/db/Programs/myIntegrit/reloaddb.txt";
    private static final String CONF_PATH = "/home/db/Programs/myIntegrit/myIntegrit.conf";
    private static String TARGET_PATH;
    private static final Logger logger = LogManager.getLogger(Daemon.class.getName());
    private static int parent_pid;

    private static byte[] digestDatabase (String filename) throws NoSuchAlgorithmException, IOException {
        FileInputStream fis = new FileInputStream((new File(filename)));
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        DigestInputStream dis = new DigestInputStream(fis, md);
        while(dis.read() != -1) { }
        dis.close();

        return md.digest();
    }

    private static void digestDatabases() throws IOException, NoSuchAlgorithmException {
        byte[] digest_clean_db = digestDatabase(CLEAN_DB);
        byte[] digest_reloaded_database = digestDatabase(RELOAD_DB);

        if(!Arrays.equals(digest_clean_db, digest_reloaded_database)) {
            logger.warn("Databases difers !!");
        }
        else { logger.info("Databases are equals."); }
    }

    private static void createDatabase(String filename) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(filename));
        System.setOut(out);

        FilesTree ft = new FilesTree();
        Files.walkFileTree(Paths.get(TARGET_PATH), ft);
    }

    private static void initializeDatabase() throws IOException, NoSuchAlgorithmException {
        createDatabase(CLEAN_DB);
        logger.info("Database Initialized.");
    }

    private static void reloadDatabase() throws IOException, NoSuchAlgorithmException {
        logger.info("Database Reloaded.");
        createDatabase(RELOAD_DB);
    }

    private static void readConfigs() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(CONF_PATH))));
        TARGET_PATH = br.readLine();
        br.close();
    }

    private static void addSignalHandlers() {
        Signal.handle(new Signal("HUP"), sig -> {
            try {
                initializeDatabase();
                Runtime.getRuntime().exec("kill -SIGTERM " + parent_pid);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });

        Signal.handle(new Signal("ALRM"), sig -> {
            try {
                reloadDatabase();
                digestDatabases();
                Runtime.getRuntime().exec("kill -SIGALRM " + parent_pid);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });

        Signal.handle(new Signal("TERM"), sig -> {
            logger.info("Terminated.");
            System.exit(1);
        });
    }

    public static void main(String args[]) throws IOException, NoSuchAlgorithmException, InterruptedException {
        parent_pid = Integer.parseInt(args[0]);
        readConfigs();
        addSignalHandlers();

        while(true) {
           Thread.sleep(Long.MAX_VALUE);
        }
    }
}

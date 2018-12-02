import sun.management.VMManagement;
import sun.misc.Signal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;

/**
 * Created by db on 27-05-2015.
 */
public class MyIntegrit {

    private static int daemon_pid;
    private static String task;
    private static final String CONF_PATH = "/home/db/Programs/myIntegrit/myIntegrit.conf";
    private static final String CLEAN_DB = "/home/db/Programs/myIntegrit/cleandb.txt";

    private static void addSignalHandlers() {
        Signal.handle(new Signal("TERM"), sig -> {
            try {
                Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Signal.handle(new Signal("ALRM"), sig -> {
            try {
                if(task.equals("reloaddb")) {
                    Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static int getPid() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Field jvm = runtime.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);
        VMManagement mgmt = (VMManagement) jvm.get(runtime);
        Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
        pid_method.setAccessible(true);

        return (Integer) pid_method.invoke(mgmt);
    }

    private static void changeTargetPath(String path) throws IOException {
        File file = new File(path);
        if(!(file.exists() && file.isDirectory())) {
            System.out.println("Invalid Target Path");
            Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
            System.exit(1);
        }

        PrintWriter pw = new PrintWriter(new FileOutputStream(CONF_PATH));
        pw.println(path);
        pw.close();
    }

    private static void changeCleanDBPath(String path) throws IOException {
        File file = new File(path);
        if(!(file.exists() && file.isFile())) {
            System.out.println("Invalid Target Clean DB File");
            Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
            System.exit(1);
        }

        Files.copy(file.toPath(), new File(CLEAN_DB).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String help(){
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t\tMyIntegrit\n\n");
        sb.append("Simple intrusion detection system.\n\n");
        sb.append("Usage: sudo service myintegrit [option]\n\n");
        sb.append("Options: \n");
        sb.append("\t- target [path]\n");
        sb.append("\tChoose [path] as the default target path.\n\n");
        sb.append("\t- createdb [file]\n");
        sb.append("\tCreate current state database from scratch or use the [file] passed through arguments.\n\n");
        sb.append("\t- reloaddb\n");
        sb.append("\tVerify current state against known db.\n\n");
        sb.append("\t- exportdb [file]\n");
        sb.append("\tExport the current clean base to given [file].\n\n");
        sb.append("\t- automatic [time in seconds]\n");
        sb.append("\tVerify current state against known db at 60 seconds or the [time in second] inserted value (optional).\n\n");
        sb.append("\t- restart [time in seconds]\n");
        sb.append("\tRestart the automatic verification of the current state against known db at 60 seconds or the [time in seconds] inserted valued (optional).\n\n");
        sb.append("\t- stop\n");
        sb.append("\tStop the program.");

        return sb.toString();
    }

    public static void main(String args[]) throws IOException, NoSuchAlgorithmException, InterruptedException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        addSignalHandlers();

        /* Initialize ProcessBuilder with Daemon args */
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "/home/db/Workspace/IntelliJ-Idea/My-Integrit/Daemon/target/daemon-1.0-jar-with-dependencies.jar", String.valueOf(getPid()));
        Process p = pb.start();

        /* Get Daemon pid */
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        daemon_pid = f.getInt(p);

        Thread.sleep(1000);
        task = args[0];

        switch(task) {
            case "target":
                changeTargetPath(args[1]);
                Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
                break;
            case "exportdb":
                if(args.length < 2) {
                    System.out.println("Please insert the destination path");
                    System.exit(1);
                }
                Files.copy(new File(CLEAN_DB).toPath(), new File(args[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
                break;
            case "createdb":
                if(args.length > 1) {
                    changeCleanDBPath(args[1]);
                    Runtime.getRuntime().exec("kill -SIGTERM " + daemon_pid);
                }
                else { Runtime.getRuntime().exec("kill -SIGHUP " + daemon_pid); }
                break;
            case "reloaddb":
                Runtime.getRuntime().exec("kill -SIGALRM " + daemon_pid);
                break;
            case "automatic":
                int sleep_time = Integer.parseInt(args[1]) * 1000;
                while (true) {
                    Runtime.getRuntime().exec("kill -SIGALRM " + daemon_pid);
                    Thread.sleep(sleep_time);
                }
            case "help":

            default:
                System.out.println(help());
                System.exit(1);
        }

        p.waitFor();
    }
}

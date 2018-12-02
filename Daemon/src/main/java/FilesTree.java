import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FilesTree extends SimpleFileVisitor<Path> {

    private static String iNode(Path file, BasicFileAttributes bfa) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(file + "\n");
        sb.append("\t"+ PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        sb.append("\t"+(Files.getFileAttributeView(file, FileOwnerAttributeView.class)).getOwner());
        sb.append("\t" + Files.readAttributes(file, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group() + "\n");
        sb.append("\t"+ bfa.creationTime());
        sb.append("\t" +bfa.lastAccessTime());
        sb.append("\t"+ bfa.lastModifiedTime() + "\n");
        sb.append("\t"+ bfa.isDirectory());
        sb.append("\t"+ bfa.isOther());
        sb.append("\t"+ bfa.isRegularFile());
        sb.append("\t"+ bfa.isSymbolicLink() + "\n");
        sb.append("\t"+ bfa.size() + "\n");

        return sb.toString();
    }

    // Print information about each type of file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes bfa) throws IOException {
        System.out.println(iNode(file, bfa));
        return CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        BasicFileAttributes bfa = Files.readAttributes(dir, BasicFileAttributes.class);
        System.out.println(iNode(dir, bfa));
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
}
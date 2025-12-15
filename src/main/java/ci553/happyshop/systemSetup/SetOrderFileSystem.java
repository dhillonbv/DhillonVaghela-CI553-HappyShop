package ci553.happyshop.systemSetup;

import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for seting up the folder structure and orderCounter file required for the order system.
 *
 * ⚠ WARNING:
 *  Running this class will WIPE ALL EXISTING ORDERS by deleting all files inside the orders folder.
 *  It resets the order system to a clean state.
 *
 * It performs the following actions:
 * 1. Deletes all existing files inside the orders folder (but retains the folder structure).
 * 2. Ensures that all required order-related folders exist:
 *    - The main orders folder (`orders/`)
 *    - Subfolders for each order state: `ordered/`, `progressing/`, and `collected/`
 * 3. Creates the orderCounter.txt file inside the 'orders/' folder if it does not already exist, initializing it to "0".
 *   - The `orderCounter.txt`
 *
 * By centralizing file system setup for order storage in this class,
 * any future changes to the order-related directory structure or initialization behavior
 * can be managed in one place, avoiding scattered logic across the codebase.
 */

public class SetOrderFileSystem {
    private static final Lock lock = new ReentrantLock();    // Create a global lock
    private static Path orderCounterPath = StorageLocation.orderCounterPath;
    private static Path[] foldersPaths = {
            StorageLocation.ordersPath,
            StorageLocation.orderedPath,
            StorageLocation.progressingPath,
            StorageLocation.collectedPath
    };

    public static void main(String[] args) throws IOException {
        deleteFilesInFolder(foldersPaths[0]);
        createFolders(foldersPaths);
        createOrderCounterFile(orderCounterPath);
    }

    // Recursively deletes all files in folder
    public static void deleteFilesInFolder(Path folder) throws IOException {
        if (Files.exists(folder)) {
            lock.lock();
            try {
                Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file); //delete individual files
                        return FileVisitResult.CONTINUE;
                    }
                });
                System.out.println("Deleted files in folder: " + folder);
            } finally {
                lock.unlock();
            }
        }
        else {
            System.out.println("Folder " + folder + " does not exist");
        }
    }

    // Create all necessary folders for storing orderCounter file, order files and images if they do not exist
    private static void createFolders(Path[] paths) throws IOException {
        for (Path path : paths) {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created folder: " + path);
            }
        }
    }

    //create the single orderCounter file and write "0" if it doesn't exist
    private static void createOrderCounterFile(Path path) throws IOException {
        // Create the file and write "0" if it doesn't exist
        if (Files.notExists(path)) {
            Files.writeString(path, "0", StandardOpenOption.CREATE_NEW);
            System.out.println("order Counter created: 0");
        }
    }
}

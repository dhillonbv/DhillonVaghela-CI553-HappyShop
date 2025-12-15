package ci553.happyshop.systemSetup;

import ci553.happyshop.storageAccess.DatabaseRWFactory;
import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The setDB class is responsible for resetting the database when the system is first initialized.
 * This class performs operations that delete and recreate the database tables, as well as insert
 * default values for a fresh start. Ensuring that everything is properly set up for the fresh database state
 *
 * WARNING: This class should only be used once when starting the system for the first time. It
 * will wipe all current data in the database and replace it with a fresh, predefined structure and data.
 *
 * Key operations:
 * 1. Deletes all existing tables in the database.
 * 2. Recreates the database tables based on the initial schema.
 * 3. Inserts default values into the newly created tables.
 * 4. Deletes all existing image files from the working image folder (images/).
 * 5. Copies all image files from the backup folder (images_resetDB/) into the working image folder.
 */

public class SetDatabase {

    //Use the shared database URL from the factory, appending `;create=true` to create the database if it doesn't exist
    private static final String dbURL = DatabaseRWFactory.dbURL + ";create=true";
                                  //the value is "jdbc:derby:happyShopDB;create=true"

    private static Path imageWorkingFolderPath = StorageLocation.imageFolderPath;
    private static Path imageBackupFolderPath = StorageLocation.imageResetFolderPath;

    private String[] tables = {"ProductTable"};
    // Currently only "ProductTable" exists, but using an array allows easy expansion
    // if more tables need to be processed in the future without changing the logic structure.

    private static final Lock lock = new ReentrantLock();    // Create a global lock

    public static void main(String[] args) throws SQLException, IOException {
        SetDatabase setDB = new SetDatabase();
        setDB.clearTables(); // clear all tables in the tables array from database if they are existing
        setDB.initializeTable();//create and initialize databse and tables
        setDB.queryTableAfterInitilization();
        deleteFilesInFolder(imageWorkingFolderPath);
        copyFolderContents(imageBackupFolderPath, imageWorkingFolderPath);

    }

    //Deletes all existing tables in the database.
    private void clearTables() throws SQLException {
        lock.lock();  // 🔒 Lock first
        try (Connection con = DriverManager.getConnection(dbURL);
             Statement statement = con.createStatement()) {
            System.out.println("Database happyShopDB is connected successfully!");
            for (String table : tables) {
                try {
                    // Try to drop table directly
                    statement.executeUpdate("DROP TABLE " + table.toUpperCase());
                    System.out.println("Dropped table: " + table);
                } catch (SQLException e) {
                    if ("42Y55".equals(e.getSQLState())) {  // 42Y55 = Table does not exist
                        System.out.println("Table " + table + " does not exist. Skipping...");
                    }
                }
            }
        }
        finally {
            lock.unlock();  // 🔓 Always unlock in finally block
        }
    }
    //Recreates the database tables Inserts default values into the newly created tables.
    private void initializeTable() throws SQLException {
        lock.lock(); // Lock to ensure thread safety

        // Table creation and insert statements
        String[] iniTableSQL = {
                // Create ProductTable
                "CREATE TABLE ProductTable(" +
                        "productID CHAR(4) PRIMARY KEY," +
                        "description VARCHAR(100)," +
                        "unitPrice DOUBLE," +
                        "image VARCHAR(100)," +
                        "inStock INT," +
                        "CHECK (inStock >= 0)" +
                        ")",

                // Insert data into ProductTable
                "INSERT INTO ProductTable VALUES('0001', '40 inch TV', 269.00,'0001.jpg',100)",
                "INSERT INTO ProductTable VALUES('0002', 'DAB Radio', 29.99, '0002.jpg',100)",
                "INSERT INTO ProductTable VALUES('0003', 'Toaster', 19.99, '0003.jpg',100)",
                "INSERT INTO ProductTable VALUES('0004', 'Watch', 29.99, '0004.jpg',100)",
                "INSERT INTO ProductTable VALUES('0005', 'Digital Camera', 89.99, '0005.jpg',100)",
                "INSERT INTO ProductTable VALUES('0006', 'MP3 player', 7.99, '0006.jpg',100)",
                "INSERT INTO ProductTable VALUES('0007', 'USB drive', 6.99, '0007.jpg',100)",
                "INSERT INTO ProductTable VALUES('0008', 'USB2 drive', 7.99, '0008.jpg',100)",
                "INSERT INTO ProductTable VALUES('0009', 'USB3 drive', 8.99, '0009.jpg',100)",
                "INSERT INTO ProductTable VALUES('0010', 'USB4 drive', 9.99, '0010.jpg',100)",
                "INSERT INTO ProductTable VALUES('0011', 'USB5 drive', 10.99, '0011.jpg',100)",
                "INSERT INTO ProductTable VALUES('0012', 'USB6 drive', 10.99, '0011.jpg',100)",
        };

        try (Connection connection = DriverManager.getConnection(dbURL)) {
            System.out.println("Database happyShopDB is created successfully!");
            connection.setAutoCommit(false); // Disable auto-commit for the batch

            try (Statement statement = connection.createStatement()) {
                // First, create the table (DDL) - Execute this one separately from DML
                statement.executeUpdate(iniTableSQL[0]);  // Execute Create Table SQL

                // Prepare and execute the insert operations (DML)
                for (int i = 1; i < iniTableSQL.length; i++) {
                    statement.addBatch(iniTableSQL[i]);  // Add insert queries to batch
                }

                // Execute all the insert statements in the batch
                statement.executeBatch();
                connection.commit(); // Commit the transaction if everything was successful

                System.out.println("Table and data initialized successfully.");

            } catch (SQLException e) {
                connection.rollback(); // Rollback the transaction in case of an error
                System.err.println("Transaction rolled back due to an error!");
                e.printStackTrace();
            }
        } finally {
            lock.unlock(); // Ensure the lock is released after the operation
        }
    }

    private void queryTableAfterInitilization() throws SQLException {
        lock.lock();
        //Query ProductTable
        String sqlQuery = "SELECT * FROM ProductTable";

        System.out.println("-------------Product Information Below -----------------");
        String title = String.format("%-12s %-20s %-10s %-10s %s",
                "productID",
                "description",
                "unitPrice",
                "inStock",
                "image");
        System.out.println(title);  // Print formatted output

        try (Connection connection = DriverManager.getConnection(dbURL);
             Statement stat = connection.createStatement()){
            ResultSet resultSet = stat.executeQuery(sqlQuery);
            while (resultSet.next()) {
                String productID = resultSet.getString("productID");
                String description = resultSet.getString("description");
                double unitPrice = resultSet.getDouble("unitPrice");
                String image = resultSet.getString("image");
                int inStock = resultSet.getInt("inStock");
                String record = String.format("%-12s %-20s %-10.2f %-10d %s", productID, description, unitPrice, inStock, image);
                System.out.println(record);  // Print formatted output
            }
        }
        finally {
            lock.unlock();
        }
    }

    // Recursively deletes all files in a folder
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


    /**
     * The method Files.walkFileTree(Path, FileVisitor) traverses (or "walks through") a directory and all of its subdirectories.
     * It accepts two arguments:
     * 1. directory (Path or folder) path from which the traversal begins (the starting point of the walk).
     * 2. A FileVisitor object that defines the actions to be performed when a file or directory is visited.
     *    The visitor is an instance of the FileVisitor interface, which provides methods for handling different events during the traversal.
     *
     * Here, we use an anonymous class to create the second argument - the instance (object) –
     * An anonymous class allows you to extend a superclass (or implement an interface) and instantiate it in a single, concise step,
     * without needing to define a separate named class. It combines both class extension and object creation into one operation,
     * typically used when you need a one-off implementation of a class or interface.
     * (Note: the object is the anonymous class's)
     *
     * We did not use Files.walkFileTree(folder, new FileVisitor<>()) because FileVisitor is an interface, and we would need to implement
     * all of its methods ourselves. Instead, we use Files.walkFileTree(folder, new SimpleFileVisitor<>()) because:
     * - SimpleFileVisitor<> is an abstract class that implements the FileVisitor interface with default method implementations.
     * - We only need to override the methods (visitFile, postVisitDirectory) that we're interested in, which simplifies our code.
     */

    // Copies all files from source folder to destination folder
    public static void copyFolderContents(Path source, Path destination) throws IOException {
        lock.lock();
        if (!Files.exists(source)) {
            throw new IOException("Source folder does not exist: " + source);
        }

        // Create destination folder if it doesn't exist
        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }

        // Copy files from source folder to destination folder
        //Files.newDirectoryStream(source): list all entries (files and folders) directly in the source directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    Path targetFile = destination.resolve(file.getFileName());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        finally {
            lock.unlock();
        }
        System.out.println("Copied files from: " + source + " → " + destination);
    }

}

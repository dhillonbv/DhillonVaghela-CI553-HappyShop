package ci553.happyshop.storageAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * The ImageFileManager class handles the file operations related to product images in the warehouse system.
 * It is used by the Warehouse model to manage product images.
 *
 * - When a product is deleted from stock, its image file is also deleted from the storage location.
 * - When a new product is added or an existing product's image is changed, the image is copied to the specified
 *  destination folder with the product's ID as the image file name, including the original file extension.
 *
 * The class provides utility methods to delete image files and copy them to a new location.
 */

public class ImageFileManager {
    /**
     * Deletes an image file from the specified folder.
     *
     * @param folder The directory where the image is stored.
     * @param fileName The name of the file to be deleted.
     * @throws IOException If an I/O error occurs during the file deletion.
     */
    public static void deleteImageFile(String folder, String fileName) throws IOException {
        Path locationFolder = Paths.get(folder); // Folder where the image is stored
        Path iPath = locationFolder.resolve(fileName); // Full path to the image file

        if (Files.exists(iPath)) { // Check if the file exists
            Files.delete(iPath); // Permanently delete the file
            System.out.println("Deleted: " + iPath);
        } else {
            System.out.println("File not found: " + iPath);
        }
    }

    /**
     * Copies an image file from the source URI to a specified destination folder with a new name.
     * The new name is the product ID (fileNameWithoutExtension) with the original file extension.
     *
     * @param sourceUri The URI of the source image file.
     * @param destinationFolder The destination folder where the image will be copied.
     * @param fileNameWithoutExtension The name to assign to the copied image file (without extension).
     * @return The name of the copied image file (with extension).
     */
    public static String copyFileToDestination(String sourceUri, String destinationFolder, String fileNameWithoutExtension) throws IOException {
        // Get the source file path and file name
        Path sourcePath = Paths.get(sourceUri);  // Source image uri (e.g., "C:/Users/shan/Desktop/mark.jpg")
        String sourceFileName = sourcePath.getFileName().toString();  // e.g., "mark.jpg"

        // Extract file extension from the source file
        String fileExtension = sourceFileName.substring(sourceFileName.lastIndexOf('.'));  // e.g., ".jpg"

        // Prepare the destination file path
        Path destinationFolderPath = Paths.get(destinationFolder);  // Destination folder path

        String fileNameWithExtension = fileNameWithoutExtension + fileExtension;
        Path destinationPath = destinationFolderPath.resolve(fileNameWithExtension);  // Combine the product ID (fileNameWithoutExtension) with the extension

        // Copy the file to the destination folder with the specified name
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File copied successfully to: " + destinationPath);
        return fileNameWithExtension;
    }

}

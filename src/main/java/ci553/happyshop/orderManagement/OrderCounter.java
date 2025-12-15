package ci553.happyshop.orderManagement;

import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * OrderCounter is responsible for generating unique, sequential orderIDs for new orders.
 *
 * <p>This class manages a persistent counter stored in an external text file:(ie,"orders/orderCounter.txt").
 * On first use, it creates the file and starts the counter at 1.
 * For subsequent calls, it reads the current number, increments it by 1, writes the updated value
 * back to the file, and returns the new order ID.</p>
 *
 * <p>File access is synchronized using a file lock to ensure safe operation in a multi-threaded
 * or multi-process environment. The method is simple to use by OrderHub
 * or any component that needs to generate order numbers.</p>
 *
 * <p>FileChannel allows exclusive locking of files or specific regions of files,
 * which prevents other threads or processes from accessing the file simultaneously,
 * ensuring data integrity. </p>
 *
 * <p> ByteBuffer allows you to work with raw byte data efficiently.
 * It interacts directly with FileChannel for reading and writing,
 * making file access faster and more flexible than traditional streams.</p>
 */

public class OrderCounter {

    public static int generateOrderId() throws IOException {
        Path path = StorageLocation.orderCounterPath;

        // Lock and increment the ID
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {

            //creates a ByteBuffer of the same size as the file — so you can read the whole thing.
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer); //Reads the file content into the buffer.
            buffer.flip(); //Prepares the buffer for reading.
            /** why we must buffer.flip();
             * After reading data into the buffer,
             * the cursor (position) is at the end of the data that was just read, not before it.
             * If you want to read the data you've just written into the buffer,
             * you need to move the cursor back to the start of the buffer so that you can read from it
             */

            //Gets the raw byte array from the buffer so you can convert it to a string or number.
            String content = new String(buffer.array()).trim();
            int currentId = Integer.parseInt(content);
            int newId = currentId + 1;

            channel.position(0); // Move to the start of the file
            channel.truncate(0); // Clear all content in the file (file size becomes 0)
            //This wraps an existing byte array into a buffer — so you can write it with channel.write().
            channel.write(ByteBuffer.wrap(String.valueOf(newId).getBytes()));

            System.out.println("OrderId was generated for now: " + newId);
            return newId;
        }
    }
}

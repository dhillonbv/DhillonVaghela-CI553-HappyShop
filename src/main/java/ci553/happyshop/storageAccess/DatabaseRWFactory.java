package ci553.happyshop.storageAccess;

/**
 * The DatabaseRWFactory class centralizes database configuration and object creation.
 * It acts as a factory pattern to provide flexible instantiation of the DatabaseRW interface.
 *
 * Responsibilities:
 * - Holds the database URL used to connect to the database.
 * - Creates instances of DatabaseRW (e.g., DerbyRW, MySQLRW, SQLiteRW).
 *
 * Benefits:
 * - Database Abstraction: Keeps the system decoupled from specific database implementations.
 * - Easy Future Changes: To switch to a different database, only this factory needs updating.
 * - Centralized Creation: Simplifies maintenance by managing instantiation in one place.
 *
 * Example Usage:
 *     String url = DatabaseRWFactory.dbURL;
 *     DatabaseRW db = DatabaseRWFactory.createDatabaseRW();
 *
 * This hides the actual implementation (e.g., DerbyRW) from the rest of the system.
 */

public class DatabaseRWFactory {

    public static String dbURL = "jdbc:derby:happyShopDB"; //or other database URL in the future (eg MySQLRW or SQLiteRW)

    /**
     * Creates an instance of DatabaseRW (currently returning DerbyRW, but can be modified to return other implementations).
     */
    public static DatabaseRW createDatabaseRW() {
        return new DerbyRW(); // or other database implementations in the future (eg MySQLRW or SQLiteRW)
    }
}


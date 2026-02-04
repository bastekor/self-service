package mx.bastekor.selfservice.constants;

/**
 * Centralized API path constants for controller mappings.
 */
public final class ApiPaths {

    /** API version prefix. */
    public static final String VERSION = "/api/v1";
    /** Base path for self-service endpoints. */
    public static final String BASE = VERSION + "/self-service";

    /** Path to list directory content. */
    public static final String DIRECTORY_CONTENT = "/directory-content";
    /** Path to fetch a file content. */
    public static final String FILE_CONTENT = "/file-content";
    /** Path to create a directory. */
    public static final String CREATE_DIRECTORY = "/create-directory";
    /** Path to create a file. */
    public static final String CREATE_FILE = "/create-file";
    /** Path to rename a directory. */
    public static final String UPDATE_DIRECTORY = "/update-directory";
    /** Path to delete a file or directory by type. */
    public static final String DELETE_BY_TYPE = "/delete/{type}";

    private ApiPaths() {
    }
}

package mx.bastekor.selfservice.constants;

public final class ApiPaths {

    public static final String VERSION = "/api/v1";
    public static final String BASE = VERSION + "/self-service";

    public static final String DIRECTORY_CONTENT = "/directory-content";
    public static final String FILE_CONTENT = "/file-content";
    public static final String CREATE_DIRECTORY = "/create-directory";
    public static final String CREATE_FILE = "/create-file";
    public static final String UPDATE_DIRECTORY = "/update-directory";
    public static final String DELETE_BY_TYPE = "/delete/{type}";

    private ApiPaths() {
    }
}

package poker.common;

public final class Protocol {
    public static final String TYPE_JOIN = "join";
    public static final String TYPE_JOIN_OK = "join_ok";
    public static final String TYPE_JOIN_ERROR = "join_error";
    public static final String TYPE_ACTION = "action";
    public static final String TYPE_STATE = "state";
    public static final String TYPE_INFO = "info";
    public static final String TYPE_ERROR = "error";

    private Protocol() {
    }
}

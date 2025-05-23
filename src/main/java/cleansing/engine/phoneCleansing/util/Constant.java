package cleansing.engine.phoneCleansing.util;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.PushBuilder;

import java.util.List;
import java.util.UUID;

public class Constant {

    // TODO: Change with the proper audio file
    public static final String AUDIO_FILE_NAME = "hello-world";
//    public static final String AUDIO_FILE_NAME = "/var/spool/asterisk/monitor/085121212342-085121212342-1746525241.111";
    private final static Dotenv dotenv = Dotenv.load();
    public static final String CALLER_ID = "CALL_" + UUID.randomUUID();
    public static final String PREFIX_PROTOCOL = "PJSIP/";
    public static String ACTION_ID = "id-call-" + System.currentTimeMillis();
    public static final String SIP_PROVIDER = "@DALNET_TEST";
    public static final String ASTERISK_SERVER = dotenv.get("ASTERISK_SERVER");
    public static final String MANAGER_USERNAME = dotenv.get("MANAGER_USERNAME");
    public static final String MANAGER_PASSWORD = dotenv.get("MANAGER_PASSWORD");
    public static final String ARI_USERNAME = dotenv.get("ARI_CLEANSING_USERNAME");
    public static final String ARI_PASSWORD = dotenv.get("ARI_CLEANSING_PASSWORD");
    public static final String PREFIX_QUIROS_GUNGS = dotenv.get("PREFIX_QUIROS_GUNGS");
    public static final String PREFIX_QUIROS_BERIJALAN_DEV = dotenv.get("PREFIX_QUIROS_BERIJALAN_DEV");
//                    Integer.parseInt(dotenv.get("ASTERISK_PORT")),,
}

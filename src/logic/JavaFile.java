package logic;

import java.time.LocalDateTime;
import java.util.logging.Logger;

public class JavaFile {

    private static final Logger LOGGER = Logger.getLogger(JavaFile.class.getName());
    private int releaseIndex;
    private String className;
    private LocalDateTime creationDate;

    public JavaFile(String className, int releaseIndex, LocalDateTime creationDate){
        this.className = className;
        this.releaseIndex = releaseIndex;
        this.creationDate = creationDate;
    }

}

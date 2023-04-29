package logic;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReleaseNameAdapter {

    private static final Logger LOGGER = Logger.getLogger(ReleaseNameAdapter.class.getName());

    private final int position;
    private final String add;

    public ReleaseNameAdapter(int position, String add){
        this.position = position;
        this.add = add;
    }


    public String deriveGitName(String jiraName) {
        String gName;

        switch(this.position) {
            case 0:
                gName = this.add+jiraName;
                break;
            case 1:
                gName = jiraName+this.add;
                break;
            default:
                gName = null;
                LOGGER.log(Level.WARNING, "Invalid position. 0 as prefix and 1 to suffix" );
        }

        return gName;
    }


}

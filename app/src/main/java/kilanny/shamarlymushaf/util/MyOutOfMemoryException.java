package kilanny.shamarlymushaf.util;

import java.util.Locale;

/**
 * Created by Yasser on 12/20/2015.
 */
public class MyOutOfMemoryException extends RuntimeException {

    public final long totalMem;
    public final long currentMem;

    public MyOutOfMemoryException(long totalMem, long currentMem) {
        this.totalMem = totalMem;
        this.currentMem = currentMem;
    }

    @Override
    public String getMessage() {
        //return super.getMessage();
        return String.format(Locale.US,
                "MyOutOfMemory: Total Memory = %d (%d MB), Used = %d (%d MB)",
                totalMem, totalMem / (1024 * 1024), currentMem, currentMem / (1024 * 1024));
    }
}

package github.mattys1.autoconnect.connection;

import github.mattys1.autoconnect.Log;

public class ConnectionManager {
    private boolean isPlanActive = false;
    private ConnectionPosition startPos = null;
    private ConnectionPosition endPos = null;

   public ConnectionPosition getEndPos() { return endPos; }

    public void beginConnection(final ConnectionPosition start) {
        assert !isPlanActive : "Attempted to start connection plan while one was in progress";
        assert startPos == null && endPos == null : "Attempted to start connection without proper cleanup of start and end vectors";

        Log.info("beggining connection, {}", start);
        startPos = start;

        isPlanActive = true;
    }

    public void confirmConnection() {
        assert isPlanActive : "Attempted to confirm connection while one was not in progress";
        assert startPos != null && endPos != null : "Attempted to confirm connection, but the start vectors were not set";

        Log.info("confirming connection {}", endPos);

        startPos = null;
        endPos = null;
        isPlanActive = false;
    }

    public void cancelConnection() {
        assert isPlanActive : "Attempted to cancel connection while one was not in progress";
        assert startPos != null : "Attempted to cancel connection, that hadn't begun";

        Log.info("cancelling connection, {}", startPos);

        startPos = null;
        isPlanActive = false;
    }

    public void updateEndPos(final ConnectionPosition end) {
       assert isPlanActive : "Attempted to update end pos on inactive plan";
       assert startPos != null : "Attempted to update connection end without defined start";

       endPos = end;

       Log.info("updating end pos, {}", endPos);
    }

    public boolean active() {
       return isPlanActive;
    }
}

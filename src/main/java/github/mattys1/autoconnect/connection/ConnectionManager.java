package github.mattys1.autoconnect.connection;

import github.mattys1.autoconnect.Log;

public class ConnectionManager {
    private boolean isPlanActive = false;
    private ConnectionPosition startPos = null;
    private ConnectionPosition endPos = null;

    public void beginConnection(final ConnectionPosition start) {
        assert !isPlanActive : "Attempted to start connection plan while one was in progress";
        assert startPos == null && endPos == null : "Attempted to start connection without proper cleanup of start and end vectors";

        Log.info("beggining connection, {}", start);
        startPos = start;

        isPlanActive = true;
    }

    public void confirmConnection(final ConnectionPosition end) {
        assert isPlanActive : "Attempted to confirm connection while one was not in progress";
        assert startPos != null && endPos == null : "Attempted to confirm connection, but the start vector was not set";

        Log.info("confirming connection, {}", end);
        endPos = end;

        startPos = null;
        endPos = null;
        isPlanActive = false;
    }

    public void cancelConnection() {
        assert isPlanActive : "Attempted to cancel connection while one was not in progress";
        assert startPos != null && endPos == null : "Attempted to cancel connection, that hadn't begun";

        Log.info("cancelling connection, {}", startPos);

        startPos = null;
        isPlanActive = false;
    }
}

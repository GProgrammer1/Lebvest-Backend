package com.lebvest.model.events;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Move docs from pending/<requestId>/ to accepted/<requestId>/.
 * If keys is null/empty, the listener will list all files under the pending prefix.
 */
public class CompanySignupAcceptedMoveEvent implements Serializable {
    private UUID requestId;
    private List<String> keys; // optional

    public CompanySignupAcceptedMoveEvent() {}

    public CompanySignupAcceptedMoveEvent(UUID requestId, List<String> keys) {
        this.requestId = requestId;
        this.keys = keys;
    }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public List<String> getKeys() { return keys; }
    public void setKeys(List<String> keys) { this.keys = keys; }
}

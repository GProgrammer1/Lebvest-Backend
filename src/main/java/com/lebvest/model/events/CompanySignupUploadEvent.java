package com.lebvest.model.events;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Upload the supplied files under the "pending" prefix of this requestId.
 */
public class CompanySignupUploadEvent implements Serializable {
    private UUID requestId;
    private List<Attachment> files;

    public CompanySignupUploadEvent() {}

    public CompanySignupUploadEvent(UUID requestId, List<Attachment> files) {
        this.requestId = requestId;
        this.files = files;
    }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public List<Attachment> getFiles() { return files; }
    public void setFiles(List<Attachment> files) { this.files = files; }
}

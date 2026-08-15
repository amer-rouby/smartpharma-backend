package com.smartpharma.service;

public interface EmailService {

    /** Sends a plain-text email. Throws if SMTP isn't configured or the send fails -
     * callers should surface that as a real error, not swallow it silently. */
    void sendPlainTextEmail(String to, String subject, String body);

    /** Sends a plain-text email with a single binary attachment (e.g. a PDF). Same
     * fail-closed behavior as sendPlainTextEmail. */
    void sendEmailWithAttachment(String to, String subject, String body,
                                  byte[] attachment, String attachmentFilename, String attachmentContentType);
}

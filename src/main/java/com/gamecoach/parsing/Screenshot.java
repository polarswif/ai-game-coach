package com.gamecoach.parsing;

import java.util.Locale;
import java.util.Set;

/**
 * An uploaded image, checked before anything is sent to the AI.
 *
 * @param bytes     the file's contents
 * @param mediaType one of the formats the Claude API accepts: image/png, image/jpeg, image/webp, image/gif
 */
public record Screenshot(byte[] bytes, String mediaType) {

    /** The Claude API's limit for one inline image. */
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    static final Set<String> SUPPORTED = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    public Screenshot {
        if (bytes == null || bytes.length == 0) {
            throw new BadUploadException("The screenshot is empty.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new BadUploadException("The screenshot is " + (bytes.length / 1024 / 1024)
                    + " MB; the limit is 5 MB. Save it as JPEG or crop it to the scoreboard.");
        }
        mediaType = mediaType == null ? "" : mediaType.toLowerCase(Locale.ROOT).strip();
        if (mediaType.equals("image/jpg")) {
            mediaType = "image/jpeg";
        }
        if (!SUPPORTED.contains(mediaType)) {
            throw new BadUploadException("Unsupported image type \"" + mediaType + "\". Use PNG, JPEG, WebP or GIF "
                    + "(iPhone HEIC photos need converting first).");
        }
    }
}

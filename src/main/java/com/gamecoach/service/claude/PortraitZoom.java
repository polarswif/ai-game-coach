package com.gamecoach.service.claude;

import com.gamecoach.parsing.Screenshot;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * A zoomed copy of the left part of a screenshot, where the hero portraits are.
 *
 * <p>Why: the Claude API shrinks any image bigger than about 1.15 megapixels before the model sees
 * it, so on a 1080p or 1440p screenshot every portrait loses detail. Cutting out just the left strip
 * keeps it under that limit at full resolution (or enlarges it), so the portraits come through
 * roughly 1.5 to 2 times sharper. The full screenshot is still sent for everything else.
 */
final class PortraitZoom {

    /** The API's resize thresholds: about 1.15 megapixels, and 1568 pixels on the long edge. */
    static final int MAX_PIXELS = 1_150_000;
    static final int MAX_EDGE = 1568;
    /** Enlarging past this adds pixels but no detail. */
    static final double MAX_SCALE = 2.0;

    private PortraitZoom() {
    }

    /**
     * The left {@code fraction} of the image, scaled to the largest size the API keeps as is.
     * Empty when the image can't be decoded (WebP and GIF aren't readable by Java out of the box),
     * in which case the reader works from the full screenshot alone, as before.
     */
    static Optional<Screenshot> leftStrip(Screenshot screenshot, double fraction) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(screenshot.bytes()));
            if (source == null || source.getWidth() < 200 || source.getHeight() < 200) {
                return Optional.empty();
            }
            int cropWidth = (int) Math.round(source.getWidth() * fraction);
            int height = source.getHeight();
            double scale = Math.min(MAX_SCALE, Math.min(
                    (double) MAX_EDGE / Math.max(cropWidth, height),
                    Math.sqrt((double) MAX_PIXELS / ((double) cropWidth * height))));
            int outWidth = Math.max(1, (int) Math.floor(cropWidth * scale));
            int outHeight = Math.max(1, (int) Math.floor(height * scale));

            BufferedImage out = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                // Source rectangle (0, 0)-(cropWidth, height) drawn into the whole output image.
                g.drawImage(source, 0, 0, outWidth, outHeight, 0, 0, cropWidth, height, null);
            } finally {
                g.dispose();
            }
            byte[] jpeg = jpeg(out);
            return jpeg.length > Screenshot.MAX_BYTES ? Optional.empty() : Optional.of(new Screenshot(jpeg, "image/jpeg"));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();   // a zoom is a bonus; never fail an upload over it
        }
    }

    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.92f);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}

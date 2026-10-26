package com.gamecoach.service.claude;

import com.gamecoach.parsing.Screenshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One labeled picture of every Marvel Rivals hero portrait, sent with each scoreboard screenshot.
 *
 * <p>Why: each hero has one fixed portrait (plus Lord and animated Champion versions of the same
 * art), but the model doesn't reliably remember which face belongs to which hero, especially for
 * newer heroes. With the real portraits beside the screenshot, naming a hero becomes matching
 * pictures, which vision models do far better than recalling.
 *
 * <p>The portraits live in {@code src/main/resources/heroes/portraits/}, listed in index.csv as
 * {@code label,file}. The sheet is drawn once, when first needed, and kept in memory.
 */
@Component
public class HeroPortraitSheet {

    private static final Logger log = LoggerFactory.getLogger(HeroPortraitSheet.class);
    private static final String FOLDER = "/heroes/portraits/";

    static final int COLUMNS = 8;
    static final int TILE = 116;
    static final int LABEL_HEIGHT = 26;

    /** A portrait and the name printed under it. */
    record Entry(String label, String file) {
    }

    private final List<Entry> entries;
    private volatile Optional<Screenshot> sheet;

    public HeroPortraitSheet() {
        this.entries = readIndex();
    }

    /** Everything in index.csv, in file order. */
    List<Entry> entries() {
        return entries;
    }

    /**
     * The sheet as a PNG, or empty if it couldn't be drawn (then the reader works from the
     * screenshot alone, as before). Drawn on first use and reused after.
     */
    Optional<Screenshot> image() {
        Optional<Screenshot> current = sheet;
        if (current == null) {
            synchronized (this) {
                if (sheet == null) {
                    sheet = draw();
                }
                current = sheet;
            }
        }
        return current;
    }

    private Optional<Screenshot> draw() {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        try {
            int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
            int cellHeight = TILE + LABEL_HEIGHT;
            BufferedImage out = new BufferedImage(COLUMNS * TILE, rows * cellHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            try {
                g.setColor(new Color(24, 24, 28));
                g.fillRect(0, 0, out.getWidth(), out.getHeight());
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                for (int i = 0; i < entries.size(); i++) {
                    int x = (i % COLUMNS) * TILE;
                    int y = (i / COLUMNS) * cellHeight;
                    BufferedImage portrait = load(entries.get(i).file());
                    if (portrait != null) {
                        g.drawImage(portrait, x + 2, y + 2, TILE - 4, TILE - 4, null);
                    }
                    drawLabel(g, entries.get(i).label(), x, y + TILE, TILE);
                }
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(out, "png", png);
            log.info("Hero portrait sheet ready: {} portraits, {}x{} px, {} KB", entries.size(),
                    out.getWidth(), out.getHeight(), png.size() / 1024);
            return Optional.of(new Screenshot(png.toByteArray(), "image/png"));
        } catch (IOException | RuntimeException | Error e) {
            // A server without fonts can fail to draw text; the reader still works without the sheet.
            log.warn("Couldn't draw the hero portrait sheet; reading screenshots without it", e);
            return Optional.empty();
        }
    }

    /** White text centered under the tile, shrunk until it fits. */
    private static void drawLabel(Graphics2D g, String label, int x, int y, int width) {
        int size = 13;
        Font font;
        FontMetrics metrics;
        do {
            font = new Font(Font.SANS_SERIF, Font.BOLD, size);
            metrics = g.getFontMetrics(font);
            size--;
        } while (metrics.stringWidth(label) > width - 6 && size >= 8);
        g.setFont(font);
        g.setColor(Color.WHITE);
        int textX = x + Math.max(3, (width - metrics.stringWidth(label)) / 2);
        int textY = y + (LABEL_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2;
        g.drawString(label, textX, textY);
    }

    private static BufferedImage load(String file) throws IOException {
        try (InputStream in = HeroPortraitSheet.class.getResourceAsStream(FOLDER + file)) {
            if (in == null) {
                log.warn("Hero portrait {} is listed in index.csv but missing", file);
                return null;
            }
            return ImageIO.read(in);
        }
    }

    private static List<Entry> readIndex() {
        List<Entry> list = new ArrayList<>();
        try (InputStream in = HeroPortraitSheet.class.getResourceAsStream(FOLDER + "index.csv")) {
            if (in == null) {
                return List.of();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int comma = line.lastIndexOf(',');
                list.add(new Entry(line.substring(0, comma).strip(), line.substring(comma + 1).strip()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read the hero portrait index", e);
        }
        return List.copyOf(list);
    }
}

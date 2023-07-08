package dev.turtywurty.turtyapi.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public final class LGBTifier {
    private static final List<Color> COLORS = List.of(
            new Color(0xFF0000),
            new Color(0xFFA500),
            new Color(0xFFFF00),
            new Color(0x008000),
            new Color(0x0000FF),
            new Color(0x4B0082)
    );

    private LGBTifier() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static BufferedImage lgbtify(BufferedImage image) {
        var newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));

        int stripeHeight = (int) Math.ceil(image.getHeight() / 6D);
        int yIndex = 0;
        for (int y = 0; y < image.getHeight(); y += stripeHeight) {
            Color color = COLORS.get(yIndex++);
            graphics.setColor(color);
            graphics.fillRect(0, y, image.getWidth(), stripeHeight);
        }

        graphics.dispose();

        return newImage;
    }
}

package dev.turtywurty.turtyapi.image;

import com.jhlabs.image.*;
import com.jhlabs.math.ImageFunction2D;
import dev.turtywurty.turtyapi.Constants;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ImageUtils {
    private static final List<String> FILTERS = List.of("mix_channels", "contrast", "error_diffusion",
            "ordered_dithering", "exposure", "gain", "gamma", "gray", "grayscale", "adjust_hsb",
            "invert_alpha", "invert_color", "levels", "lookup", "map_colors", "channel_mask", "posterize", "quantize",
            "rescale", "rgb_adjust", "solarize", "threshold", "tritone", "bicubic_scale", "wrap_circle", "diffuse",
            "displace", "dissolve", "field_warp", "kaleidoscope", "marble", "mirror", "offset", "perspective",
            "pinch", "polar", "ripple", "shear", "lens_distort", "underwater", "tile", "twirl", "warp", "ripple",
            "block_mosaic", "border", "chrome", "color_halftone", "crystallize", "emboss", "feedback", "halftone",
            "light", "noise", "pointillize", "drop_shadow", "bump_map", "rubber_stamp", "weave", "smear", "sparkle",
            "blur", "box_blur", "edge_emboss", "convolve", "despeckle", "gaussian", "glow", "high_pass", "lens_blur",
            "dilation", "median", "erosion", "motion_blur", "oil_painting", "light_rays", "reduce_noise", "sharpen",
            "smart_blur", "unsharp", "variable_blur", "edge_detect_dog", "edge_detect", "edge_detect_laplace");

    public static Optional<BufferedImage> loadImage(URL url) {
        try {
            return Optional.of(ImageIO.read(url));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load image from url: " + url, exception);
            return Optional.empty();
        }
    }

    public static Optional<BufferedImage> loadImage(InputStream stream) {
        try {
            return Optional.of(ImageIO.read(stream));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load image from stream", exception);
            return Optional.empty();
        }
    }

    public static Optional<BufferedImage> loadImage(byte[] bytes) {
        try {
            return Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(bytes)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load image from bytes", exception);
            return Optional.empty();
        }
    }

    public static Optional<BufferedImage> loadImage(String base64) {
        try {
            return Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64))));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load image from base64", exception);
            return Optional.empty();
        }
    }

    public static byte[] toBytes(BufferedImage image) {
        try {
            var baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to convert image to bytes", exception);
            return new byte[0];
        }
    }

    public static String toBase64(BufferedImage image) {
        return Base64.getEncoder().encodeToString(toBytes(image));
    }

    public static Optional<BufferedImage> validateURL(String urlStr, Optional<Context> context) {
        if (urlStr == null || urlStr.isBlank()) {
            context.ifPresent(ctx -> ctx.status(HttpStatus.BAD_REQUEST).result("You must provide a URL!"));
            return Optional.empty();
        }

        // decode url
        urlStr = URLDecoder.decode(urlStr, StandardCharsets.UTF_8);

        // validate url
        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException ex) {
            context.ifPresent(ctx -> ctx.status(HttpStatus.BAD_REQUEST).result("Invalid URL!"));
            return Optional.empty();
        }

        // validate image
        return ImageUtils.loadImage(url);
    }

    public static boolean isValidFilter(String filter) {
        return FILTERS.contains(filter.toLowerCase(Locale.ROOT).trim());
    }

    public static BufferedImage getResized(BufferedImage image, int width, int height) {
        var resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = resized.createGraphics();

        graphics.drawImage(image, 0, 0, width, height, null);

        graphics.dispose();
        return resized;
    }

    public static BufferedImage getRotated(BufferedImage image, int angle) {
        var rotated = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = rotated.createGraphics();

        graphics.rotate(Math.toRadians(angle), image.getWidth() / 2.0, image.getHeight() / 2.0);
        graphics.drawImage(image, 0, 0, null);

        graphics.dispose();
        return rotated;
    }

    public static BufferedImage getFlipped(BufferedImage image, boolean horizontal) {
        var flipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = flipped.createGraphics();

        int x = horizontal ? image.getWidth() : 0;
        int y = horizontal ? 0 : image.getHeight();
        int width = horizontal ? -image.getWidth() : image.getWidth();
        int height = horizontal ? image.getHeight() : -image.getHeight();
        graphics.drawImage(image, x, y, width, height, null);

        graphics.dispose();
        return flipped;
    }

    public static BufferedImage getBlurred(BufferedImage image, int strength) {
        var blurred = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = blurred.createGraphics();

        graphics.drawImage(image, new GaussianFilter(strength), 0, 0);
        graphics.dispose();

        return blurred;
    }

    public static Optional<BufferedImage> handleFiltering(BufferedImage image, String filterStr, Context ctx) {
        BufferedImage filtered = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = filtered.createGraphics();

//        BufferedImageOp filter = switch (filterStr) {
//            case "mix_channels":
//                var mixFilter = new ChannelMixFilter();
//                mixFilter.setRedBlue(0);
//                mixFilter.setGreenRed(0);
//                mixFilter.setBlueGreen(0);
//
//                String into = ctx.queryParam("into");
//                if (into != null && !into.isBlank()) {
//                    switch (into.toLowerCase(Locale.ROOT)) {
//                        case "red", "r" -> mixFilter.setIntoR(127);
//                        case "green", "g" -> mixFilter.setIntoG(127);
//                        case "blue", "b" -> mixFilter.setIntoB(127);
//                    }
//                }
//
//                yield mixFilter;
//            case "contrast":
//                var contrastFilter = new ContrastFilter();
//                contrastFilter.setBrightness(0.0f);
//                contrastFilter.setContrast(1.0f);
//
//                Float brightness = ctx.queryParamAsClass("brightness", Float.class).getOrDefault(null);
//                if (brightness != null) {
//                    contrastFilter.setBrightness(brightness);
//                }
//
//                Float contrast = ctx.queryParamAsClass("contrast", Float.class).getOrDefault(null);
//                if (contrast != null) {
//                    contrastFilter.setContrast(contrast);
//                }
//
//                yield contrastFilter;
//            case "error_diffusion":
//                var errorDiffusionFilter = new DiffusionFilter();
//
//                int levels = ctx.queryParamAsClass("levels", Integer.class).getOrDefault(2);
//                boolean colorDither = !ctx.queryParamAsClass("mode", String.class).getOrDefault("").equalsIgnoreCase("monochrome");
//                boolean serpentine = ctx.queryParamAsClass("serpentine", Boolean.class).getOrDefault(false);
//
//                errorDiffusionFilter.setLevels(levels);
//                errorDiffusionFilter.setColorDither(colorDither);
//                errorDiffusionFilter.setSerpentine(serpentine);
//                yield errorDiffusionFilter;
//            case "ordered_dithering":
//                var orderedDitheringFilter = new DitherFilter();
//
//                levels = ctx.queryParamAsClass("levels", Integer.class).getOrDefault(2);
//                orderedDitheringFilter.setLevels(levels);
//                yield orderedDitheringFilter;
//            case "exposure":
//                var exposureFilter = new ExposureFilter();
//
//                float exposure = ctx.queryParamAsClass("exposure", Float.class).getOrDefault(1F);
//                if (exposure >= 0) {
//                    exposureFilter.setExposure(exposure);
//                }
//
//                yield exposureFilter;
//                case "gain":
//                    var gainFilter = new GainFilter();
//
//                    float gain = ctx.queryParamAsClass("gain", Float.class).getOrDefault(0.5F);
//                    if (gain >= 0) {
//                        gainFilter.setGain(gain);
//                    }
//
//                    float bias = ctx.queryParamAsClass("bias", Float.class).getOrDefault(0.5F);
//                    if (bias >= 0) {
//                        gainFilter.setBias(bias);
//                    }
//
//                    yield gainFilter;
//            case "gamma":
//                var gammaFilter = new GammaFilter();
//
//                float gamma = ctx.queryParamAsClass("gamma", Float.class).getOrDefault(1F);
//
//                float redGamma = ctx.queryParamAsClass("red_gamma", Float.class).getOrDefault(1F);
//                float greenGamma = ctx.queryParamAsClass("green_gamma", Float.class).getOrDefault(1F);
//                float blueGamma = ctx.queryParamAsClass("blue_gamma", Float.class).getOrDefault(1F);
//                if (redGamma >= 0 && greenGamma >= 0 && blueGamma >= 0) {
//                    gammaFilter.setGamma(redGamma, greenGamma, blueGamma);
//
//                    if (gamma >= 0 && gamma != redGamma && gamma != greenGamma && gamma != blueGamma) {
//                        gammaFilter.setGamma(gamma);
//                    }
//                }
//
//                yield gammaFilter;
//            case "gray":
//                yield new GrayFilter();
//            case "grayscale":
//                yield new GrayscaleFilter();
//            case "adjust_hsb":
//                var hsbFilter = new HSBAdjustFilter();
//
//                float hue = ctx.queryParamAsClass("hue", Float.class).getOrDefault(0F);
//                float saturation = ctx.queryParamAsClass("saturation", Float.class).getOrDefault(1F);
//                brightness = ctx.queryParamAsClass("brightness", Float.class).getOrDefault(1F);
//                if (hue >= 0) {
//                    hsbFilter.setHFactor(hue);
//                }
//
//                if (saturation >= 0) {
//                    hsbFilter.setSFactor(saturation);
//                }
//
//                if (brightness >= 0) {
//                    hsbFilter.setBFactor(brightness);
//                }
//
//                yield hsbFilter;
//            case "invert_alpha":
//                yield new InvertAlphaFilter();
//            case "invert_color":
//                yield new InvertFilter();
//            case "levels":
//                var levelsFilter = new LevelsFilter();
//
//                float lowLevel = ctx.queryParamAsClass("low_level", Float.class).getOrDefault(0F);
//                float highLevel = ctx.queryParamAsClass("high_level", Float.class).getOrDefault(1F);
//                float lowOutputLevel = ctx.queryParamAsClass("low_output_level", Float.class).getOrDefault(0F);
//                float highOutputLevel = ctx.queryParamAsClass("high_output_level", Float.class).getOrDefault(1F);
//
//                if (lowLevel >= 0) {
//                    levelsFilter.setLowLevel(lowLevel);
//                }
//
//                if (highLevel >= 0) {
//                    levelsFilter.setHighLevel(highLevel);
//                }
//
//                if (lowOutputLevel >= 0) {
//                    levelsFilter.setLowOutputLevel(lowOutputLevel);
//                }
//
//                if (highOutputLevel >= 0) {
//                    levelsFilter.setHighOutputLevel(highOutputLevel);
//                }
//
//                yield levelsFilter;
//            case "lookup":
//                var lookupFilter = new LookupFilter();
//
//                // parse gradient
//                String gradient = ctx.queryParam("gradient");
//                if (gradient != null && !gradient.isBlank()) {
//                    String[] gradientParts = gradient.split(",");
//                    if (gradientParts.length > 0) {
//                        int[] gradientInts = new int[gradientParts.length];
//                        for (int i = 0; i < gradientParts.length; i++) {
//                            gradientInts[i] = Integer.parseInt(gradientParts[i]);
//                        }
//
//                        // int[] rgb
//                        lookupFilter.setColormap(new Gradient(gradientInts));
//                    }
//                }
//
//                String grayscale = ctx.queryParam("grayscale");
//                if(grayscale != null) {
//                    lookupFilter.setColormap(new GrayscaleColormap());
//                }
//
//                // linear
//                String linear = ctx.queryParam("linear");
//                if(linear != null && !linear.isBlank()) {
//                    String[] linearParts = linear.split(",");
//                    if (linearParts.length != 2) {
//                        ctx.status(HttpStatus.BAD_REQUEST).result("Invalid linear gradient");
//                        yield null;
//                    }
//
//                    int color1 = Integer.parseInt(linearParts[0]);
//                    int color2 = Integer.parseInt(linearParts[1]);
//                    lookupFilter.setColormap(new LinearColormap(color1, color2));
//                }
//
//                // spectrum
//                String spectrum = ctx.queryParam("spectrum");
//                if(spectrum != null) {
//                    lookupFilter.setColormap(new SpectrumColormap());
//                }
//
//                // TODO: spline
//
//                yield lookupFilter;
//            case "map_colors":
//                String oldColorStr = ctx.queryParam("old_color");
//                String newColorStr = ctx.queryParam("new_color");
//
//                if(oldColorStr != null && newColorStr != null && !oldColorStr.isBlank() && !newColorStr.isBlank()) {
//                    int oldColor = Integer.parseInt(oldColorStr);
//                    int newColor = Integer.parseInt(newColorStr);
//
//                    yield new MapColorsFilter(oldColor, newColor);
//                }
//
//                yield new MapColorsFilter();
//            case "channel_mask":
//                var maskFilter = new MaskFilter();
//
//                String mask = ctx.queryParam("mask");
//                if(mask != null && !mask.isBlank()) {
//                    maskFilter.setMask(Integer.parseInt(mask));
//                }
//
//                yield maskFilter;
//            case "posterize":
//                var posterizeFilter = new PosterizeFilter();
//
//                int numLevels = ctx.queryParamAsClass("num_levels", Integer.class).getOrDefault(6);
//                if(numLevels >= 0) {
//                    posterizeFilter.setNumLevels(numLevels);
//                }
//
//                yield posterizeFilter;
//            case "quantize":
//                var quantizeFilter = new QuantizeFilter();
//
//                int numColors = ctx.queryParamAsClass("num_colors", Integer.class).getOrDefault(256);
//                if(numColors >= 0) {
//                    quantizeFilter.setNumColors(numColors);
//                }
//
//                boolean dither = ctx.queryParamAsClass("dither", Boolean.class).getOrDefault(false);
//                quantizeFilter.setDither(dither);
//
//                serpentine = ctx.queryParamAsClass("serpentine", Boolean.class).getOrDefault(true);
//                quantizeFilter.setSerpentine(serpentine);
//
//                yield quantizeFilter;
//            case "rescale":
//                var rescaleFilter = new RescaleFilter();
//
//                float scaleFactor = ctx.queryParamAsClass("scale_factor", Float.class).getOrDefault(1F);
//                if(scaleFactor >= 0) {
//                    rescaleFilter.setScale(scaleFactor);
//                }
//
//                yield rescaleFilter;
//            case "rgb_adjust":
//                var rgbAdjustFilter = new RGBAdjustFilter();
//
//                float redFactor = ctx.queryParamAsClass("red_factor", Float.class).getOrDefault(0F);
//                float greenFactor = ctx.queryParamAsClass("green_factor", Float.class).getOrDefault(0F);
//                float blueFactor = ctx.queryParamAsClass("blue_factor", Float.class).getOrDefault(0F);
//                if(redFactor >= 0) {
//                    rgbAdjustFilter.setRFactor(redFactor);
//                }
//
//                if(greenFactor >= 0) {
//                    rgbAdjustFilter.setGFactor(greenFactor);
//                }
//
//                if(blueFactor >= 0) {
//                    rgbAdjustFilter.setBFactor(blueFactor);
//                }
//
//                yield rgbAdjustFilter;
//            case "solarize":
//                yield new SolarizeFilter();
//            case "threshold":
//                var thresholdFilter = new ThresholdFilter();
//
//                // lower threshold, upper threshold, white, black
//                int lowerThreshold = ctx.queryParamAsClass("lower_threshold", Integer.class).getOrDefault(127);
//                int upperThreshold = ctx.queryParamAsClass("upper_threshold", Integer.class).getOrDefault(127);
//                int white = ctx.queryParamAsClass("white", Integer.class).getOrDefault(0xFFFFFF);
//                int black = ctx.queryParamAsClass("black", Integer.class).getOrDefault(0x000000);
//
//                if(lowerThreshold >= 0) {
//                    thresholdFilter.setLowerThreshold(lowerThreshold);
//                }
//
//                if(upperThreshold >= 0) {
//                    thresholdFilter.setUpperThreshold(upperThreshold);
//                }
//
//                if(white >= 0) {
//                    thresholdFilter.setWhite(white);
//                }
//
//                if(black >= 0) {
//                    thresholdFilter.setBlack(black);
//                }
//
//                yield thresholdFilter;
//            case "tritone":
//                var tritoneFilter = new TritoneFilter();
//
//                int low = ctx.queryParamAsClass("low", Integer.class).getOrDefault(0xFF000000);
//                int mid = ctx.queryParamAsClass("mid", Integer.class).getOrDefault(0xFF888888);
//                int high = ctx.queryParamAsClass("high", Integer.class).getOrDefault(0xFFFFFFFF);
//
//                if(low >= 0) {
//                    tritoneFilter.setShadowColor(low);
//                }
//
//                if(mid >= 0) {
//                    tritoneFilter.setMidColor(mid);
//                }
//
//                if(high >= 0) {
//                    tritoneFilter.setHighColor(high);
//                }
//
//                yield tritoneFilter;
//            case "bicubic_scale":
//                int width = ctx.queryParamAsClass("width", Integer.class).getOrDefault(32);
//                int height = ctx.queryParamAsClass("height", Integer.class).getOrDefault(32);
//
//                if(width < 0 || height < 0) {
//                    ctx.status(HttpStatus.BAD_REQUEST).result("Invalid width or height");
//                    yield null;
//                }
//
//                yield new BicubicScaleFilter(width, height);
//            case "circle":
//                var circleFilter = new CircleFilter();
//
//                float radius = ctx.queryParamAsClass("radius", Float.class).getOrDefault(10F);
//                float heightF = ctx.queryParamAsClass("height", Float.class).getOrDefault(20F);
//                float angle = ctx.queryParamAsClass("angle", Float.class).getOrDefault(0F);
//                float spreadAngle = ctx.queryParamAsClass("spread_angle", Float.class).getOrDefault((float)Math.PI);
//                float centreX = ctx.queryParamAsClass("centre_x", Float.class).getOrDefault(0.5F);
//                float centreY = ctx.queryParamAsClass("centre_y", Float.class).getOrDefault(0.5F);
//
//                if(radius >= 0) {
//                    circleFilter.setRadius(radius);
//                }
//
//                if(heightF >= 0) {
//                    circleFilter.setHeight(heightF);
//                }
//
//                if(angle >= 0) {
//                    circleFilter.setAngle(angle);
//                }
//
//                if(spreadAngle >= 0) {
//                    circleFilter.setSpreadAngle(spreadAngle);
//                }
//
//                if(centreX >= 0) {
//                    circleFilter.setCentreX(centreX);
//                }
//
//                if(centreY >= 0) {
//                    circleFilter.setCentreY(centreY);
//                }
//
//                parseEdgeActionAndInterpolation(circleFilter, ctx);
//
//                yield circleFilter;
//            case "diffuse":
//                var diffuseFilter = new DiffuseFilter();
//
//                int scale = ctx.queryParamAsClass("scale", Integer.class).getOrDefault(4);
//                if(scale >= 0) {
//                    diffuseFilter.setScale(scale);
//                }
//
//                parseEdgeActionAndInterpolation(diffuseFilter, ctx);
//
//                yield diffuseFilter;
//            case "displace":
//                var displaceFilter = new DisplaceFilter();
//
//                float amount = ctx.queryParamAsClass("amount", Float.class).getOrDefault(1F);
//                if(amount >= 0) {
//                    displaceFilter.setAmount(amount);
//                }
//
//                parseEdgeActionAndInterpolation(displaceFilter, ctx);
//
//                yield displaceFilter;
//            case "dissolve":
//                var dissolveFilter = new DissolveFilter();
//
//                float density = ctx.queryParamAsClass("density", Float.class).getOrDefault(1F);
//                if(density >= 0) {
//                    dissolveFilter.setDensity(density);
//                }
//
//                float softness = ctx.queryParamAsClass("softness", Float.class).getOrDefault(0F);
//                if(softness >= 0) {
//                    dissolveFilter.setSoftness(softness);
//                }
//
//                yield dissolveFilter;
//            case "field_warp":
//                var fieldWarpFilter = new FieldWarpFilter();
//
//                // amount, power, strength
//                float amountF = ctx.queryParamAsClass("amount", Float.class).getOrDefault(1F);
//                float power = ctx.queryParamAsClass("power", Float.class).getOrDefault(1F);
//                float strength = ctx.queryParamAsClass("strength", Float.class).getOrDefault(2F);
//
//                if(amountF >= 0) {
//                    fieldWarpFilter.setAmount(amountF);
//                }
//
//                if(power >= 0) {
//                    fieldWarpFilter.setPower(power);
//                }
//
//                if(strength >= 0) {
//                    fieldWarpFilter.setStrength(strength);
//                }
//
//                yield fieldWarpFilter;
//            case "kaleidoscope":
//                var kaleidoscopeFilter = new KaleidoscopeFilter();
//
//                angle = ctx.queryParamAsClass("angle", Float.class).getOrDefault(0F);
//                float angle2 = ctx.queryParamAsClass("angle2", Float.class).getOrDefault(0F);
//                centreX = ctx.queryParamAsClass("centre_x", Float.class).getOrDefault(0.5F);
//                centreY = ctx.queryParamAsClass("centre_y", Float.class).getOrDefault(0.5F);
//                int sides = ctx.queryParamAsClass("sides", Integer.class).getOrDefault(3);
//                radius = ctx.queryParamAsClass("radius", Float.class).getOrDefault(0F);
//
//                if(angle >= 0) {
//                    kaleidoscopeFilter.setAngle(angle);
//                }
//
//                if(angle2 >= 0) {
//                    kaleidoscopeFilter.setAngle2(angle2);
//                }
//
//                if(centreX >= 0) {
//                    kaleidoscopeFilter.setCentreX(centreX);
//                }
//
//                if(centreY >= 0) {
//                    kaleidoscopeFilter.setCentreY(centreY);
//                }
//
//                if(sides >= 0) {
//                    kaleidoscopeFilter.setSides(sides);
//                }
//
//                if(radius >= 0) {
//                    kaleidoscopeFilter.setRadius(radius);
//                }
//
//                yield kaleidoscopeFilter;
//            case "marble":
//                var marbleFilter = new MarbleFilter();
//
//                float xScale = ctx.queryParamAsClass("x_scale", Float.class).getOrDefault(4F);
//                float yScale = ctx.queryParamAsClass("y_scale", Float.class).getOrDefault(4F);
//                amount = ctx.queryParamAsClass("amount", Float.class).getOrDefault(1F);
//                float turbulence = ctx.queryParamAsClass("turbulence", Float.class).getOrDefault(1F);
//
//                if(xScale >= 0) {
//                    marbleFilter.setXScale(xScale);
//                }
//
//                if(yScale >= 0) {
//                    marbleFilter.setYScale(yScale);
//                }
//
//                if(amount >= 0) {
//                    marbleFilter.setAmount(amount);
//                }
//
//                if(turbulence >= 0) {
//                    marbleFilter.setTurbulence(turbulence);
//                }
//
//                yield marbleFilter;
//            case "mirror":
//                var mirrorFilter = new MirrorFilter();
//
//                float opacity = ctx.queryParamAsClass("opacity", Float.class).getOrDefault(1F);
//                centreY = ctx.queryParamAsClass("centre_y", Float.class).getOrDefault(0.5F);
//                float distance = ctx.queryParamAsClass("distance", Float.class).getOrDefault(0F);
//                angle = ctx.queryParamAsClass("angle", Float.class).getOrDefault(0F);
//                float rotation = ctx.queryParamAsClass("rotation", Float.class).getOrDefault(0F);
//                float gap = ctx.queryParamAsClass("gap", Float.class).getOrDefault(0F);
//
//                if(opacity >= 0) {
//                    mirrorFilter.setOpacity(opacity);
//                }
//
//                if(centreY >= 0) {
//                    mirrorFilter.setCentreY(centreY);
//                }
//
//                if(distance >= 0) {
//                    mirrorFilter.setDistance(distance);
//                }
//
//                if(angle >= 0) {
//                    mirrorFilter.setAngle(angle);
//                }
//
//                if(rotation >= 0) {
//                    mirrorFilter.setRotation(rotation);
//                }
//
//                if(gap >= 0) {
//                    mirrorFilter.setGap(gap);
//                }
//
//                yield mirrorFilter;
//            case "offset":
//                var offsetFilter = new OffsetFilter();
//
//                int xOffset = ctx.queryParamAsClass("x_offset", Integer.class).getOrDefault(0);
//                int yOffset = ctx.queryParamAsClass("y_offset", Integer.class).getOrDefault(0);
//                boolean wrap = ctx.queryParamAsClass("wrap", Boolean.class).getOrDefault(false);
//
//                if (xOffset >= 0) {
//                    offsetFilter.setXOffset(xOffset);
//                }
//
//                if (yOffset >= 0) {
//                    offsetFilter.setYOffset(yOffset);
//                }
//
//                offsetFilter.setWrap(wrap);
//
//                yield offsetFilter;
//
//        }
//
//        if(filter == null) {
//            return Optional.empty();
//        }

        graphics.drawImage(image, 0, 0, null);

        graphics.dispose();
        return Optional.of(filtered);
    }

    private static void parseEdgeActionAndInterpolation(TransformFilter filter, Context ctx) {
        int edgeAction = ctx.queryParamAsClass("edge_action", Integer.class).getOrDefault(TransformFilter.ZERO);
        if(edgeAction == TransformFilter.ZERO || edgeAction == TransformFilter.CLAMP || edgeAction == TransformFilter.WRAP) {
            filter.setEdgeAction(edgeAction);
        }

        int interpolation = ctx.queryParamAsClass("interpolation", Integer.class).getOrDefault(TransformFilter.BILINEAR);
        if(interpolation == TransformFilter.NEAREST_NEIGHBOUR || interpolation == TransformFilter.BILINEAR) {
            filter.setInterpolation(interpolation);
        }
    }
}

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

public class IconGen {
    public static void main(String[] args) {
        try {
            File baseDir = new File(".");
            File logoFile = new File(baseDir, "src/main/res/drawable/zenlock_logo_1779606754101.png");
            if (!logoFile.exists()) {
                System.out.println("Logo file does not exist: " + logoFile.getAbsolutePath());
                System.exit(1);
            }
            BufferedImage logoImg = ImageIO.read(logoFile);
            if (logoImg == null) {
                System.out.println("Could not read logo via ImageIO: " + logoFile.getAbsolutePath());
                System.exit(1);
            }

            Map<String, Integer> densities = new HashMap<>();
            densities.put("mdpi", 48);
            densities.put("hdpi", 72);
            densities.put("xhdpi", 96);
            densities.put("xxhdpi", 144);
            densities.put("xxxhdpi", 192);

            for (Map.Entry<String, Integer> entry : densities.entrySet()) {
                String d = entry.getKey();
                int size = entry.getValue();

                File destDir = new File(baseDir, "src/main/res/mipmap-" + d);
                destDir.mkdirs();

                // 1. ic_launcher.png (Rounded/Square Rect)
                BufferedImage launcherImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = launcherImg.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                GradientPaint gp = new GradientPaint(0f, 0f, Color.decode("#0F0F12"), (float) size, (float) size, Color.decode("#171530"));
                g.setPaint(gp);
                g.fillRect(0, 0, size, size);

                int logoSize = (int) (size * 66.0 / 108.0);
                int offset = (size - logoSize) / 2;
                g.drawImage(logoImg, offset, offset, logoSize, logoSize, null);
                g.dispose();

                ImageIO.write(launcherImg, "png", new File(destDir, "ic_launcher.png"));

                // 2. ic_launcher_round.png (Circular)
                BufferedImage roundImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gRound = roundImg.createGraphics();
                gRound.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gRound.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                gRound.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                Ellipse2D.Float circle = new Ellipse2D.Float(0f, 0f, (float) size, (float) size);
                gRound.setClip(circle);

                gRound.setPaint(gp);
                gRound.fillRect(0, 0, size, size);

                gRound.drawImage(logoImg, offset, offset, logoSize, logoSize, null);
                gRound.dispose();

                ImageIO.write(roundImg, "png", new File(destDir, "ic_launcher_round.png"));
            }

            // 3. custom_app_icon.png in drawable folder (512x512) for general purposes
            File customIconFile = new File(baseDir, "src/main/res/drawable/custom_app_icon.png");
            int size = 512;
            BufferedImage customImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gCustom = customImg.createGraphics();
            gCustom.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gCustom.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gCustom.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            GradientPaint gp = new GradientPaint(0f, 0f, Color.decode("#0F0F12"), (float) size, (float) size, Color.decode("#171530"));
            gCustom.setPaint(gp);
            gCustom.fillRect(0, 0, size, size);

            int logoSize = (int) (size * 66.0 / 108.0);
            int offset = (size - logoSize) / 2;
            gCustom.drawImage(logoImg, offset, offset, logoSize, logoSize, null);
            gCustom.dispose();

            ImageIO.write(customImg, "png", customIconFile);

            System.out.println("IconGen successfully generated all icons.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

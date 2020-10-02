package tmp_image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class Main {
    public static void main(String[] args) {
        File file = new File("/home/ruslan/geo" + "/picture.jpg");
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Color color = new Color(image.getRGB(20, 21));

        int blue = color.getBlue();
        int red = color.getRed();
        int green = color.getGreen();
        System.out.println(blue + " " + red + " " + green);
        Color newColor = new Color(255, 255, 255);
        for (int i = 10, j = 100; i <= 150; i++)
            image.setRGB(j, i, newColor.getRGB());
        File output = new File("/home/ruslan/geo/picture2.jpg");
        try {
            ImageIO.write(image, "jpg", output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
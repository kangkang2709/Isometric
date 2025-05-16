package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FontGenerator {

    private static final String VIETNAMESE_CHARS = generateVietnameseCharacters();

    private static String generateVietnameseCharacters() {
        StringBuilder builder = new StringBuilder();
        for (char c = '\u0041'; c <= '\u1EF9'; c++) {
            if (Character.isLetter(c)) {
                builder.append(c);
            }
        }
        for (char c = '0'; c <= '9'; c++) {
            builder.append(c);
        }
        builder.append(".-[]:,_*"); // Add dot and minus for float numbers
        return builder.toString();
    }

    public static BitmapFont generateVietNameseFont(String fontName, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/" + fontName));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = VIETNAMESE_CHARS;
        parameter.color = Color.WHITE;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }
}
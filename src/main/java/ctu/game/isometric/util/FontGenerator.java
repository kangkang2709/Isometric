package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FontGenerator {

    private static final String VIETNAMESE_CHARS = generateVietnameseCharacters();

    private static String generateVietnameseCharacters() {
        String vietnameseLetters = "AÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬBCDĐEÉÈẺẼẸÊẾỀỂỄỆFGHIÍÌỈĨỊJKLMNOÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢPQRSTUÚÙỦŨỤƯỨỪ" +
                "ỬỮỰVWXYÝỲỶỸỴZ" +
                "aáàảãạăắằẳẵặâấầẩẫậbcdđeéèẻẽẹêếềểễệfghiíìỉĩịjklmnoóòỏõọôốồổỗộơớờởỡợpqrstuúùủũụưứừ" +
                "ửữựvwxyýỳỷỹỵz0123456789";
        String specialCharacters = ".-[]:,_*/><!@#$%^&-=`~(){}'|?+=;";
        return vietnameseLetters + specialCharacters;
    }

    public static BitmapFont generateVietNameseFont(String fontName, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/" + fontName));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.characters = VIETNAMESE_CHARS;
            parameter.color = Color.WHITE;
            return generator.generateFont(parameter);
        } finally {
            generator.dispose();
        }
    }
}
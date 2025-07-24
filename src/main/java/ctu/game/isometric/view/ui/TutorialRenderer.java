package ctu.game.isometric.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class TutorialRenderer extends UIRenderer {
    private String[] tutorialPages;
    private int currentTutorialPage;

    public TutorialRenderer(SpriteBatch batch, BitmapFont font, BitmapFont titleFont,
                            BitmapFont inputFont, ShapeRenderer shapeRenderer) {
        super(batch, font, titleFont, inputFont, shapeRenderer);

        this.tutorialPages = new String[]{
                "HƯỚNG DẪN CHIẾN ĐẤU - Trang 1/4",
                "KỸ NĂNG:\n" +
                        "-Attack - Đòn đánh vật lý cơ bản (Không tốn mana)\n" +
                        "-Word - Dùng từ đã học ngẫu nhiên để gây sát thương (5 mana)\n" +
                        "-TypeW - Gõ từ thủ công, nếu sai sẽ tự gây sát thương (5 mana)\n" +
                        "-Heal - Hồi máu (10 mana)\n" +
                        "-Defend - Tăng phòng thủ và hồi mana (Không tốn mana)",

                "HƯỚNG DẪN CHIẾN ĐẤU - Trang 2/4",
                "CƠ CHẾ CHIẾN ĐẤU:\n" +
                        "-Đây là hệ thống chiến đấu theo lượt: bạn và kẻ địch thay phiên nhau\n hành động.\n" +
                        "-Mỗi lượt, bạn chọn một kỹ năng để sử dụng.\n" +
                        "-Mana là năng lượng cần để dùng kỹ năng — khi cạn mana, bạn sẽ không\n thể dùng kỹ năng nữa.\n" +
                        "-Kẻ địch có hành vi khác nhau — có thể tấn công, phòng thủ, hoặc\n hồi máu. Hãy quan sát để chọn chiến thuật phù hợp.",

                "HƯỚNG DẪN CHIẾN ĐẤU - Trang 3/4",
                "CƠ CHẾ CHIẾN ĐẤU:\n" +
                        "-Sát thương gây ra = ATK (tấn công) - DEF (phòng thủ) của địch,tối thiểu là 1.\n" +
                        "-Kỹ năng 1 Word gây sát thương lớn: Word Score + ATK - DEF.\n" +
                        "-Word Score là điểm của từ trong từ điển bạn đã học — từ càng khó thì\n điểm càng cao.\n" +
                        "-Kỹ năng TypeW cho phép bạn gõ bất kỳ từ nào. Nếu đúng, sát thương\n rất mạnh. Nếu sai, bạn tự nhận sát thương.\n",

                "HƯỚNG DẪN CHIẾN ĐẤU - Trang 4/4",
                "MẸO:\n" +
                        "-Học từ mới để tăng sát thương kỹ năng Word.\n" +
                        "-TypeW rất mạnh nếu bạn gõ đúng, nhưng dễ gây hại nếu gõ sai.\n" +
                        "-Dùng Defend để hồi mana và tăng chỉ số phòng thủ.\n" +
                        "-Luôn chú ý lượng máu — Heal kịp lúc để tránh bị hạ gục.\n" +
                        "-Di chuột vào kỹ năng để xem chi tiết mô tả (tooltip).\n" +
                        "-Nhấn ESC để tạm dừng bất cứ lúc nào."
        };
    }

    public void setCurrentPage(int page) {
        this.currentTutorialPage = page;
    }

    public int getCurrentPage() {
        return currentTutorialPage;
    }

    public int getMaxPages() {
        return tutorialPages.length / 2;
    }

    @Override
    public void render() {
        float tutorialWidth = 700;
        float tutorialHeight = 500;
        float tutorialX = (SCREEN_WIDTH - tutorialWidth) / 2;
        float tutorialY = (SCREEN_HEIGHT - tutorialHeight) / 2;

        // Draw tutorial background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.95f);
        shapeRenderer.rect(tutorialX, tutorialY, tutorialWidth, tutorialHeight);

        // Border
        shapeRenderer.setColor(0.4f, 0.4f, 0.6f, 1);
        shapeRenderer.rect(tutorialX, tutorialY, tutorialWidth, 3);
        shapeRenderer.rect(tutorialX, tutorialY + tutorialHeight - 3, tutorialWidth, 3);
        shapeRenderer.rect(tutorialX, tutorialY, 3, tutorialHeight);
        shapeRenderer.rect(tutorialX + tutorialWidth - 3, tutorialY, 3, tutorialHeight);

        // Navigation buttons
        float buttonWidth = 100;
        float buttonHeight = 40;
        float prevButtonX = tutorialX + 50;
        float nextButtonX = tutorialX + tutorialWidth - 150;
        float closeButtonX = tutorialX + tutorialWidth - 110;
        float buttonY = tutorialY + 30;
        float closeButtonY = tutorialY + tutorialHeight - 50;

        // Previous button (if not first page)
        if (currentTutorialPage > 0) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
            shapeRenderer.rect(prevButtonX, buttonY, buttonWidth, buttonHeight);
        }

        // Next button (if not last page)
        if (currentTutorialPage < tutorialPages.length - 2) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
            shapeRenderer.rect(nextButtonX, buttonY, buttonWidth, buttonHeight);
        }

        // Close button
        shapeRenderer.setColor(0.3f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(closeButtonX, closeButtonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Draw tutorial content
        batch.begin();

        // Title
        titleFont.setColor(Color.CYAN);
        titleFont.draw(batch, tutorialPages[currentTutorialPage],
                tutorialX + 50, tutorialY + tutorialHeight - 50);

        // Content
        inputFont.setColor(Color.WHITE);
        String content = tutorialPages[currentTutorialPage + 1];
        String[] lines = content.split("\n");

        float lineY = tutorialY + tutorialHeight - 100;
        for (String line : lines) {
            inputFont.draw(batch, line, tutorialX + 50, lineY);
            lineY -= 25;
        }

        // Navigation button text
        font.setColor(Color.WHITE);
        if (currentTutorialPage > 0) {
            font.draw(batch, "Lùi", prevButtonX + 20, buttonY + 25);
        }
        if (currentTutorialPage < tutorialPages.length - 2) {
            font.draw(batch, "Tiếp", nextButtonX + 35, buttonY + 25);
        }
        font.draw(batch, "Đóng", closeButtonX + 30, closeButtonY + 25);

        // Page indicator
        font.setColor(Color.LIGHT_GRAY);
        String pageInfo = "Trang " + ((currentTutorialPage / 2) + 1) + " of " + (tutorialPages.length / 2);
        font.draw(batch, pageInfo, tutorialX + tutorialWidth / 2 - 40, tutorialY + 20);

        batch.end();
    }

    public boolean handleClick(float screenX, float screenY) {
        float tutorialWidth = 700;
        float tutorialHeight = 500;
        float tutorialX = (SCREEN_WIDTH - tutorialWidth) / 2;
        float tutorialY = (SCREEN_HEIGHT - tutorialHeight) / 2;

        float buttonWidth = 100;
        float buttonHeight = 40;
        float prevButtonX = tutorialX + 50;
        float nextButtonX = tutorialX + tutorialWidth - 150;
        float closeButtonX = tutorialX + tutorialWidth - 110;
        float buttonY = tutorialY + 30;
        float closeButtonY = tutorialY + tutorialHeight - 50;

        // Previous page
        if (currentTutorialPage > 0 &&
                screenX >= prevButtonX && screenX <= prevButtonX + buttonWidth &&
                screenY >= buttonY && screenY <= buttonY + buttonHeight) {
            currentTutorialPage -= 2;
            if (currentTutorialPage < 0) currentTutorialPage = 0;
            return true;
        }

        // Next page
        if (currentTutorialPage < tutorialPages.length - 2 &&
                screenX >= nextButtonX && screenX <= nextButtonX + buttonWidth &&
                screenY >= buttonY && screenY <= buttonY + buttonHeight) {
            currentTutorialPage += 2;
            return true;
        }

        // Close tutorial
        if (screenX >= closeButtonX && screenX <= closeButtonX + buttonWidth &&
                screenY >= closeButtonY && screenY <= closeButtonY + buttonHeight) {
            return false; // Signal to close tutorial
        }

        return true; // Consume input while tutorial is open
    }
}
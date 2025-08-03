package ctu.game.isometric.controller;

import ctu.game.isometric.model.game.Tutorial;

import java.util.*;

public class TutorialManager {
    private final Map<String, List<Tutorial>> tutorials;


    public TutorialManager() {
        this.tutorials = new HashMap<>();
        initializeTutorials();
    }


    public void initializeTutorials() {
        addTutorial("movement", new Tutorial("1", "Di chuyển bằng bàn phím", "Dùng các phím mũi tên hoặc WASD để di chuyển nhân vật trên bản đồ.", "movement_keyboard.png", "movement", false));
        addTutorial("movement", new Tutorial("2", "Di chuyển bằng chuột", "Nhấp chuột vào một vị trí bất kỳ (trừ vật cản và NPC) để nhân vật tự động di chuyển đến đó.Một số bản đồ không cho phép dùng chuột.", "movement_mouse.png", "movement", false));

        addTutorial("stats", new Tutorial("1", "Tổng quan chỉ số", "Chỉ số nhân vật ảnh hưởng đến hiệu suất chơi game. Nhấn F1 để mở bảng chỉ số:", "stats_overview.png", "stats", false));
        addTutorial("stats", new Tutorial("2", "Chỉ số cơ bản", " + HP (Health Points) cho biết lượng sát thương nhân vật có thể chịu trước khi bị hạ gục. Nếu HP về 0, bạn thua.\n" +
                " + MP (Mana Points) - được dùng để thực hiện các hành động đặc biệt hoặc sử dụng vật phẩm. Hãy quản lý MP cẩn thận! \n" +
                " + Tấn công (ATK) - ATK quyết định lượng sát thương bạn gây ra cho kẻ địch.Tăng ATK để hạ gục kẻ địch nhanh hơn.\n " +
                " + DEF giúp giảm sát thương bạn nhận từ các đòn tấn công của kẻ địch. DEF càng cao thì càng ít bị tổn thương.\n" +
                " + Cấp độ thể hiện tiến trình của bạn. Nhận EXP (điểm kinh nghiệm) để lên cấp và tăng chỉ số (x1.5 mỗi cấp). Kinh nghiệm yêu cầu mỗi cấp là x50 kinh nghiệm ban đầu..", "", "stats", false));
        addTutorial("stats", new Tutorial("3", "Điểm số", "Điểm số thể hiện thành tích tổng thể của bạn trong game. Kiếm điểm bằng cách hoàn thành nhiệm vụ và mini-game.\nĐiểm có thể dùng như tiền tệ để mua bán vật phẩm với NPC Trader tại Village Forest", "score.png", "stats", false));


        addTutorial("quests", new Tutorial("1", "Tổng quan Nhiệm vụ", "Bạn có thể nhận nhiệm vụ tại Bảng Nhiệm Vụ bằng cách nói chuyện với NPC trong Quán Rượu (Tavern Map). Hoàn thành nhiệm vụ để nhận EXP và vật phẩm.", "quest_overview.png", "quests", false));
        addTutorial("quests", new Tutorial("2", "Bảng nhiệm vụ", "Nhiệm vụ có thể nhận từ NPC trong Quán Rượu hoặc Bảng Nhiệm Vụ. Mỗi nhiệm vụ có mô tả và yêu cầu cụ thể và phần thưởng tương ứng.", "quest_accept.png", "quests", false));
        addTutorial("quests", new Tutorial("3", "Theo dõi Nhiệm vụ", "Dùng nhật ký nhiệm vụ (Q) để theo dõi các nhiệm vụ đang làm. Nhật ký hiển thị mục tiêu và tiến trình. Có thể nộp nhiệm vụ tại Bảng Nhiệm Vụ.", "quest_tracking.png", "quests", false));

        addTutorial("inventory", new Tutorial("1", "Tổng quan Túi đồ", "Mở túi đồ (I) để xem tất cả vật phẩm bạn đã thu thập. Túi đồ hiển thị biểu tượng, số lượng và mô tả vật phẩm.", "inventory_overview.png", "inventory", false));
        addTutorial("inventory", new Tutorial("2", "Chọn Vật phẩm", "Nhấp vào ô vật phẩm để xem chi tiết, bao gồm hiệu ứng và mô tả. Vật phẩm được chọn có thể dùng hoặc loại bỏ.", "inventory.png", "inventory", false));
        addTutorial("inventory", new Tutorial("3", "Sử dụng Vật phẩm", "Một số vật phẩm có thể dùng để hồi máu, tăng sức mạnh hoặc hồi mana. Nhấp vào nút 'Dùng' để sử dụng vật phẩm đã chọn.", "inventory.png", "inventory", false));
        addTutorial("inventory", new Tutorial("4", "Vứt Vật phẩm", "Nếu muốn bỏ một vật phẩm, hãy chọn và nhấp nút 'Vứt'. Cẩn thận: vật phẩm đã vứt không thể khôi phục.", "inventory.png", "inventory", false));
        addTutorial("inventory", new Tutorial("5", "Loại Vật phẩm", "Vật phẩm có thể có các công dụng khác nhau: hồi HP/MP, tăng chỉ số (buff), nhiệm vụ hoặc vật phẩm cốt truyện. (Chi tiết hiệu quả xem ở túi đổ).", "inventory.png", "inventory", false));

        addTutorial("achievement", new Tutorial("1", "Tổng quan Thành tựu", "Thành tựu là những mục tiêu đặc biệt bạn có thể hoàn thành trong quá trình chơi. Mở khóa thành tựu giúp nhận thưởng và đánh dấu tiến trình của bạn.", "achievement_overview.png", "achievement", false));
        addTutorial("achievement", new Tutorial("2", "Xem Thành tựu", "Mở menu Thành tựu (F3) để xem những thành tựu đã mở hoặc còn khóa. Mỗi thành tựu có mô tả và thanh tiến trình.", "achievement.png", "achievement", false));
        addTutorial("achievement", new Tutorial("3", "Loại Thành tựu", "Thành tựu có thể đạt được qua việc đánh bại kẻ địch, chiến thắng trận đấu, tìm từ, hoặc hoàn thành nhiệm vụ. Một số yêu cầu hành động cụ thể.", "achievement.png", "achievement", false));
        addTutorial("achievement", new Tutorial("4", "Theo dõi Tiến trình", "Tiến trình thành tựu được cập nhật tự động. Kiểm tra thanh tiến trình và mô tả để biết bạn còn cách bao xa để mở khóa.", "achievement.png", "achievement", false));
        addTutorial("achievement", new Tutorial("5", "Nhận Thưởng", "Khi mở khóa thành tựu,phần thưởng sẽ tự động cập nhật như Điểm số (x10 giá trị mục tiêu của thành tựu).", "achievement.png", "achievement", false));

        addTutorial("maze", new Tutorial("1", "Tổng quan Mê cung", "Mê cung (21x21) là bản đồ được tạo ngẫu nhiên với đường đi, ngõ cụt và phần thưởng ẩn. Mục tiêu là tìm lối ra. Có 10 tầng: hoàn thành 1 tầng để quay về căn cứ, hoàn thành cả 10 tầng để đánh bại trùm cuối.", "maze_overview.png", "maze", false));
        addTutorial("maze", new Tutorial("2", "Sự kiện Bắt buộc", "Một số sự kiện phải được hoàn thành để mở đường đi tiếp. Bao gồm trận chiến, giải đố hoặc thử thách đặc biệt.", "event_mandatory.png", "maze", false));
        addTutorial("maze", new Tutorial("3", "Điểm Bắt đầu & Kết thúc", "Mỗi mê cung có điểm bắt đầu và điểm kết thúc. Lối ra thường nằm xa điểm vào và có thể yêu cầu giải đố hoặc đánh bại kẻ địch.", "maze_start_end.png", "maze", false));
        addTutorial("maze", new Tutorial("4", "Lối giả & Ngõ cụt", "Một số đường dẫn tới ngõ cụt hoặc lối ra giả. Hãy khám phá cẩn thận và dùng bản đồ nhỏ để tránh lạc. Chỉ có 1 lối dẫn về căn cứ (Map Village Forest), lối còn lại dẫn vào Dungeon. Hoàn thành Dungeon để quay về căn cứ.", "maze_start_end.png", "maze", false));
        addTutorial("maze", new Tutorial("5", "Rương Vật phẩm", "Rương được ẩn khắp mê cung. Mở rương để nhận vật phẩm. Một số rương nằm gần đường chính, số khác nằm ở nơi khó tìm.", "maze_chest.png", "maze", false));
        addTutorial("maze", new Tutorial("6", "Mini-Game", "Một số sự kiện yêu cầu hoàn thành mini-game như đố chữ hoặc thử thách từ vựng (ngẫu nhiên 1/3 loại mini-game). Hoàn thành sẽ nhận điểm số và 1 lần tung xúc xắc thưởng.", "event_minigame.png", "maze", false));
        addTutorial("maze", new Tutorial("7", "Kẻ Địch & Bẫy", "Kẻ địch và bẫy được đặt trong mê cung. Hãy chuẩn bị chiến đấu và cẩn thận với các nguy hiểm tiềm ẩn.", "maze_enemy_trap.png", "maze", false));
        addTutorial("maze", new Tutorial("8", "Sự kiện Chiến đấu", "Tại sự kiện chiến đấu, bạn có thể chọn đánh nhau hoặc tung xúc xắc để né tránh. Nếu tung ra giá trị bằng hoặc lớn hơn yêu cầu, bạn sẽ vượt qua. Có 2 loại chiến đấu theo lượt — hãy cẩn thận.", "event_battle.png", "maze", false));
        addTutorial("maze", new Tutorial("9", "Cơ chế Tung Xúc Xắc", "Trong sự kiện xúc xắc, nhấn 'DICE' để tung. Nếu kết quả đạt hoặc vượt qua giá trị mục tiêu, bạn thành công và có thể tiếp tục.", "event_dice_roll.png", "maze", false));
        addTutorial("maze", new Tutorial("10", "Ba Viên Ngọc Kỳ Bí", "Ở các tầng 4, 6 và 8, bạn sẽ đối mặt với những sinh vật canh giữ viên ngọc cổ đại: Ý Niệm, Biểu Đồ và Dòng Chảy. Đánh bại chúng để tiến gần hơn đến mục tiêu trở về thế giới của mình.", "", "maze", false));

        addTutorial("combat", new Tutorial(
                "1",
                "Tổng quan chiến đấu",
                "Chiến đấu trong game là hình thức đánh theo lượt giữa bạn và kẻ địch. Mục tiêu là sử dụng từ vựng để gây sát thương hoặc dùng vật phẩm hỗ trợ nhằm đánh bại kẻ địch.",
                "combat_overview.png",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "2",
                "Lượt của bạn",
                "- Nhấn nút \"Kỹ Năng\" để mở lưới kí tự, chọn các chữ cái trên lưới để tạo thành một từ tiếng Anh.\n" +
                        "- Sau khi chọn xong, nhấn \"CAST\" để tấn công." +
                        "- Sát thương dựa trên độ dài và điểm số của từ. Nếu từ không hợp lệ, bạn sẽ mất lượt!\n" +
                        " **Không nghĩ ra từ? Hãy dùng \"Tấn công thường\" để gây sát thương cơ bản.\n" +
                        " **Mở Pause Menu để xem chi tiết cách tính điểm và sát thương.",
                "turn-char.png",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "3",
                "Lượt của kẻ địch",
                "Kẻ địch sẽ thực hiện các hành động như tấn công, hồi máu, hoặc tạo hiệu ứng bất lợi cho bạn. Sức mạnh của chúng sẽ tăng theo cấp độ của bạn.\n\n" +
                        "** Di chuột vào kẻ địch để xem thông tin và chuẩn bị chiến thuật phù hợp.",
                "turn-enemy.png",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "4",
                "Máu và Năng lượng",
                "- **HP (Máu):** Nếu về 0, bạn sẽ thua trận.\n" +
                        "- **Mana (Năng lượng):** Dùng để kích hoạt vật phẩm hỗ trợ.\n" +
                        "Giữ HP và Mana luôn ở mức an toàn để sống sót lâu hơn!",
                "combat_health_mana.png",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "5",
                "Sử dụng vật phẩm",
                "Trong lượt của bạn, nhấn nút \"Vật phẩm\" để mở túi đồ. Chọn vật phẩm để sử dụng:\n" +
                        "- Hồi máu\n" +
                        "- Tăng sức mạnh\n" +
                        "- Gây sát thương đặc biệt\n" +
                        "Lưu ý: Sau khi dùng vật phẩm, lượt sẽ chuyển sang kẻ địch.",
                "combat_items.png",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "6",
                "Cơ chế từ vựng",
                "Từ càng dài và phức tạp thì sát thương gây ra càng cao.\n\n" +
                        "- Các nguyên âm (A, E, I, O, U) và chữ cái hiếm (Z, Q, X, J, K) sẽ giúp tăng điểm tấn công.\n" +
                        "- Một số từ đặc biệt còn tạo ra hiệu ứng mạnh mẽ lên kẻ địch hoặc hỗ trợ bạn.",
                "",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "7",
                "Hiệu ứng đặc biệt (Phần 1)",
                "Một số từ vựng hoặc vật phẩm sẽ gây hiệu ứng đặc biệt, như:\n" +
                        "- **Buff:** Tăng chỉ số có lợi cho bạn.\n" +
                        "- **Burn, Freeze, Toxic:** Gây bất lợi cho kẻ địch.\n\n" +
                        "⏳ Hiệu ứng chỉ tồn tại trong lượt của bạn và có số lượt giới hạn.",
                "special_effect.png",
                "combat",
                false
        ));

        addTutorial("combat", new Tutorial(
                "8",
                "Hiệu ứng đặc biệt (Phần 2)",
                "**Buffs:**\n" +
                        "- Tăng Tấn Công: +5 sát thương\n" +
                        "- Tăng Phòng Thủ: +5 giáp\n" +
                        "- Hồi Máu: Phục hồi mỗi lượt\n\n" +
                        "**Debuffs:**\n" +
                        "- **Freeze:** Kẻ địch bị đóng băng, không thể hành động và hồi 5 HP. Có 50% cơ hội thoát mỗi lượt.\n" +
                        "- **Burn:** Gây bỏng, trừ 2% máu mỗi lượt và giảm 20% sát thương của địch.\n" +
                        "- **Toxic:** Gây độc, trừ máu tăng dần theo mỗi lượt.\n\n" +
                        "⚠️ *Freeze* và *Burn* không thể tồn tại cùng lúc. Khi một hiệu ứng mới xung đột, cả hai sẽ bị hủy.\n" +
                        "💡 Mẹo: Tận dụng các từ đặc biệt để tạo hiệu ứng có lợi!",
                "",
                "combat",
                false
        ));


        addTutorial("combat", new Tutorial("8", "Chiến thắng & Thất bại", "Đánh bại kẻ địch để nhận phần thưởng và kinh nghiệm. Nếu bạn thua, trò chơi sẽ kết thúc hoặc bạn sẽ phải bắt đầu lại từ điểm lưu gần nhất.", "combat_victory_defeat.png", "combat", false));
        addTutorial("combat", new Tutorial("9", "Cơ chế Boss", "Khi đối đầu với Boss, chúng sẽ có sức mạnh và phòng thủ cao hơn. Một số Boss có hiệu ứng đặc biệt như vô hiệu hóa ô chữ, giảm kích thước lưới hoặc hồi máu liên tục.", "combat_boss_mechanics.png", "combat", false));
        addTutorial("combat", new Tutorial("10", "Chú thích ", "Nhật ký giao chiến hiển thị các hành động và thời gian còn lại của trận đấu. Sử dụng thanh cuộn để xem lại các lượt trước đó. Nếu hết thời gian sẽ tính là bạn thua.", "combat_log.png", "combat", false));

        addTutorial("damage", new Tutorial("1", "Tổng quan Tính Sát Thương", "Sát thương trong chiến đấu được tính dựa trên từ bạn tạo và chỉ số của kẻ địch. Hiểu rõ cơ chế sẽ giúp bạn tối ưu hóa lượng sát thương gây ra.", "", "damage", false));
        addTutorial("damage", new Tutorial("2", "Cách Tính Sát Thương của Người Chơi", "Sát thương của bạn được tính theo từ bạn tạo ra:\n- Mỗi chữ cái: +1 điểm\n- Mỗi nguyên âm (A, E, I, O, U): +2 điểm\n- Từ dài hơn 5 chữ cái: +2 điểm\n- Chữ hiếm (Z, Q, X, J, K): +2 điểm mỗi chữ\n- Danh từ (noun): +1 điểm\n- Động từ (verb): +2 điểm\n- Tính từ (adjective): +2 điểm\n- Trạng từ (adverb): +3 điểm\nSát thương cuối cùng = tổng điểm của từ + tấn công + cấp độ - phòng thủ kẻ địch." +
                "\n Tấn công thường: cấp độ + tấn công trừ phòng thủ địch (Min = 1)", "", "damage", false));
        addTutorial("damage", new Tutorial("3", "Cách Tính Sát Thương của Kẻ Địch", "Sát thương của kẻ địch = ngẫu nhiên (1-10) + tấn công căn bản + cấp độ người chơi - phòng thủ người chơi. Một số kẻ địch có thể gây thêm sát thương hoặc áp dụng hiệu ứng xấu tùy vào kỹ năng của chúng.", "", "damage", false));
        addTutorial("damage", new Tutorial("4", "Cơ Chế Phòng Thủ", "Chỉ số phòng thủ của bạn giúp giảm sát thương nhận vào. Phòng thủ càng cao, sát thương càng thấp. Hãy sử dụng vật phẩm hoặc hiệu ứng tăng cường để nâng cao phòng thủ trước khi chiến đấu.", "", "damage", false));
        addTutorial("damage", new Tutorial("5", "Mẹo Tăng Sát Thương", "Tạo những từ dài có chứa nguyên âm và chữ hiếm để tối đa hóa sát thương.\n Dùng vật phẩm hoặc buff để tăng hệ số tấn công và gây sát thương lớn hơn.", "", "damage", false));

    }

    public void addTutorial(String type, Tutorial tutorial) {
        if (tutorials.containsKey(type)) {
            tutorials.get(type).add(tutorial);
            tutorials.get(type).sort(Comparator.comparing(Tutorial::getId));
        } else {
            LinkedList<Tutorial> tutorialList = new LinkedList<>();
            tutorialList.add(tutorial);
            tutorials.put(type, tutorialList);
        }
    }


    public List<Tutorial> getTutorialsByType(String type) {
        return tutorials.getOrDefault(type, List.of());
    }

}
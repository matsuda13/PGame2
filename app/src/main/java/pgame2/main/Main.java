package pgame2.main;

import javax.swing.SwingUtilities;
import pgame2.menu.MainMenu;

public class Main {
    /** 開発用機能の有効状態。 */
    public static final boolean DEBUG = true;

    /** Swingのイベントスレッドでメニューを起動する。 */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::open);
    }
}

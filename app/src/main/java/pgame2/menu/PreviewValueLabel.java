package pgame2.menu;

import java.awt.*;
import javax.swing.*;

/** 現在値を残し、次の行に予測値と差分を示す数値欄。 */
public final class PreviewValueLabel extends JLabel {
    /** 項目名。 */
    private final String title;
    /** 最後に反映した確定値。 */
    private int value;

    /** 予測の有無で左側のレイアウトを動かさない。 */
    public PreviewValueLabel(String title) {
        this.title = title;
        setFont(new Font(Font.DIALOG, Font.PLAIN, 21));
        setForeground(new Color(41, 53, 65));
        setPreferredSize(new Dimension(177, 62));
        setMaximumSize(new Dimension(220, 62));
        setMinimumSize(new Dimension(177, 62));
        setVerticalAlignment(SwingConstants.TOP);
        showValue(0);
    }

    /** 最新の確定値だけを表示する。 */
    public void showValue(int value) {
        this.value = value;
        clearPreview();
    }

    /** 実際の値を変更せず、行動後の値と増減を表示する。 */
    public void showPreview(int current, int after, String unavailable) {
        value = current;
        long delta = (long) after - current;
        String color = !unavailable.isEmpty() ? "#b42828" : delta > 0 ? "#267d46" : delta < 0 ? "#b44b45" : "#686868";
        String difference = delta == 0 ? "±0" : (delta > 0 ? "+" : "") + delta;
        setText("<html>" + title + "   " + current + "<br><span style='font-size:12pt;color:" + color
                + "'>→ " + after + " (" + difference + ")</span></html>");
        setToolTipText(unavailable.isEmpty() ? "行動直後の予測（小目標報酬・日終了処理は含みません）" : unavailable);
        getAccessibleContext().setAccessibleDescription(title + " " + current + "から" + after + "、増減" + difference
                + (unavailable.isEmpty() ? "" : "、" + unavailable));
    }

    /** 選択解除時は最新の確定値へ戻す。 */
    public void clearPreview() {
        setText(title + "   " + value);
        setToolTipText(null);
        getAccessibleContext().setAccessibleDescription(null);
    }
}

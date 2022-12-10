package link.infra.borderlessmining.util;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class UnprotectedButtonWidget extends ButtonWidget {
    public UnprotectedButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
    }
}
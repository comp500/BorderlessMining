package link.infra.borderlessmining.dxgl;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class DXGLTestingMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new Screen(new LiteralText("DXGL testing menu")) {
			@Override
			protected void init() {
				addDrawableChild(new ButtonWidget(100, 100, 150, 20, new LiteralText("Attach"), widget -> {
					Window window = MinecraftClient.getInstance().getWindow();
					((DXGLWindowHooks)(Object)window).dxgl_attach(new DXGLWindow(window));
				}));

				addDrawableChild(new ButtonWidget(100, 130, 150, 20, new LiteralText("Detach"), widget -> {
					Window window = MinecraftClient.getInstance().getWindow();
					((DXGLWindowHooks)(Object)window).dxgl_detach();
				}));
			}

			@Override
			public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
				renderBackground(matrices);
				super.render(matrices, mouseX, mouseY, delta);
			}
		};
	}
}

package link.infra.borderlessmining.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;


public abstract class ConfigScreen extends Screen {
	private final Screen parent;
	private ConfigListWidget entries;

	protected ConfigScreen(Text title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	@Override
	protected final void init() {
		entries = new ConfigListWidget(client, width, height - 64, 32, 25);
		addElements();
		addDrawableChild(entries);
		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {save(); client.setScreen(parent);})
			.position(this.width / 2 - 100, this.height - 27)
			.size(200, 20)
			.build()
		);
	}

	private static class ConfigListWidget extends ElementListWidget<ConfigListEntry> {
		public ConfigListWidget(MinecraftClient minecraftClient, int width, int height, int y, int itemHeight) {
			super(minecraftClient, width, height, y, itemHeight);
		}

		@Override
		public int addEntry(ConfigListEntry entry) {
			return super.addEntry(entry);
		}

		public int getRowWidth() {
			return 400;
		}

		protected int getScrollbarPositionX() {
			return super.getScrollbarPositionX() + 32;
		}

		public Style getHoveredStyle(int mouseX, int mouseY) {
			Optional<Element> hovered = hoveredElement(mouseX, mouseY);
			//noinspection OptionalIsPresent
			if (hovered.isPresent()) {
				return ((ConfigListEntry)hovered.get()).getHoveredStyle(mouseX, mouseY);
			}
			return null;
		}
	}

	public static class ConfigListEntry extends ElementListWidget.Entry<ConfigListEntry> {
		private final List<ClickableWidget> buttons;

		public ConfigListEntry(List<ClickableWidget> buttons) {
			this.buttons = buttons;
		}

		@Override
		public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			for (ClickableWidget widget : buttons) {
				widget.setY(y);
				widget.render(context, mouseX, mouseY, tickDelta);
			}
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return buttons;
		}

		@Override
		public List<? extends Element> children() {
			return buttons;
		}

		public Style getHoveredStyle(int mouseX, int mouseY) {
			return null;
		}
	}

	@Override
	public final void removed() {
		save();
	}

	public abstract void save();

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		super.render(drawContext, mouseX, mouseY, delta);
		Style hoveredStyle = entries.getHoveredStyle(mouseX, mouseY);
		if (hoveredStyle != null) {
			drawContext.drawHoverEvent(this.textRenderer, hoveredStyle, mouseX, mouseY);
		}
		drawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 16777215);
	}

	public abstract void addElements();
	// Builder methods - should be called in the method you should override

	public void addOption(SimpleOption<?> opt) {
		entries.addEntry(new ConfigListEntry(Collections.singletonList(opt.createWidget(client.options, width / 2 - 155, 0, 310))));
	}

	public void addOptionsRow(SimpleOption<?> opt, SimpleOption<?> opt2) {
		entries.addEntry(new ConfigListEntry(Arrays.asList(
			opt.createWidget(client.options, width / 2 - 155, 0, 150),
			opt2.createWidget(client.options, width / 2 - 155 + 160, 0, 150))));
	}

	public static class ConfigListHeader extends ConfigListEntry {
		private final Text headerText;
		private final TextRenderer textRenderer;
		private final int width;
		private final int textWidth;
		private final Screen screen;

		public ConfigListHeader(Text headerText, TextRenderer textRenderer, int width, Screen screen) {
			super(Collections.emptyList());
			this.headerText = headerText;
			this.textRenderer = textRenderer;
			this.width = width;
			this.textWidth = textRenderer.getWidth(headerText);
			this.screen = screen;
		}

		@Override
		public void render(DrawContext drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			drawContext.drawCenteredTextWithShadow(textRenderer, headerText, width / 2, y + 5, 16777215);
		}

		private Style getStyleAt(int mouseX) {
			int min = (width / 2) - (textWidth / 2);
			int max = (width / 2) + (textWidth / 2);
			if (mouseX >= min && mouseX <= max) {
				return textRenderer.getTextHandler().getStyleAt(headerText, mouseX - min);
			}
			return null;
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return screen.handleTextClick(getStyleAt((int) mouseX));
		}

		@Override
		public Style getHoveredStyle(int mouseX, int mouseY) {
			Style style = getStyleAt(mouseX);
			if (style != null && style.getHoverEvent() != null) {
				return style;
			}
			return null;
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return Collections.singletonList(new Selectable() {
				@Override
				public Selectable.SelectionType getType() {
					return Selectable.SelectionType.HOVERED;
				}

				@Override
				public void appendNarrations(NarrationMessageBuilder builder) {
					builder.put(NarrationPart.TITLE, headerText);
				}
			});
		}
	}

	public void addHeading(Text text) {
		entries.addEntry(new ConfigListHeader(text, textRenderer, width, this));
	}

	public static class ConfigListTextField extends ConfigListEntry {
		private final TextFieldWidget textField;
		private final int textWidth;
		private final TextRenderer textRenderer;
		private final int x;

		public ConfigListTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text description, Supplier<String> getter, Consumer<String> setter, Predicate<String> validator) {
			super(Collections.singletonList(makeField(textRenderer, x, y, width, height, description)));
			this.textField = (TextFieldWidget) children().get(0);
			this.textWidth = textRenderer.getWidth(description);
			this.textRenderer = textRenderer;
			this.x = x;
			textField.setText(getter.get());
			textField.setChangedListener(value -> {
				if (validator.test(value)) {
					setter.accept(value);
					textField.setEditableColor(14737632);
				} else {
					textField.setEditableColor(16711680);
				}
			});
		}

		private static TextFieldWidget makeField(TextRenderer textRenderer, int x, int y, int width, int height, Text description) {
			return new TextFieldWidget(textRenderer, x + (width / 2) + 7, y, (width / 2) - 8, height, description) {
				@Override
				public void appendClickableNarrations(NarrationMessageBuilder builder) {
					builder.put(NarrationPart.TITLE, getNarrationMessage()); // Use the narration message which includes the description
				}
			};
		}

		@Override
		public Style getHoveredStyle(int mouseX, int mouseY) {
			int max = this.x + textWidth;
			if (mouseX >= this.x && mouseX <= max) {
				Style style = textRenderer.getTextHandler().getStyleAt(textField.getMessage(), mouseX - this.x);
				if (style != null && style.getHoverEvent() != null) {
					return style;
				}
			}
			return null;
		}

		@Override
		public void render(DrawContext drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			drawContext.drawTextWithShadow(textRenderer, textField.getMessage(), this.x, y + 5, 16777215);
			super.render(drawContext, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
		}
	}

	public void addTextField(Text description, Supplier<String> getter, Consumer<String> setter) {
		addTextField(description, getter, setter, Objects::nonNull);
	}

	public void addTextField(Text description, Supplier<String> getter, Consumer<String> setter, Predicate<String> validator) {
		entries.addEntry(new ConfigListTextField(textRenderer, width / 2 - 154, 0, 308, 18, description, getter, setter, validator));
	}

	public void addIntField(Text description, Supplier<Integer> getter, Consumer<Integer> setter) {
		addTextField(description, () -> getter.get().toString(), value -> setter.accept(Integer.parseInt(value)), value -> {
			try {
				Integer.parseInt(value);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		});
	}

	public void addFloatField(Text description, Supplier<Float> getter, Consumer<Float> setter) {
		addTextField(description, () -> getter.get().toString(), value -> setter.accept(Float.parseFloat(value)), value -> {
			try {
				Float.parseFloat(value);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		});
	}
}

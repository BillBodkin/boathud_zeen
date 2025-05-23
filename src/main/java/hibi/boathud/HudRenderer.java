package hibi.boathud;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class HudRenderer {

	private static final Identifier WIDGETS_TEXTURE = Identifier.tryParse("boathud","textures/widgets.png");
	private static final Identifier ZEEN_LOWER_TEXTURE = Identifier.tryParse("boathud","textures/zeen_lower.png");
	private static final Identifier ZEEN_MIDDLE_TEXTURE = Identifier.tryParse("boathud","textures/zeen_middle.png");
	private static final Identifier ZEEN_UPPER_TEXTURE = Identifier.tryParse("boathud","textures/zeen_upper.png");
	private final MinecraftClient client;

    // The index to be used in these scales is the bar type (stored internally as an integer, defined in Config)
	//                                        Pack     Mixed      Blue
	private static final double[] MIN_V =   {   0d,       8d,      40d}; // Minimum display speed (m/s)
	private static final double[] MAX_V =   {  40d,      70d,      70d}; // Maximum display speed (m/s)
	private static final double[] SCALE_V = {4.55d, 182d/62d, 182d/30d}; // Pixels for 1 unit of speed (px*s/m) (BarWidth / (VMax - VMin))
	// V coordinates for each bar type in the texture file
	//                                    Pk Mix Blu
	private static final int[] BAR_OFF = { 0, 10, 20};
	private static final int[] BAR_ON =  { 5, 15, 25};

	// Used for lerping
	private double displayedSpeed = 0.0d;

	public HudRenderer(MinecraftClient client) {
		this.client = client;
	}

	public void render(DrawContext graphics, float tickDelta) {
        int scaledWidth = this.client.getWindow().getScaledWidth();
        int scaledHeight = this.client.getWindow().getScaledHeight();
		int i = scaledWidth / 2;
		int nameLen = this.client.textRenderer.getWidth(Common.hudData.name);

		// Render boilerplate
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Lerping the displayed speed with the actual speed against how far we are into the tick not only is mostly accurate,
		// but gives the impression that it's being updated faster than 20 hz (which it isn't)
		this.displayedSpeed = MathHelper.lerp(tickDelta, this.displayedSpeed, Common.hudData.speed);

		if(Config.extended) {
			// Overlay texture and bar
			graphics.drawTexture(WIDGETS_TEXTURE, i - 91, scaledHeight - 83, 0, 70, 182, 33);
			this.renderBar(graphics, i - 91, scaledHeight - 83);

			// Sprites
			if(Common.hudData.isDriver) {
				// Left-right
				graphics.drawTexture(WIDGETS_TEXTURE, i - 86, scaledHeight - 65, 61, this.client.options.leftKey.isPressed() ? 38 : 30, 17, 8);
				graphics.drawTexture(WIDGETS_TEXTURE, i - 63, scaledHeight - 65, 79, this.client.options.rightKey.isPressed() ? 38 : 30, 17, 8);
				// Brake-throttle bar
				graphics.drawTexture(WIDGETS_TEXTURE, i, scaledHeight - 55, 0, this.client.options.forwardKey.isPressed() ? 45 : 40, 61, 5);
				graphics.drawTexture(WIDGETS_TEXTURE, i - 61, scaledHeight - 55, 0, this.client.options.backKey.isPressed() ? 35 : 30, 61, 5);
			}

			// Ping
			this.renderPing(graphics, i + 75 - nameLen, scaledHeight - 65);
			
			// Text
			// First Row
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, scaledHeight - 76, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i, scaledHeight - 76, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.gFormat, Common.hudData.g), i + 58, scaledHeight - 76, 0xFFFFFF);
			// Second Row
			graphics.drawTextWithShadow(this.client.textRenderer, Common.hudData.name, i + 88 - nameLen, scaledHeight - 65, 0xFFFFFF);

		} else { // Compact mode
			// hibi's compact mode (v 1.2.0):
			// graphics.drawTexture(WIDGETS_TEXTURE, i - 91, this.scaledHeight - 83, 0, 50, 182, 20);
			// this.renderBar(graphics, i - 91, this.scaledHeight - 83);
			// this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, this.scaledHeight - 76, 0xFFFFFF);
			// this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i + 58, this.scaledHeight - 76, 0xFFFFFF);

			// Sotumney's compact mode (v 1.2.1):
			graphics.drawTexture(WIDGETS_TEXTURE, i - 91, scaledHeight - 61, 0, 50, 182, 20);
			this.renderBar(graphics, i - 91, scaledHeight - 61);
			graphics.drawTexture(WIDGETS_TEXTURE, i - 21, scaledHeight - 55, 61, this.client.options.leftKey.isPressed() ? 38 : 30, 17, 8);
			graphics.drawTexture(WIDGETS_TEXTURE, i + 3, scaledHeight - 55, 79, this.client.options.rightKey.isPressed() ? 38 : 30, 17, 8);
			graphics.drawTexture(WIDGETS_TEXTURE, i, scaledHeight - 45, 0, this.client.options.forwardKey.isPressed() ? 45 : 40, 61, 5);
			graphics.drawTexture(WIDGETS_TEXTURE, i - 61, scaledHeight - 45, 0, this.client.options.backKey.isPressed() ? 35 : 30, 61, 5);
			this.typeCentered(graphics, String.format(Config.speedFormat, this.displayedSpeed * Config.speedRate), i - 58, scaledHeight - 54, 0xFFFFFF);
			this.typeCentered(graphics, String.format(Config.angleFormat, Common.hudData.driftAngle), i + 58, scaledHeight - 54, 0xFFFFFF);
		}
		RenderSystem.disableBlend();
	}

	/** Renders the speed bar atop the HUD, uses displayedSpeed to, well, display the speed. */
	private void renderBar(DrawContext graphics, int x, int y) {
		graphics.drawTexture(WIDGETS_TEXTURE, x, y, 0, BAR_OFF[Config.barType], 182, 5);
		if(Common.hudData.speed < MIN_V[Config.barType]) return;
//		if(Common.hudData.speed > MAX_V[Config.barType]) {
//			if(this.client.world.getTime() % 2 == 0) return;
//			graphics.drawTexture(WIDGETS_TEXTURE, x, y, 0, BAR_ON[Config.barType], 182, 5);
//			return;
//		}

		//graphics.drawTexture(WIDGETS_TEXTURE, x, y, 0, BAR_ON[Config.barType], (int)((this.displayedSpeed - MIN_V[Config.barType]) * SCALE_V[Config.barType]), 5);

		// Zeen

		double speedToUse = Math.min(displayedSpeed, MAX_V[Config.barType]);

		graphics.getMatrices().push();
		graphics.getMatrices().translate(x, y - 10, 0);
		graphics.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f));
		graphics.getMatrices().scale(15f / 256f, 12f / 256f, 15f / 256f);
		graphics.drawTexture(ZEEN_LOWER_TEXTURE, 0, 0, 0, 0, 256, 256);

		int fh = (int)((speedToUse - MIN_V[Config.barType]) * SCALE_V[Config.barType] * 256f / 12f);
		graphics.drawTexture(ZEEN_MIDDLE_TEXTURE, 0, -fh, 0, 0, 256, fh);

		graphics.drawTexture(ZEEN_UPPER_TEXTURE, 0, -256 - fh, 0, 0, 256, 256);

		graphics.getMatrices().pop();
	}

	/** Implementation is cloned from the notchian ping display in the tab player list.	 */
	private void renderPing(DrawContext graphics, int x, int y) {
		int offset = 0;
		if(Common.hudData.ping < 0) {
			offset = 40;
		}
		else if(Common.hudData.ping < 150) {
			offset = 0;
		}
		else if(Common.hudData.ping < 300) {
			offset = 8;
		}
		else if(Common.hudData.ping < 600) {
			offset = 16;
		}
		else if(Common.hudData.ping < 1000) {
			offset = 24;
		}
		else {
			offset = 32;
		}
		graphics.drawTexture(WIDGETS_TEXTURE, x, y, 246, offset, 10, 8);
	}

	/** Renders a piece of text centered horizontally on an X coordinate. */
	private void typeCentered(DrawContext graphics, String text, int centerX, int y, int color) {
		graphics.drawTextWithShadow(this.client.textRenderer, text, centerX - this.client.textRenderer.getWidth(text) / 2, y, color);
	}
}

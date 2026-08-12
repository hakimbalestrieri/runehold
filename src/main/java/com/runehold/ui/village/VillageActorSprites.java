package com.runehold.ui.village;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

final class VillageActorSprites
{
	private static final int NATIVE_WIDTH = 16;
	private static final int NATIVE_HEIGHT = 22;
	private final Map<String, BufferedImage> cache = new HashMap<>();

	BufferedImage get(VillageActor actor, int targetHeight, int frame)
	{
		int height = Math.max(12, targetHeight);
		String key = actor.getRole() + ":" + actor.getPose() + ":"
			+ actor.getDirection() + ":" + (frame & 1) + ":" + height;
		BufferedImage cached = cache.get(key);
		if (cached != null)
		{
			return cached;
		}

		BufferedImage nativeSprite = new BufferedImage(
			NATIVE_WIDTH, NATIVE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = nativeSprite.createGraphics();
		paintNative(g, actor, frame);
		g.dispose();

		int width = Math.max(8, NATIVE_WIDTH * height / NATIVE_HEIGHT);
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(nativeSprite, 0, 0, width, height, null);
		graphics.dispose();
		cache.put(key, scaled);
		return scaled;
	}

	private static void paintNative(Graphics2D g, VillageActor actor, int frame)
	{
		Color outline = new Color(0x15100B);
		Color skin = new Color(0xB88958);
		Color shirt = actor.getRole() == VillageActor.Role.WORKER
			? new Color(0x8A5A32) : new Color(0x536E8D);
		Color legs = new Color(0x3C2B1F);
		Color metal = new Color(0xBFC1B8);
		int bob = actor.getPose() == VillageActor.Pose.WALK ? frame & 1 : 0;

		g.setColor(outline);
		g.fillRect(5, 2 + bob, 6, 5);
		g.fillRect(4, 7 + bob, 8, 8);
		g.fillRect(4, 15, 3, 5);
		g.fillRect(9, 15, 3, 5);
		g.setColor(skin);
		g.fillRect(6, 3 + bob, 4, 4);
		g.setColor(shirt);
		g.fillRect(5, 8 + bob, 6, 6);
		g.setColor(legs);
		g.fillRect(5, 15, 2, 4);
		g.fillRect(9, 15, 2, 4);

		if (actor.getRole() == VillageActor.Role.WORKER)
		{
			int raised = actor.getPose() == VillageActor.Pose.WORK && (frame & 1) == 0 ? -3 : 1;
			g.setColor(outline);
			g.fillRect(11, 7 + raised, 2, 9);
			g.fillRect(9, 6 + raised, 6, 2);
			g.setColor(metal);
			g.fillRect(12, 8 + raised, 1, 7);
			g.fillRect(10, 7 + raised, 4, 1);
			return;
		}

		int x = actor.getDirection() == VillageActor.Direction.WEST ? 3 : 12;
		g.setColor(outline);
		g.fillRect(x, 4, 2, 15);
		g.setColor(metal);
		g.fillRect(x, 4, 1, 14);
	}
}

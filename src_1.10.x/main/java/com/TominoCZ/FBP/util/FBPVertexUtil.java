package com.TominoCZ.FBP.util;

import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;

public final class FBPVertexUtil
{
	public static float[] calculateUV(Vector3f from, Vector3f to, EnumFacing facing1)
	{
		EnumFacing facing = facing1;
		if (facing == null)
		{
			if (from.y == to.y)
			{
				facing = EnumFacing.UP;
			} else if (from.x == to.x)
			{
				facing = EnumFacing.EAST;
			} else if (from.z == to.z)
			{
				facing = EnumFacing.SOUTH;
			} else
			{
				return null; // !?
			}
		}

		switch (facing)
		{
		case DOWN:
			return new float[] { from.x, 16.0F - to.z, to.x, 16.0F - from.z };
		case UP:
			return new float[] { from.x, from.z, to.x, to.z };
		case NORTH:
			return new float[] { 16.0F - to.x, 16.0F - to.y, 16.0F - from.x, 16.0F - from.y };
		case SOUTH:
			return new float[] { from.x, 16.0F - to.y, to.x, 16.0F - from.y };
		case WEST:
			return new float[] { from.z, 16.0F - to.y, to.z, 16.0F - from.y };
		case EAST:
			return new float[] { 16.0F - to.z, 16.0F - to.y, 16.0F - from.z, 16.0F - from.y };
		}

		return null;
	}

	public static BakedQuad clone(BakedQuad quad)
	{
		return new BakedQuad(quad.getVertexData(), quad.getTintIndex(), quad.getFace(), quad.getSprite(),
				quad.shouldApplyDiffuseLighting(), quad.getFormat());
	}

	public static int multiplyColor(int src, int dst)
	{
		int out = 0;
		for (int i = 0; i < 32; i += 8)
		{
			out |= ((((src >> i) & 0xFF) * ((dst >> i) & 0xFF) / 0xFF) & 0xFF) << i;
		}
		return out;
	}

	public static BakedQuad recolorQuad(BakedQuad quad, int color)
	{
		int c = DefaultVertexFormats.BLOCK.getColorOffset() / 4;
		int v = DefaultVertexFormats.BLOCK.getNextOffset() / 4;
		int[] vertexData = quad.getVertexData();
		for (int i = 0; i < 4; i++)
		{
			vertexData[v * i + c] = multiplyColor(vertexData[v * i + c], color);
		}
		return quad;
	}

	public static void addRecoloredQuads(List<BakedQuad> src, int color, List<BakedQuad> target, EnumFacing facing)
	{
		for (BakedQuad quad : src)
		{
			BakedQuad quad1 = clone(quad);
			int c = DefaultVertexFormats.BLOCK.getColorOffset() / 4;
			int v = DefaultVertexFormats.BLOCK.getNextOffset() / 4;
			int[] vertexData = quad1.getVertexData();
			for (int i = 0; i < 4; i++)
			{
				vertexData[v * i + c] = multiplyColor(vertexData[v * i + c], color);
			}
			target.add(quad1);
		}
	}
}
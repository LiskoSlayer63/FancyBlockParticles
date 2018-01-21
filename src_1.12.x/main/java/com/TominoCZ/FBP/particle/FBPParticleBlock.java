package com.TominoCZ.FBP.particle;

import javax.vecmath.Vector2d;

import org.lwjgl.opengl.GL11;

import com.TominoCZ.FBP.FBP;
import com.TominoCZ.FBP.util.FBPRenderUtil;
import com.TominoCZ.FBP.vector.FBPVector3d;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class FBPParticleBlock extends Particle {

	public BlockPos pos;

	Block block;
	IBlockState blockState;

	BlockModelRenderer mr;

	IBakedModel modelPrefab;

	Minecraft mc;

	EnumFacing facing;

	FBPVector3d prevRot;
	FBPVector3d rot;

	long textureSeed;

	float startingHeight;
	float startingAngle;
	float step = 0.00275f;

	float height;
	float prevHeight;

	float smoothHeight;

	boolean lookingUp;
	boolean spawned = false;
	long tick = -1;

	boolean blockSet = false;

	TileEntity tileEntity;

	@SuppressWarnings("incomplete-switch")
	public FBPParticleBlock(World worldIn, double posXIn, double posYIn, double posZIn, IBlockState state, long rand) {
		super(worldIn, posXIn, posYIn, posZIn);

		pos = new BlockPos(posXIn, posYIn, posZIn);

		mc = Minecraft.getMinecraft();

		facing = mc.player.getHorizontalFacing();

		lookingUp = Float.valueOf(MathHelper.wrapDegrees(mc.player.rotationPitch)) <= 0;

		prevHeight = height = startingHeight = (float) FBP.random.nextDouble(0.065, 0.115);
		startingAngle = (float) FBP.random.nextDouble(0.03125, 0.0635);

		prevRot = new FBPVector3d();
		rot = new FBPVector3d();

		switch (facing) {
		case EAST:
			rot.z = -startingAngle;
			rot.x = -startingAngle;
			break;
		case NORTH:
			rot.x = -startingAngle;
			rot.z = startingAngle;
			break;
		case SOUTH:
			rot.x = startingAngle;
			rot.z = -startingAngle;
			break;
		case WEST:
			rot.z = startingAngle;
			rot.x = startingAngle;
			break;
		}

		textureSeed = rand;

		block = (blockState = state).getBlock();

		mr = mc.getBlockRendererDispatcher().getBlockModelRenderer();

		this.canCollide = false;

		modelPrefab = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);

		if (modelPrefab == null) {
			canCollide = true;
			this.isExpired = true;
		}

		tileEntity = worldIn.getTileEntity(pos);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onUpdate() {
		if (++particleAge >= 10)
			killParticle();

		if (!canCollide) {
			IBlockState s = mc.world.getBlockState(pos);

			if (s.getBlock() != FBP.FBPBlock || s.getBlock() == block) {
				if (blockSet && s.getBlock() == Blocks.AIR) {
					// the block was destroyed during the animation
					killParticle();

					FBP.FBPBlock.onBlockDestroyedByPlayer(mc.world, pos, s);
					mc.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
					return;
				}

				FBP.FBPBlock.copyState(mc.world, pos, blockState, this);
				mc.world.setBlockState(pos, FBP.FBPBlock.getDefaultState(), 2);

				Chunk c = mc.world.getChunkFromBlockCoords(pos);
				c.resetRelightChecks();
				c.setLightPopulated(true);

				FBPRenderUtil.markBlockForRender(pos);

				blockSet = true;
			}

			spawned = true;
		}

		if (this.isExpired || mc.isGamePaused())
			return;

		prevHeight = height;

		prevRot.copyFrom(rot);

		switch (facing) {
		case EAST:
			rot.z += step;
			rot.x += step;
			break;
		case NORTH:
			rot.x += step;
			rot.z -= step;
			break;
		case SOUTH:
			rot.x -= step;
			rot.z += step;
			break;
		case WEST:
			rot.z -= step;
			rot.x -= step;
			break;
		}

		height -= step * 5f;

		step *= 1.5678982f;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void renderParticle(BufferBuilder buff, Entity entityIn, float partialTicks, float rotationX,
			float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		if (this.isExpired)
			return;

		if (canCollide) {
			Block b = mc.world.getBlockState(pos).getBlock();
			if (block != b && b != Blocks.AIR && mc.world.getBlockState(pos).getBlock() != blockState.getBlock()) {
				mc.world.setBlockState(pos, blockState, 2);

				if (tileEntity != null)
					mc.world.setTileEntity(pos, tileEntity);

				mc.world.sendPacketToServer(new CPacketPlayerDigging(Action.ABORT_DESTROY_BLOCK, pos, facing));

				FBPRenderUtil.markBlockForRender(pos);

				// cleanup just to make sure it gets removed
				FBP.INSTANCE.eventHandler.removePosEntry(pos);
			}
			if (tick >= 1) {
				killParticle();
				return;
			}

			tick++;
		}
		if (!spawned)
			return;

		float f = 0, f1 = 0, f2 = 0, f3 = 0;

		float f5 = (float) (prevPosX + (posX - prevPosX) * partialTicks - interpPosX) - 0.5f;
		float f6 = (float) (prevPosY + (posY - prevPosY) * partialTicks - interpPosY) - 0.5f;
		float f7 = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ) - 0.5f;

		smoothHeight = ((float) (prevHeight + (height - prevHeight) * (double) partialTicks));

		final FBPVector3d smoothRot = rot.partialVec(prevRot, partialTicks);

		if (smoothHeight <= 0)
			smoothHeight = 0;

		FBPVector3d t = new FBPVector3d(0, smoothHeight, 0);
		FBPVector3d tRot = new FBPVector3d(0, smoothHeight, 0);

		switch (facing) {
		case EAST:
			if (smoothRot.z > 0) {
				this.canCollide = true;
				smoothRot.z = 0;
				smoothRot.x = 0;
			}

			t.x = -smoothHeight;
			t.z = smoothHeight;

			tRot.x = 1;
			break;
		case NORTH:
			if (smoothRot.z < 0) {
				this.canCollide = true;
				smoothRot.x = 0;
				smoothRot.z = 0;
			}

			t.x = smoothHeight;
			t.z = smoothHeight;
			break;
		case SOUTH:
			if (smoothRot.x < 0) {
				this.canCollide = true;
				smoothRot.x = 0;
				smoothRot.z = 0;
			}

			t.x = -smoothHeight;
			t.z = -smoothHeight;

			tRot.x = 1;
			tRot.z = 1;
			break;
		case WEST:
			if (smoothRot.z < 0) {
				this.canCollide = true;
				smoothRot.z = 0;
				smoothRot.x = 0;
			}

			t.x = smoothHeight;
			t.z = -smoothHeight;

			tRot.z = 1;
			break;
		}

		if (FBP.spawnPlaceParticles && canCollide && tick == 0) {
			if ((!(FBP.frozen && !FBP.spawnWhileFrozen)
					&& (FBP.spawnRedstoneBlockParticles || block != Blocks.REDSTONE_BLOCK))
					&& mc.gameSettings.particleSetting < 2) {
				spawnParticles();
			}
		}
		buff.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());

		Tessellator.getInstance().draw();
		mc.getRenderManager().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

		GlStateManager.pushMatrix();

		GlStateManager.enableCull();
		GlStateManager.enableColorMaterial();
		GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT_AND_DIFFUSE);

		GlStateManager.translate(f5, f6, f7);

		GlStateManager.translate(tRot.x, tRot.y, tRot.z);

		GlStateManager.rotate((float) Math.toDegrees(smoothRot.x), 1, 0, 0);
		GlStateManager.rotate((float) Math.toDegrees(smoothRot.z), 0, 0, 1);

		GlStateManager.translate(-tRot.x, -tRot.y, -tRot.z);
		GlStateManager.translate(t.x, t.y, t.z);

		if (FBP.animSmoothLighting)
			mr.renderModelSmooth(mc.world, modelPrefab, blockState, pos, buff, false, textureSeed);
		else
			mr.renderModelFlat(mc.world, modelPrefab, blockState, pos, buff, false, textureSeed);

		buff.setTranslation(0, 0, 0);

		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();

		mc.getTextureManager().bindTexture(FBP.LOCATION_PARTICLE_TEXTURE);
		buff.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
	}

	private void spawnParticles() {
		if (mc.world.getBlockState(pos.offset(EnumFacing.DOWN)).getBlock() instanceof BlockAir)
			return;

		AxisAlignedBB aabb = block.getSelectedBoundingBox(blockState, mc.world, pos);

		// z- = north
		// x- = west // block pos

		Vector2d[] corners = new Vector2d[] { new Vector2d(aabb.minX, aabb.minZ), new Vector2d(aabb.maxX, aabb.maxZ),

				new Vector2d(aabb.minX, aabb.maxZ), new Vector2d(aabb.maxX, aabb.minZ) };

		Vector2d middle = new Vector2d(pos.getX() + 0.5f, pos.getZ() + 0.5f);

		for (Vector2d corner : corners) {
			double mX = middle.x - corner.x;
			double mZ = middle.y - corner.y;

			mX /= -0.5;
			mZ /= -0.5;

			mc.effectRenderer.addEffect(new FBPParticleDigging(mc.world, corner.x, pos.getY() + 0.1f, corner.y, mX, 0,
					mZ, 1, 1, 1, block.getActualState(blockState, mc.world, pos), null, 0.6f, this.particleTexture)
							.multipleParticleScaleBy(0.5f).multiplyVelocity(0.5f));
		}

		for (Vector2d corner : corners) {
			if (corner == null)
				continue;

			double mX = middle.x - corner.x;
			double mZ = middle.y - corner.y;

			mX /= -0.45;
			mZ /= -0.45;

			mc.effectRenderer.addEffect(
					new FBPParticleDigging(mc.world, corner.x, pos.getY() + 0.1f, corner.y, mX / 3, 0, mZ / 3, 1, 1, 1,
							block.getActualState(blockState, mc.world, pos), null, 0.6f, this.particleTexture)
									.multipleParticleScaleBy(0.75f).multiplyVelocity(0.75f));
		}
	}

	public void killParticle() {
		this.isExpired = true;

		FBP.FBPBlock.blockNodes.remove(pos);
		FBP.INSTANCE.eventHandler.removePosEntry(pos);
	}

	@Override
	public void setExpired() {
		FBP.INSTANCE.eventHandler.removePosEntry(pos);
	}
}

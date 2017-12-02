package com.TominoCZ.FBP.gui;

import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.TominoCZ.FBP.FBP;
import com.TominoCZ.FBP.handler.FBPConfigHandler;
import com.TominoCZ.FBP.handler.FBPKeyInputHandler;
import com.TominoCZ.FBP.keys.FBPKeyBindings;
import com.TominoCZ.FBP.model.FBPModelHelper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;

public class FBPGuiFastAdd extends GuiScreen {

	FBPGuiButtonException animation, particle;

	final BlockPos selectedPos;
	final IBlockState selectedBlock;

	ItemStack displayItemStack;

	boolean closing = false;

	public FBPGuiFastAdd(BlockPos selected) {
		this.mc = Minecraft.getMinecraft();

		selectedPos = selected;
		IBlockState state = mc.theWorld.getBlockState(selectedPos);

		selectedBlock = state.getBlock() == FBP.FBPBlock ? FBP.FBPBlock.blockNodes.get(selectedPos).state : state;

		ItemStack is = selectedBlock.getActualState(mc.theWorld, selectedPos).getBlock().getPickBlock(selectedBlock,
				mc.objectMouseOver, mc.theWorld, selectedPos, mc.thePlayer);

		TileEntity te = mc.theWorld.getTileEntity(selectedPos);

		try {
			if (te != null)
				mc.storeTEInStack(is, te);
		} catch (Throwable t) {
		}

		displayItemStack = is.copy();
	}

	public FBPGuiFastAdd(ItemStack is) {
		this.mc = Minecraft.getMinecraft();

		selectedPos = null;
		selectedBlock = Block.getBlockFromName(is.getItem().getRegistryName().toString())
				.getStateFromMeta(is.getMetadata());

		displayItemStack = is.copy();
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void initGui() {
		this.buttonList.clear();

		animation = new FBPGuiButtonException(0, this.width / 2 - 100 - 30, this.height / 2 - 30 + 35, "", false,
				FBP.INSTANCE.isInExceptions(selectedBlock.getBlock(), false));
		particle = new FBPGuiButtonException(1, this.width / 2 + 100 - 30, this.height / 2 - 30 + 35, "", true,
				FBP.INSTANCE.isInExceptions(selectedBlock.getBlock(), true));

		Item ib = Item.getItemFromBlock(selectedBlock.getBlock());
		Block b = ib instanceof ItemBlock ? ((ItemBlock) ib).getBlock() : null;

		animation.enabled = b != null && !(b instanceof BlockDoublePlant)
				&& FBPModelHelper.isModelValid(b.getDefaultState());
		particle.enabled = selectedBlock.getBlock() != Blocks.REDSTONE_BLOCK;

		FBPGuiButton guide = new FBPGuiButton(-1, animation.xPosition + 30, animation.yPosition + 30 - 10,
				(animation.enabled ? "\u00A7a<" : "\u00A7c<") + "             "
						+ (particle.enabled ? "\u00A7a>" : "\u00A7c>"),
				false, false);
		guide.enabled = false;

		this.buttonList.addAll(Arrays.asList(new GuiButton[] { guide, animation, particle }));
	}

	@Override
	public void updateScreen() {
		Mouse.setGrabbed(true);

		boolean keyUp = false;

		if (selectedPos != null && (mc.objectMouseOver == null
				|| !mc.objectMouseOver.typeOfHit.equals(RayTraceResult.Type.BLOCK)
				|| mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() != selectedBlock.getBlock()
						&& mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() != FBP.FBPBlock)) {
			keyUp = true;
			FBPKeyInputHandler.INSTANCE.onInput();
		}
		try {
			if (!Keyboard.isKeyDown(FBPKeyBindings.FBPFastAdd.getKeyCode())
					|| (selectedPos == null && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))) {
				keyUp = true;
			}
		} catch (Exception e) {
			try {
				if (!Mouse.isButtonDown(FBPKeyBindings.FBPFastAdd.getKeyCode() + 100)
						|| (selectedPos == null && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))) {
					keyUp = true;
				}
			} catch (Exception e1) {
				closing = true;
				e.printStackTrace();
			}
		}

		if (closing || keyUp) {
			Block b = selectedBlock.getBlock();

			GuiButton selected = animation.isMouseOver() ? animation : (particle.isMouseOver() ? particle : null);

			if (selected != null) {
				boolean isParticle = particle.isMouseOver();

				if (selected.enabled) {
					if (!FBP.INSTANCE.isInExceptions(b, isParticle))
						FBP.INSTANCE.addException(b, isParticle);
					else
						FBP.INSTANCE.removeException(b, isParticle);

					if (isParticle)
						FBPConfigHandler.writeParticleExceptions();
					else
						FBPConfigHandler.writeAnimExceptions();

					if (FBP.enableDing)
						mc.thePlayer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.045f, 1.6f);
				}
			}

			if (keyUp)
				FBPKeyInputHandler.INSTANCE.onInput();

			mc.displayGuiScreen(null);
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		GuiButton clicked = animation.isMouseOver() ? animation : (particle.isMouseOver() ? particle : null);

		if (clicked != null && clicked.enabled)
			closing = true;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();

		// LIMIT MOUSE POS
		int optionRadius = 30;
		mouseX = MathHelper.clamp_int(mouseX, animation.xPosition + optionRadius, particle.xPosition + optionRadius);
		mouseY = height / 2 + 35;

		// RENDER BLOCK
		int x = width / 2 - 32;
		int y = height / 2 - 30 - 60;

		GlStateManager.enableDepth();
		GlStateManager.enableLight(0);
		GlStateManager.translate(x, y, 0);
		GlStateManager.scale(4, 4, 4);
		GlStateManager.enableColorMaterial();
		this.itemRender.renderItemAndEffectIntoGUI(this.mc.thePlayer, displayItemStack, 0, 0);

		this.itemRender.zLevel = 0.0F;
		this.zLevel = 0.0F;

		GlStateManager.scale(0.25, 0.25, 0.25);
		GlStateManager.translate(-x, -y, 0);

		// BLOCK INFO
		String itemName = (selectedPos == null ? displayItemStack.getItem() : selectedBlock.getBlock())
				.getRegistryName().toString();
		itemName = ((itemName.contains(":") ? "\u00A76\u00A7l" : "\u00A7a\u00A7l") + itemName).replaceAll(":",
				"\u00A7c\u00A7l:\u00A7a\u00A7l");

		FBPGuiHelper._drawCenteredString(fontRendererObj, itemName, width / 2, height / 2 - 19, 0);

		// EXCEPTIONS INFO
		String animationText1 = animation.enabled
				? (animation.isMouseOver() ? (animation.isInExceptions ? "\u00A7c\u00A7lREMOVE" : "\u00A7a\u00A7lADD")
						: "")
				: "\u00A7c\u00A7lCAN'T BE ANIMATED";
		String particleText1 = particle.enabled
				? (particle.isMouseOver() ? (particle.isInExceptions ? "\u00A7c\u00A7lREMOVE" : "\u00A7a\u00A7lADD")
						: "")
				: "\u00A7c\u00A7lCAN'T BE ADDED";

		FBPGuiHelper._drawCenteredString(fontRendererObj, animationText1, animation.xPosition + 30,
				animation.yPosition + 65, 0);
		FBPGuiHelper._drawCenteredString(fontRendererObj, particleText1, particle.xPosition + 30,
				particle.yPosition + 65, 0);

		if (animation.isMouseOver())
			FBPGuiHelper._drawCenteredString(fontRendererObj, "\u00A7a\u00A7lPLACE ANIMATION", animation.xPosition + 30,
					animation.yPosition - 12, 0);
		if (particle.isMouseOver())
			FBPGuiHelper._drawCenteredString(fontRendererObj, "\u00A7a\u00A7lPARTICLES", particle.xPosition + 30,
					particle.yPosition - 12, 0);

		this.drawCenteredString(fontRendererObj, "\u00A7LAdd Block to Exceptions", width / 2, 20,
				fontRendererObj.getColorCode('a'));

		mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/widgets.png"));

		// RENDER SCREEN
		super.drawScreen(mouseX, mouseY, partialTicks);

		// RENDER MOUSE
		mc.getTextureManager().bindTexture(FBP.FBP_WIDGETS);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		GlStateManager.enableBlend();

		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

		GuiButton mouseOver = animation.isMouseOver() ? animation : (particle.isMouseOver() ? particle : null);

		int imageDiameter = 20;

		this.drawTexturedModalRect(mouseX - imageDiameter / 2, mouseY - imageDiameter / 2,
				mouseOver != null && !mouseOver.enabled ? 256 - imageDiameter * 2 : 256 - imageDiameter,
				256 - imageDiameter, imageDiameter, imageDiameter);
	}
}

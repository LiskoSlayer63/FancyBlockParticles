package com.TominoCZ.FBP;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;

import com.TominoCZ.FBP.block.FBPAnimationDummyBlock;
import com.TominoCZ.FBP.handler.FBPConfigHandler;
import com.TominoCZ.FBP.handler.FBPEventHandler;
import com.TominoCZ.FBP.handler.FBPKeyInputHandler;
import com.TominoCZ.FBP.handler.FBPRenderGuiHandler;
import com.TominoCZ.FBP.keys.FBPKeyBindings;
import com.google.common.base.Throwables;

import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mod(clientSideOnly = true, modid = FBP.MODID)
public class FBP {
	@Instance(FBP.MODID)
	public static FBP INSTANCE;

	protected final static String MODID = "fbp";

	public static final ResourceLocation LOCATION_PARTICLE_TEXTURE = new ResourceLocation("textures/particle/particles.png");
	public static final ResourceLocation RAIN_TEXTURES = new ResourceLocation("textures/environment/rain.png");
	public static final ResourceLocation SNOW_TEXTURES = new ResourceLocation("textures/environment/snow.png");
	
	public static File exceptionsFile = null;
	public static File config = null;

	public static int lastIDAdded;

	public static int minAge, maxAge;

	public static double scaleMult, gravityMult, rotationMult;

	public static boolean isServer = false;

	public static boolean enabled = true;
	public static boolean showInMillis = false;
	public static boolean infiniteDuration = false;
	public static boolean randomRotation = true, cartoonMode = false, spawnWhileFrozen = true,
			spawnRedstoneBlockParticles = false, smoothTransitions = true, randomFadingSpeed = true,
			entityCollision = false, bounceOffWalls = true, rollParticles = false, smartBreaking = true,
			fancyPlaceAnim = true, fancyWeather = true, fancyFlame = true, fancySmoke = true, frozen = false;

	public ConcurrentSet<String> blockExceptions = new ConcurrentSet<String>();
	public ConcurrentSet<String> defaultBlockExceptions = new ConcurrentSet<String>();

	public static ThreadLocalRandom random = ThreadLocalRandom.current();

	public static final Vec3d[] CUBE = { new Vec3d(-1, -1, 1), new Vec3d(-1, 1, 1), new Vec3d(1, 1, 1),
			new Vec3d(1, -1, 1),

			new Vec3d(1, -1, -1), new Vec3d(1, 1, -1), new Vec3d(-1, 1, -1), new Vec3d(-1, -1, -1),

			new Vec3d(-1, -1, -1), new Vec3d(-1, 1, -1), new Vec3d(-1, 1, 1), new Vec3d(-1, -1, 1),

			new Vec3d(1, -1, 1), new Vec3d(1, 1, 1), new Vec3d(1, 1, -1), new Vec3d(1, -1, -1),

			new Vec3d(1, 1, -1), new Vec3d(1, 1, 1), new Vec3d(-1, 1, 1), new Vec3d(-1, 1, -1),

			new Vec3d(-1, -1, -1), new Vec3d(-1, -1, 1), new Vec3d(1, -1, 1), new Vec3d(1, -1, -1) };

	public static MethodHandle setSourcePos;

	public static FBPAnimationDummyBlock FBPBlock = new FBPAnimationDummyBlock();

	public FBPEventHandler eventHandler = new FBPEventHandler();

	public FBP() {
		if (isDev())
			ReflectionHelper.setPrivateValue(Session.class, Minecraft.getMinecraft().getSession(), "ILikeMyMommy",
					"username", "field_74286_b");

		INSTANCE = this;

		defaultBlockExceptions.add(Blocks.AIR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.VINE.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.SKULL.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.BARRIER.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.STANDING_BANNER.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.COBBLESTONE_WALL.getRegistryName().toString());
		
		defaultBlockExceptions.add(Blocks.BED.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.CHEST.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.ENDER_CHEST.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.TRAPPED_CHEST.getRegistryName().toString());
		
		defaultBlockExceptions.add(Blocks.OAK_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.DARK_OAK_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.ACACIA_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.SPRUCE_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.JUNGLE_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.IRON_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.BIRCH_DOOR.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.STANDING_SIGN.getRegistryName().toString());
		defaultBlockExceptions.add(Blocks.WALL_SIGN.getRegistryName().toString());
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		if (evt.getSide().isServer())
			isServer = true;

		config = new File(evt.getModConfigurationDirectory() + "/FBP/Particle.properties");
		exceptionsFile = new File(evt.getModConfigurationDirectory() + "/FBP/AnimBlockExceptions.txt");

		FBPConfigHandler.init();

		FBPKeyBindings.init();

		FMLCommonHandler.instance().bus().register(new FBPKeyInputHandler());
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(eventHandler);
		FMLCommonHandler.instance().bus().register(eventHandler);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(new FBPRenderGuiHandler());

		MethodHandles.Lookup lookup = MethodHandles.publicLookup();

		GameRegistry.registerBlock(FBPBlock);

		try {
			setSourcePos = lookup
					.unreflectSetter(ReflectionHelper.findField(ParticleDigging.class, "field_181019_az", "sourcePos"));
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static boolean isEnabled() {
		boolean result = enabled;

		if (!result)
			frozen = false;

		return result;
	}

	public static boolean isDev() {
		return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
	}

	public boolean canBlockBeAnimated(Block b) {
		return !isInExceptions(b);
	}
	
	public boolean isInExceptions(Block b) {
		if (b == null)
			return true;
		
		return blockExceptions.contains(b.getRegistryName().toString());
	}
	
	public void addException(Block b) {
		if (b == null)
			return;
		
		String name = b.getRegistryName().toString();
		
		if (!blockExceptions.contains(name))
			blockExceptions.add(name);
	}
	
	public void removeException(Block b) {
		if (b == null)
			return;
		
		String name = b.getRegistryName().toString();
		
		if (blockExceptions.contains(name))
			blockExceptions.remove(name);
	}

	public void resetExceptions() {
		blockExceptions.clear();
		blockExceptions.addAll(defaultBlockExceptions);
	}

	public void addException(String name) {
		if (StringUtils.isEmpty(name))
			return;
		
		Iterator it = Block.REGISTRY.getKeys().iterator();
		
		while(it.hasNext())
		{
			ResourceLocation rl = ((ResourceLocation)it.next());
			String s = rl.toString();
			
			if (s.equals(name))
			{
				Block b = Block.REGISTRY.getObject(rl);
				addException(b);
				break;
			}
		}
	}
}
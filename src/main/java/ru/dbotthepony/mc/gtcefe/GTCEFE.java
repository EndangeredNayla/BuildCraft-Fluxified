
// Copyright (C) 2018 DBot

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all copies
// or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
// FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package ru.dbotthepony.mc.gtcefe;

import org.apache.logging.log4j.Logger;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ru.dbotthepony.mc.gtcefe.eu.EnergyProvider;
import ru.dbotthepony.mc.gtcefe.eu.EnergyProviderItem;

@Mod(
	modid = GTCEFE.MODID,
	name = GTCEFE.NAME,
	dependencies = "after:gregtech;",
	version = GTCEFE.VERSION)
public class GTCEFE {
	public static final String MODID = "gtcefe";
	public static final String NAME = "GregTechCE FE Adapter";
	public static final String VERSION = "1.1-1.8.5.436";
	public static final long RATIO = 4;
	public static final int RATIO_INT = 4;
	public ResourceLocation resourceLocation;
	public static Logger logger;

	public static final long MAX_VALUE_AS_LONG = Long.MAX_VALUE / RATIO;
	public static final long OVERFLOW_CHECK = Integer.MAX_VALUE / RATIO;
	public static final int MAX_VALUE_AS_INT = Integer.MAX_VALUE / RATIO_INT;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		resourceLocation = new ResourceLocation(GTCEFE.MODID, "fecapability");
		logger = event.getModLog();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (!Loader.isModLoaded("gregtech")) {
			return;
		}

		MinecraftForge.EVENT_BUS.register(this);
	}
	@SubscribeEvent
	public void attachTileCapability(AttachCapabilitiesEvent<TileEntity> event) {
		event.addCapability(resourceLocation, new EnergyProvider(event.getObject()));
	}

	@SubscribeEvent
	public void attachItemCapability(AttachCapabilitiesEvent<ItemStack> event) {
		event.addCapability(resourceLocation, new EnergyProviderItem(event.getObject()));
	}
}


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

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyProvider implements ICapabilityProvider {
	private final ICapabilityProvider upvalue;
	private IEnergyContainer cache;
	private EnergyContainerWrapper container;

	public EnergyProvider(ICapabilityProvider entCap) {
		upvalue = entCap;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityEnergy.ENERGY && upvalue.hasCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY) {
			IEnergyContainer getcontainer = upvalue.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);

			if (getcontainer == null) {
				return null;
			}

			if (cache != getcontainer) {
				cache = getcontainer;
				container = new EnergyContainerWrapper(getcontainer);
			}

			return (T) container;
		}

		return null;
	}

	class EnergyContainerWrapper implements IEnergyStorage {
		private IEnergyContainer container;

		public EnergyContainerWrapper(IEnergyContainer container) {
			this.container = container;
		}

		private int maxSpeedIn() {
			long result = container.getInputAmperage() * container.getInputVoltage() * GTCEFE.RATIO;

			if (result > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			return (int) result;
		}

		private int maxSpeedOut() {
			long result = container.getOutputAmperage() * container.getOutputVoltage() * GTCEFE.RATIO;

			if (result > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			return (int) result;
		}

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			if (!canReceive()) {
				return 0;
			}

			if (maxReceive < GTCEFE.RATIO * container.getInputVoltage()) {
				return 0;
			}

			int speed = maxSpeedIn();

			if (maxReceive > speed) {
				maxReceive = speed;
			}

			maxReceive = maxReceive - maxReceive % GTCEFE.RATIO_INT;

			long missing = container.getEnergyCanBeInserted() * GTCEFE.RATIO;

			if (missing <= 0L) {
				return 0;
			}

			if (missing < maxReceive) {
				maxReceive = (int) missing;
			}

			if (!simulate) {
				container.changeEnergy(maxReceive / GTCEFE.RATIO);
			}

			return maxReceive;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			if (!canExtract()) {
				return 0;
			}

			if (maxExtract < GTCEFE.RATIO * container.getOutputVoltage()) {
				return 0;
			}

			int speed = maxSpeedOut();

			if (maxExtract > speed) {
				maxExtract = speed;
			}

			maxExtract = maxExtract - maxExtract % GTCEFE.RATIO_INT;

			long stored = container.getEnergyStored() * GTCEFE.RATIO;

			if (stored <= 0L) {
				return 0;
			}

			if (stored < maxExtract) {
				maxExtract = (int) stored;
			}

			if (!simulate) {
				container.changeEnergy(-maxExtract / GTCEFE.RATIO);
			}

			return maxExtract;
		}

		@Override
		public int getEnergyStored() {
			long stored = container.getEnergyStored() * GTCEFE.RATIO;

			if (stored > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			return (int) stored;
		}

		@Override
		public int getMaxEnergyStored() {
			long maximal = container.getEnergyCapacity() * GTCEFE.RATIO;

			if (maximal > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			return (int) maximal;
		}

		@Override
		public boolean canExtract() {
			return container.outputsEnergy(null);
		}

		@Override
		public boolean canReceive() {
			return container.inputsEnergy(null);
		}
	}
}

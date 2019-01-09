
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

import javax.annotation.Nullable;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.common.pipelike.cable.tile.CableEnergyContainer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyProvider implements ICapabilityProvider {
	private final ICapabilityProvider upvalue;
	private final EnergyContainerWrapper[] facesRF = new EnergyContainerWrapper[7];
	private GregicEnergyContainerWrapper wrapper;
	private boolean gettingValue = false;

	public EnergyProvider(ICapabilityProvider entCap) {
		upvalue = entCap;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (gettingValue) {
			return false;
		}

		//if (capability != GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER) {
		if (capability != CapabilityEnergy.ENERGY && capability != GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER) {
			return false;
		}

		if (capability == CapabilityEnergy.ENERGY) {
			int faceID = facing == null ? 6 : facing.getIndex();

			if (facesRF[faceID] == null) {
				facesRF[faceID] = new EnergyContainerWrapper(upvalue.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, facing), facing);
			}

			gettingValue = true;
			boolean result = facesRF[faceID].isValid();
			gettingValue = false;
			return result;
		}

		if (wrapper == null) {
			wrapper = new GregicEnergyContainerWrapper(upvalue);
		}

		gettingValue = true;
		boolean result = wrapper.isValid(facing);
		gettingValue = false;

		return result;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (gettingValue) {
			return null;
		}

		if (!hasCapability(capability, facing)) {
			return null;
		}

		if (capability == CapabilityEnergy.ENERGY) {
			int faceID = facing == null ? 6 : facing.getIndex();

			if (facesRF[faceID] == null) {
				facesRF[faceID] = new EnergyContainerWrapper(upvalue.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, facing), facing);
			}

			gettingValue = true;

			if (facesRF[faceID].isValid()) {
				gettingValue = false;
				return (T) facesRF[faceID];
			}

			gettingValue = false;

			return null;
		}

		if (wrapper == null) {
			wrapper = new GregicEnergyContainerWrapper(upvalue);
		}

		gettingValue = true;

		if (wrapper.isValid(facing)) {
			gettingValue = false;
			return (T) wrapper;
		}

		gettingValue = false;

		return null;
	}

	class GregicEnergyContainerWrapper implements IEnergyContainer {
		private final ICapabilityProvider upvalue;
		private final IEnergyStorage[] facesRF = new IEnergyStorage[7];

		public GregicEnergyContainerWrapper(ICapabilityProvider upvalue) {
			this.upvalue = upvalue;
		}

		boolean isValid(EnumFacing face) {
			if (upvalue.hasCapability(CapabilityEnergy.ENERGY, face)) {
				return true;
			}

			if (face == null) {
				for (EnumFacing face2 : EnumFacing.VALUES) {
					if (upvalue.hasCapability(CapabilityEnergy.ENERGY, face2)) {
						return true;
					}
				}
			}

			return false;
		}

		private IEnergyStorage getStorageCap() {
			IEnergyStorage container = def();

			if (container != null && container.getMaxEnergyStored() > 0) {
				return container;
			}

			for (EnumFacing face : EnumFacing.VALUES) {
				container = facesRF[face.getIndex()];

				if (container == null) {
					container = upvalue.getCapability(CapabilityEnergy.ENERGY, face);
					facesRF[face.getIndex()] = container;
				}

				if (container != null && container.getMaxEnergyStored() > 0) {
					return container;
				}
			}

			return container;
		}

		private IEnergyStorage getAcceptionCap() {
			IEnergyStorage container = def();

			if (container != null && container.receiveEnergy(Integer.MAX_VALUE, true) > 0) {
				return container;
			}

			for (EnumFacing face : EnumFacing.VALUES) {
				container = facesRF[face.getIndex()];

				if (container == null) {
					container = upvalue.getCapability(CapabilityEnergy.ENERGY, face);
					facesRF[face.getIndex()] = container;
				}

				if (container != null && container.receiveEnergy(Integer.MAX_VALUE, true) > 0) {
					return container;
				}
			}

			return container;
		}

		@Override
		public long acceptEnergyFromNetwork(EnumFacing facing, long voltage, long amperage) {
			int faceID = facing == null ? 6 : facing.getIndex();
			IEnergyStorage container = facesRF[faceID];

			if (container == null) {
				container = upvalue.getCapability(CapabilityEnergy.ENERGY, facing);
				facesRF[faceID] = container;
			}

			if (container == null) {
				return 0L;
			}

			long maximalValue = voltage * amperage * GTCEFE.RATIO;

			if (maximalValue > Integer.MAX_VALUE) {
				maximalValue = Integer.MAX_VALUE;
			}

			int receive = container.receiveEnergy((int) maximalValue, true);
			receive -= receive % (voltage * GTCEFE.RATIO);

			if (receive == 0) {
				return 0L;
			}

			return container.receiveEnergy(receive, false) / (voltage * GTCEFE.RATIO);
		}

		@Override
		public long changeEnergy(long delta) {
			IEnergyStorage container = getStorageCap();

			if (container == null) {
				return 0L;
			}

			if (delta == 0L) {
				return 0L;
			}

			if (delta < 0L) {
				long extractValue = delta * GTCEFE.RATIO;

				if (extractValue > Integer.MAX_VALUE) {
					extractValue = Integer.MAX_VALUE;
				}

				int extract = container.extractEnergy((int) extractValue, true);
				extract -= extract % GTCEFE.RATIO_INT;
				return container.extractEnergy(extract, false) / GTCEFE.RATIO;
			}

			long receiveValue = delta * GTCEFE.RATIO;

			if (receiveValue > Integer.MAX_VALUE) {
				receiveValue = Integer.MAX_VALUE;
			}

			int receive = container.receiveEnergy((int) receiveValue, true);
			receive -= receive % GTCEFE.RATIO_INT;
			return container.receiveEnergy(receive, false) / GTCEFE.RATIO;
		}

		@Nullable
		private IEnergyStorage def() {
			if (facesRF[6] == null) {
				facesRF[6] = upvalue.getCapability(CapabilityEnergy.ENERGY, null);
			}

			return facesRF[6];
		}

		@Override
		public long getEnergyCapacity() {
			IEnergyStorage cap = getStorageCap();

			if (cap == null) {
				return 0L;
			}

			int value = cap.getMaxEnergyStored();
			value -= value % GTCEFE.RATIO_INT;
			return value / GTCEFE.RATIO_INT;
		}

		@Override
		public long getEnergyStored() {
			IEnergyStorage cap = getStorageCap();

			if (cap == null) {
				return 0L;
			}

			int value = cap.getEnergyStored();
			value -= value % GTCEFE.RATIO_INT;
			return value / GTCEFE.RATIO_INT;
		}

		@Override
		public long getInputAmperage() {
			IEnergyStorage container = getAcceptionCap();

			if (container == null) {
				return 0L;
			}

			long voltage = getInputVoltage();

			if (voltage == GTValues.V[GTValues.V.length]) {
				return 1L;
			}

			for (int index = 0; index < GTValues.V.length; index++) {
				if (GTValues.V[index] == voltage) {
					long voltageNext = GTValues.V[index + 1] * GTCEFE.RATIO;

					if (voltageNext > Integer.MAX_VALUE) {
						voltageNext = Integer.MAX_VALUE;
					}

					int allowedInput = container.receiveEnergy((int) voltageNext, true);

					if (allowedInput < voltage * GTCEFE.RATIO) {
						return 1L;
					}

					allowedInput -= allowedInput % voltage * GTCEFE.RATIO;
					return allowedInput / (voltage * GTCEFE.RATIO);
				}
			}

			return 1L;
		}

		@Override
		public long getInputVoltage() {
			IEnergyStorage container = getStorageCap();

			if (container == null) {
				return 0L;
			}

			long grabMaxInput = container.receiveEnergy(Integer.MAX_VALUE, true);
			grabMaxInput -= grabMaxInput % GTCEFE.RATIO;

			if (grabMaxInput == 0) {
				return 0L;
			}

			grabMaxInput /= GTCEFE.RATIO;

			long value = GTValues.V[0];

			if (grabMaxInput < value) {
				return 0L;
			}

			for (long value2 : GTValues.V) {
				if (value2 < grabMaxInput) {
					break;
				} else {
					value = value2;
				}
			}

			return value;
		}

		@Override
		public boolean inputsEnergy(EnumFacing facing) {
			int faceID = facing == null ? 6 : facing.getIndex();
			IEnergyStorage container = facesRF[faceID];

			if (container == null) {
				container = upvalue.getCapability(CapabilityEnergy.ENERGY, facing);
				facesRF[faceID] = container;
			}

			if (container == null) {
				return false;
			}

			return container.canReceive();
		}

		@Override
		public boolean outputsEnergy(EnumFacing arg0) {
			// return container.canExtract();
			// we just want to receive energy from ENet without hacks
			// FE based blocks will push energy on it's own to us using EnergyContainerWrapper.
			return false;
		}
	}

	class EnergyContainerWrapper implements IEnergyStorage {
		private final IEnergyContainer container;
		private EnumFacing facing = null;

		public EnergyContainerWrapper(IEnergyContainer container, EnumFacing facing) {
			this.container = container;
			this.facing = facing;
		}

		boolean isValid() {
			return container != null && !(container instanceof GregicEnergyContainerWrapper);
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

		private int voltageIn() {
			long result = container.getInputVoltage() * GTCEFE.RATIO;

			if (result > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			return (int) result;
		}

		private int voltageOut() {
			long result = container.getOutputVoltage() * GTCEFE.RATIO;

			if (result > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}

			return (int) result;
		}

		// eNet in gregtech is private
		// im unable to workaround cable burning.
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			if (!canReceive()) {
				return 0;
			}

			int speed = maxSpeedIn();

			if (maxReceive > speed) {
				maxReceive = speed;
			}

			maxReceive -= maxReceive % GTCEFE.RATIO_INT;
			maxReceive -= maxReceive % voltageIn();

			if (maxReceive <= 0 || maxReceive < voltageIn()) {
				return 0;
			}

			long missing = container.getEnergyCanBeInserted() * GTCEFE.RATIO;

			if (missing <= 0L || missing < voltageIn()) {
				return 0;
			}

			if (missing < maxReceive) {
				maxReceive = (int) missing;
			}

			if (!simulate) {
				int ampers = (int) container.acceptEnergyFromNetwork(this.facing, container.getInputVoltage(), maxReceive / (GTCEFE.RATIO * container.getInputVoltage()));
				return ampers * voltageIn();
			}

			return maxReceive;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			if (!canExtract()) {
				return 0;
			}

			int speed = maxSpeedOut();

			if (maxExtract > speed) {
				maxExtract = speed;
			}

			maxExtract -= maxExtract % GTCEFE.RATIO_INT;
			maxExtract -= maxExtract % voltageOut();

			if (maxExtract <= 0) {
				return 0;
			}

			long stored = container.getEnergyStored() * GTCEFE.RATIO;

			if (stored <= 0L) {
				return 0;
			}

			if (stored < maxExtract) {
				maxExtract = (int) stored;
			}

			//GTCEFE.logger.info(maxExtract);

			if (!simulate) {
				return (int) (container.removeEnergy(maxExtract / GTCEFE.RATIO) * GTCEFE.RATIO);
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
			if (container instanceof CableEnergyContainer) {
				return false;
			}

			return container.outputsEnergy(this.facing);
		}

		@Override
		public boolean canReceive() {
			return container.inputsEnergy(this.facing);
		}
	}
}

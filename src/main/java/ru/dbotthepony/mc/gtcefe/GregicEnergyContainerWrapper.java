
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
import gregtech.api.capability.IEnergyContainer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class GregicEnergyContainerWrapper implements IEnergyContainer {
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

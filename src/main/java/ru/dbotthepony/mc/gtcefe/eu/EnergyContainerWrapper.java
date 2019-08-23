
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

package ru.dbotthepony.mc.gtcefe.eu;
import ru.dbotthepony.mc.gtcefe.GTCEFE;

import gregtech.api.capability.IEnergyContainer;
import gregtech.common.pipelike.cable.tile.CableEnergyContainer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyContainerWrapper implements IEnergyStorage {
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

		if (maxReceive == 1 && simulate) {
			// assuming we hit mekanism
			return container.getEnergyCanBeInserted() > 0L ? 1 : 0;
		}

		int speed = maxSpeedIn();

		if (maxReceive > speed) {
			maxReceive = speed;
		}

		int voltageIn = voltageIn();

		maxReceive -= maxReceive % GTCEFE.RATIO_INT;
		maxReceive -= maxReceive % voltageIn;

		if (maxReceive <= 0 || maxReceive < voltageIn) {
			return 0;
		}

		long missing = container.getEnergyCanBeInserted();

		if (missing <= 0L || missing < voltageIn) {
			return 0;
		}

		if (missing >= GTCEFE.MAX_VALUE_AS_LONG) {
			missing = GTCEFE.MAX_VALUE_AS_LONG;
		}

		missing *= GTCEFE.RATIO;

		if (missing < maxReceive) {
			maxReceive = (int) missing;
		}

		if (!simulate) {
			long voltage = container.getInputVoltage();
			int ampers = (int) container.acceptEnergyFromNetwork(this.facing, voltage, maxReceive / (GTCEFE.RATIO * voltage));
			return ampers * voltageIn;
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

		long stored = container.getEnergyStored();

		if (stored <= 0L) {
			return 0;
		}

		if (stored >= GTCEFE.MAX_VALUE_AS_LONG) {
			stored = GTCEFE.MAX_VALUE_AS_LONG;
		}

		stored *= GTCEFE.RATIO;

		if (stored < maxExtract) {
			maxExtract = (int) stored;
		}

		if (!simulate) {
			return (int) (container.removeEnergy(maxExtract / GTCEFE.RATIO) * GTCEFE.RATIO);
		}

		return maxExtract;
	}

	@Override
	public int getEnergyStored() {
		long value = container.getEnergyStored();

		if (value >= GTCEFE.MAX_VALUE_AS_LONG || value > GTCEFE.OVERFLOW_CHECK) {
			return Integer.MAX_VALUE;
		}

		return (int) (value * GTCEFE.RATIO);
	}

	@Override
	public int getMaxEnergyStored() {
		long value = container.getEnergyCapacity();

		if (value >= GTCEFE.MAX_VALUE_AS_LONG || value > GTCEFE.OVERFLOW_CHECK) {
			return Integer.MAX_VALUE;
		}

		return (int) (value * GTCEFE.RATIO);
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

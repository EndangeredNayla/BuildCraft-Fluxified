
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

package ru.dbotthepony.mc.gtcefe.mj;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjPassiveProvider;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.IEnergyStorage;
import ru.dbotthepony.mc.gtcefe.BCFE;

class EnergyContainerMJ implements IEnergyStorage {
	protected final TileEntity upvalue;
	EnumFacing face = EnumFacing.SOUTH;

	private EnumFacing lastFace;
	protected IMjConnector connector;
	protected IMjReadable read;
	protected IMjReceiver receiver;
	protected IMjPassiveProvider passive;

	public EnergyContainerMJ(TileEntity upvalue) {
		this.upvalue = upvalue;
	}

	EnergyContainerMJ face(EnumFacing face) {
		this.face = face;
		return this;
	}

	boolean isValid() {
		return this.upvalue.hasCapability(MjAPI.CAP_RECEIVER, this.face);
	}

	EnergyContainerMJ updateValues() {
		if (lastFace == face) {
			return this;
		}

		lastFace = face;
		connector = this.upvalue.getCapability(MjAPI.CAP_CONNECTOR, this.face);
		read = this.upvalue.getCapability(MjAPI.CAP_READABLE, this.face);
		receiver = this.upvalue.getCapability(MjAPI.CAP_RECEIVER, this.face);
		passive = this.upvalue.getCapability(MjAPI.CAP_PASSIVE_PROVIDER, this.face);

		return this;
	}

	int antiOverflow(long value) {
		if (value > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return (int) value;
	}

	int toRF(long microJoules) {
		long ratio = BCFE.conversionRatio();

		if (microJoules < ratio) {
			return 0;
		}

		microJoules -= microJoules % ratio;
		microJoules /= ratio;

		return antiOverflow(microJoules);
	}

	long fromRF(int rf) {
		return rf * BCFE.conversionRatio();
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		if (!canReceive()) {
			return 0;
		}

		maxReceive = Math.min(calcMaxReceive(), maxReceive);

		long value = fromRF(maxReceive);
		long simulated = receiver.receivePower(value, true);

		if (simulated == 0L) {
			if (!simulate) {
				receiver.receivePower(value, false);
			}

			return maxReceive;
		}

		value -= simulated;

		long ratio = BCFE.conversionRatio();

		if (value % ratio != 0) {
			value -= value % ratio;
		}

		simulated = receiver.receivePower(value, true);

		if (simulated % ratio != 0) {
			return 0;
		}

		if (!simulate) {
			return toRF(value - receiver.receivePower(value, false));
		}

		return toRF(value - simulated);
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		if (!canExtract()) {
			return 0;
		}

		long value = fromRF(maxExtract);
		long simulated = passive.extractPower(BCFE.conversionRatio(), value, true);

		simulated -= simulated % BCFE.conversionRatio();

		if (simulated == 0L) {
			return 0;
		}

		if (!simulate) {
			return toRF(passive.extractPower(BCFE.conversionRatio(), simulated, true));
		}

		return toRF(simulated);
	}

	int calcMaxReceive() {
		if (read == null) {
			return Integer.MAX_VALUE;
		}

		return getMaxEnergyStored() - getEnergyStored();
	}

	@Override
	public int getEnergyStored() {
		return read != null ? toRF(read.getStored()) : 0;
	}

	@Override
	public int getMaxEnergyStored() {
		return read != null ? toRF(read.getCapacity()) : 0;
	}

	@Override
	public boolean canExtract() {
		return passive != null;
	}

	@Override
	public boolean canReceive() {
		return receiver != null && receiver.canReceive();
	}
}

package custom

import freechips.rocketchip.config.Config
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.devices.tilelink.{BootROMLocated, BootROMParams}

import sys.process._
import freechips.rocketchip.devices.tilelink.BootROMParams

class WithCustomMemPort(base: BigInt, size: BigInt) extends Config((site, here, up) => {
  case ExtMem => Some(MemoryPortParams(MasterPortParams(
                      base = base,
                      size = size,
                      beatBytes = site(MemoryBusKey).beatBytes,
                      idBits = 1), 1))
})

class WithCustomMMIOPort(base: BigInt, size: BigInt) extends Config((site, here, up) => {
  case ExtBus => Some(MasterPortParams(
                      base = base,
                      size = size,
                      beatBytes = site(MemoryBusKey).beatBytes,
                      idBits = 1))
})

class WithCustomSlavePort extends Config((site, here, up) => {
  case ExtIn  => Some(SlavePortParams(beatBytes = 8, idBits = 2, sourceBits = 2))
})

class WithCustomBootROM extends Config((site, here, up) => {
  case BootROMLocated(x) => {
    // invoke makefile for sdboot
    val make = s"make -C ./custom/bootrom"
    require (make.! == 0, "Failed to build bootrom")
    Some(BootROMParams(
        address = 0x10000,
        size = 0x10000,
        hang = 0x10000,
        contentFileName = s"./custom/bootrom/bootrom.img"
    ))
  }
})

class BaseRocketConfig extends Config(
  new WithCustomMemPort(x"8000_0000", x"1000_0000") ++
  new WithCustomMMIOPort(x"6000_0000", x"2000_0000") ++
  new WithCustomSlavePort ++
  new WithCustomBootROM ++
  new WithTimebase(BigInt(1000000)) ++ // 1 MHz
  new WithDTS("freechips,rocketchip-unknown", Nil) ++
  new WithNExtTopInterrupts(0) ++
  new BaseSubsystemConfig
)

class DefaultRocketConfig extends Config(new WithNBigCores(1) ++ new WithCoherentBusTopology ++ new BaseRocketConfig)

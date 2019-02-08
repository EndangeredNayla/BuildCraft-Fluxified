
# Minecraft mod

This simple mod adds native duplex support of Forge Energy to

Gregtech Community Edition machinery and FE->EU support for EU based items (at ratio of 1 EU to 4 FE)

and BuildCraft machinery (Minecraft Joules) (at ratio of 1 MJ to 40 FE)

# Manual Compiling

To manually compile the mod you need to (assuming you can already straightforward compile mods using ./gradlew build)
download latest .jar file of the gregtech community edition (or any working version of gregtech community edition),
put it in repository folder and rename it to `gtce.jar`; launch `git submodules init; git submodules update` and run `./gradlew build`

This is required since i don't repack gregtech community edition's APIs

Or grab [precompiled for Minecraft 1.12.2 version](https://i.dbotthepony.ru/2019/02/gtcefe-1.0.jar)

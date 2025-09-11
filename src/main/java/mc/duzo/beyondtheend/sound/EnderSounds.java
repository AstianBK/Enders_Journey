package mc.duzo.beyondtheend.sound;

import mc.duzo.beyondtheend.EndersJourney;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EnderSounds {
	public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EndersJourney.MODID);

	public static RegistryObject<SoundEvent> REALM_SONG = register("realm_song");

	private static RegistryObject<SoundEvent> register(String name) {
		ResourceLocation id = new ResourceLocation(EndersJourney.MODID, name);
		return REGISTER.register(name, () -> new SoundEvent(id));
	}

	public static void register(IEventBus bus) {
		REGISTER.register(bus);
	}
}

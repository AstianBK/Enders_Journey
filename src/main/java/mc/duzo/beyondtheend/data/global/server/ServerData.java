package mc.duzo.beyondtheend.data.global.server;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.realm.RealmManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Data that will be saved to the world in .nbt form
 * For saving across server restarts.
 * Remember to call markDirty() after setting a value to ensure it saves
 *
 * @author duzo
 */
public class ServerData extends SavedData {
	private RealmManager realmManager;
	public RealmManager getRealmManager() {
		if (this.realmManager == null) {
			EndersJourney.LOGGER.warn("Missing realm manager! Creating..");
			this.realmManager = new RealmManager();
		}

		return this.realmManager;
	}

	public static ServerData get() {
		DimensionDataStorage manager = EndersJourney.getServer().getLevel(Level.OVERWORLD).getDataStorage();

		ServerData state = manager.computeIfAbsent(
				ServerData::load,
				ServerData::new,
				EndersJourney.MODID
		);

		state.setDirty(); // bad code

		return state;
	}

	@Override
	public CompoundTag save(CompoundTag data) {
		data.put("RealmManager",this.getRealmManager().serialise());
		return data;
	}
	public static ServerData load(CompoundTag data) {
		ServerData created = new ServerData();
		CompoundTag tag=data.getCompound("RealmManager");
		created.realmManager = new RealmManager(tag);
		return created;
	}

}

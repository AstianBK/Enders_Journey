package mc.duzo.ender_journey.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Se dispara justo después de que un jugador complete uno de los 24
 * advancements de Ender Eye ({@code endrem:main/*_eye}), con los hechos
 * concretos que ocurrieron en ese desbloqueo (corazón extra, subida de
 * nivel del storage, apertura de portal).
 * <p>
 * Pensado para que otros mods (p. ej. EnderEyesGUI) puedan construir un
 * resumen/overlay sin duplicar la lógica de {@code Events#onAdvancementEarn}.
 */
public class EnderEyeMilestoneEvent extends Event {

	public enum PortalOpened { NONE, NETHER, END, FINAL }

	private final ServerPlayer player;
	private final ResourceLocation advancementId;
	private final int eyesEarned;
	private final int heartsGiven;
	private final boolean storageUpgraded;
	private final PortalOpened portalOpened;
	private final int chunksAdded;
	private final int chunksTotal;

	public EnderEyeMilestoneEvent(ServerPlayer player, ResourceLocation advancementId, int eyesEarned,
	                               int heartsGiven, boolean storageUpgraded, PortalOpened portalOpened,
	                               int chunksAdded, int chunksTotal) {
		this.player = player;
		this.advancementId = advancementId;
		this.eyesEarned = eyesEarned;
		this.heartsGiven = heartsGiven;
		this.storageUpgraded = storageUpgraded;
		this.portalOpened = portalOpened;
		this.chunksAdded = chunksAdded;
		this.chunksTotal = chunksTotal;
	}

	public ServerPlayer getPlayer() { return player; }
	public ResourceLocation getAdvancementId() { return advancementId; }
	public int getEyesEarned() { return eyesEarned; }
	public int getHeartsGiven() { return heartsGiven; }
	public boolean isStorageUpgraded() { return storageUpgraded; }
	public PortalOpened getPortalOpened() { return portalOpened; }
	public int getChunksAdded() { return chunksAdded; }
	public int getChunksTotal() { return chunksTotal; }
}

package mc.duzo.beyondtheend.world.dimension;

import com.mojang.math.Vector3d;
import mc.duzo.beyondtheend.EndersJourney;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class EnderDimensions {
	public static final ResourceKey<Level> REALM_KEY = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(EndersJourney.MODID, "the_forgotten_realm"));
	public static final ResourceKey<DimensionType> REALM_TYPE = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, REALM_KEY.registry());

	public static void initialise() {
		// nothing
	}
	public static boolean isInDimension(LivingEntity entity, ResourceKey<Level> key) {
		return key.equals(entity.getLevel().dimension());
	}


	public static void teleport(LivingEntity entity, ServerLevel target, Vector3d pos, float yaw, float pitch) {
		if (entity instanceof ServerPlayer player) {
			target.getServer().execute(() -> teleportToWorld(player, target, pos, player.getYHeadRot(), player.getXRot()));
			return;
		}

		if (entity.getLevel().dimension().equals(target.dimension())) {
			entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
			return;
		}

		entity.changeDimension(target);
		entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
	}

	private static void teleportToWorld(ServerPlayer player, ServerLevel target, Vector3d pos, float yaw, float pitch) {
		player.teleportTo(target, pos.x, pos.y, pos.z, yaw, pitch);
		player.giveExperiencePoints(0);

		player.getActiveEffects().forEach(effect -> {
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
		});
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}
}

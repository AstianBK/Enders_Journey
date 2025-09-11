package mc.duzo.beyondtheend.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

public interface PortalPlayer extends INBTSerializable<CompoundTag> {

    Player getPlayer();
    int getEyesEarn();
    void setEyesEarn(int value);
    void plusEye(ResourceLocation location);
    void addEye(ResourceLocation location);

    static LazyOptional<PortalPlayer> get(Player player) {
        return player.getCapability(BkCapabilities.PORTAL_PLAYER_CAPABILITY);
    }
    void setListEye(List<ResourceLocation> list);
    List<ResourceLocation> getList();

    public void setPlayer(Player player);

    void onLogout();
    void onLogin();

    void onJoinLevel();

    void copyFrom(PortalPlayer capability,Player player,Player newPlayer);

    void onUpdate();

    void setCanSpawnInAether(boolean canSpawnInAether);
    boolean canSpawnInAether();

    void givePortalItem();
    void setCanGetPortal(boolean canGetPortal);
    boolean canGetPortal();

    void setInPortal(boolean inPortal);
    boolean isInPortal();

    void setPortalTimer(int timer);
    int getPortalTimer();

    float getPortalAnimTime();
    float getPrevPortalAnimTime();

    void setHitting(boolean isHitting);
    boolean isHitting();

    void setMoving(boolean isMoving);
    boolean isMoving();

    void setJumping(boolean isJumping);
    boolean isJumping();
    boolean haveEye(ResourceLocation location);

    int checkEyes(Level level);
}

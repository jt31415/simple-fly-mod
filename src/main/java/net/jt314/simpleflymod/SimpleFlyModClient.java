package net.jt314.simpleflymod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class SimpleFlyModClient implements ClientModInitializer {

    private static final double MINFALL = 0.04;
    private static final int MAXFLOATINGTICKS = 20;
    private int ticksFloating = 0;
    private double oldY = 0.0;

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

    }

    public void sendPosition(MinecraftClient client, Vec3d pos) {
        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false);
        client.player.networkHandler.sendPacket(packet);
    }

    public void tick(MinecraftClient client) {

        if (client.player == null) return;

        Vec3d pos = client.player.getPos();

        if (pos.getY() < oldY - MINFALL) {
            ticksFloating = 0;
        } else {
            ticksFloating++;
        }

        oldY = pos.getY();

        client.player.getAbilities().allowFlying = true;

        Vec3d blockUnder = pos.subtract(0.0,MINFALL,0.0);
        boolean blockUnderIsAir;
        blockUnderIsAir = client.player.getWorld().getBlockState(
                new BlockPos(new Vec3i((int)blockUnder.x,(int)blockUnder.y,(int)blockUnder.z))).isAir();

        if (ticksFloating >= MAXFLOATINGTICKS && blockUnderIsAir) {
            sendPosition(client, blockUnder);
            ticksFloating = 0;
        }

    }
}

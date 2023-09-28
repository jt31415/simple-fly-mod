package net.jt314.simpleflymod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.glfw.GLFW;

public class SimpleFlyModClient implements ClientModInitializer {

    private static final double MINFALL = 0.04;
    private static final int MAXFLOATINGTICKS = 20;
    private int ticksFloating = 0;
    private double oldY = 0.0;
    private boolean toggled = true;

    private KeyBinding key = new KeyBinding("key.fly.flying", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.fly.default");

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            drawContext.drawText(renderer, "Flying: " + (this.toggled ? "§2enabled" : "§Cdisabled"), 2, 2, 0xffffff, true);
        });

        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager
                    .literal("fly")
                    .executes(context -> {
                        onToggle(MinecraftClient.getInstance());
                        context.getSource().sendFeedback(Text.of("Flying toggled"));
                        return 1;
            }));
        }));

    }

    public void sendPosition(MinecraftClient client, Vec3d pos) {
        PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, false);
        client.player.networkHandler.sendPacket(packet);
    }

    public void onToggle(MinecraftClient client) {
        toggled = !toggled;
        boolean inFlyMode = client.player.isCreative() || client.player.isSpectator();
        client.player.getAbilities().allowFlying = inFlyMode;
        client.player.getAbilities().flying = inFlyMode && !client.player.isOnGround();
    }

    public void tick(MinecraftClient client) {

        if (key.wasPressed()) onToggle(client);

        if (client.player == null) return;
        if (!toggled) return;

        client.player.getAbilities().allowFlying = true;

        Vec3d pos = client.player.getPos();

        if (pos.getY() < oldY - MINFALL) {
            ticksFloating = 0;
        } else {
            ticksFloating++;
        }

        oldY = pos.getY();

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

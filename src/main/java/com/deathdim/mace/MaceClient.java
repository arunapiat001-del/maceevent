package com.deathdim.mace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

public class MaceClient implements ClientModInitializer {
    public static final double RADIUS = 3.8;   // jarak kamera dari pemain
    public static final double HEIGHT = -0.9;  // negatif = kamera rendah, menatap ke atas
    private static final double SPEED = 0.55;  // kecepatan putar kamera (radian/detik)

    private static final long RAISE_MS = 700, CAM_IN_MS = 700, LOWER_MS = 700, CAM_OUT_MS = 1000;

    private static boolean active = false, angleInit = false;
    private static long startMs = 0, durMs = 1;
    private static double angle0 = 0;
    private static Perspective previous = Perspective.FIRST_PERSON;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(MacePayload.ID, MacePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MacePayload.ID, (p, ctx) -> start(p.ticks()));
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> reset());

        // Test tanpa plugin: /macetest
        ClientCommandRegistrationCallback.EVENT.register((d, r) ->
                d.register(ClientCommandManager.literal("macetest").executes(c -> {
                    start(100);
                    return 1;
                })));
    }

    public static void start(int ticks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!active) {
            previous = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
        active = true;
        angleInit = false;
        startMs = System.currentTimeMillis();
        durMs = Math.max(2000L, ticks * 50L);
    }

    public static void reset() {
        if (active) MinecraftClient.getInstance().options.setPerspective(previous);
        active = false;
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** Waktu berjalan (ms), atau -1 kalau tidak aktif. Otomatis selesai saat durasi habis. */
    private static long elapsed() {
        if (!active) return -1;
        long t = System.currentTimeMillis() - startMs;
        if (t >= durMs) {
            reset();
            return -1;
        }
        return t;
    }

    /** 0 = lengan normal, 1 = lengan terangkat penuh. */
    public static float armWeight() {
        long t = elapsed();
        if (t < 0) return 0f;
        return Math.min(smooth(t / (float) RAISE_MS), smooth((durMs - t) / (float) LOWER_MS));
    }

    /** 0 = kamera normal, 1 = kamera sinematik penuh. */
    public static float camWeight() {
        long t = elapsed();
        if (t < 0) return 0f;
        return Math.min(smooth(t / (float) CAM_IN_MS), smooth((durMs - t) / (float) CAM_OUT_MS));
    }

    /** Titik awal orbit diambil dari posisi kamera normal (di belakang pemain). */
    public static void initAngle(double dx, double dz) {
        if (angleInit || dx * dx + dz * dz < 1.0) return;
        angle0 = Math.atan2(dz, dx);
        angleInit = true;
    }

    public static double angle() {
        return angle0 + (System.currentTimeMillis() - startMs) / 1000.0 * SPEED;
    }

    private static long lastLog = 0;

    public static void debug(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastLog < 1000) return;
        lastLog = now;
        System.out.println("[maceevent] " + msg);
    }

    /** Dipanggil dari mixin model: mengangkat lengan yang memegang mace. */
    public static void applyPose(BipedEntityModel<?> m, BipedEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState ps)) return;
        float w = armWeight();
        if (w <= 0f) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        double dx = ps.x - mc.player.getX();
        double dy = ps.y - mc.player.getY();
        double dz = ps.z - mc.player.getZ();
        boolean mine = ps.id == mc.player.getId() || dx * dx + dy * dy + dz * dz < 1.0;
        debug("applyPose id=" + ps.id + " idSaya=" + mc.player.getId() + " cocok=" + mine + " w=" + w);
        if (!mine) return;

        boolean right = ps.mainArm == Arm.RIGHT;
        ModelPart arm = right ? m.rightArm : m.leftArm;
        arm.pitch = MathHelper.lerp(w, arm.pitch, -2.9f);            // lengan lurus ke atas
        arm.yaw = MathHelper.lerp(w, arm.yaw, 0f);
        arm.roll = MathHelper.lerp(w, arm.roll, right ? -0.15f : 0.15f);
        m.head.pitch = MathHelper.lerp(w, m.head.pitch, -0.45f);     // kepala menengadah
    }

    /** Mengubah pose lengan di data render sebelum model menghitung sudutnya (seperti melempar trident). */
    public static void applyState(BipedEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState ps)) return;
        if (armWeight() < 0.5f) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || ps.id != mc.player.getId()) return;
        BipedEntityModel.ArmPose raised = BipedEntityModel.ArmPose.values()[4]; // pose lempar trident
        if (ps.mainArm == Arm.RIGHT) ps.rightArmPose = raised;
        else ps.leftArmPose = raised;
    }
}

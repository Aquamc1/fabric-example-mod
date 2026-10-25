package com.example.chestesp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.AbstractChestBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * PojavLauncher (Fabric 26.1.1) Compatible Wide-Range Chest ESP Mod
 * Renders color-coded wireframe bounding boxes around containers wall-hack style.
 * Fully documented in English.
 */
public class ChestESPMod implements ClientModInitializer {

    // Range in blocks for scanning (64 blocks covers a huge area around the player)
    private static final double MAX_SCAN_RANGE_SQ = 64.0 * 64.0;

    @Override
    public void onInitializeClient() {
        // Register the world render event (Runs every frame after translucent elements are rendered)
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            MatrixStack matrices = context.matrixStack();
            Vec3d cameraPos = context.camera().getPos();
            Vec3d playerPos = client.player.getPos();

            // Disable depth test to draw lines through walls/terrain
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            // 1. SCAN REGULAR BLOCK ENTITIES (Chests, Ender Chests, Barrels, Shulker Boxes)
            for (BlockEntity blockEntity : client.world.blockEntities) {
                BlockPos pos = blockEntity.getPos();

                // Skip container scanning if it's beyond the 64-block range threshold
                if (pos.getSquaredDistance(playerPos.x, playerPos.y, playerPos.z) > MAX_SCAN_RANGE_SQ) {
                    continue;
                }

                // Check container types and assign color scheme (RGBA)
                float red = 1.0f, green = 1.0f, blue = 1.0f, alpha = 0.7f;
                boolean isContainer = false;

                if (blockEntity instanceof AbstractChestBlockEntity) {
                    // Normal / Trapped Chests: Bright Orange/Gold
                    red = 1.0f; green = 0.65f; blue = 0.0f;
                    isContainer = true;
                } else if (blockEntity instanceof EnderChestBlockEntity) {
                    // Ender Chests: Magenta / Purple
                    red = 0.8f; green = 0.1f; blue = 1.0f;
                    isContainer = true;
                } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
                    // Shulker Boxes: Neon Cyan
                    red = 0.0f; green = 0.9f; blue = 1.0f;
                    isContainer = true;
                } else if (blockEntity instanceof BarrelBlockEntity) {
                    // Barrels: Lime Green
                    red = 0.2f; green = 1.0f; blue = 0.2f;
                    isContainer = true;
                }

                if (isContainer) {
                    matrices.push();
                    // Offset model matrices to camera coordinates
                    matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();

                    // Render Outlined Box
                    renderBoundingBox(buffer, tessellator, matrix, 0, 0, 0, 1, 1, 1, red, green, blue, alpha);
                    matrices.pop();
                }
            }

            // 2. SCAN ENTITIES IN WORLD (Chest Minecarts)
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity instanceof ChestMinecartEntity minecart) {
                    if (minecart.squaredDistanceTo(playerPos) <= MAX_SCAN_RANGE_SQ) {
                        Vec3d mPos = minecart.getPos();
                        matrices.push();
                        // Minecart bounding box position translation
                        matrices.translate(mPos.x - cameraPos.x - 0.5, mPos.y - cameraPos.y, mPos.z - cameraPos.z - 0.5);
                        Matrix4f matrix = matrices.peek().getPositionMatrix();

                        // Render Yellow Box for Minecart Chests
                        renderBoundingBox(buffer, tessellator, matrix, 0, 0, 0, 1, 0.8f, 1, 1.0f, 1.0f, 0.0f, 0.8f);
                        matrices.pop();
                    }
                }
            }

            // Re-enable depth testing to avoid breaking core rendering pipelines
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }

    /**
     * Helper method to render a complete 3D wireframe box
     */
    private void renderBoundingBox(BufferBuilder buffer, Tessellator tessellator, Matrix4f matrix,
                                   float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                   float r, float g, float b, float a) {
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Bottom Square
        line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Top Square
        line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Vertical Edges
        line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

        tessellator.draw();
    }

    /**
     * Draws a line between two 3D vectors with color parameters
     */
    private void line(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
    }
}

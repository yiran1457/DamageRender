package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.yiran.damagerender.ClientConfig;

public class DamageString {
    private float x;
    private float y;
    private float z;

    private float vX;
    private float vY;
    private float vZ;

    private String displayText;
    private float halfWidth;
    private float life;
    private float maxLife;
    private static final float INITIAL_UPWARD_SPEED = 1.2f;
    /** 飘字基础缩放，所有实例固定为此值（构造时设定，运行时不变）。供外层预算 baseView 复用。 */
    public static final float RENDER_SCALE = 0.04f;
    private float scale;
    private int color;
    private int colorRgb;

    private float amount;
    private final int entityId;
    private String damageType;
    private float fadeStartLife;
    private static final float DRAG_FACTOR = 0.9f;
    private static final float HOVER_THRESHOLD = 0.001f;
    private static final float FADE_START_RATIO = 0.4f;
    private static final float SHRINK_DURATION = 3f;

    /**
     * 每飘字变换复用矩阵，避免 PoseStack.pushPose 复制 4×4+3×3 矩阵与 mulPose 旋转 normal 矩阵的开销。
     * 渲染在客户端主线程单线程执行，复用安全。
     */
    private static final Matrix4f TMP_MATRIX = new Matrix4f();

    public DamageString(int entityId, float x, float y, float z, float damage, int color, String damageType) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;

        this.vX = (float) (Math.random() - 0.5) * 0.25f;
        this.vZ = (float) (Math.random() - 0.5) * 0.25f;
        this.vY = INITIAL_UPWARD_SPEED;

        this.life = ClientConfig.DAMAGE_STRING_LIFE.get();
        this.maxLife = this.life;
        this.fadeStartLife =SHRINK_DURATION;
        this.scale = RENDER_SCALE;
        this.colorRgb = color & 0x00FFFFFF;
        this.color = (0xFF << 24) | this.colorRgb;
        this.damageType = damageType;

        this.amount = Math.abs(damage);
        formatDamage();
    }

    public void mergeDamage(float additional, float newX, float newZ) {
        this.amount += Math.abs(additional);
        this.x = newX;
        this.z = newZ;
        // 重置水平动量，使飘字从实体位置重新扩散；不修改 y 轴位置和动量
        this.vX = (float) (Math.random() - 0.5) * 0.25f;
        this.vZ = (float) (Math.random() - 0.5) * 0.25f;
        formatDamage();
        this.life = this.maxLife;
        this.color = (0xFF << 24) | this.colorRgb;
    }

    public String getDamageType() {
        return damageType;
    }

    public int getEntityId() {
        return entityId;
    }

    public float getAmount() {
        return amount;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getLife() {
        return life;
    }

    public float getMaxLife() {
        return maxLife;
    }

    public boolean isDead() {
        return life <= 0;
    }

    private void formatDamage() {
        if (amount < 1) {
            this.displayText = String.format("%.2f", amount);
        } else if (amount < 10) {
            this.displayText = String.format("%.1f", amount);
        } else {
            this.displayText = String.format("%.0f", amount);
        }
        // 使用纹理图集宽度计算居中偏移，替代 font.width
        this.halfWidth = -DamageNumberRenderer.getTextWidth(this.displayText) / 2f;
    }

    private void update(float partialTick) {
        this.life -= partialTick;

        if (this.life > 0) {
            this.vY *= DRAG_FACTOR;
            this.vX *= DRAG_FACTOR;
            this.vZ *= DRAG_FACTOR;

            if (Math.abs(this.vY) < HOVER_THRESHOLD) this.vY = 0;
            if (Math.abs(this.vX) < HOVER_THRESHOLD) this.vX = 0;
            if (Math.abs(this.vZ) < HOVER_THRESHOLD) this.vZ = 0;

            this.x += this.vX * partialTick;
            this.y += this.vY * partialTick;
            this.z += this.vZ * partialTick;
        }

        // 淡出阶段更新 alpha；否则 alpha 保持 255（构造器已设好）
        if (this.life < fadeStartLife) {
            int alpha = Mth.clamp((int) ((this.life / fadeStartLife) * 255), 0, 255);
            this.color = (alpha << 24) | this.colorRgb;
        }
    }

    /**
     * 渲染单个飘字。
     *
     * @param viewMatrix 外层 PoseStack 当前矩阵（已含 translate(-cameraPos)），循环内复用其引用
     * @param baseRS     循环外预算的 R·S（相机旋转×缩放，含 Y 翻转），所有飘字共享
     * @param consumer   顶点消费者
     * @param partialTick 帧插值
     */
    public void render(Matrix4f viewMatrix, Matrix4f baseRS, VertexConsumer consumer, float partialTick) {
        update(partialTick);

        // 已死亡或完全透明则跳过渲染（避免无谓的矩阵变换和顶点生成）
        if (life <= 0 || (this.color >>> 24) == 0) return;

        // TMP = viewMatrix · T(x,y,z) · baseRS，post-multiply 顺序与原版 PoseStack 同构
        TMP_MATRIX.load(viewMatrix);
        TMP_MATRIX.multiply(com.mojang.math.Matrix4f.createTranslateMatrix(x, y, z));
        TMP_MATRIX.multiply(baseRS);

        // 缩小阶段（life<20）额外 post-multiply 动态缩放；多数飘字走快路径不进此分支。
        if (life < SHRINK_DURATION) {
            float s = life / SHRINK_DURATION;
            TMP_MATRIX.multiply(com.mojang.math.Matrix4f.createScaleMatrix(s, s, s));
        }

        // 使用纹理图集渲染伤害数字，替代 Font.drawInBatch
        DamageNumberRenderer.renderNumber(
                TMP_MATRIX,
                consumer,
                displayText,
                halfWidth,
                -DamageNumberRenderer.CHAR_HEIGHT / 2f,
                this.color,
                LightTexture.FULL_BRIGHT
        );
    }
}
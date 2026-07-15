package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >1.21.1 {
/*import net.minecraft.util.LightCoordsUtil;
*///?} else {
import net.minecraft.client.renderer.LightTexture;
//?}
import net.minecraft.util.Mth;
import net.yiran.damagerender.ClientConfig;

/** 单个伤害飘字的客户端状态与动画。 */
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
    private static final float INITIAL_UPWARD_SPEED = 0.20f;
    /** 所有飘字共用的基础缩放。 */
    public static final float RENDER_SCALE = 0.04f;
    /** 每次合并伤害时使用的缩放倍率。 */
    private static final float MERGE_SCALE_STEP = 1.01f;
    /** 计算伤害数值缩放时复用的对数倒数。 */
    private static float invLogBase = (float) (1.0 / Math.log(ClientConfig.BASE_SCALE_LOG_BASE.get()));

    /** 配置变更后刷新对数缓存。 */
    public static void refreshLogBase() {
        invLogBase = (float) (1.0 / Math.log(ClientConfig.BASE_SCALE_LOG_BASE.get()));
    }

//? if =1.19.2 {
    /*private float scale;
*///?}
    private float baseScale;
    /** 由伤害合并累积的持续缩放。 */
    private float mergeScale;
    /** 合并时短暂放大的反馈效果，会衰减回 {@code 1}。 */
    private float mergeBounceScale;
    private static final float BOUNCE_PEAK_FACTOR = 0.5f;
    private int color;
    private int colorRgb;

    private float amount;
    private final int entityId;
    private String damageType;
    private float fadeStartLife;
    /** 每 tick 的速度衰减倍率。 */
    static final float DRAG_FACTOR = 0.9f;
    private static final float HOVER_THRESHOLD = 0.001f;
    private static final float HORIZONTAL_SPEED = 0.2f;
    /** 合并时重新指向目标所需的最小水平距离平方。 */
    private static final float MERGE_AIM_THRESHOLD = 2f;
    private static final float SHRINK_DURATION = 3f;
    static final float BOUNCE_DECAY = 0.85f;

    /** 当前飘字四边形复用的列主序矩阵。 */
    private static final float[] TMP_MATRIX = new float[16];

    public DamageString(int entityId, float x, float y, float z, float damage, int color, String damageType) {
        this(entityId, x, y, z, damage, color, damageType, 0);
    }

    /** 创建飘字，并恢复预合并伤害累积的缩放。 */
    public DamageString(int entityId, float x, float y, float z, float damage, int color, String damageType, int mergeCount) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;

        this.vX = (float) (Math.random() - 0.5) * HORIZONTAL_SPEED;
        this.vZ = (float) (Math.random() - 0.5) * HORIZONTAL_SPEED;
        this.vY = INITIAL_UPWARD_SPEED;

        this.life = ClientConfig.DAMAGE_STRING_LIFE.get();
        this.maxLife = this.life;
        this.fadeStartLife = SHRINK_DURATION;
//? if =1.19.2 {
        /*this.scale = RENDER_SCALE;
*///?}
        this.mergeScale = scaleForMergeCount(mergeCount);
        this.mergeBounceScale = 1f;
        this.colorRgb = color & 0x00FFFFFF;
        this.color = (0xFF << 24) | this.colorRgb;
        this.damageType = damageType;

        this.amount = Math.abs(damage);
        formatDamage();
    }

    /** 计算预合并伤害的缩放，并限制在配置上限内。 */
    private static float scaleForMergeCount(int mergeCount) {
        if (mergeCount <= 0) return 1f;
        float maxScale = ClientConfig.MERGE_SCALE_MAX.get().floatValue();
        float scale = 1f;
        for (int i = 0; i < mergeCount && scale < maxScale; i++) {
            scale *= MERGE_SCALE_STEP;
        }
        return Math.min(scale, maxScale);
    }

    public void mergeDamage(float additional, float newX, float newZ) {
        this.amount += Math.abs(additional);
        // 改变水平速度而非直接瞬移，保证合并动画连续。
        aimHorizontalVelocity(newX, newZ);
        float maxScale = ClientConfig.MERGE_SCALE_MAX.get().floatValue();
        this.mergeScale = Math.min(this.mergeScale * MERGE_SCALE_STEP, maxScale);
        this.mergeBounceScale = 1f + (this.mergeScale - 1f) * BOUNCE_PEAK_FACTOR;
        formatDamage();
        if (ClientConfig.MERGE_RESET_LIFE.get()) {
            this.life = this.maxLife;
        }
        this.color = (0xFF << 24) | this.colorRgb;
    }

    /** 当实体移动足够远时，重新调整飘字的水平速度。 */
    private void aimHorizontalVelocity(float targetX, float targetZ) {
        float dx = targetX - this.x;
        float dz = targetZ - this.z;
        float distSq = dx * dx + dz * dz;
        if (distSq < MERGE_AIM_THRESHOLD) {
            return;
        }
        float dist = (float) Math.sqrt(distSq);
        float invDist = 1f / dist;
        float dirX = dx * invDist;
        float dirZ = dz * invDist;
        float perpX = -dirZ;
        float perpZ = dirX;
        float baseSpeed = dist * (1f - DRAG_FACTOR);
        float speed = baseSpeed * (float) (0.8 + Math.random() * 0.4);
        float strafe = (float) (Math.random() - 0.5) * 0.4f * baseSpeed;
        this.vX = dirX * speed + perpX * strafe;
        this.vZ = dirZ * speed + perpZ * strafe;
    }

    public String getDamageType() {
        return damageType;
    }

    /** 返回合并伤害累计的缩放。 */
    public float getMergeScale() {
        return mergeScale;
    }

    /** 恢复合并缩放，并限制在配置上限内。 */
    public void setMergeScale(float mergeScale) {
        float maxScale = ClientConfig.MERGE_SCALE_MAX.get().floatValue();
        this.mergeScale = Mth.clamp(mergeScale, 1, maxScale);
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
        // 使用图集宽度而非游戏字体度量计算居中偏移。
        this.halfWidth = -DamageNumberRenderer.getTextWidth(this.displayText) / 2f;
        this.baseScale = (float) Math.log(amount) * invLogBase;
    }

    private void update(float partialTick, float drag, float bounceDecay) {
        this.life -= partialTick;

        if (this.life > 0) {
            this.vY *= drag;
            this.vX *= drag;
            this.vZ *= drag;

            if (Math.abs(this.vY) < HOVER_THRESHOLD) this.vY = 0;
            if (Math.abs(this.vX) < HOVER_THRESHOLD) this.vX = 0;
            if (Math.abs(this.vZ) < HOVER_THRESHOLD) this.vZ = 0;

            this.x += this.vX * partialTick;
            this.y += this.vY * partialTick;
            this.z += this.vZ * partialTick;

            if (this.mergeBounceScale > 1f) {
                float delta = this.mergeBounceScale - 1f;
                delta *= bounceDecay;
                if (delta < 0.001f) delta = 0f;
                this.mergeBounceScale = 1f + delta;
            }
        }

        if (this.life < fadeStartLife) {
            int alpha = Mth.clamp((int) ((this.life / fadeStartLife) * 255), 0, 255);
            this.color = (alpha << 24) | this.colorRgb;
        }
    }

    /** 更新动画状态并写入当前飘字的纹理四边形。 */
    public void render(float[] viewMatrix, float[] baseRS, VertexConsumer consumer, float partialTick,
                       float drag, float bounceDecay) {
        update(partialTick, drag, bounceDecay);

        if (life <= 0 || (this.color >>> 24) == 0) return;

        float shrink = (life < SHRINK_DURATION) ? (life * 0.5f + 3) / (SHRINK_DURATION + 3) : 1f;
        float s = shrink * (mergeScale + baseScale + mergeBounceScale - 1);

        Mat4Util.mulViewTranslateBaseScale(TMP_MATRIX, viewMatrix, baseRS, x, y, z, s);

        DamageNumberRenderer.renderNumber(
                TMP_MATRIX,
                consumer,
                displayText,
                halfWidth,
                -DamageNumberRenderer.CHAR_HEIGHT / 2f,
                this.color,
//? if >1.21.1 {
                /*LightCoordsUtil.FULL_BRIGHT
*///?} else {
                LightTexture.FULL_BRIGHT
//?}
        );
    }
}

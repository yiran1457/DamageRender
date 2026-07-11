package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
    private static final float INITIAL_UPWARD_SPEED = 0.3f;
    /**
     * 飘字基础缩放，所有实例固定为此值（构造时设定，运行时不变）。供外层预算 baseView 复用。
     */
    public static final float RENDER_SCALE = 0.04f;
    /**
     * 合并放大步长：每次 mergeDamage 缩放因子乘以此值（+1%）。
     */
    private static final float MERGE_SCALE_STEP = 1.01f;
    private float scale;
    /**
     * 合并累积的额外缩放因子，初始 1.0，每次合并 ×1.01，受 MERGE_SCALE_MAX 上限约束。
     */
    private float mergeScale;
    private int color;
    private int colorRgb;

    private float amount;
    private final int entityId;
    private String damageType;
    private float fadeStartLife;
    private static final float DRAG_FACTOR = 0.9f;
    private static final float HOVER_THRESHOLD = 0.001f;
    /** 水平扩散动量幅度，构造与合并复用。 */
    private static final float HORIZONTAL_SPEED = 0.05f;
    /** 合并时朝目标方向给速，距离小于此值视为已在目标处，回退纯随机扩散。 */
    private static final float MERGE_AIM_THRESHOLD = 0.01f;
    private static final float FADE_START_RATIO = 0.4f;
    private static final float SHRINK_DURATION = 3f;

    /**
     * 每飘字变换结果复用矩阵（列主序 float[16]），由 {@link Mat4Util#mulViewTranslateBaseScale} 写入。
     */
    private static final float[] TMP_MATRIX = new float[16];

    public DamageString(int entityId, float x, float y, float z, float damage, int color, String damageType) {
        this(entityId, x, y, z, damage, color, damageType, 0);
    }

    /**
     * 构造飘字并按已合并次数初始化放大因子。
     *
     * @param mergeCount 该飘字在构造前已经合并的伤害段数（预合并/实体合并产生的累积），
     *                   每次合并对应 ×1.01 放大；0 表示单条伤害不放大。
     */
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
        this.scale = RENDER_SCALE;
        this.mergeScale = scaleForMergeCount(mergeCount);
        this.colorRgb = color & 0x00FFFFFF;
        this.color = (0xFF << 24) | this.colorRgb;
        this.damageType = damageType;

        this.amount = Math.abs(damage);
        formatDamage();
    }

    /**
     * 按"已合并次数"计算放大因子：每次合并 ×{@link #MERGE_SCALE_STEP}，受 {@link ClientConfig#MERGE_SCALE_MAX} 约束。
     * 用累乘代替 Math.pow，避免每次构造调浮点幂函数。
     */
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
        // 不硬瞬移 xz，改为把水平动量指向新伤害位置，飘字自然漂过去停在附近（drag 衰减使其渐近不到达）
        aimHorizontalVelocity(newX, newZ);
        // 每次合并放大 1%，受配置上限约束
        float maxScale = ClientConfig.MERGE_SCALE_MAX.get().floatValue();
        this.mergeScale = Math.min(this.mergeScale * MERGE_SCALE_STEP, maxScale);
        formatDamage();
        this.life = this.maxLife;
        this.color = (0xFF << 24) | this.colorRgb;
    }

    /**
     * 把水平动量 (vX,vZ) 设为朝 (targetX,targetZ) 方向，幅度 HORIZONTAL_SPEED × 随机系数，
     * 并叠加一个小的垂直偏转使到达点散布在目标"旁边"而非正中心。y 轴位置和动量不动。
     *
     * <p>已在目标处（距离 < {@link #MERGE_AIM_THRESHOLD}）时回退纯随机扩散，避免归一化除零。
     */
    private void aimHorizontalVelocity(float targetX, float targetZ) {
        float dx = targetX - this.x;
        float dz = targetZ - this.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < MERGE_AIM_THRESHOLD) {
            // 已在目标处，随机扩散
            this.vX = (float) (Math.random() - 0.5) * HORIZONTAL_SPEED;
            this.vZ = (float) (Math.random() - 0.5) * HORIZONTAL_SPEED;
            return;
        }
        // 单位方向 + 垂直方向（dz/perp, -dx/perp），后者叠加随机偏转实现"旁边"散布
        float invDist = 1f / dist;
        float dirX = dx * invDist;
        float dirZ = dz * invDist;
        float perpX = -dirZ;
        float perpZ = dirX;
        // 主速度幅度随机化（与原随机动量同量级），垂直偏转取其一半以内
        float speed = (float) (0.5 + Math.random() * 0.5) * HORIZONTAL_SPEED; // [0.5,1.0]×幅度
        float strafe = (float) (Math.random() - 0.5) * HORIZONTAL_SPEED;      // 垂直散布
        this.vX = dirX * speed + perpX * strafe;
        this.vZ = dirZ * speed + perpZ * strafe;
    }

    public String getDamageType() {
        return damageType;
    }

    /**
     * 当前合并放大因子（含预合并累积），1.0 表示未放大。供实体级合并继承放大状态。
     */
    public float getMergeScale() {
        return mergeScale;
    }

    /**
     * 直接设置合并放大因子，供实体级合并新建 combined 时继承来源飘字的累积放大。
     * 受 {@link ClientConfig#MERGE_SCALE_MAX} 上限约束。
     */
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
        // 使用纹理图集宽度计算居中偏移，替代 font.width
        this.halfWidth = -DamageNumberRenderer.getTextWidth(this.displayText) / 2f;
    }

    private void update(float partialTick) {
        this.life -= partialTick;

        if (this.life > 0) {
            // 帧率无关衰减：每 tick 衰减为 DRAG_FACTOR 倍，故一帧（partialTick tick）衰减为 DRAG_FACTOR^partialTick。
            // 原写法 v*=DRAG_FACTOR 每帧固定比例，高帧率衰减过快导致飘字停得太早。
            float drag = (float) Math.pow(DRAG_FACTOR, partialTick);
            this.vY *= drag;
            this.vX *= drag;
            this.vZ *= drag;

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
     * 渲染单个飘字：矩阵 {@code V·T(x,y,z)·baseRS·S(s)} 由 {@link Mat4Util#mulViewTranslateBaseScale} 一次性预算。
     *
     * @param viewMatrix  外层 PoseStack 当前矩阵（已含 translate(-cameraPos)），列主序 float[16]，循环内复用
     * @param baseRS      循环外预算的 R·S（相机旋转×缩放，含 Y 翻转），列主序 float[16]，所有飘字共享
     * @param consumer    顶点消费者
     * @param partialTick 帧插值
     */
    public void render(float[] viewMatrix, float[] baseRS, VertexConsumer consumer, float partialTick) {
        update(partialTick);

        // 已死亡或完全透明则跳过渲染
        if (life <= 0 || (this.color >>> 24) == 0) return;

        // 缩小阶段额外缩放，叠加合并放大因子；多数飘字走 shrink=1 快路径
        float shrink = (life < SHRINK_DURATION) ? life / SHRINK_DURATION : 1f;
        float s = shrink * mergeScale;

        Mat4Util.mulViewTranslateBaseScale(TMP_MATRIX, viewMatrix, baseRS, x, y, z, s);

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
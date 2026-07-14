package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >1.21.1 {
/*import net.minecraft.util.LightCoordsUtil;
*///?} else {
import net.minecraft.client.renderer.LightTexture;
//?}
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
    private static final float INITIAL_UPWARD_SPEED = 0.20f;
         // 飘字基础缩放，所有实例固定为此值（构造时设定，运行时不变）。供外层预算 baseView 复用。
     //
    public static final float RENDER_SCALE = 0.04f;
         // 合并放大步长：每次 mergeDamage 缩放因子乘以此值。
     //
    private static final float MERGE_SCALE_STEP = 1.01f;
         // 1 / ln(BASE_SCALE_LOG_BASE) 的缓存值，配置变更时重算。
     // 换底公式：log_b(x) = ln(x) / ln(b) = ln(x) * INV_LOG_BASE，
     // 避免每次 formatDamage 重复计算 1/ln(b)。
     //
    private static float invLogBase = (float) (1.0 / Math.log(ClientConfig.BASE_SCALE_LOG_BASE.get()));

         // 配置变更时调用，重算缓存的对数底数倒数。
     //
    public static void refreshLogBase() {
        invLogBase = (float) (1.0 / Math.log(ClientConfig.BASE_SCALE_LOG_BASE.get()));
    }

//? if =1.19.2 {
    /*private float scale;
*///?}
    private float baseScale;
         // 合并累积的额外缩放因子，初始 1.0，每次合并 ×1.01，受 MERGE_SCALE_MAX 上限约束。
     //
    private float mergeScale;
         // 合并弹跳缩放因子，1.0=无弹跳。合并时跳到 >1.0，每帧指数衰减回 1.0。
     // 最终渲染缩放 = mergeScale × mergeBounceScale，弹跳叠加在合并累积之上。
     //
    private float mergeBounceScale;
         // 弹跳峰值系数：合并时 mergeBounceScale 设为 1 + (mergeScale-1) × 此值。
     // 即弹跳幅度与当前 mergeScale 成正比——合并累积越大，弹跳越明显。
     //
    private static final float BOUNCE_PEAK_FACTOR = 0.5f;
    private int color;
    private int colorRgb;

    private float amount;
    private final int entityId;
    private String damageType;
    private float fadeStartLife;
         // 拖拽衰减率：每 tick 速度衰减为此倍。循环外预算 DRAG_FACTOR^partialTick 后传入。
     //
    static final float DRAG_FACTOR = 0.9f;
    private static final float HOVER_THRESHOLD = 0.001f;
         // 水平扩散动量幅度，构造与合并复用。
     //
    private static final float HORIZONTAL_SPEED = 0.2f;
         // 合并时新老坐标距离小于此值则不动量转移（飘字保持原动量），≥此值才朝新位置漂移。
     //
    private static final float MERGE_AIM_THRESHOLD = 2f;
    private static final float SHRINK_DURATION = 3f;
         // 弹跳衰减率：每 tick 衰减为此倍（向 1.0 收敛）。循环外预算 BOUNCE_DECAY^partialTick 后传入。
     //
    static final float BOUNCE_DECAY = 0.85f;

         // 每飘字变换结果复用矩阵（列主序 float[16]），由 {@link Mat4Util#mulViewTranslateBaseScale} 写入。
     //
    private static final float[] TMP_MATRIX = new float[16];

    public DamageString(int entityId, float x, float y, float z, float damage, int color, String damageType) {
        this(entityId, x, y, z, damage, color, damageType, 0);
    }

         // 构造飘字并按已合并次数初始化放大因子。
     //
     // @param mergeCount 该飘字在构造前已经合并的伤害段数（预合并/实体合并产生的累积），
     //                   每次合并对应 ×1.01 放大；0 表示单条伤害不放大。
     //
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

         // 按"已合并次数"计算放大因子：每次合并 ×{@link #MERGE_SCALE_STEP}，受 {@link ClientConfig#MERGE_SCALE_MAX} 约束。
     // 用累乘代替 Math.pow，避免每次构造调浮点幂函数。
     //
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
        // 弹跳缩放：基于合并后的 mergeScale 额外放大，后续逐帧衰减回 1.0
        this.mergeBounceScale = 1f + (this.mergeScale - 1f) * BOUNCE_PEAK_FACTOR;
        formatDamage();
        if (ClientConfig.MERGE_RESET_LIFE.get()) {
            this.life = this.maxLife;
        }
        this.color = (0xFF << 24) | this.colorRgb;
    }

         // 把水平动量 (vX,vZ) 设为朝 (targetX,targetZ) 方向，速度自适应距离使飘字最终停在目标附近
     // （剩余距离 < {@link #MERGE_AIM_THRESHOLD}），下次同位置合并不再触发动量转移。
     // 叠加小的垂直偏转使到达点散布在目标"旁边"而非正中心。y 轴位置和动量不动。
     //
     // <p>新老坐标距离² < {@link #MERGE_AIM_THRESHOLD} 时不改动量——飘字保持当前水平动量
     // 继续其原有飘动，避免小位移抖动让飘字乱跑；仅当实体明显移动才追过去。
     //
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
        // 自适应速度：v₀ = dist × (1 - drag)，使 drag 衰减下总位移 ≈ dist，飘字停在目标处
        // 乘一个 [0.8,1.2] 随机系数，让多次合并的飘字散布在目标周围而非精确重合
        float baseSpeed = dist * (1f - DRAG_FACTOR);
        float speed = baseSpeed * (float) (0.8 + Math.random() * 0.4);
        // 垂直散布取速度的 ±20%，实现"旁边"效果
        float strafe = (float) (Math.random() - 0.5) * 0.4f * baseSpeed;
        this.vX = dirX * speed + perpX * strafe;
        this.vZ = dirZ * speed + perpZ * strafe;
    }

    public String getDamageType() {
        return damageType;
    }

         // 当前合并放大因子（含预合并累积），1.0 表示未放大。供实体级合并继承放大状态。
     //
    public float getMergeScale() {
        return mergeScale;
    }

         // 直接设置合并放大因子，供实体级合并新建 combined 时继承来源飘字的累积放大。
     // 受 {@link ClientConfig#MERGE_SCALE_MAX} 上限约束。
     //
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
        this.baseScale = (float) Math.log(amount) * invLogBase;
    }

    private void update(float partialTick, float drag, float bounceDecay) {
        this.life -= partialTick;

        if (this.life > 0) {
            // 帧率无关衰减：drag = DRAG_FACTOR^partialTick 由外层循环外预算一次，所有飘字复用。
            this.vY *= drag;
            this.vX *= drag;
            this.vZ *= drag;

            if (Math.abs(this.vY) < HOVER_THRESHOLD) this.vY = 0;
            if (Math.abs(this.vX) < HOVER_THRESHOLD) this.vX = 0;
            if (Math.abs(this.vZ) < HOVER_THRESHOLD) this.vZ = 0;

            this.x += this.vX * partialTick;
            this.y += this.vY * partialTick;
            this.z += this.vZ * partialTick;

            // 弹跳缩放衰减：向 1.0 指数收敛，帧率无关
            if (this.mergeBounceScale > 1f) {
                // bounceDelta 每帧衰减，bounceScale = 1 + delta
                float delta = this.mergeBounceScale - 1f;
                delta *= bounceDecay;
                if (delta < 0.001f) delta = 0f;
                this.mergeBounceScale = 1f + delta;
            }
        }

        // 淡出阶段更新 alpha；否则 alpha 保持 255（构造器已设好）
        if (this.life < fadeStartLife) {
            int alpha = Mth.clamp((int) ((this.life / fadeStartLife) * 255), 0, 255);
            this.color = (alpha << 24) | this.colorRgb;
        }
    }

         // 渲染单个飘字：矩阵 {@code V·T(x,y,z)·baseRS·S(s)} 由 {@link Mat4Util#mulViewTranslateBaseScale} 一次性预算。
     //
     // @param viewMatrix  外层 PoseStack 当前矩阵（已含 translate(-cameraPos)），列主序 float[16]，循环内复用
     // @param baseRS      循环外预算的 R·S（相机旋转×缩放，含 Y 翻转），列主序 float[16]，所有飘字共享
     // @param consumer    顶点消费者
     // @param partialTick 帧插值
     // @param drag        外层预算的 DRAG_FACTOR^partialTick，循环内复用避免逐飘字 Math.pow
     // @param bounceDecay 外层预算的 BOUNCE_DECAY^partialTick，循环内复用避免逐飘字 Math.pow
     //
    public void render(float[] viewMatrix, float[] baseRS, VertexConsumer consumer, float partialTick,
                       float drag, float bounceDecay) {
        update(partialTick, drag, bounceDecay);

        // 已死亡或完全透明则跳过渲染
        if (life <= 0 || (this.color >>> 24) == 0) return;

        // 缩小阶段额外缩放，叠加合并累积 + 弹跳缩放因子
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

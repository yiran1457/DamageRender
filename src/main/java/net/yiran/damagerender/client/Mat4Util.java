package net.yiran.damagerender.client;

/**
 * 为伤害数字专用的无分配矩阵运算。
 *
 * <p>数组采用 JOML 的列主序布局。飘字顶点恒为 {@code (x, y, 0, 1)}，因此只计算
 * {@code V · T · R · S} 的第 0、1、3 列；其余列不参与顶点变换。</p>
 */
public final class Mat4Util {
    private Mat4Util() {}

    /**
     * 计算 {@code V · T(x,y,z) · baseRS · S(s)} 中飘字需要的列并写入 {@code out}。
     * 第 2 列固定写零；{@code baseRS} 必须不含平移以外的齐次项。
     */
    public static void mulViewTranslateBaseScale(float[] out, float[] v, float[] baseRS,
                                                 float x, float y, float z, float s) {
        float v00 = v[0],  v10 = v[1],  v20 = v[2],  v30 = v[3];
        float v01 = v[4],  v11 = v[5],  v21 = v[6],  v31 = v[7];
        float v02 = v[8],  v12 = v[9],  v22 = v[10], v32 = v[11];
        float v03 = v[12], v13 = v[13], v23 = v[14], v33 = v[15];

        float b00 = baseRS[0],  b10 = baseRS[1],  b20 = baseRS[2];
        float b01 = baseRS[4],  b11 = baseRS[5],  b21 = baseRS[6];
        float b03 = baseRS[12], b13 = baseRS[13], b23 = baseRS[14];

        float m00 = Math.fma(v01, b10, v00 * b00);  m00 = Math.fma(v02, b20, m00);  m00 *= s;
        float m10 = Math.fma(v11, b10, v10 * b00);  m10 = Math.fma(v12, b20, m10);  m10 *= s;
        float m20 = Math.fma(v21, b10, v20 * b00);  m20 = Math.fma(v22, b20, m20);  m20 *= s;
        float m30 = Math.fma(v31, b10, v30 * b00);  m30 = Math.fma(v32, b20, m30);  m30 *= s;

        float m01 = Math.fma(v01, b11, v00 * b01);  m01 = Math.fma(v02, b21, m01);  m01 *= s;
        float m11 = Math.fma(v11, b11, v10 * b01);  m11 = Math.fma(v12, b21, m11);  m11 *= s;
        float m21 = Math.fma(v21, b11, v20 * b01);  m21 = Math.fma(v22, b21, m21);  m21 *= s;
        float m31 = Math.fma(v31, b11, v30 * b01);  m31 = Math.fma(v32, b21, m31);  m31 *= s;

        float t0 = b03 + x, t1 = b13 + y, t2 = b23 + z;
        float m03 = Math.fma(v01, t1, v00 * t0);  m03 = Math.fma(v02, t2, m03);  m03 += v03;
        float m13 = Math.fma(v11, t1, v10 * t0);  m13 = Math.fma(v12, t2, m13);  m13 += v13;
        float m23 = Math.fma(v21, t1, v20 * t0);  m23 = Math.fma(v22, t2, m23);  m23 += v23;
        float m33 = Math.fma(v31, t1, v30 * t0);  m33 = Math.fma(v32, t2, m33);  m33 += v33;

        out[0]  = m00; out[1]  = m10; out[2]  = m20; out[3]  = m30;
        out[4]  = m01; out[5]  = m11; out[6]  = m21; out[7]  = m31;
        out[8]  = 0f;  out[9]  = 0f;  out[10] = 0f;  out[11] = 0f;
        out[12] = m03; out[13] = m13; out[14] = m23; out[15] = m33;
    }

    /** Transforms a local {@code (x, y, 0)} vertex with a matrix produced above. */
    public static void transformVertex(float[] m, float px, float py, float[] outXYZ) {
        float m00 = m[0],  m10 = m[1],  m20 = m[2];
        float m01 = m[4],  m11 = m[5],  m21 = m[6];
        float m03 = m[12], m13 = m[13], m23 = m[14];
        outXYZ[0] = Math.fma(m01, py, Math.fma(m00, px, m03));
        outXYZ[1] = Math.fma(m11, py, Math.fma(m10, px, m13));
        outXYZ[2] = Math.fma(m21, py, Math.fma(m20, px, m23));
    }
}

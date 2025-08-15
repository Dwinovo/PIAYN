package com.dwinovo.piayn.client.renderer.catnip.theme;

/**
 * 颜色类，不依赖Catnip
 * 支持RGBA颜色操作和转换
 */
public class Color {
    
    // 常用颜色常量
    public static final Color WHITE = new Color(0xFFFFFF, false);
    public static final Color BLACK = new Color(0x000000, false);
    public static final Color RED = new Color(0xFF0000, false);
    public static final Color GREEN = new Color(0x00FF00, false);
    public static final Color BLUE = new Color(0x0000FF, false);
    public static final Color YELLOW = new Color(0xFFFF00, false);
    public static final Color CYAN = new Color(0x00FFFF, false);
    public static final Color MAGENTA = new Color(0xFF00FF, false);
    
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;
    
    /**
     * 从整数颜色值创建Color
     * @param color 颜色值（RGB或ARGB）
     * @param hasAlpha 是否包含alpha通道
     */
    public Color(int color, boolean hasAlpha) {
        if (hasAlpha) {
            this.alpha = (color >> 24) & 0xFF;
            this.red = (color >> 16) & 0xFF;
            this.green = (color >> 8) & 0xFF;
            this.blue = color & 0xFF;
        } else {
            this.alpha = 255; // 完全不透明
            this.red = (color >> 16) & 0xFF;
            this.green = (color >> 8) & 0xFF;
            this.blue = color & 0xFF;
        }
    }
    
    /**
     * 从RGBA分量创建Color
     */
    public Color(int red, int green, int blue, int alpha) {
        this.red = clamp(red, 0, 255);
        this.green = clamp(green, 0, 255);
        this.blue = clamp(blue, 0, 255);
        this.alpha = clamp(alpha, 0, 255);
    }
    
    /**
     * 从RGB分量创建Color（alpha=255）
     */
    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }
    
    /**
     * 从浮点RGBA分量创建Color
     */
    public Color(float red, float green, float blue, float alpha) {
        this.red = (int) (clamp(red, 0.0f, 1.0f) * 255);
        this.green = (int) (clamp(green, 0.0f, 1.0f) * 255);
        this.blue = (int) (clamp(blue, 0.0f, 1.0f) * 255);
        this.alpha = (int) (clamp(alpha, 0.0f, 1.0f) * 255);
    }
    
    /**
     * 从浮点RGB分量创建Color（alpha=1.0）
     */
    public Color(float red, float green, float blue) {
        this(red, green, blue, 1.0f);
    }
    
    // Getter方法 - 整数值
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }
    public int getAlpha() { return alpha; }
    
    // Getter方法 - 浮点值（0.0-1.0）
    public float getRedAsFloat() { return red / 255.0f; }
    public float getGreenAsFloat() { return green / 255.0f; }
    public float getBlueAsFloat() { return blue / 255.0f; }
    public float getAlphaAsFloat() { return alpha / 255.0f; }
    
    /**
     * 获取RGB颜色值（不包含alpha）
     */
    public int getRGB() {
        return (red << 16) | (green << 8) | blue;
    }
    
    /**
     * 获取ARGB颜色值（包含alpha）
     */
    public int getARGB() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * 创建此颜色的副本
     */
    public Color copy() {
        return new Color(red, green, blue, alpha);
    }
    
    /**
     * 创建带有新alpha值的颜色
     */
    public Color withAlpha(int alpha) {
        return new Color(red, green, blue, alpha);
    }
    
    /**
     * 创建带有新alpha值的颜色（浮点）
     */
    public Color withAlpha(float alpha) {
        return new Color(red, green, blue, (int) (clamp(alpha, 0.0f, 1.0f) * 255));
    }
    
    /**
     * 使颜色变亮
     */
    public Color brighter() {
        return brighter(1.2f);
    }
    
    /**
     * 使颜色变亮指定倍数
     */
    public Color brighter(float factor) {
        int newRed = Math.min(255, (int) (red * factor));
        int newGreen = Math.min(255, (int) (green * factor));
        int newBlue = Math.min(255, (int) (blue * factor));
        return new Color(newRed, newGreen, newBlue, alpha);
    }
    
    /**
     * 使颜色变暗
     */
    public Color darker() {
        return darker(0.8f);
    }
    
    /**
     * 使颜色变暗指定倍数
     */
    public Color darker(float factor) {
        int newRed = (int) (red * factor);
        int newGreen = (int) (green * factor);
        int newBlue = (int) (blue * factor);
        return new Color(newRed, newGreen, newBlue, alpha);
    }
    
    /**
     * 混合两个颜色
     * @param other 另一个颜色
     * @param ratio 混合比例（0.0-1.0）
     */
    public Color mix(Color other, float ratio) {
        ratio = clamp(ratio, 0.0f, 1.0f);
        float invRatio = 1.0f - ratio;
        
        int newRed = (int) (red * invRatio + other.red * ratio);
        int newGreen = (int) (green * invRatio + other.green * ratio);
        int newBlue = (int) (blue * invRatio + other.blue * ratio);
        int newAlpha = (int) (alpha * invRatio + other.alpha * ratio);
        
        return new Color(newRed, newGreen, newBlue, newAlpha);
    }
    
    /**
     * 限制值在指定范围内
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 限制值在指定范围内
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Color color = (Color) obj;
        return red == color.red && green == color.green && blue == color.blue && alpha == color.alpha;
    }
    
    @Override
    public int hashCode() {
        return getARGB();
    }
    
    @Override
    public String toString() {
        return String.format("Color[r=%d, g=%d, b=%d, a=%d] (#%08X)", red, green, blue, alpha, getARGB());
    }
    
    // 静态工厂方法
    
    /**
     * 从HSV创建颜色
     * @param hue 色调 (0-360)
     * @param saturation 饱和度 (0.0-1.0)
     * @param value 明度 (0.0-1.0)
     */
    public static Color fromHSV(float hue, float saturation, float value) {
        return fromHSV(hue, saturation, value, 1.0f);
    }
    
    /**
     * 从HSVA创建颜色
     */
    public static Color fromHSV(float hue, float saturation, float value, float alpha) {
        hue = hue % 360.0f;
        if (hue < 0) hue += 360.0f;
        
        saturation = clamp(saturation, 0.0f, 1.0f);
        value = clamp(value, 0.0f, 1.0f);
        alpha = clamp(alpha, 0.0f, 1.0f);
        
        float c = value * saturation;
        float x = c * (1 - Math.abs((hue / 60.0f) % 2 - 1));
        float m = value - c;
        
        float r, g, b;
        
        if (hue < 60) {
            r = c; g = x; b = 0;
        } else if (hue < 120) {
            r = x; g = c; b = 0;
        } else if (hue < 180) {
            r = 0; g = c; b = x;
        } else if (hue < 240) {
            r = 0; g = x; b = c;
        } else if (hue < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        return new Color(r + m, g + m, b + m, alpha);
    }
}

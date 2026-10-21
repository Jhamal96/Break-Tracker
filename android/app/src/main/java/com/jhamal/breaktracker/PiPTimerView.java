package com.jhamal.breaktracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import java.util.Locale;

/**
 * Draws a full-size countdown directly in Android's native PiP surface.
 * PiP taps are handled by the OS, so styles are changed through system RemoteActions.
 */
public final class PiPTimerView extends View {
    public static final String[] STYLES = {
        "Digital", "Clock Dial", "Speedometer", "Minimal",
        "Neon Pulse", "Aurora Glass", "Retro Flip", "Racing HUD",
        "Cosmic Orbit", "Sunset Glow", "Pixel Arcade", "Zen Garden",
        "Ocean Depths", "Blueprint"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int style = 0;
    private long seconds = 0;
    private long totalSeconds = 900;
    private String label = "No active break";

    public PiPTimerView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    public void setStyle(int styleIndex) {
        style = Math.floorMod(styleIndex, STYLES.length);
        invalidate();
    }

    public void setCountdown(long remainingSeconds, long durationSeconds, String description) {
        seconds = Math.max(0, remainingSeconds);
        totalSeconds = Math.max(1, durationSeconds);
        label = description == null || description.isEmpty() ? "No active break" : description;
        invalidate();
    }

    private int rgb(String hex) { return Color.parseColor(hex); }

    private void fill(Canvas c, int color) {
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color);
        c.drawColor(color);
    }

    private void gradient(Canvas c, String start, String finish) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE);
        paint.setShader(new LinearGradient(0, 0, 360, 202, rgb(start), rgb(finish), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 360, 202, paint); paint.setShader(null);
    }

    private void box(Canvas c, float x, float y, float w, float h, float r, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setShader(null); paint.setColor(color);
        rect.set(x, y, x + w, y + h); c.drawRoundRect(rect, r, r, paint);
    }

    private void outline(Canvas c, float x, float y, float w, float h, float r, int color, float width) {
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width);
        paint.setColor(color); paint.setShader(null); rect.set(x, y, x + w, y + h);
        c.drawRoundRect(rect, r, r, paint); paint.setStyle(Paint.Style.FILL);
    }

    private void circle(Canvas c, float x, float y, float radius, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setShader(null); paint.setColor(color);
        c.drawCircle(x, y, radius, paint);
    }

    private void ring(Canvas c, float x, float y, float radius, int background, int foreground, float thickness) {
        paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(thickness);
        paint.setStrokeCap(Paint.Cap.ROUND);
        rect.set(x - radius, y - radius, x + radius, y + radius);
        paint.setColor(background); c.drawArc(rect, -90, 360, false, paint);
        paint.setColor(foreground); c.drawArc(rect, -90, 360 * progress(), false, paint);
        paint.setStrokeCap(Paint.Cap.BUTT); paint.setStyle(Paint.Style.FILL);
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) {
        paint.setColor(color); paint.setShader(null); paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width); c.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas c, String s, float x, float y, float size, int color,
                      boolean bold, boolean mono, boolean italic) {
        paint.setStyle(Paint.Style.FILL); paint.setShader(null); paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(size);
        paint.setTypeface(Typeface.create(mono ? Typeface.MONOSPACE : Typeface.DEFAULT,
            italic ? Typeface.BOLD_ITALIC : bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(s, x, y, paint);
    }

    private void header(Canvas c, String heading, int color) {
        text(c, heading, 180, 24, 12, color, true, false, false);
    }

    private String time() {
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private float progress() {
        return Math.max(0f, Math.min(1f, seconds / (float) totalSeconds));
    }

    private int accent(int normal) {
        return seconds > 0 && seconds <= 180 ? rgb("#FCA5A5") : normal;
    }

    private String shortLabel() {
        String value = label.replace('\n', ' ');
        return value.length() > 32 ? value.substring(0, 29) + "..." : value;
    }

    private void footer(Canvas c, int color) { text(c, shortLabel(), 180, 181, 13, color, true, false, false); }

    private void bar(Canvas c, float x, float y, float w, float h, int bg, int fg) {
        box(c, x, y, w, h, h / 2, bg);
        if (progress() > 0) box(c, x, y, w * progress(), h, h / 2, accent(fg));
    }

    @Override protected void onDraw(Canvas original) {
        super.onDraw(original);
        if (getWidth() == 0 || getHeight() == 0) return;
        original.save();
        original.scale(getWidth() / 360f, getHeight() / 202f);
        switch (style) {
            case 1: clock(original); break;
            case 2: speedometer(original); break;
            case 3: minimal(original); break;
            case 4: neon(original); break;
            case 5: aurora(original); break;
            case 6: flip(original); break;
            case 7: racing(original); break;
            case 8: orbit(original); break;
            case 9: sunset(original); break;
            case 10: pixel(original); break;
            case 11: zen(original); break;
            case 12: ocean(original); break;
            case 13: blueprint(original); break;
            default: digital(original); break;
        }
        original.restore();
    }

    private void digital(Canvas c) {
        gradient(c, "#0F172A", "#1E3A8A");
        header(c, "◷   BREAK TRACKER", rgb("#93C5FD"));
        text(c, time(), 180, 114, 72, rgb("#FFFFFF"), true, false, false);
        footer(c, rgb("#DBEAFE"));
        bar(c, 47, 152, 266, 7, rgb("#334155"), rgb("#38BDF8"));
    }

    private void clock(Canvas c) {
        gradient(c, "#0C1934", "#1E3A8A");
        header(c, "CLOCK DIAL", rgb("#93C5FD"));
        ring(c, 180, 102, 69, rgb("#334155"), accent(rgb("#38BDF8")), 10);
        text(c, time(), 180, 111, 31, Color.WHITE, true, false, false);
        text(c, "REMAINING", 180, 134, 10, rgb("#93C5FD"), true, false, false);
        footer(c, rgb("#E2E8F0"));
    }

    private void speedometer(Canvas c) {
        gradient(c, "#08121F", "#142D43");
        header(c, "SPEEDOMETER", rgb("#67E8F9"));
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(13); rect.set(88, 49, 272, 233);
        paint.setColor(rgb("#334155")); c.drawArc(rect, 180, 180, false, paint);
        paint.setColor(accent(rgb("#22D3EE"))); c.drawArc(rect, 180, 180 * progress(), false, paint);
        paint.setStyle(Paint.Style.FILL); paint.setStrokeCap(Paint.Cap.BUTT);
        text(c, time(), 180, 141, 55, Color.WHITE, true, false, false);
        footer(c, rgb("#A5F3FC"));
    }

    private void minimal(Canvas c) {
        fill(c, rgb("#FAFAF9"));
        header(c, "MINIMAL / BREAK", rgb("#64748B"));
        text(c, time(), 180, 123, 78, rgb("#0F172A"), false, false, false);
        footer(c, rgb("#475569"));
        bar(c, 80, 148, 200, 3, rgb("#E2E8F0"), rgb("#334155"));
    }

    private void neon(Canvas c) {
        gradient(c, "#0F0729", "#31134C");
        header(c, "✦   NEON PULSE", rgb("#F0ABFC"));
        box(c, 35, 49, 290, 113, 13, rgb("#24113C"));
        outline(c, 35, 49, 290, 113, 13, rgb("#E879F9"), 2.5f);
        line(c, 40, 56, 65, 56, rgb("#22D3EE"), 4);
        line(c, 294, 155, 319, 155, rgb("#22D3EE"), 4);
        text(c, time(), 180, 115, 66, accent(rgb("#F0ABFC")), true, false, false);
        footer(c, rgb("#A5F3FC"));
        bar(c, 70, 143, 220, 4, rgb("#4C1D65"), rgb("#E879F9"));
    }

    private void aurora(Canvas c) {
        gradient(c, "#0D9488", "#4C1D95");
        circle(c, 50, 166, 95, rgb("#146C78"));
        circle(c, 309, 34, 111, rgb("#5540A1"));
        header(c, "AURORA GLASS", rgb("#CCFBF1"));
        box(c, 34, 49, 292, 122, 21, rgb("#5591AF"));
        outline(c, 34, 49, 292, 122, 21, rgb("#C4E9F5"), 1.5f);
        text(c, "TAKE A MOMENT", 180, 76, 11, rgb("#CCFBF1"), true, false, false);
        text(c, time(), 180, 130, 63, Color.WHITE, true, false, false);
        footer(c, rgb("#F0FDFA"));
    }

    private void flip(Canvas c) {
        fill(c, rgb("#151B25"));
        header(c, "▦   RETRO FLIP", rgb("#FDE047"));
        String digits = time().replace(":", "");
        for (int i=0; i<4; i++) {
            float x = 48 + i*63 + (i >= 2 ? 12 : 0);
            box(c, x, 58, 56, 83, 8, rgb("#303846"));
            box(c, x, 100, 56, 41, 7, rgb("#232A35"));
            line(c, x, 100, x+56, 100, rgb("#090E17"), 2);
            text(c, digits.substring(i,i+1), x+28, 121, 61, Color.WHITE, true, true, false);
        }
        text(c, ":", 180, 116, 48, rgb("#FACC15"), true, false, false);
        footer(c, rgb("#E5E7EB"));
    }

    private void racing(Canvas c) {
        gradient(c, "#070B12", "#29221D");
        box(c, -50, 20, 95, 240, 0, rgb("#9A3412"));
        header(c, "⚡   RACING HUD / PIT STOP", rgb("#FDBA74"));
        text(c, time(), 181, 126, 79, Color.WHITE, true, false, true);
        footer(c, rgb("#FED7AA"));
        for(int i=0;i<16;i++) {
            int color = i / 16f < progress() ? accent(rgb("#F97316")) : rgb("#334155");
            box(c, 44 + i*17, 145, 12, 8, 1, color);
        }
    }

    private void orbit(Canvas c) {
        gradient(c, "#080C20", "#242C60");
        header(c, "✧   COSMIC ORBIT", rgb("#C4B5FD"));
        for (int i=0;i<20;i++) {
            float x=(i*67)%345+8, y=(i*43)%145+29;
            circle(c,x,y,i%3==0?1.5f:0.8f,rgb("#A5B4FC"));
        }
        ring(c,180,101,69,rgb("#313A67"),accent(rgb("#A78BFA")),9);
        circle(c,180,101,61,rgb("#101934"));
        text(c,time(),180,111,32,Color.WHITE,true,false,false);
        text(c,"REMAINING",180,136,9,rgb("#C4B5FD"),true,false,false);
        footer(c,rgb("#DDD6FE"));
    }

    private void sunset(Canvas c) {
        gradient(c,"#56235E","#E7805D");
        circle(c,180,100,87,rgb("#FFBE80"));
        circle(c,180,100,71,rgb("#FFD8A0"));
        header(c,"☀   SUNSET GLOW",rgb("#FFF7ED"));
        text(c,time(),180,120,70,Color.WHITE,true,false,false);
        footer(c,rgb("#FFF7ED"));
        box(c,127,139,106,20,12,rgb("#85445C"));
        text(c,"GOLDEN HOUR",180,153,10,Color.WHITE,true,false,false);
    }

    private void pixel(Canvas c) {
        fill(c,rgb("#0B211B"));
        for (int i=0;i<360;i+=18) line(c,i,0,i,202,rgb("#163D2E"),1);
        for (int i=0;i<202;i+=18) line(c,0,i,360,i,rgb("#163D2E"),1);
        header(c,"▣   PIXEL ARCADE",rgb("#86EFAC"));
        box(c,33,49,294,121,0,rgb("#09291D"));
        outline(c,33,49,294,121,0,rgb("#4ADE80"),3);
        text(c,"BREAK.EXE",180,75,13,rgb("#86EFAC"),true,true,false);
        text(c,time(),180,128,56,rgb("#BBF7D0"),true,true,false);
        footer(c,rgb("#BBF7D0"));
        for(int i=0;i<18;i++)box(c,62+i*13,144,10,6,0,
            i/18f < progress() ? rgb("#4ADE80") : rgb("#14532D"));
    }

    private void zen(Canvas c) {
        fill(c,rgb("#FAF6ED"));
        header(c,"ZEN GARDEN",rgb("#63735E"));
        line(c,153,54,207,54,rgb("#92A08A"),2);
        text(c,"SLOW DOWN",180,80,13,rgb("#7F9679"),false,false,false);
        text(c,time(),180,135,70,rgb("#384D3D"),false,false,false);
        footer(c,rgb("#657466"));
        circle(c,180,154,3,rgb("#92A08A"));
    }

    private void ocean(Canvas c) {
        gradient(c,"#07172C","#08738F");
        header(c,"≋   OCEAN DEPTHS",rgb("#67E8F9"));
        box(c,63,56,234,97,48,rgb("#096B88"));
        outline(c,63,56,234,97,48,rgb("#67E8F9"),2);
        circle(c,89,92,6,rgb("#35B8D7"));
        circle(c,273,112,4,rgb("#22D3EE"));
        text(c,time(),180,124,60,Color.WHITE,true,false,false);
        footer(c,rgb("#BAE6FD"));
    }

    private void blueprint(Canvas c) {
        fill(c,rgb("#16325A"));
        for(int x=0;x<360;x+=22)line(c,x,0,x,202,rgb("#2A4B75"),1);
        for(int y=0;y<202;y+=22)line(c,0,y,360,y,rgb("#2A4B75"),1);
        header(c,"⌗   BLUEPRINT",rgb("#BFDBFE"));
        box(c,35,49,290,119,0,rgb("#0E2B50"));
        outline(c,35,49,290,119,0,rgb("#93C5FD"),1.5f);
        text(c,"TIMER / ACTIVE",180,75,11,rgb("#93C5FD"),true,true,false);
        text(c,time(),180,130,61,rgb("#E0F2FE"),true,true,false);
        footer(c,rgb("#DBEAFE"));
    }
}

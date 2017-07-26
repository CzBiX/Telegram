/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.telegram.ui.Components.TypefaceSpan;

public class Emoji {
    private static boolean inited = false;
    public static final Typeface typeface;

    static {
        typeface = AndroidUtilities.getTypeface("fonts/NotoColorEmoji.ttf");
    }

    public static void invalidateAll(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) {
                invalidateAll(g.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            view.invalidate();
        }
    }

    public static String fixEmoji(String emoji) {
        char ch;
        int lenght = emoji.length();
        for (int a = 0; a < lenght; a++) {
            ch = emoji.charAt(a);
            if (ch >= 0xD83C && ch <= 0xD83E) {
                if (ch == 0xD83C && a < lenght - 1) {
                    ch = emoji.charAt(a + 1);
                    if (ch == 0xDE2F || ch == 0xDC04 || ch == 0xDE1A || ch == 0xDD7F) {
                        emoji = emoji.substring(0, a + 2) + "\uFE0F" + emoji.substring(a + 2);
                        lenght++;
                        a += 2;
                    } else {
                        a++;
                    }
                } else {
                    a++;
                }
            } else if (ch == 0x20E3) {
                return emoji;
            } else if (ch >= 0x203C && ch <= 0x3299) {
                if (EmojiData.emojiToFE0FMap.containsKey(ch)) {
                    emoji = emoji.substring(0, a + 1) + "\uFE0F" + emoji.substring(a + 1);
                    lenght++;
                    a++;
                }
            }
        }
        return emoji;
    }

    private static boolean inArray(char c, char[] a) {
        for (char cc : a) {
            if (cc == c) {
                return true;
            }
        }
        return false;
    }

    public static CharSequence replaceEmoji(CharSequence text) {
        return replaceEmoji(text, null, 0, false);
    }

    public static CharSequence replaceEmoji(CharSequence cs, Paint.FontMetricsInt fontMetrics, int size, boolean createNew) {
        return replaceEmoji(cs, fontMetrics, size, createNew, null);
    }

    public static CharSequence replaceEmoji(CharSequence cs, Paint.FontMetricsInt fontMetrics, int size, boolean createNew, int[] emojiOnly) {
        if (MessagesController.getInstance().useSystemEmoji || cs == null || cs.length() == 0) {
            return cs;
        }
        //String str = "\"\uD83D\uDC68\uD83C\uDFFB\u200D\uD83C\uDFA4\""
        //SpannableStringLight.isFieldsAvailable();
        //SpannableStringLight s = new SpannableStringLight(cs.toString());
        Spannable s;
        if (!createNew && cs instanceof Spannable) {
            s = (Spannable) cs;
        } else {
            s = Spannable.Factory.getInstance().newSpannable(cs.toString());
        }
        long buf = 0;
        int emojiCount = 0;
        char c;
        int startIndex = -1;
        int startLength = 0;
        int previousGoodIndex = 0;
        StringBuilder emojiCode = new StringBuilder(16);
        StringBuilder addionalCode = new StringBuilder(2);
        boolean nextIsSkinTone;
        TypefaceSpan span;
        int length = cs.length();
        boolean doneEmoji = false;
        int nextValidLength;
        boolean nextValid;
        //s.setSpansCount(emojiCount);

        try {
            for (int i = 0; i < length; i++) {
                c = cs.charAt(i);
                if (c >= 0xD83C && c <= 0xD83E || (buf != 0 && (buf & 0xFFFFFFFF00000000L) == 0 && (buf & 0xFFFF) == 0xD83C && (c >= 0xDDE6 && c <= 0xDDFF))) {
                    if (startIndex == -1) {
                        startIndex = i;
                    }
                    emojiCode.append(c);
                    startLength++;
                    buf <<= 16;
                    buf |= c;
                } else if (emojiCode.length() > 0 && (c == 0x2640 || c == 0x2642 || c == 0x2695)) {
                    emojiCode.append(c);
                    startLength++;
                    buf = 0;
                    doneEmoji = true;
                } else if (buf > 0 && (c & 0xF000) == 0xD000) {
                    emojiCode.append(c);
                    startLength++;
                    buf = 0;
                    doneEmoji = true;
                } else if (c == 0x20E3) {
                    if (i > 0) {
                        char c2 = cs.charAt(previousGoodIndex);
                        if ((c2 >= '0' && c2 <= '9') || c2 == '#' || c2 == '*') {
                            startIndex = previousGoodIndex;
                            startLength = i - previousGoodIndex + 1;
                            emojiCode.append(c2);
                            emojiCode.append(c);
                            doneEmoji = true;
                        }
                    }
                } else if ((c == 0x00A9 || c == 0x00AE || c >= 0x203C && c <= 0x3299) && EmojiData.dataCharsMap.containsKey(c)) {
                    if (startIndex == -1) {
                        startIndex = i;
                    }
                    startLength++;
                    emojiCode.append(c);
                    doneEmoji = true;
                } else if (startIndex != -1) {
                    emojiCode.setLength(0);
                    startIndex = -1;
                    startLength = 0;
                    doneEmoji = false;
                } else if (c != 0xfe0f) {
                    if (emojiOnly != null) {
                        emojiOnly[0] = 0;
                        emojiOnly = null;
                    }
                }
                if (doneEmoji && i + 2 < length && cs.charAt(i + 1) == 0xD83C) {
                    char next = cs.charAt(i + 2);
                    if (next >= 0xDFFB && next <= 0xDFFF) {
                        emojiCode.append(cs.subSequence(i + 1, i + 3));
                        startLength += 2;
                        i += 2;
                    }
                }
                previousGoodIndex = i;
                for (int a = 0; a < 3; a++) {
                    if (i + 1 < length) {
                        c = cs.charAt(i + 1);
                        if (a == 1) {
                            if (c == 0x200D && emojiCode.length() > 0) {
                                emojiCode.append(c);
                                i++;
                                startLength++;
                                doneEmoji = false;
                            }
                        } else {
                            if (c >= 0xFE00 && c <= 0xFE0F) {
                                i++;
                                startLength++;
                            }
                        }
                    }
                }
                if (doneEmoji && i + 2 < length && cs.charAt(i + 1) == 0xD83C) {
                    char next = cs.charAt(i + 2);
                    if (next >= 0xDFFB && next <= 0xDFFF) {
                        emojiCode.append(cs.subSequence(i + 1, i + 3));
                        startLength += 2;
                        i += 2;
                    }
                }
                if (doneEmoji) {
                    if (emojiOnly != null) {
                        emojiOnly[0]++;
                    }

                    span = new TypefaceSpan(typeface, size);
                    s.setSpan(span, startIndex, startIndex + startLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    emojiCount++;

                    startLength = 0;
                    startIndex = -1;
                    emojiCode.setLength(0);
                    doneEmoji = false;
                }
                if (Build.VERSION.SDK_INT < 23 && emojiCount >= 50) {
                    break;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            return cs;
        }
        return s;
    }

    public static class EmojiSpan extends ImageSpan {
        private Paint.FontMetricsInt fontMetrics = null;
        private int size = AndroidUtilities.dp(20);

        public EmojiSpan(Drawable d, int verticalAlignment, int s, Paint.FontMetricsInt original) {
            super(d, verticalAlignment);
            fontMetrics = original;
            if (original != null) {
                size = Math.abs(fontMetrics.descent) + Math.abs(fontMetrics.ascent);
                if (size == 0) {
                    size = AndroidUtilities.dp(20);
                }
            }
        }

        public void replaceFontMetrics(Paint.FontMetricsInt newMetrics, int newSize) {
            fontMetrics = newMetrics;
            size = newSize;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm == null) {
                fm = new Paint.FontMetricsInt();
            }

            if (fontMetrics == null) {
                int sz = super.getSize(paint, text, start, end, fm);

                int offset = AndroidUtilities.dp(8);
                int w = AndroidUtilities.dp(10);
                fm.top = -w - offset;
                fm.bottom = w - offset;
                fm.ascent = -w - offset;
                fm.leading = 0;
                fm.descent = w - offset;

                return sz;
            } else {
                if (fm != null) {
                    fm.ascent = fontMetrics.ascent;
                    fm.descent = fontMetrics.descent;

                    fm.top = fontMetrics.top;
                    fm.bottom = fontMetrics.bottom;
                }
                if (getDrawable() != null) {
                    getDrawable().setBounds(0, 0, size, size);
                }
                return size;
            }
        }
    }
}

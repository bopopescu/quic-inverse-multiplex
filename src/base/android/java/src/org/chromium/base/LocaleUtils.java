// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.base;

import android.annotation.TargetApi;
import android.os.Build;

import org.chromium.base.annotations.CalledByNative;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class provides the locale related methods.
 */
public class LocaleUtils {
    /**
     * Guards this class from being instantiated.
     */
    private LocaleUtils() {
    }

    private static final Map<String, String> LANGUAGE_MAP_FOR_CHROMIUM;
    private static final Map<String, String> LANGUAGE_MAP_FOR_ANDROID;

    static {
        HashMap<String, String> mapForChromium = new HashMap<>();
        mapForChromium.put("iw", "he"); // Hebrew
        mapForChromium.put("ji", "yi"); // Yiddish
        mapForChromium.put("in", "id"); // Indonesian
        mapForChromium.put("tl", "fil"); // Filipino
        LANGUAGE_MAP_FOR_CHROMIUM = Collections.unmodifiableMap(mapForChromium);
    }

    static {
        HashMap<String, String> mapForAndroid = new HashMap<>();
        mapForAndroid.put("und", ""); // Undefined
        mapForAndroid.put("fil", "tl"); // Filipino
        LANGUAGE_MAP_FOR_ANDROID = Collections.unmodifiableMap(mapForAndroid);
    }

    /**
     * Java keeps deprecated language codes for Hebrew, Yiddish and Indonesian but Chromium uses
     * updated ones. Similarly, Android uses "tl" while Chromium uses "fil" for Tagalog/Filipino.
     * So apply a mapping here.
     * See http://developer.android.com/reference/java/util/Locale.html
     * @return a updated language code for Chromium with given language string.
     */
    public static String getUpdatedLanguageForChromium(String language) {
        String updatedLanguageCode = LANGUAGE_MAP_FOR_CHROMIUM.get(language);
        return updatedLanguageCode == null ? language : updatedLanguageCode;
    }

    /**
     * @return a locale with updated language codes for Chromium, with translated modern language
     *         codes used by Chromium.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Locale getUpdatedLocaleForChromium(Locale locale) {
        String languageForChrome = LANGUAGE_MAP_FOR_CHROMIUM.get(locale.getLanguage());
        if (languageForChrome == null) {
            return locale;
        }
        return new Locale.Builder().setLocale(locale).setLanguage(languageForChrome).build();
    }

    /**
     * Android uses "tl" while Chromium uses "fil" for Tagalog/Filipino.
     * So apply a mapping here.
     * See http://developer.android.com/reference/java/util/Locale.html
     * @return a updated language code for Android with given language string.
     */
    public static String getUpdatedLanguageForAndroid(String language) {
        String updatedLanguageCode = LANGUAGE_MAP_FOR_ANDROID.get(language);
        return updatedLanguageCode == null ? language : updatedLanguageCode;
    }

    /**
     * @return a locale with updated language codes for Android, from translated modern language
     *         codes used by Chromium.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Locale getUpdatedLocaleForAndroid(Locale locale) {
        String languageForAndroid = LANGUAGE_MAP_FOR_ANDROID.get(locale.getLanguage());
        if (languageForAndroid == null) {
            return locale;
        }
        return new Locale.Builder().setLocale(locale).setLanguage(languageForAndroid).build();
    }

    /**
     * This function creates Locale object from xx-XX style string where xx is language code
     * and XX is a country code. This works for API level lower than 21.
     * @return the locale that best represents the language tag.
     */
    public static Locale forLanguageTagCompat(String languageTag) {
        String[] tag = languageTag.split("-");
        if (tag.length == 0) {
            return new Locale("");
        }
        String language = getUpdatedLanguageForAndroid(tag[0]);
        if ((language.length() != 2 && language.length() != 3) || language.equals("und")) {
            return new Locale("");
        }
        if (tag.length == 1) {
            return new Locale(language);
        }
        String country = tag[1];
        if (country.length() != 2 && country.length() != 3) {
            return new Locale(language);
        }
        return new Locale(language, country);
    }

    /**
     * This function creates Locale object from xx-XX style string where xx is language code
     * and XX is a country code.
     * @return the locale that best represents the language tag.
     */
    public static Locale forLanguageTag(String languageTag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Locale locale = Locale.forLanguageTag(languageTag);
            return getUpdatedLocaleForAndroid(locale);
        }
        return forLanguageTagCompat(languageTag);
    }

    /**
     * Converts Locale object to the BCP 47 compliant string format.
     * This works for API level lower than 24.
     * @return a well-formed IETF BCP 47 language tag with language and country code that
     *         represents this locale.
     */
    public static String toLanguageTagCompat(Locale locale) {
        String language = getUpdatedLanguageForChromium(locale.getLanguage());
        String country = locale.getCountry();
        if (language.equals("no") && country.equals("NO") && locale.getVariant().equals("NY")) {
            return "nn-NO";
        }
        return country.isEmpty() ? language : language + "-" + country;
    }

    /**
     * Converts Locale object to the BCP 47 compliant string format.
     *
     * Note that for Android M or before, we cannot use Locale.getLanguage() and
     * Locale.toLanguageTag() for this purpose. Since Locale.getLanguage() returns deprecated
     * language code even if the Locale object is constructed with updated language code. As for
     * Locale.toLanguageTag(), it does a special conversion from deprecated language code to updated
     * one, but it is only usable for Android N or after.
     * @return a well-formed IETF BCP 47 language tag with language and country code that
     *         represents this locale.
     */
    public static String toLanguageTag(Locale locale) {
        // TODO(yirui): use '>= N' once SDK is updated to include the version code
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            locale = getUpdatedLocaleForChromium(locale);
            return locale.toLanguageTag();
        }
        return toLanguageTagCompat(locale);
    }

    /**
     * @return a well-formed IETF BCP 47 language tag with language and country code that
     *         represents a default locale.
     */
    @CalledByNative
    public static String getDefaultLocaleString() {
        Locale locale = Locale.getDefault();
        return toLanguageTag(locale);
    }

    /**
     * @return The default country code set during install.
     */
    @CalledByNative
    private static String getDefaultCountryCode() {
        CommandLine commandLine = CommandLine.getInstance();
        return commandLine.hasSwitch(BaseSwitches.DEFAULT_COUNTRY_CODE_AT_INSTALL)
                ? commandLine.getSwitchValue(BaseSwitches.DEFAULT_COUNTRY_CODE_AT_INSTALL)
                : Locale.getDefault().getCountry();
    }

}

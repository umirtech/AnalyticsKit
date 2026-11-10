package com.umirtech.analyticskit

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AnalyticsKit"
private const val MAX_PARAM_CHARS = 100
private val EVENT_NAME = Regex("[A-Za-z][A-Za-z0-9_]{0,39}")

/**
 * Firebase Analytics wrapper with deny-first consent mode, reusable across apps.
 *
 * Contract:
 * - [init] once from `Application.onCreate` (call [applyConsentDefaults] right after).
 * - Collection is **deny-first**: analytics and ad storage start DENIED; call
 *   [setConsent] when your UMP/consent flow resolves.
 * - [logEvent] accepts only capped, sanitized string params; callers are
 *   responsible for never passing raw user content (paths, commands, files).
 * - Every method silently no-ops when Firebase is not configured
 *   (no `google-services.json` / no [FirebaseApp]).
 */
object AnalyticsKit {

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var enabled: Boolean = true

    private val _initialized = MutableStateFlow(false)

    /** True once [init] has run (regardless of Firebase availability). */
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    fun init(context: Context, enabled: Boolean = true) {
        appContext = context.applicationContext
        this.enabled = enabled
        _initialized.value = true
    }

    /** Deny-first defaults; safe to call before/without consent. */
    fun applyConsentDefaults() {
        val fa = instance() ?: return
        runCatching {
            fa.setConsent(
                mapOf(
                    FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
                        FirebaseAnalytics.ConsentStatus.DENIED,
                    FirebaseAnalytics.ConsentType.AD_STORAGE to
                        FirebaseAnalytics.ConsentStatus.DENIED,
                ),
            )
            fa.setAnalyticsCollectionEnabled(false)
        }.onFailure { Log.w(TAG, "consent defaults failed", it) }
    }

    /**
     * Applies the user's consent choice.
     * [analyticsStorage] false keeps the deny-first state (cookieless pings only).
     * [adStorage] stays under the ad SDK's control unless explicitly granted.
     */
    fun setConsent(analyticsStorage: Boolean, adStorage: Boolean = false) {
        val fa = instance() ?: return
        runCatching {
            if (analyticsStorage) {
                fa.setAnalyticsCollectionEnabled(true)
                fa.setConsent(
                    mapOf(
                        FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
                            FirebaseAnalytics.ConsentStatus.GRANTED,
                        FirebaseAnalytics.ConsentType.AD_STORAGE to
                            consentStatus(adStorage),
                    ),
                )
            } else {
                fa.setAnalyticsCollectionEnabled(false)
            }
        }.onFailure { Log.w(TAG, "setConsent failed", it) }
    }

    /**
     * Logs a sanitized event. Blank/invalid string params are dropped; keys
     * and values are capped at [MAX_PARAM_CHARS] characters.
     *
     * [numericParams] are logged as numeric Bundle values so they can be
     * registered as GA4 custom metrics (for example `duration_ms`). Callers
     * must still only pass aggregate numbers, never user content.
     */
    fun logEvent(
        name: String,
        params: Map<String, String> = emptyMap(),
        numericParams: Map<String, Long> = emptyMap(),
    ) {
        val fa = instance() ?: return
        if (!EVENT_NAME.matches(name)) {
            Log.w(TAG, "event name rejected")
            return
        }
        runCatching {
            fa.logEvent(name, android.os.Bundle().apply {
                sanitizeParams(params).forEach { (key, value) -> putString(key, value) }
                sanitizeNumericParams(numericParams).forEach { (key, value) -> putLong(key, value) }
            })
        }.onFailure { Log.w(TAG, "logEvent failed", it) }
    }

    /** Convenience screen view; omit for screens whose name is user content. */
    fun logScreenView(screenName: String) {
        logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName),
        )
    }

    internal fun sanitizeParams(params: Map<String, String>): Map<String, String> =
        params
            .filterKeys { it.isNotBlank() && it.length <= MAX_PARAM_CHARS }
            .mapValues { (_, value) ->
                if (value.length <= MAX_PARAM_CHARS) value else value.take(MAX_PARAM_CHARS)
            }
            .filterValues { it.isNotBlank() }

    internal fun sanitizeNumericParams(params: Map<String, Long>): Map<String, Long> =
        params.filterKeys { it.isNotBlank() && it.length <= MAX_PARAM_CHARS }

    private fun consentStatus(granted: Boolean): FirebaseAnalytics.ConsentStatus =
        if (granted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED

    private fun instance(): FirebaseAnalytics? {
        if (!enabled) return null
        val ctx = appContext ?: return null
        return runCatching {
            if (FirebaseApp.getApps(ctx).isEmpty()) return null
            FirebaseAnalytics.getInstance(ctx)
        }.getOrNull()
    }
}

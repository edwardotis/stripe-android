package com.stripe.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Source
import com.stripe.android.model.Token
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent
import com.stripe.android.utils.ContextUtils.packageInfo

/**
 * Util class to create logging items, which are fed as [Map][java.util.Map] objects in
 * query parameters to our server.
 */
internal class AnalyticsDataFactory @VisibleForTesting internal constructor(
    private val packageManager: PackageManager?,
    private val packageInfo: PackageInfo?,
    private val packageName: String,
    private val publishableKey: String
) {

    internal constructor(context: Context, publishableKey: String) : this(
        context.applicationContext.packageManager,
        context.applicationContext.packageInfo,
        context.applicationContext.packageName.orEmpty(),
        publishableKey
    )

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ThreeDS2UiType.NONE, ThreeDS2UiType.TEXT, ThreeDS2UiType.SINGLE_SELECT,
        ThreeDS2UiType.MULTI_SELECT, ThreeDS2UiType.OOB, ThreeDS2UiType.HTML)
    private annotation class ThreeDS2UiType {
        companion object {
            const val NONE = "none"
            const val TEXT = "text"
            const val SINGLE_SELECT = "single_select"
            const val MULTI_SELECT = "multi_select"
            const val OOB = "oob"
            const val HTML = "html"
        }
    }

    @JvmSynthetic
    internal fun createAuthParams(
        event: AnalyticsEvent,
        intentId: String
    ): Map<String, Any> {
        return createParams(
            event,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createAuthSourceParams(
        event: AnalyticsEvent,
        sourceId: String?
    ): Map<String, Any> {
        return createParams(
            event,
            extraParams = sourceId?.let { mapOf(FIELD_SOURCE_ID to it) }
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeParams(
        event: AnalyticsEvent,
        intentId: String,
        uiTypeCode: String
    ): Map<String, Any> {
        return createParams(
            event,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_3DS2_UI_TYPE to get3ds2UiType(uiTypeCode))
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeErrorParams(
        intentId: String,
        runtimeErrorEvent: RuntimeErrorEvent
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.Auth3ds2ChallengeErrored,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_ERROR_DATA to mapOf(
                    "type" to "runtime_error_event",
                    "error_code" to runtimeErrorEvent.errorCode,
                    "error_message" to runtimeErrorEvent.errorMessage
                ))
        )
    }

    @JvmSynthetic
    internal fun create3ds2ChallengeErrorParams(
        intentId: String,
        protocolErrorEvent: ProtocolErrorEvent
    ): Map<String, Any> {
        val errorMessage = protocolErrorEvent.errorMessage
        val errorData = mapOf(
            "type" to "protocol_error_event",
            "sdk_trans_id" to protocolErrorEvent.sdkTransactionId,
            "error_code" to errorMessage.errorCode,
            "error_description" to errorMessage.errorDescription,
            "error_details" to errorMessage.errorDetails,
            "trans_id" to errorMessage.transactionId
        )

        return createParams(
            AnalyticsEvent.Auth3ds2ChallengeErrored,
            extraParams = createIntentParam(intentId)
                .plus(FIELD_ERROR_DATA to errorData)
        )
    }

    @JvmSynthetic
    internal fun createTokenCreationParams(
        productUsageTokens: Set<String>?,
        @Token.TokenType tokenType: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.TokenCreate,
            productUsageTokens = productUsageTokens,
            tokenType = tokenType
        )
    }

    @JvmSynthetic
    internal fun createPaymentMethodCreationParams(
        paymentMethodId: String?,
        paymentMethodType: PaymentMethod.Type?,
        productUsageTokens: Set<String>?
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.PaymentMethodCreate,
            sourceType = paymentMethodType?.code,
            productUsageTokens = productUsageTokens,
            extraParams = paymentMethodId?.let {
                mapOf(FIELD_PAYMENT_METHOD_ID to it)
            }
        )
    }

    @JvmSynthetic
    internal fun createSourceCreationParams(
        @Source.SourceType sourceType: String,
        productUsageTokens: Set<String>? = null
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SourceCreate,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createSourceRetrieveParams(sourceId: String): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SourceRetrieve,
            extraParams = mapOf(FIELD_SOURCE_ID to sourceId)
        )
    }

    @JvmSynthetic
    internal fun createAddSourceParams(
        productUsageTokens: Set<String>? = null,
        @Source.SourceType sourceType: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerAddSource,
            productUsageTokens = productUsageTokens,
            sourceType = sourceType
        )
    }

    @JvmSynthetic
    internal fun createDeleteSourceParams(
        productUsageTokens: Set<String>?
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerDeleteSource,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createAttachPaymentMethodParams(
        productUsageTokens: Set<String>?
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerAttachPaymentMethod,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createDetachPaymentMethodParams(
        productUsageTokens: Set<String>?
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.CustomerDetachPaymentMethod,
            productUsageTokens = productUsageTokens
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentConfirmationParams(
        paymentMethodType: String? = null
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.PaymentIntentConfirm,
            sourceType = paymentMethodType
        )
    }

    @JvmSynthetic
    internal fun createPaymentIntentRetrieveParams(
        intentId: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.PaymentIntentRetrieve,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentConfirmationParams(
        paymentMethodType: String?,
        intentId: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SetupIntentConfirm,
            extraParams = createIntentParam(intentId)
                .plus(
                    paymentMethodType?.let {
                        mapOf(FIELD_PAYMENT_METHOD_TYPE to it)
                    }.orEmpty()
                )
        )
    }

    @JvmSynthetic
    internal fun createSetupIntentRetrieveParams(
        intentId: String
    ): Map<String, Any> {
        return createParams(
            AnalyticsEvent.SetupIntentRetrieve,
            extraParams = createIntentParam(intentId)
        )
    }

    @JvmSynthetic
    internal fun createParams(
        event: AnalyticsEvent,
        productUsageTokens: Set<String>? = null,
        @Source.SourceType sourceType: String? = null,
        @Token.TokenType tokenType: String? = null,
        extraParams: Map<String, Any>? = null
    ): Map<String, Any> {
        return createStandardParams(event)
            .plus(createAppDataParams())
            .plus(
                productUsageTokens.takeUnless { it.isNullOrEmpty() }?.let {
                    mapOf(FIELD_PRODUCT_USAGE to it.toList())
                }.orEmpty()
            )
            .plus(sourceType?.let { mapOf(FIELD_SOURCE_TYPE to it) }.orEmpty())
            .plus(createTokenTypeParam(sourceType, tokenType))
            .plus(extraParams.orEmpty())
    }

    private fun createTokenTypeParam(
        @Source.SourceType sourceType: String? = null,
        @Token.TokenType tokenType: String? = null
    ): Map<String, String> {
        val value = when {
            tokenType != null -> tokenType
            // This is not a source event, so to match iOS we log a token without type
            // as type "unknown"
            sourceType == null -> "unknown"
            else -> null
        }

        return value?.let {
            mapOf(FIELD_TOKEN_TYPE to it)
        }.orEmpty()
    }

    private fun createStandardParams(
        event: AnalyticsEvent
    ): Map<String, Any> {
        return mapOf(
            FIELD_ANALYTICS_UA to ANALYTICS_UA,
            FIELD_EVENT to event.toString(),
            FIELD_PUBLISHABLE_KEY to publishableKey,
            FIELD_OS_NAME to Build.VERSION.CODENAME,
            FIELD_OS_RELEASE to Build.VERSION.RELEASE,
            FIELD_OS_VERSION to Build.VERSION.SDK_INT,
            FIELD_DEVICE_TYPE to DEVICE_TYPE,
            FIELD_BINDINGS_VERSION to BuildConfig.VERSION_NAME
        )
    }

    internal fun createAppDataParams(): Map<String, Any> {
        return when {
            packageManager != null && packageInfo != null -> {
                mapOf(
                    FIELD_APP_NAME to getAppName(packageInfo, packageManager),
                    FIELD_APP_VERSION to packageInfo.versionCode
                )
            }
            else -> emptyMap()
        }
    }

    private fun getAppName(
        packageInfo: PackageInfo?,
        packageManager: PackageManager
    ): CharSequence {
        return packageInfo?.applicationInfo?.loadLabel(packageManager).takeUnless {
            it.isNullOrBlank()
        } ?: packageName
    }

    internal companion object {
        internal const val FIELD_PRODUCT_USAGE = "product_usage"
        internal const val FIELD_ANALYTICS_UA = "analytics_ua"
        internal const val FIELD_APP_NAME = "app_name"
        internal const val FIELD_APP_VERSION = "app_version"
        internal const val FIELD_BINDINGS_VERSION = "bindings_version"
        internal const val FIELD_DEVICE_TYPE = "device_type"
        internal const val FIELD_EVENT = "event"
        internal const val FIELD_ERROR_DATA = "error"
        internal const val FIELD_INTENT_ID = "intent_id"
        internal const val FIELD_OS_NAME = "os_name"
        internal const val FIELD_OS_RELEASE = "os_release"
        internal const val FIELD_OS_VERSION = "os_version"
        internal const val FIELD_PAYMENT_METHOD_ID = "payment_method_id"
        internal const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        internal const val FIELD_PUBLISHABLE_KEY = "publishable_key"
        internal const val FIELD_SOURCE_ID = "source_id"
        internal const val FIELD_SOURCE_TYPE = "source_type"
        internal const val FIELD_3DS2_UI_TYPE = "3ds2_ui_type"
        internal const val FIELD_TOKEN_TYPE = "token_type"

        @JvmSynthetic
        internal val VALID_PARAM_FIELDS: Set<String> = setOf(
            FIELD_ANALYTICS_UA, FIELD_APP_NAME, FIELD_APP_VERSION, FIELD_BINDINGS_VERSION,
            FIELD_DEVICE_TYPE, FIELD_EVENT, FIELD_OS_VERSION, FIELD_OS_NAME, FIELD_OS_RELEASE,
            FIELD_PRODUCT_USAGE, FIELD_PUBLISHABLE_KEY, FIELD_SOURCE_TYPE, FIELD_TOKEN_TYPE
        )

        private const val ANALYTICS_PREFIX = "analytics"
        private const val ANALYTICS_NAME = "stripe_android"
        private const val ANALYTICS_VERSION = "1.0"

        private val DEVICE_TYPE: String = "${Build.MANUFACTURER}_${Build.BRAND}_${Build.MODEL}"

        internal const val ANALYTICS_UA = "$ANALYTICS_PREFIX.$ANALYTICS_NAME-$ANALYTICS_VERSION"

        @ThreeDS2UiType
        private fun get3ds2UiType(uiTypeCode: String): String {
            return when (uiTypeCode) {
                "01" -> ThreeDS2UiType.TEXT
                "02" -> ThreeDS2UiType.SINGLE_SELECT
                "03" -> ThreeDS2UiType.MULTI_SELECT
                "04" -> ThreeDS2UiType.OOB
                "05" -> ThreeDS2UiType.HTML
                else -> ThreeDS2UiType.NONE
            }
        }

        private fun createIntentParam(intentId: String): Map<String, String> {
            return mapOf(
                FIELD_INTENT_ID to intentId
            )
        }
    }
}

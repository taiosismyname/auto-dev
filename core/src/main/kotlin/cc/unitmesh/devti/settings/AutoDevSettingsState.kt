package cc.unitmesh.devti.settings

import cc.unitmesh.devti.settings.locale.DEFAULT_HUMAN_LANGUAGE
import cc.unitmesh.devti.settings.locale.HUMAN_LANGUAGES
import cc.unitmesh.devti.settings.miscs.MAX_TOKEN_LENGTH
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "cc.unitmesh.devti.settings.DevtiSettingsState", storages = [Storage("DevtiSettings.xml")])
class AutoDevSettingsState : PersistentStateComponent<AutoDevSettingsState> {
    var delaySeconds = ""

    var customOpenAiHost = ""
    var customEngineServer = ""
    var customEngineToken = ""
    var customModel = ""

    var customEngineResponseFormat = "\$.choices[0].delta.content"
    /**
     * should be a json
     * {
     *     'customHeaders': { 'headerName': 'headerValue', 'headerName2': 'headerValue2' ... },
     *     'customFields' : { 'bodyFieldName': 'bodyFieldValue', 'bodyFieldName2': 'bodyFieldValue2' ... }
     *     'messageKey': {'role': 'roleKeyName', 'content': 'contentKeyName'}
     * }
     *
     * @see docs/custom-llm-server.md
     */
    var customEngineRequestFormat = """{ "customFields": {"model": "deepseek-chat", "temperature": 0.0, "stream": true} }"""

    var language = DEFAULT_HUMAN_LANGUAGE
    var maxTokenLength = MAX_TOKEN_LENGTH.toString()

    fun fetchMaxTokenLength(): Int = maxTokenLength.toIntOrNull() ?: MAX_TOKEN_LENGTH

    fun fetchLocalLanguage(display: String = language) : String {
        return HUMAN_LANGUAGES.getAbbrByDispay(display)
    }

    @Synchronized
    override fun getState(): AutoDevSettingsState = this

    @Synchronized
    override fun loadState(state: AutoDevSettingsState) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val maxTokenLength: Int get() = getInstance().fetchMaxTokenLength()
        val language: String get() = getInstance().fetchLocalLanguage()

        fun getInstance(): AutoDevSettingsState {
            return ApplicationManager.getApplication().getService(AutoDevSettingsState::class.java).state
        }
    }
}

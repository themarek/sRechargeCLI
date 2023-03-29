import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

object SRecharge {
    val client = HttpClient(Java) {
        install(Logging) {
            level = LogLevel.NONE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5*1000
        }
        BrowserUserAgent()
        engine {
            config{
                version(java.net.http.HttpClient.Version.HTTP_2)
            }
        }
    }

    var apiCookie = ""

    suspend fun getInformation() =
        Json { ignoreUnknownKeys = true }.decodeFromString<Information>(
            client.get("https://ui-chargepoints.shellrecharge.com/api/facade/v1/me/asset-overview") {
                headers {
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append(HttpHeaders.Cookie, "language.code.selection=en; tnm_api=\"$apiCookie\"")
                }
            }.bodyAsText()
        )

    suspend fun startLoading(id: String, rfid: String) =
        sessionAction(id, "start", """{"rfid":"$rfid","evseNo":0}""")
    suspend fun stopLoading(id: String) =
        sessionAction(id, "stop", """{"evseNo":0}""")
    suspend fun resetStation(id: String) = sessionAction(id, "reset", "")
    suspend fun sessionAction(id: String, action: String, body: String): String {
        return client.post("https://ui-chargepoints.shellrecharge.com/api/facade/v1/charge-points/$id/remote-control/$action") {
            headers {
                append(HttpHeaders.ContentType, "application/json;charset=utf-8")
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Cookie, "language.code.selection=en; tnm_api=\"$apiCookie\"")
            }
            setBody(body)
        }.bodyAsText()
    }

    suspend fun getSessionId(userEmail: String, userPwd: String) {
        val firstResponse = client.get("https://account.shellrecharge.com/")
        val (formEmail, formPwd, formBool) = getFormFields(firstResponse.bodyAsText())
        var endpointid = firstResponse.bodyAsText().findText("lift_page", ".*var\\s*lift_page\\s*=\\s*\"(\\w+)\".*")

        for (i in 1..5) {
            println("Trying to get a session id $i. time")
            val responseLogin: HttpResponse = client.submitForm(
                url = "https://account.shellrecharge.com/ajax_request/$endpointid-00/",
                formParameters = Parameters.build {
                    append(formEmail, userEmail)
                    append(formPwd, userPwd)
                    append(formBool, "true")
                }
            ) {
                headers {
                    append(HttpHeaders.ContentType, "application/x-www-form-urlencoded; charset=UTF-8")
                    append(HttpHeaders.Cookie, "JSESSIONID=${firstResponse.getCookie("JSESSIONID")}")
                    append(
                        HttpHeaders.Accept,
                        "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript, */*; q=0.01"
                    )
                }
            }
            val cookie = responseLogin.getCookie("tnm_api")
            println("API Key $cookie")
            if (cookie.isNotEmpty()) {
                apiCookie = cookie
                return
            }
        }
    }

    private fun getFormFields(body: String): Triple<String, String, String> {
        val inputs = Jsoup.parse(body).select("input")
        val formEmail = inputs.select("#login-email")[0].attr("name")
        val formPwd = inputs.select("#login-pwd")[0].attr("name")
        val formBool = inputs.find { it.attr("type") == "hidden" && it.attr("value") == "true" }!!.attr("name")
        return Triple(formEmail, formPwd, formBool)
    }

    fun HttpResponse.getCookie(name: String): String {
        val cookies = this.headers.entries()
            .find{it.key=="set-cookie"}?.value?.filter { it.startsWith(name) }
        if (!cookies.isNullOrEmpty())
                return cookies[0].replace(Regex("$name=([^;]+);.+"), "$1")
        return ""
    }

    fun String.findText(textInLine: String, replace: String): String = this.split("\n")
        .find { it.indexOf(textInLine) >= 0 }!!
        .replace(Regex(replace), "$1")

}


@Serializable
data class Information(val chargePoints: Array<ChargePoint>, val chargeTokens: Array<ChargeToken>)

@Serializable
data class ChargePoint(val evses: Array<EVSE>? = null, val name: String, val serial: String, val uuid: String)

@Serializable
data class ChargeToken(val name: String, val printedNumber: String, val rfid: String, val uuid: String)

@Serializable
data class EVSE(val evseId: String, val number: Int, val occupyingToken: Token, val status: String)

@Serializable
data class Token(val printedNumber: String = "", val rfid: String = "", val timestamp: String = "")

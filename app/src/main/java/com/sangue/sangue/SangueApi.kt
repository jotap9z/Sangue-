package com.sangue.sangue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(
    val status: Int,
    mensagem: String
) : Exception(mensagem)

object SangueApi {

    var usuario by mutableStateOf<JSONObject?>(null)
        private set

    private var token: String? = null

    val autenticado: Boolean
        get() = usuario != null && token != null

    suspend fun entrar(
        email: String,
        senha: String
    ): JSONObject {
        val corpo = JSONObject()
            .put("email", email.trim())
            .put("senha", senha)

        val resposta = objeto(
            metodo = "POST",
            caminho = "/api/auth/login",
            corpo = corpo,
            autenticado = false
        )

        val novoToken = resposta.getString("token")
        val novoUsuario = resposta.getJSONObject("usuario")

        withContext(Dispatchers.Main) {
            token = novoToken
            usuario = novoUsuario
        }

        return novoUsuario
    }

    suspend fun atualizarUsuario(): JSONObject {
        val resposta = objeto(
            metodo = "GET",
            caminho = "/api/auth/me"
        )

        withContext(Dispatchers.Main) {
            usuario = resposta
        }

        return resposta
    }

    suspend fun sair() {
        try {
            objeto(
                metodo = "POST",
                caminho = "/api/auth/logout"
            )
        } finally {
            withContext(Dispatchers.Main) {
                limparSessao()
            }
        }
    }

    fun limparSessao() {
        token = null
        usuario = null
    }

    suspend fun objeto(
        metodo: String,
        caminho: String,
        corpo: JSONObject? = null,
        autenticado: Boolean = true
    ): JSONObject {
        val resposta = requisitar(
            metodo = metodo,
            caminho = caminho,
            corpo = corpo,
            autenticado = autenticado
        )

        return if (resposta.isBlank()) {
            JSONObject()
        } else {
            JSONObject(resposta)
        }
    }

    suspend fun lista(
        caminho: String,
        autenticado: Boolean = true
    ): List<JSONObject> {
        val resposta = requisitar(
            metodo = "GET",
            caminho = caminho,
            corpo = null,
            autenticado = autenticado
        )

        val array = JSONArray(resposta)

        return (0 until array.length()).map { indice ->
            array.getJSONObject(indice)
        }
    }

    private suspend fun requisitar(
        metodo: String,
        caminho: String,
        corpo: JSONObject?,
        autenticado: Boolean
    ): String = withContext(Dispatchers.IO) {
        require(caminho.startsWith("/api/")) {
            "Endereço de operação inválido."
        }

        require(metodo in setOf("GET", "POST", "PUT", "DELETE")) {
            "Método de operação inválido."
        }

        val tokenDaRequisicao = token

        if (autenticado && tokenDaRequisicao == null) {
            throw ApiException(
                401,
                "Entre na sua conta para continuar."
            )
        }

        val enderecoBase = ConfigApi.BASE_URL
            .trim()
            .trimEnd('/')

        val urlBase = URL(enderecoBase)

        require(
            urlBase.protocol in setOf("http", "https") &&
                    urlBase.host.isNotBlank()
        ) {
            "O endereço da API está inválido."
        }

        val conexao = URL(enderecoBase + caminho)
            .openConnection() as HttpURLConnection

        try {
            conexao.requestMethod = metodo
            conexao.connectTimeout = 15000
            conexao.readTimeout = 20000
            conexao.instanceFollowRedirects = false

            conexao.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (autenticado) {
                conexao.setRequestProperty(
                    "Authorization",
                    "Bearer $tokenDaRequisicao"
                )
            }

            if (corpo != null) {
                conexao.doOutput = true

                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                conexao.outputStream.use { stream ->
                    stream.write(
                        corpo.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }
            }

            val status = conexao.responseCode

            val stream = if (status in 200..299) {
                if (status == 204) {
                    null
                } else {
                    conexao.inputStream
                }
            } else {
                conexao.errorStream
            }

            val resposta = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                if (status == 401 && autenticado) {
                    withContext(Dispatchers.Main) {
                        if (token == tokenDaRequisicao) {
                            limparSessao()
                        }
                    }

                    throw ApiException(
                        status,
                        "Sua sessão expirou. Entre novamente."
                    )
                }

                val mensagem = runCatching {
                    JSONObject(resposta)
                        .optString("erro")
                        .takeIf { it.isNotBlank() }
                }.getOrNull()

                throw ApiException(
                    status,
                    mensagem ?: mensagemPadrao(status)
                )
            }

            resposta
        } catch (exception: IOException) {
            throw ApiException(
                0,
                "Não foi possível conectar à API. " +
                        "Confira se o servidor está ligado " +
                        "e se o endereço e a rede estão corretos."
            )
        } finally {
            conexao.disconnect()
        }
    }

    private fun mensagemPadrao(status: Int): String {
        return when (status) {
            400 -> "Confira os dados informados."
            401 -> "Entre na sua conta para continuar."
            403 -> "Sua conta não tem permissão para esta operação."
            404 -> "O registro solicitado não foi encontrado."
            409 -> "Os dados foram alterados ou a operação não está disponível."
            in 500..599 -> "O servidor não conseguiu concluir a operação."
            else -> "Não foi possível concluir a operação."
        }
    }
}
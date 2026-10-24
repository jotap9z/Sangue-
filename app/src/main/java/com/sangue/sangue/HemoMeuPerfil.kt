package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

private val VermelhoMeuPerfilHemo = Color(0xFFE21C2C)
private val CinzaMeuPerfilHemo = Color(0xFF64748B)

@Composable
fun HemoMeuPerfilScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var nome by remember(usuarioId) {
        mutableStateOf("")
    }
    var email by remember(usuarioId) {
        mutableStateOf("")
    }
    var telefone by remember(usuarioId) {
        mutableStateOf("")
    }
    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }
    var salvando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var alterandoSenha by remember(usuarioId) {
        mutableStateOf(false)
    }
    var carregado by remember(usuarioId) {
        mutableStateOf(false)
    }
    var tentativa by remember(usuarioId) {
        mutableStateOf(0)
    }
    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var mensagem by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }

    val ocupado = carregando || salvando || alterandoSenha

    LaunchedEffect(usuarioId, tentativa) {
        carregando = true
        carregado = false
        erro = null

        try {
            val dados = SangueApi.atualizarUsuario()

            check(
                dados.getLong("id") == usuarioId &&
                        dados.getString("tipoUsuario") == "hemocentro"
            )

            nome = dados.getString("nome")
            email = dados.getString("email")

            telefone = if (dados.isNull("telefone")) {
                ""
            } else {
                dados.optString("telefone")
                    .filter { it in '0'..'9' }
            }

            carregado = true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível consultar seu perfil."
        } catch (_: Exception) {
            erro = "Não foi possível consultar seu perfil."
        } finally {
            carregando = false
        }
    }

    BackHandler {
        if (!ocupado) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !ocupado,
                    onClick = onNavigateBack
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Text(
                    "Meu perfil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (carregando || salvando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = VermelhoMeuPerfilHemo
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                "Dados da sua conta",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Estas informações pertencem ao integrante conectado " +
                        "e não alteram o cadastro da instituição.",
                color = CinzaMeuPerfilHemo,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            erro?.let {
                Text(
                    it,
                    color = VermelhoMeuPerfilHemo,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            mensagem?.let {
                Text(
                    it,
                    color = Color(0xFF166534),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (carregado) {
                CampoContaPerfil(
                    label = "Nome completo",
                    value = nome,
                    enabled = !ocupado,
                    onValueChange = {
                        nome = it
                        mensagem = null
                    }
                )

                CampoContaPerfil(
                    label = "E-mail de acesso",
                    value = email,
                    enabled = !ocupado,
                    readOnly = true,
                    keyboardType = KeyboardType.Email,
                    onValueChange = {}
                )

                CampoContaPerfil(
                    label = "Telefone",
                    value = telefone,
                    enabled = !ocupado,
                    keyboardType = KeyboardType.Phone,
                    mascara = if (telefone.length > 10) {
                        "(##) #####-####"
                    } else {
                        "(##) ####-####"
                    },
                    onValueChange = {
                        telefone = it
                            .filter { caractere ->
                                caractere in '0'..'9'
                            }
                            .take(11)

                        mensagem = null
                    }
                )

                Button(
                    enabled = !ocupado,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermelhoMeuPerfilHemo
                    ),
                    onClick = {
                        erro = null
                        mensagem = null

                        val nomeInformado = nome.trim()

                        when {
                            nomeInformado.length !in 2..100 -> {
                                erro = "Informe um nome com 2 a 100 caracteres."
                            }

                            telefone.isNotBlank() &&
                                    telefone.length !in 10..11 -> {
                                erro = "Informe o telefone com DDD " +
                                        "e 10 ou 11 dígitos."
                            }

                            else -> {
                                val telefoneInformado = telefone
                                salvando = true

                                scope.launch {
                                    var salvo = false

                                    try {
                                        val resposta = SangueApi.objeto(
                                            metodo = "PUT",
                                            caminho = "/api/conta/me",
                                            corpo = JSONObject()
                                                .put(
                                                    "nome",
                                                    nomeInformado
                                                )
                                                .put(
                                                    "telefone",
                                                    telefoneInformado
                                                        .ifBlank { null }
                                                        ?: JSONObject.NULL
                                                )
                                        )

                                        check(
                                            resposta.getLong("id") ==
                                                    usuarioId
                                        )

                                        salvo = true
                                        SangueApi.atualizarUsuario()

                                        nome = resposta.getString("nome")
                                        mensagem = "Perfil atualizado."
                                    } catch (
                                        exception: CancellationException
                                    ) {
                                        throw exception
                                    } catch (exception: ApiException) {
                                        erro = if (salvo) {
                                            "Os dados foram salvos, mas " +
                                                    "não foi possível " +
                                                    "atualizar a sessão. " +
                                                    "Toque em Atualizar dados."
                                        } else {
                                            (exception.message
                                                ?: "Não foi possível salvar.") +
                                                    " Atualize os dados " +
                                                    "para conferir o resultado."
                                        }
                                    } catch (_: Exception) {
                                        erro = "Não foi possível confirmar " +
                                                "a atualização. Atualize " +
                                                "os dados para conferir."
                                    } finally {
                                        salvando = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (salvando) {
                            "Salvando..."
                        } else {
                            "Salvar alterações"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                enabled = !ocupado,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                onClick = {
                    mensagem = null
                    tentativa++
                }
            ) {
                Text("Atualizar dados")
            }

            if (carregado) {
                Spacer(modifier = Modifier.height(32.dp))

                CamposSenhaPerfil(
                    enabled = !carregando && !salvando,
                    onOcupadoChange = {
                        alterandoSenha = it
                    },
                    onLogout = onLogout
                )
            }

            if (!SangueApi.autenticado && !ocupado) {
                Button(
                    onClick = {
                        SangueApi.limparSessao()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Voltar ao login")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
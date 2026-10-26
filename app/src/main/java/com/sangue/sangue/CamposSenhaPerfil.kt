package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

private val VermelhoCamposPerfil = Color(0xFFE21C2C)
private val CinzaCamposPerfil = Color(0xFF64748B)

@Composable
fun CampoContaPerfil(
    label: String,
    value: String,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    mascara: String? = null,
    onValueChange: (String) -> Unit
) {
    var senhaVisivel by remember {
        mutableStateOf(false)
    }

    val transformacaoMascara = remember(mascara) {
        mascara?.let { MascaraCampoPerfil(it) }
    }

    val transformacao = when {
        isPassword && !senhaVisivel ->
            PasswordVisualTransformation()

        transformacaoMascara != null ->
            transformacaoMascara

        else ->
            VisualTransformation.None
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        visualTransformation = transformacao,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) {
                KeyboardType.Password
            } else {
                keyboardType
            }
        ),
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    enabled = enabled,
                    onClick = {
                        senhaVisivel = !senhaVisivel
                    }
                ) {
                    Icon(
                        imageVector = if (senhaVisivel) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (senhaVisivel) {
                            "Ocultar senha"
                        } else {
                            "Mostrar senha"
                        }
                    )
                }
            }
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VermelhoCamposPerfil,
            focusedLabelColor = VermelhoCamposPerfil,
            unfocusedBorderColor = Color(0xFFE2E8F0),
            disabledTextColor = Color(0xFF1E293B)
        )
    )
}

@Composable
fun CamposSenhaPerfil(
    enabled: Boolean = true,
    onOcupadoChange: (Boolean) -> Unit = {},
    onLogout: () -> Unit
) {
    val usuarioId = SangueApi.usuario?.optLong("id")
    val scope = rememberCoroutineScope()

    var senhaAtual by remember(usuarioId) {
        mutableStateOf("")
    }
    var novaSenha by remember(usuarioId) {
        mutableStateOf("")
    }
    var confirmarSenha by remember(usuarioId) {
        mutableStateOf("")
    }
    var processando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var erro by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var concluido by remember(usuarioId) {
        mutableStateOf(false)
    }
    var resultadoIncerto by remember(usuarioId) {
        mutableStateOf(false)
    }

    val bloqueado = concluido || resultadoIncerto
    val camposHabilitados = enabled && !processando && !bloqueado

    fun limparSenhas() {
        senhaAtual = ""
        novaSenha = ""
        confirmarSenha = ""
    }

    fun irParaLogin() {
        limparSenhas()
        SangueApi.limparSessao()
        onLogout()
    }

    BackHandler(enabled = processando || bloqueado) {
        // Aguarda o resultado da operação ou o retorno ao login.
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Alterar senha",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Preencha os campos abaixo somente se desejar trocar " +
                    "sua senha. Após a alteração, entre novamente.",
            fontSize = 13.sp,
            color = CinzaCamposPerfil
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoContaPerfil(
            label = "Senha atual",
            value = senhaAtual,
            enabled = camposHabilitados,
            isPassword = true,
            onValueChange = {
                senhaAtual = it
                erro = null
            }
        )

        CampoContaPerfil(
            label = "Nova senha",
            value = novaSenha,
            enabled = camposHabilitados,
            isPassword = true,
            onValueChange = {
                novaSenha = it
                erro = null
            }
        )

        CampoContaPerfil(
            label = "Confirmar nova senha",
            value = confirmarSenha,
            enabled = camposHabilitados,
            isPassword = true,
            onValueChange = {
                confirmarSenha = it
                erro = null
            }
        )

        Text(
            "A nova senha deve ter pelo menos 6 caracteres, " +
                    "uma letra maiúscula e um caractere especial.",
            fontSize = 12.sp,
            color = CinzaCamposPerfil
        )

        erro?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = VermelhoCamposPerfil,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = camposHabilitados,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VermelhoCamposPerfil
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = {
                erro = null

                val senhaValida = novaSenha.length >= 6 &&
                        novaSenha.toByteArray(Charsets.UTF_8).size <= 72 &&
                        novaSenha.any { it.isUpperCase() } &&
                        novaSenha.any {
                            !it.isLetterOrDigit() &&
                                    !it.isWhitespace()
                        }

                when {
                    !SangueApi.autenticado -> {
                        erro = "Sua sessão expirou. Entre novamente."
                    }

                    senhaAtual.isBlank() -> {
                        erro = "Informe sua senha atual."
                    }

                    senhaAtual.toByteArray(Charsets.UTF_8).size > 72 -> {
                        erro = "Confira sua senha atual."
                    }

                    !senhaValida -> {
                        erro = "A nova senha deve ter pelo menos " +
                                "6 caracteres, uma letra maiúscula " +
                                "e um caractere especial, " +
                                "com limite de 72 bytes."
                    }

                    novaSenha != confirmarSenha -> {
                        erro = "A confirmação da nova senha não confere."
                    }

                    novaSenha == senhaAtual -> {
                        erro = "A nova senha deve ser diferente da atual."
                    }

                    else -> {
                        val atualInformada = senhaAtual
                        val novaInformada = novaSenha

                        processando = true
                        onOcupadoChange(true)

                        scope.launch {
                            var aguardarLogin = false

                            try {
                                SangueApi.objeto(
                                    metodo = "POST",
                                    caminho = "/api/conta/me/senha",
                                    corpo = JSONObject()
                                        .put(
                                            "senhaAtual",
                                            atualInformada
                                        )
                                        .put(
                                            "novaSenha",
                                            novaInformada
                                        )
                                )

                                limparSenhas()
                                aguardarLogin = true
                                concluido = true
                            } catch (
                                exception: CancellationException
                            ) {
                                throw exception
                            } catch (exception: ApiException) {
                                if (
                                    exception.status == 0 ||
                                    exception.status == 401 ||
                                    exception.status >= 500
                                ) {
                                    limparSenhas()
                                    aguardarLogin = true
                                    resultadoIncerto = true
                                } else {
                                    erro = exception.message
                                        ?: "Não foi possível alterar a senha."
                                }
                            } catch (_: Exception) {
                                limparSenhas()
                                aguardarLogin = true
                                resultadoIncerto = true
                            } finally {
                                processando = false

                                if (!aguardarLogin) {
                                    onOcupadoChange(false)
                                }
                            }
                        }
                    }
                }
            }
        ) {
            if (processando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                if (processando) {
                    "Alterando senha..."
                } else {
                    "Salvar nova senha"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (concluido) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Senha alterada") },
            text = {
                Text(
                    "As sessões desta conta foram encerradas. " +
                            "Entre novamente com a nova senha."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { irParaLogin() }
                ) {
                    Text("Ir para o login")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

    if (resultadoIncerto) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Confira seu acesso") },
            text = {
                Text(
                    "Não foi possível confirmar a troca. " +
                            "Ela pode ter sido concluída. " +
                            "Volte ao login e tente entrar com a nova senha. " +
                            "Se ela não funcionar, tente a anterior."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !processando,
                    onClick = {
                        processando = true

                        scope.launch {
                            try {
                                if (SangueApi.autenticado) {
                                    SangueApi.sair()
                                }
                            } catch (
                                exception: CancellationException
                            ) {
                                throw exception
                            } catch (_: Exception) {
                                SangueApi.limparSessao()
                            } finally {
                                processando = false
                            }

                            irParaLogin()
                        }
                    }
                ) {
                    Text(
                        if (processando) {
                            "Saindo..."
                        } else {
                            "Voltar ao login"
                        }
                    )
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }
}

private class MascaraCampoPerfil(
    private val mascara: String
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val posicoes = mutableListOf<Int>()
        var utilizados = 0

        val formatado = buildString {
            for (caractere in mascara) {
                if (utilizados >= original.length) break

                if (caractere == '#') {
                    posicoes.add(length)
                    append(original[utilizados])
                    utilizados++
                } else {
                    append(caractere)
                }
            }

            while (utilizados < original.length) {
                posicoes.add(length)
                append(original[utilizados])
                utilizados++
            }
        }

        val mapeamento = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val seguro = offset.coerceIn(0, original.length)

                return if (seguro == 0) {
                    0
                } else {
                    posicoes[seguro - 1] + 1
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val seguro = offset.coerceIn(0, formatado.length)
                return posicoes.count { it < seguro }
            }
        }

        return TransformedText(
            AnnotatedString(formatado),
            mapeamento
        )
    }
}
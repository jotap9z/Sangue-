package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

internal val VermelhoRecurso = Color(0xFFE21C2C)

@Composable
internal fun TelaRecurso(titulo: String, onBack: () -> Unit, conteudo: @Composable ColumnScope.() -> Unit) {
    TelaOriginal(titulo, onBack, conteudo = conteudo)
}

@Composable
internal fun CartaoRecurso(conteudo: @Composable ColumnScope.() -> Unit) {
    CartaoOriginal(conteudo = conteudo)
}

@Composable
internal fun ErroRecurso(erro: String?) { erro?.let { Text(it, color = VermelhoRecurso) } }

@Composable
fun NotificacoesReaisScreen(onBack: () -> Unit, onAbrir: (String, Long?) -> Unit = { _, _ -> }) {
    val scope = rememberCoroutineScope()
    var itens by remember(SangueApi.usuario?.optLong("id")) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var atualizar by remember { mutableStateOf(0) }
    var mais by remember { mutableStateOf(true) }
    LaunchedEffect(atualizar) {
        ocupado = true
        try { itens = SangueApi.lista("/api/conta/me/notificacoes"); mais = itens.size == 50; erro = null }
        catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
    }
    TelaRecurso("Notificações", onBack) {
        Text("Caixa interna atualizada ao consultar. Não envia notificações push com o aplicativo fechado.")
        TextButton(enabled = !ocupado, onClick = { atualizar++ }) { Text("Atualizar") }
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErroRecurso(erro)
        if (!ocupado && itens.isEmpty()) Text("Nenhuma notificação.")
        itens.forEach { n -> CartaoRecurso {
            Text(n.getString("titulo"), fontWeight = FontWeight.Bold)
            Text(n.getString("texto"))
            SeloOriginal(if (n.isNull("lidaEm")) "Não lida" else "Lida", if (n.isNull("lidaEm")) VermelhoOriginal else CinzaOriginal)
            TextButton(enabled = !ocupado, onClick = {
                ocupado = true
                scope.launch {
                    try {
                        SangueApi.objeto("POST", "/api/conta/me/notificacoes/${n.getLong("id")}/ler")
                        onAbrir(n.getString("destino"), if (n.isNull("referenciaId")) null else n.getLong("referenciaId"))
                        atualizar++
                    } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                }
            }) { Text("Abrir", color = VermelhoRecurso) }
        } }
        if (mais && itens.isNotEmpty()) TextButton(enabled = !ocupado, onClick = {
            ocupado = true
            scope.launch {
                try { val lote = SangueApi.lista("/api/conta/me/notificacoes?antes=${itens.last().getLong("id")}"); itens = itens + lote; mais = lote.size == 50 }
                catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
            }
        }) { Text("Carregar anteriores") }
    }
}

@Composable
fun PreferenciasReaisScreen(onBack: () -> Unit, onLogout: () -> Unit, exclusao: Boolean = false) {
    val scope = rememberCoroutineScope()
    val contexto = androidx.compose.ui.platform.LocalContext.current
    var p by remember { mutableStateOf<JSONObject?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var senha by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf(false) }
    var bloqueada by remember { mutableStateOf(false) }
    var tentativa by remember { mutableStateOf(0) }
    LaunchedEffect(tentativa) {
        if (!exclusao) {
            ocupado = true
            try { p = SangueApi.objeto("GET", "/api/conta/me/preferencias"); erro = null }
            catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
        }
    }
    TelaRecurso(if (exclusao) "Solicitar exclusão da conta" else "Preferências", { if (!ocupado) { if (bloqueada) onLogout() else onBack() } }) {
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErroRecurso(erro)
        mensagem?.let { Text(it) }
        if (exclusao) {
            Text("Ao solicitar, seu acesso será bloqueado e todas as sessões serão encerradas. Os dados e históricos serão preservados enquanto o tratamento da solicitação não for definido. Esta ação não apaga os registros.")
            if (!bloqueada) {
                OutlinedTextField(senha, { senha = it }, label = { Text("Senha atual") }, visualTransformation = PasswordVisualTransformation(), enabled = !ocupado, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                Button(enabled = !ocupado && senha.isNotBlank(), onClick = { confirmar = true }, colors = ButtonDefaults.buttonColors(containerColor = VermelhoRecurso), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) { Text("Solicitar exclusão e bloquear acesso") }
            } else Button(onClick = onLogout, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Voltar para o login") }
        } else {
            p?.let { atual ->
                val institucional = SangueApi.usuario?.optString("tipoUsuario") == "hemocentro"
                val opcoes = if (institucional) listOf("avisosEmergencia" to "Estoque crítico, conforme meu acesso", "avisosMensagens" to "Mensagens recebidas")
                else listOf("avisosAgenda" to "Atualizações de agendamento e intervalo", "avisosEmergencia" to "Chamados na minha cidade", "avisosMensagens" to "Mensagens recebidas")
                opcoes.forEach { (chave, rotulo) ->
                    CartaoOriginal {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(rotulo, Modifier.weight(1f).padding(end = 12.dp), color = TextoOriginal)
                            Switch(checked = atual.getBoolean(chave), enabled = !ocupado, onCheckedChange = { p = JSONObject(atual.toString()).put(chave, it); mensagem = null },
                                colors = SwitchDefaults.colors(checkedTrackColor = VermelhoOriginal))
                        }
                    }
                }
                Text("As preferências controlam a criação de novos avisos na caixa interna. Avisos já registrados são preservados.")
                Button(enabled = !ocupado, onClick = {
                    ocupado = true
                    scope.launch {
                        try { p = SangueApi.objeto("PUT", "/api/conta/me/preferencias", atual); mensagem = "Preferências salvas."; erro = null }
                        catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                    }
                }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Salvar preferências") }
            } ?: TextButton(enabled = !ocupado, onClick = { tentativa++ }) { Text("Tentar novamente") }
        }
    }
    if (confirmar) AlertDialog(onDismissRequest = { confirmar = false }, title = { Text("Bloquear a conta?") }, text = { Text("A solicitação será registrada e o acesso será encerrado. Os dados ainda não serão apagados.") }, confirmButton = { TextButton(onClick = {
        confirmar = false
        ocupado = true
        scope.launch {
            try {
                mensagem = SangueApi.objeto("POST", "/api/conta/me/solicitacao-exclusao", JSONObject().put("senhaAtual", senha)).getString("mensagem")
                senha = ""
                SangueApi.limparSessao()
                bloqueada = true
                erro = null
                android.widget.Toast.makeText(contexto, mensagem, android.widget.Toast.LENGTH_LONG).show()
                onLogout()
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                erro = e.message
                if (e !is ApiException || e.status == 0 || e.status >= 500 || e.status == 403) {
                    mensagem = "Não foi possível confirmar o resultado. A solicitação pode ter sido registrada. Volte ao login para verificar seu acesso."
                    SangueApi.limparSessao()
                    bloqueada = true
                    senha = ""
                    android.widget.Toast.makeText(contexto, mensagem, android.widget.Toast.LENGTH_LONG).show()
                    onLogout()
                }
            } finally { ocupado = false }
        }
    }) { Text("Confirmar") } }, dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
}

@Composable
fun RecuperacaoRealDialog(onFechar: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var concluida by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!ocupado) onFechar() }, title = { Text("Recuperar senha") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
            ErroRecurso(erro)
            mensagem?.let { Text(it) }
            if (!concluida) {
                OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, enabled = !ocupado, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                TextButton(enabled = !ocupado && email.isNotBlank(), onClick = {
                    ocupado = true
                    scope.launch {
                        try { mensagem = SangueApi.objeto("POST", "/api/auth/recuperacao", JSONObject().put("email", email), false).getString("mensagem"); erro = null }
                        catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                    }
                }) { Text("Enviar código por e-mail") }
                OutlinedTextField(token, { token = it.trim() }, label = { Text("Código recebido") }, enabled = !ocupado, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                OutlinedTextField(senha, { senha = it }, label = { Text("Nova senha") }, enabled = !ocupado, visualTransformation = PasswordVisualTransformation(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                Text("Pelo menos 6 caracteres, uma maiúscula e um caractere especial. Código válido por 15 minutos.", fontSize = 12.sp)
            }
        }
    }, confirmButton = { if (!concluida) TextButton(enabled = !ocupado && token.isNotBlank() && senha.isNotBlank(), onClick = {
        ocupado = true
        scope.launch {
            try { mensagem = SangueApi.objeto("POST", "/api/auth/recuperacao/redefinir", JSONObject().put("token", token).put("novaSenha", senha), false).getString("mensagem"); senha = ""; token = ""; concluida = true; erro = null }
            catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
        }
    }) { Text("Redefinir senha") } }, dismissButton = { TextButton(enabled = !ocupado, onClick = onFechar) { Text("Fechar") } }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
}

internal fun dataHoraRecurso(valor: String): String = runCatching {
    val origem = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ROOT).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    val semFracao = valor.replace(Regex("\\.\\d+(?=Z$)"), "")
    val data = requireNotNull(origem.parse(semFracao))
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(data)
}.getOrDefault(valor)
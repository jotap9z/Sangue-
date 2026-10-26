package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.net.URLEncoder

private val VermelhoChat = Color(0xFFE21C2C)
private val TextoChat = Color(0xFF1E293B)
private val CinzaChat = Color(0xFF475569)
private val BordaChat = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversasReaisScreen(
    onBack: () -> Unit,
    conversaInicial: Long? = null,
    centroInicial: Long? = null,
    rodape: @Composable (Boolean) -> Unit = {},
    onOutroDestino: ((String) -> Unit)? = null
) {
    val uid = SangueApi.usuario?.optLong("id")
    val institucional = SangueApi.usuario?.optString("tipoUsuario") == "hemocentro"
    val acesso = LocalAcessoHemocentro.current
    val somenteLeitura = institucional && acesso.permissao == "SOMENTE_VISUALIZACAO"
    val escritaPermitida = !institucional || acesso.escrita
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val listaMensagens = rememberLazyListState()
    val tecladoAberto = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    var conversa by rememberSaveable(uid, conversaInicial, centroInicial) { mutableStateOf(conversaInicial) }
    var contatos by remember(uid) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var contatosAntigos by remember(uid) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var contatoDireto by remember(uid) { mutableStateOf<JSONObject?>(null) }
    var busca by rememberSaveable(uid) { mutableStateOf("") }
    var mensagens by remember(uid, conversa) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var texto by rememberSaveable(uid, conversa) { mutableStateOf("") }
    var chave by rememberSaveable(uid, conversa) { mutableStateOf(UUID.randomUUID().toString()) }
    var envioIncerto by rememberSaveable(uid, conversa) { mutableStateOf(false) }
    var enviando by remember(uid, conversa) { mutableStateOf(false) }
    var paginando by remember(uid, conversa) { mutableStateOf(false) }
    var carregando by remember(uid, conversa, centroInicial) { mutableStateOf(true) }
    var erro by remember(uid, conversa) { mutableStateOf<String?>(null) }
    var erroEnvio by remember(uid, conversa) { mutableStateOf<String?>(null) }
    var maisContatos by remember(uid) { mutableStateOf(false) }
    var paginouContatos by remember(uid) { mutableStateOf(false) }
    var maisMensagens by remember(uid, conversa) { mutableStateOf(false) }
    var aberturaPendente by remember(uid, centroInicial) { mutableStateOf(centroInicial != null && conversaInicial == null) }
    var atualizar by remember { mutableStateOf(0) }
    var rolagem by remember(uid, conversa) { mutableStateOf(0) }
    var novasMensagens by remember(uid, conversa) { mutableStateOf(false) }
    var notificacoes by remember { mutableStateOf(false) }

    val todosContatos = (contatos + contatosAntigos + listOfNotNull(contatoDireto))
        .distinctBy { it.getLong("id") }
    val selecionado = todosContatos.firstOrNull { it.getLong("id") == conversa }
    fun nome(c: JSONObject): String = c.getString(if (institucional) "nomeDoador" else "nomeHemocentro")
    val nomeContato = selecionado?.let { nome(it) }
    val ocupado = enviando || paginando
    val voltar: () -> Unit = {
        if (!ocupado) {
            if (conversa != null && conversaInicial == null && centroInicial == null) {
                conversa = null
            } else onBack()
        }
    }
    BackHandler(onBack = voltar)

    suspend fun lote(id: Long, antes: Long? = null): List<JSONObject> {
        val caminho = "/api/conta/me/conversas/$id/mensagens" + (antes?.let { "?antes=$it" } ?: "")
        return SangueApi.lista(caminho).also { recebidas ->
            check(recebidas.all { it.getLong("conversaId") == id })
        }
    }

    LaunchedEffect(uid, centroInicial, atualizar) {
        if (centroInicial != null && conversa == null) {
            aberturaPendente = true
            carregando = true
            try {
                val resposta = SangueApi.objeto("POST", "/api/doadores/me/conversas/$centroInicial")
                contatoDireto = resposta
                conversa = resposta.getLong("id")
                erro = null
            } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message ?: "Não foi possível abrir a conversa." } finally { aberturaPendente = false; carregando = false }
        }
    }
    LaunchedEffect(uid, conversa) {
        if (conversa != null && todosContatos.none { it.getLong("id") == conversa }) {
            try { contatoDireto = SangueApi.objeto("GET", "/api/conta/me/conversas/$conversa") } catch (e: CancellationException) { throw e } catch (_: Exception) { /* O histórico possui seu próprio tratamento de erro. */ }
        }
    }
    LaunchedEffect(uid, busca) {
        contatosAntigos = emptyList()
        paginouContatos = false
    }
    LaunchedEffect(uid, conversa, atualizar, owner, busca) {
        val alvo = conversa
        if (alvo == null) delay(250)
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                if (centroInicial != null && alvo == null) return@repeatOnLifecycle
                try {
                    if (alvo == null) {
                        contatos = SangueApi.lista("/api/conta/me/conversas?nome=${URLEncoder.encode(busca.trim(), "UTF-8")}")
                        if (!paginouContatos) maisContatos = contatos.size == 50
                    } else {
                        val ultimoConhecido = mensagens.maxOfOrNull { it.getLong("id") }
                        var pagina = lote(alvo)
                        val recebidas = pagina.toMutableList()
                        if (mensagens.isEmpty()) maisMensagens = pagina.size == 50
                        while (ultimoConhecido != null && pagina.size == 50 && pagina.minOf { it.getLong("id") } > ultimoConhecido) {
                            pagina = lote(alvo, pagina.minOf { it.getLong("id") })
                            recebidas.addAll(pagina)
                        }
                        val antesDoFim = ultimoConhecido == null || listaMensagens.firstVisibleItemIndex <= 1
                        val chegaram = recebidas.any { it.getLong("id") > (ultimoConhecido ?: 0L) }
                        mensagens = (mensagens + recebidas).distinctBy { it.getLong("id") }
                        if (chegaram) {
                            if (antesDoFim) rolagem++ else novasMensagens = true
                        }
                        if (envioIncerto && recebidas.any { it.optString("chaveEnvio") == chave && it.optString("conteudo") == texto.trim() }) {
                            texto = ""
                            chave = UUID.randomUUID().toString()
                            envioIncerto = false
                            erroEnvio = null
                        }
                    }
                    erro = null
                } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message ?: "Não foi possível atualizar as mensagens." } finally { carregando = false }
                delay(8000)
            }
        }
    }
    LaunchedEffect(rolagem) {
        if (mensagens.isNotEmpty()) listaMensagens.animateScrollToItem(0)
        novasMensagens = false
    }

    fun enviar() {
        val alvo = conversa ?: return
        if (enviando || !escritaPermitida || texto.isBlank() || !SangueApi.autenticado) return
        val conteudo = texto.trim()
        val chaveEnviada = chave
        enviando = true
        erroEnvio = null
        scope.launch {
            try {
                val salva = SangueApi.objeto("POST", "/api/conta/me/conversas/$alvo/mensagens",
                    JSONObject().put("conteudo", conteudo).put("chaveEnvio", chaveEnviada))
                check(salva.getLong("conversaId") == alvo && salva.getString("chaveEnvio") == chaveEnviada)
                mensagens = (mensagens + salva).distinctBy { it.getLong("id") }
                texto = ""
                chave = UUID.randomUUID().toString()
                envioIncerto = false
                rolagem++
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                erroEnvio = e.message ?: "Não foi possível enviar a mensagem."
                envioIncerto = e !is ApiException || e.status == 0 || e.status >= 500
            } finally { enviando = false }
        }
    }

    if (notificacoes) {
        NotificacoesReaisScreen(onBack = { notificacoes = false }, onAbrir = { destino, ref ->
            if (destino == "chat" && ref != null) { conversa = ref; notificacoes = false }
            else if (onOutroDestino != null) { notificacoes = false; onOutroDestino(destino) }
        })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.background(Color.White)) {
                CabecalhoOriginal(
                    titulo = if (conversa == null) { if (institucional) "Chats com doadores" else "Minhas conversas" }
                    else nomeContato ?: if (institucional) "Chat com doador" else "Chat com hemocentro",
                    onBack = voltar, enabled = !ocupado,
                    acao = {
                        if (institucional && conversa == null && onOutroDestino != null) {
                            IconButton(onClick = { notificacoes = true }, enabled = !ocupado) { Icon(Icons.Outlined.Notifications, "Abrir notificações", tint = TextoChat) }
                        } else {
                            IconButton(onClick = { atualizar++ }, enabled = !ocupado && !carregando && !aberturaPendente) { Icon(Icons.Outlined.Refresh, "Atualizar mensagens", tint = TextoChat) }
                        }
                    }
                )
                if (carregando || aberturaPendente) LinearProgressIndicator(Modifier.fillMaxWidth(), color = VermelhoChat)
            }
        },
        bottomBar = {
            Column(Modifier.background(Color.White)) {
                if (conversa != null) {
                    HorizontalDivider(color = BordaChat)
                    if (!escritaPermitida) Text(if (somenteLeitura) "Seu acesso permite apenas consultar mensagens." else "Conferindo seu acesso ao chat…", Modifier.padding(16.dp), color = CinzaChat, fontSize = 14.sp)
                    else Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        erroEnvio?.let { Text(it, color = VermelhoChat, fontSize = 14.sp) }
                        if (envioIncerto) {
                            Text("O envio ainda não foi confirmado.", color = TextoChat, fontSize = 14.sp)
                            TextButton(onClick = { enviar() }, enabled = !enviando) { Text("Tentar enviar novamente", color = VermelhoChat) }
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = texto,
                                onValueChange = { texto = it.take(2000) },
                                enabled = !enviando && !envioIncerto,
                                placeholder = { Text(if (institucional) "Digite uma resposta…" else "Digite sua mensagem…", color = CinzaChat) },
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                shape = RoundedCornerShape(26.dp),
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VermelhoChat, unfocusedBorderColor = BordaChat,
                                    unfocusedContainerColor = Color(0xFFF8FAFC), focusedContainerColor = Color.White
                                )
                            )
                            FilledIconButton(
                                onClick = { enviar() }, enabled = !enviando && texto.isNotBlank(),
                                modifier = Modifier.size(56.dp), shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = VermelhoChat, contentColor = Color.White)
                            ) {
                                if (enviando) CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = "Enviando mensagem" }, color = Color.White, strokeWidth = 2.dp)
                                else Icon(Icons.AutoMirrored.Filled.Send, if (envioIncerto) "Tentar enviar novamente" else "Enviar mensagem")
                            }
                        }
                    }
                }
                if (!tecladoAberto) rodape(!ocupado)
            }
        }
    ) { padding ->
        if (conversa == null) {
            val filtrados = todosContatos.filter { busca.isBlank() || nome(it).contains(busca.trim(), ignoreCase = true) }.sortedBy { nome(it).lowercase() }
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item("busca") {
                    OutlinedTextField(busca, { busca = it.take(100) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), singleLine = true,
                        label = { Text("Buscar conversa por nome") }, placeholder = { Text("Nome do contato") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = { if (busca.isNotEmpty()) IconButton(onClick = { busca = "" }) { Icon(Icons.Outlined.Close, "Limpar busca") } },
                        shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VermelhoChat, unfocusedBorderColor = BordaChat))
                }
                erro?.let { aviso -> item("erro") { AvisoChat(aviso) { atualizar++ } } }
                if (!carregando && !aberturaPendente && erro == null && filtrados.isEmpty()) item("vazio") {
                    Text(if (busca.isNotBlank()) "Nenhuma conversa encontrada pelo nome informado."
                    else if (institucional) "Nenhuma conversa ainda. As conversas iniciadas pelos doadores aparecerão aqui."
                    else "Nenhuma conversa ainda. Abra os detalhes de um hemocentro e toque em Conversar com a instituição.", color = CinzaChat)
                }
                items(filtrados, key = { it.getLong("id") }) { c ->
                    Card(onClick = { conversa = c.getLong("id") }, enabled = !ocupado, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BordaChat), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(nome(c), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoChat)
                                Text(if (c.isNull("ultimaMensagem")) "Comece uma conversa" else c.getString("ultimaMensagem"), fontSize = 14.sp,
                                    color = CinzaChat, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!c.isNull("ultimaEnviadaEm")) Text(dataHoraRecurso(c.getString("ultimaEnviadaEm")), modifier = Modifier.padding(start = 8.dp), fontSize = 11.sp, color = CinzaChat)
                        }
                    }
                }
                if (maisContatos) item("mais") {
                    OutlinedButton(enabled = !ocupado, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                        paginando = true
                        scope.launch {
                            try {
                                val antes = todosContatos.minOf { it.getLong("id") }
                                val recebidos = SangueApi.lista("/api/conta/me/conversas?antes=$antes&nome=${URLEncoder.encode(busca.trim(), "UTF-8")}")
                                contatosAntigos = (contatosAntigos + recebidos).distinctBy { it.getLong("id") }
                                maisContatos = recebidos.size == 50
                                paginouContatos = true
                                erro = null
                            } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { paginando = false }
                        }
                    }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) { Text(if (paginando) "Carregando…" else "Carregar mais conversas") }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                    Text(if (institucional) "Atendimento pela equipe da instituição" else "Conversa com a equipe do hemocentro", Modifier.padding(12.dp), color = CinzaChat, fontSize = 14.sp)
                }
                erro?.let { aviso -> AvisoChat(aviso) { atualizar++ } }
                if (novasMensagens) TextButton(onClick = { rolagem++ }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Ir para novas mensagens", color = VermelhoChat) }
                if (!carregando && erro == null && mensagens.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Esta conversa ainda não tem mensagens.", color = CinzaChat, modifier = Modifier.padding(24.dp))
                } else LazyColumn(
                    state = listaMensagens, reverseLayout = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mensagens.sortedByDescending { it.getLong("id") }, key = { it.getLong("id") }) { m -> BalaoChatReal(m, m.getLong("remetenteId") == uid) }
                    if (maisMensagens) item("anteriores") {
                        OutlinedButton(enabled = !ocupado, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            val alvo = conversa
                            if (alvo != null && mensagens.isNotEmpty()) {
                                paginando = true
                                scope.launch {
                                    try {
                                        val recebidas = lote(alvo, mensagens.minOf { it.getLong("id") })
                                        mensagens = (mensagens + recebidas).distinctBy { it.getLong("id") }
                                        maisMensagens = recebidas.size == 50
                                    } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { paginando = false }
                                }
                            }
                        }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) { Text(if (paginando) "Carregando…" else "Carregar mensagens anteriores") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoChat(texto: String, onTentar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(texto, color = VermelhoChat, fontSize = 14.sp)
        TextButton(onClick = onTentar) { Text("Tentar novamente", color = VermelhoChat) }
    }
}

@Composable
private fun BalaoChatReal(m: JSONObject, minha: Boolean) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, horizontalAlignment = if (minha) Alignment.End else Alignment.Start) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(0.88f),
            color = if (minha) VermelhoChat else Color(0xFFF1F5F9),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (minha) 16.dp else 4.dp, bottomEnd = if (minha) 4.dp else 16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (minha) "Você" else m.getString("nomeRemetente"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (minha) Color.White else TextoChat)
                SelectionContainer { Text(m.getString("conteudo"), fontSize = 16.sp, lineHeight = 23.sp, color = if (minha) Color.White else TextoChat) }
                Text(dataHoraRecurso(m.getString("enviadaEm")), fontSize = 12.sp, color = if (minha) Color.White else CinzaChat)
            }
        }
    }
}
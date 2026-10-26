package com.sangue.sangue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun EmergenciaScreen(onNavigateBack: () -> Unit = {}, onNavigateToAgendar: () -> Unit = {}, onAgendarCentro: (Long) -> Unit = { onNavigateToAgendar() }, chamadoInicial: Long? = null) {
    ChamadosReaisScreen(false, onNavigateBack, onAgendarCentro, chamadoInicial)
}

@Composable
fun HemoChamadosReaisScreen(onBack: () -> Unit) { ChamadosReaisScreen(true, onBack, {}) }

@Composable
private fun ChamadosReaisScreen(institucional: Boolean, onBack: () -> Unit, onAgendar: (Long) -> Unit, chamadoInicial: Long? = null) {
    val scope = rememberCoroutineScope()
    val acesso = LocalAcessoHemocentro.current
    var itens by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selecionado by remember { mutableStateOf<JSONObject?>(null) }
    var formulario by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("O+") }
    var urgencia by remember { mutableStateOf("ALTA") }
    var meta by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var encerrar by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var incerto by remember { mutableStateOf(false) }
    var atualizar by remember { mutableStateOf(0) }
    var pagina by remember { mutableStateOf(0) }
    var totalPaginas by remember { mutableStateOf(0) }
    val base = if (institucional) "/api/hemocentros/me/chamados" else "/api/chamados"
    LaunchedEffect(atualizar, pagina) {
        ocupado = true
        try {
            val r = SangueApi.objeto("GET", "$base?pagina=$pagina&tamanho=20", autenticado = institucional)
            val array = r.getJSONArray("itens")
            itens = (0 until array.length()).map { if (institucional) array.getJSONObject(it).getJSONObject("chamado") else array.getJSONObject(it) }
            totalPaginas = r.getInt("totalPaginas")
            erro = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message; itens = emptyList() } finally { ocupado = false }
    }
    LaunchedEffect(chamadoInicial) {
        if (chamadoInicial != null) {
            try { selecionado = SangueApi.objeto("GET", "/api/chamados/$chamadoInicial", autenticado = false) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message }
        }
    }
    fun preparar(c: JSONObject?) {
        titulo = c?.optString("titulo").orEmpty()
        tipo = c?.optString("tipoSanguineo") ?: "O+"
        urgencia = c?.optString("urgencia") ?: "ALTA"
        meta = c?.optInt("metaDoadores")?.toString().orEmpty()
        texto = c?.optString("mensagem").orEmpty()
        formulario = true
        incerto = false
        erro = null
    }
    val c = selecionado
    TelaRecurso(if (institucional) "Chamados da instituição" else "Chamados emergenciais", {
        if (!ocupado) {
            if (formulario) { formulario = false; incerto = false; atualizar++ }
            else if (c != null) selecionado = null else onBack()
        }
    }) {
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErroRecurso(erro)
        mensagem?.let { Text(it) }
        if (formulario) {
            Text(if (c == null) "Novo chamado" else "Editar chamado #${c.getLong("id")}", fontWeight = FontWeight.Bold)
            OutlinedTextField(titulo, { titulo = it.take(150) }, label = { Text("Título") }, enabled = !ocupado && !incerto, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            Text("Tipo sanguíneo")
            listOf(listOf("O+", "O-", "A+", "A-"), listOf("B+", "B-", "AB+", "AB-")).forEach { tipos -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { tipos.forEach { t -> FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t) }, enabled = !ocupado && !incerto) } } }
            Text("Urgência")
            listOf("BAIXA", "MEDIA", "ALTA", "CRITICA").forEach { nivel -> FilterChip(selected = urgencia == nivel, onClick = { urgencia = nivel }, label = { Text(nivel) }, enabled = !ocupado && !incerto) }
            OutlinedTextField(meta, { meta = it }, label = { Text("Meta de doadores") }, enabled = !ocupado && !incerto, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            OutlinedTextField(texto, { texto = it.take(2000) }, label = { Text("Mensagem") }, enabled = !ocupado && !incerto, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            Button(enabled = !ocupado && !incerto && acesso.emitirChamado, onClick = {
                val quantidade = meta.toIntOrNull()
                if (quantidade == null || quantidade < 1 || texto.isBlank() || titulo.isBlank()) erro = "Preencha título, mensagem e meta positiva."
                else {
                    ocupado = true
                    scope.launch {
                        try {
                            val corpo = JSONObject().put("tipoSanguineo", tipo).put("urgencia", urgencia).put("metaDoadores", quantidade).put("mensagem", texto).put("titulo", titulo)
                            if (c != null) corpo.put("versao", c.getLong("versao"))
                            val r = SangueApi.objeto(if (c == null) "POST" else "PUT", if (c == null) base else "$base/${c.getLong("id")}", corpo)
                            selecionado = r.getJSONObject("chamado")
                            formulario = false
                            mensagem = "Chamado salvo."
                            erro = null
                            atualizar++
                        } catch (e: CancellationException) { throw e } catch (e: Exception) {
                            erro = e.message
                            incerto = e !is ApiException || e.status == 0 || e.status >= 500 || e.status == 409
                        } finally { ocupado = false }
                    }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = VermelhoRecurso), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) { Text("Salvar chamado") }
            if (incerto) Text("O resultado não pôde ser confirmado ou houve conflito. Volte e consulte o chamado antes de tentar outra alteração.")
        } else if (c != null) {
            CartaoOriginal(destaque = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.sirene), contentDescription = null, modifier = Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.getString("titulo"), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VermelhoOriginal)
                        Text("Tipo ${c.getString("tipoSanguineo")}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = VermelhoOriginal)
                    }
                }
                Text(c.getString("nomeHemocentro"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeloOriginal(c.getString("urgencia"))
                    SeloOriginal(c.getString("status"), CinzaOriginal)
                }
                Text(c.getString("mensagem"))
                Text("Interesses ativos: ${c.getLong("respostasAtivas")} / meta ${c.getInt("metaDoadores")}")
                Text("O interesse não confirma reserva, aptidão ou doação. Doações só são registradas após a coleta presencial.")
            }
            if (institucional) {
                if (acesso.emitirChamado && c.getString("status") == "ATIVO") {
                    OutlinedButton(enabled = !ocupado, onClick = { preparar(c) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) { Text("Editar") }
                    OutlinedTextField(motivo, { motivo = it.take(500) }, label = { Text("Motivo do encerramento") }, enabled = !ocupado, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    Button(enabled = !ocupado && motivo.trim().length >= 3, onClick = { encerrar = true }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Encerrar chamado") }
                }
                InteressadosChamadoReal(c.getLong("id"))
            } else if (c.getString("status") == "ATIVO") {
                Button(enabled = !ocupado, onClick = {
                    ocupado = true
                    scope.launch {
                        try { SangueApi.objeto("POST", "/api/doadores/me/chamados/${c.getLong("id")}/responder"); mensagem = "Interesse registrado. Escolha um horário para agendar."; erro = null }
                        catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = VermelhoRecurso), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) { Text("Registrar interesse") }
                OutlinedButton(enabled = !ocupado, onClick = {
                    ocupado = true
                    scope.launch {
                        try { SangueApi.objeto("DELETE", "/api/doadores/me/chamados/${c.getLong("id")}/resposta"); mensagem = "Interesse retirado."; erro = null }
                        catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                    }
                }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) { Text("Retirar meu interesse") }
                Button(onClick = { onAgendar(c.getLong("hemocentroId")) }, enabled = !ocupado, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Agendar neste hemocentro") }
            }
        } else {
            TextButton(enabled = !ocupado, onClick = { atualizar++ }) { Text("Atualizar") }
            if (institucional && acesso.emitirChamado) Button(enabled = !ocupado, onClick = { preparar(null) }, colors = ButtonDefaults.buttonColors(containerColor = VermelhoRecurso), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) { Text("Criar chamado") }
            if (!ocupado && itens.isEmpty()) Text("Nenhum chamado encontrado.")
            itens.forEach { item -> CartaoOriginal(destaque = item.optString("urgencia") == "CRITICA") {
                Text(item.getString("titulo"), fontWeight = FontWeight.Bold)
                Text("${item.getString("nomeHemocentro")} — ${item.getString("tipoSanguineo")}")
                Text("${item.getString("urgencia")} • ${item.getString("status")}")
                TextButton(enabled = !ocupado, onClick = { selecionado = item; mensagem = null; erro = null }) { Text("Ver detalhes", color = VermelhoRecurso) }
            } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !ocupado && pagina > 0, onClick = { pagina-- }) { Text("Anterior") }
                Text("Página ${pagina + 1}")
                TextButton(enabled = !ocupado && pagina + 1 < totalPaginas, onClick = { pagina++ }) { Text("Próxima") }
            }
        }
    }
    if (encerrar && c != null) AlertDialog(onDismissRequest = { encerrar = false }, title = { Text("Encerrar chamado?") }, text = { Text("O chamado deixará de receber novos interesses.") }, confirmButton = { TextButton(onClick = {
        encerrar = false
        ocupado = true
        scope.launch {
            try {
                selecionado = SangueApi.objeto("POST", "$base/${c.getLong("id")}/encerrar", JSONObject().put("motivo", motivo.trim()).put("versao", c.getLong("versao"))).getJSONObject("chamado")
                mensagem = "Chamado encerrado."
                erro = null
                atualizar++
            } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message; selecionado = null; atualizar++ } finally { ocupado = false }
        }
    }) { Text("Confirmar") } }, dismissButton = { TextButton(onClick = { encerrar = false }) { Text("Cancelar") } }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
}

@Composable
private fun InteressadosChamadoReal(id: Long) {
    var lista by remember(id) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var erro by remember(id) { mutableStateOf<String?>(null) }
    var pagina by remember(id) { mutableStateOf(0) }
    var total by remember(id) { mutableStateOf(0) }
    var carregando by remember(id) { mutableStateOf(true) }
    LaunchedEffect(id, pagina) {
        carregando = true
        try {
            val r = SangueApi.objeto("GET", "/api/hemocentros/me/chamados/$id/interessados?pagina=$pagina&tamanho=20")
            val a = r.getJSONArray("itens")
            lista = (0 until a.length()).map { a.getJSONObject(it) }
            total = r.getInt("totalPaginas")
            erro = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { carregando = false }
    }
    Text("Doadores interessados", fontWeight = FontWeight.Bold)
    if (carregando) LinearProgressIndicator(Modifier.fillMaxWidth())
    ErroRecurso(erro)
    if (!carregando && lista.isEmpty()) Text("Nenhum interesse registrado.")
    lista.forEach { Text("${it.getString("nome")} • ${it.getString("tipoSanguineoInformado")}") }
    Row {
        TextButton(enabled = !carregando && pagina > 0, onClick = { pagina-- }) { Text("Anterior") }
        TextButton(enabled = !carregando && pagina + 1 < total, onClick = { pagina++ }) { Text("Próxima") }
    }
}
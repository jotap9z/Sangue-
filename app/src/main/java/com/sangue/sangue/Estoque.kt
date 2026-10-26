package com.sangue.sangue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

@Composable
fun EstoqueScreen(onNavigateBack: () -> Unit = {}, onNavigateToAgendar: () -> Unit = {}, onAgendarCentro: (Long) -> Unit = { onNavigateToAgendar() }) {
    var centros by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var escolhido by remember { mutableStateOf<JSONObject?>(null) }
    var ocupado by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var atualizar by remember { mutableStateOf(0) }
    LaunchedEffect(atualizar) {
        ocupado = true
        try { centros = SangueApi.lista("/api/hemocentros/buscar", false); erro = null }
        catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
    }
    val h = escolhido
    TelaOriginal("Estoque de sangue", { if (h == null) onNavigateBack() else escolhido = null }, rodape = {
        if (h != null) RodapeOriginal { BotaoOriginal("Quero ajudar") { onAgendarCentro(h.getLong("id")) } }
    }) {
        if (h == null) {
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
            ErroRecurso(erro)
            TextButton(enabled = !ocupado, onClick = { atualizar++ }) { Text("Atualizar instituições") }
            if (!ocupado && centros.isEmpty()) Text("Nenhuma instituição cadastrada.")
            centros.forEach { centro -> CartaoRecurso {
                Text(centro.getString("nome"), fontWeight = FontWeight.Bold)
                Text("${centro.getString("cidade")} — ${centro.getString("estado")}")
                TextButton(onClick = { escolhido = centro }) { Text("Consultar estoque", color = VermelhoRecurso) }
            } }
        } else {
            CartaoOriginal(destaque = true) { Text(h.getString("nome"), fontWeight = FontWeight.Bold, color = VermelhoOriginal); Text("Consulte as necessidades desta instituição", color = CinzaOriginal) }
            EstoquePublicoReal(h.getLong("id"))

        }
    }
}

@Composable
internal fun EstoquePublicoReal(hemocentroId: Long) {
    var itens by remember(hemocentroId) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var ocupado by remember(hemocentroId) { mutableStateOf(true) }
    var erro by remember(hemocentroId) { mutableStateOf<String?>(null) }
    var atualizar by remember { mutableStateOf(0) }
    LaunchedEffect(hemocentroId, atualizar) {
        ocupado = true
        itens = emptyList()
        try {
            val r = SangueApi.objeto("GET", "/api/hemocentros/$hemocentroId/estoque", autenticado = false).getJSONArray("itens")
            itens = (0 until r.length()).map { r.getJSONObject(it) }
            erro = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Situação do estoque", fontWeight = FontWeight.Bold)
        val criticos = itens.filter { it.optString("situacao") == "CRITICO" }.map { it.getString("tipoSanguineo") }
        if (criticos.isNotEmpty()) CartaoOriginal(destaque = true) {
            Text("Precisamos de você", color = VermelhoOriginal, fontWeight = FontWeight.Bold)
            Text("Estoque crítico: ${criticos.joinToString(", ")}", color = TextoOriginal)
        }
        TextButton(enabled = !ocupado, onClick = { atualizar++ }) { Text("Atualizar estoque") }
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErroRecurso(erro)
        itens.chunked(2).forEach { par -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            par.forEach { i -> Box(Modifier.weight(1f)) { CartaoRecurso {
                val situacao = i.getString("situacao")
                val (rotulo, cor) = when (situacao) {
                    "CRITICO" -> "Crítico" to VermelhoRecurso
                    "BAIXO" -> "Baixo" to Color(0xFFF59E0B)
                    "NORMAL" -> "Normal" to Color(0xFF16A34A)
                    else -> "Não informado" to Color(0xFF64748B)
                }
                Text(i.getString("tipoSanguineo"), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = TextoOriginal)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(cor, CircleShape))
                    Text(rotulo, color = cor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (!i.isNull("atualizadoEm")) Text("Atualizado em ${dataHoraRecurso(i.getString("atualizadoEm"))}", fontSize = 11.sp, color = CinzaOriginal)
            } } }
            if (par.size == 1) Spacer(Modifier.weight(1f))
        } }
        Text("Crítico: abaixo do mínimo. Baixo: abaixo do ideal. Normal: a partir do ideal. Não informado: sem registro de estoque.", color = CinzaOriginal, fontSize = 12.sp)
        Text("A situação é a última registrada pela instituição. Confirme a disponibilidade para atendimento diretamente com o hemocentro.", color = CinzaOriginal, fontSize = 12.sp)
    }
}
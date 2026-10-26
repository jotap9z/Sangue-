package com.sangue.sangue

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun RelatorioRealScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val hoje = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("America/Sao_Paulo") }.format(java.util.Date()) }
    var de by remember { mutableStateOf(hoje.take(8) + "01") }
    var ate by remember { mutableStateOf(hoje) }
    var dados by remember { mutableStateOf<JSONObject?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    TelaRecurso("Relatórios", { if (!ocupado) onBack() }) {
        Text("Indicadores da instituição. Datas no horário America/Sao_Paulo; período máximo de 366 dias.")
        OutlinedTextField(de, { de = it }, label = { Text("De — AAAA-MM-DD") }, enabled = !ocupado, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        OutlinedTextField(ate, { ate = it }, label = { Text("Até — AAAA-MM-DD") }, enabled = !ocupado, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        Button(enabled = !ocupado, onClick = {
            if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(de) || !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(ate)) erro = "Informe as datas no formato AAAA-MM-DD."
            else {
                ocupado = true
                dados = null
                scope.launch {
                    try { dados = SangueApi.objeto("GET", "/api/hemocentros/me/relatorios?de=$de&ate=$ate"); erro = null }
                    catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                }
            }
        }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Consultar") }
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErroRecurso(erro)
        dados?.let { d ->
            val agendamentos = d.getJSONArray("agendamentosPorStatus")
            val doacoes = d.getJSONArray("doacoesPorTipo")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CartaoOriginal(modifier = Modifier.weight(1f), destaque = true) {
                    Text((0 until agendamentos.length()).sumOf { agendamentos.getJSONObject(it).getLong("total") }.toString(), color = VermelhoOriginal, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Agendamentos", color = CinzaOriginal)
                }
                CartaoOriginal(modifier = Modifier.weight(1f)) {
                    Text((0 until doacoes.length()).sumOf { doacoes.getJSONObject(it).getLong("total") }.toString(), color = VermelhoOriginal, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Doações", color = CinzaOriginal)
                }
            }
            CartaoRecurso {
                Text("Agendamentos por situação", fontWeight = FontWeight.Bold)
                val a = d.getJSONArray("agendamentosPorStatus")
                if (a.length() == 0) Text("Nenhum agendamento no período.")
                for (i in 0 until a.length()) { val r = a.getJSONObject(i); Text("${r.getString("status")}: ${r.getLong("total")}") }
            }
            CartaoRecurso {
                Text("Doações realizadas por tipo", fontWeight = FontWeight.Bold)
                val a = d.getJSONArray("doacoesPorTipo")
                if (a.length() == 0) Text("Nenhuma doação registrada no período.")
                for (i in 0 until a.length()) { val r = a.getJSONObject(i); Text("${r.getString("tipoSanguineo")}: ${r.getLong("total")}") }
            }
            Text("Os agendamentos são agrupados pela data reservada. As doações são agrupadas pela data em que a coleta foi registrada. Não representam automaticamente entradas no estoque.")
        }
    }
}

@Composable
fun CoordenadasReaisScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(true) }
    var valido by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var atualizar by remember { mutableStateOf(0) }
    LaunchedEffect(atualizar) {
        ocupado = true
        try {
            val r = SangueApi.objeto("GET", "/api/hemocentros/me")
            latitude = if (r.isNull("latitude")) "" else r.getDouble("latitude").toString()
            longitude = if (r.isNull("longitude")) "" else r.getDouble("longitude").toString()
            valido = true
            erro = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message; valido = false } finally { ocupado = false }
    }
    TelaRecurso("Localização da instituição", { if (!ocupado) onBack() }) {
        Text("Informe as coordenadas verificadas do local de coleta, usando ponto decimal. Não use a localização do administrador se ele estiver em outro endereço. Deixe ambas vazias para remover as coordenadas.")
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErroRecurso(erro)
        mensagem?.let { Text(it) }
        OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, enabled = !ocupado && valido, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, enabled = !ocupado && valido, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        Button(enabled = !ocupado && valido, onClick = {
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()
            if ((latitude.isNotBlank() || longitude.isNotBlank()) && (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0)) erro = "Informe as duas coordenadas válidas."
            else {
                ocupado = true
                scope.launch {
                    try {
                        SangueApi.objeto("PUT", "/api/hemocentros/me/coordenadas", JSONObject().put("latitude", lat ?: JSONObject.NULL).put("longitude", lon ?: JSONObject.NULL))
                        mensagem = "Localização salva."
                        erro = null
                    } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message } finally { ocupado = false }
                }
            }
        }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Salvar localização") }
        TextButton(enabled = !ocupado, onClick = { atualizar++ }) { Text("Recarregar") }
    }
}
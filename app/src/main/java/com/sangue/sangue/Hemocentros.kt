package com.sangue.sangue

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.coroutines.resume

@Composable
fun HemocentrosScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendar: () -> Unit = {},
    onAgendarCentro: (Long) -> Unit = { onNavigateToAgendar() }
) {
    val context = LocalContext.current
    var selecionado by remember { mutableStateOf<JSONObject?>(null) }
    var chat by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var uf by remember { mutableStateOf("") }
    var lista by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var atualizar by remember { mutableStateOf(0) }
    var localizar by remember { mutableStateOf(0) }
    var local by remember { mutableStateOf<Location?>(null) }
    var localizando by remember { mutableStateOf(false) }
    var avisoLocal by remember { mutableStateOf<String?>(null) }
    val permissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) localizar++ else avisoLocal = "Permissão negada. Você pode continuar pesquisando por nome, cidade e estado."
    }
    LaunchedEffect(localizar) {
        if (localizar > 0) {
            localizando = true
            try {
                local = obterLocalizacaoHemocentros(context)
                avisoLocal = if (local == null) "Localização indisponível. Confira se o serviço de localização está ligado e tente novamente." else "Distância aproximada em linha reta, não é distância de rota nem estimativa de espera."
            } catch (e: CancellationException) { throw e } catch (_: Exception) { avisoLocal = "Não foi possível obter a localização. A pesquisa continua disponível." } finally { localizando = false }
        }
    }
    LaunchedEffect(atualizar) {
        carregando = true
        erro = null
        try {
            fun cod(s: String) = URLEncoder.encode(s.trim(), "UTF-8")
            lista = SangueApi.lista("/api/hemocentros/buscar?nome=${cod(nome)}&cidade=${cod(cidade)}&estado=${cod(uf)}", false)
        } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message; lista = emptyList() } finally { carregando = false }
    }
    val h = selecionado
    if (chat && h != null) {
        ConversasReaisScreen(onBack = { chat = false }, centroInicial = h.getLong("id"))
        return
    }
    if (h != null) {
        TelaOriginal("Detalhes do hemocentro", { selecionado = null }, rodape = {
            RodapeOriginal {
                BotaoOriginal("Agendar doação") { onAgendarCentro(h.getLong("id")) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotaoOriginal("Abrir rota", secundario = true, modifier = Modifier.weight(1f)) { abrirMapa(context, enderecoCentroReal(h), null) }
                    BotaoOriginal("Chat", secundario = true, modifier = Modifier.weight(1f)) { chat = true }
                }
            }
        }) {
            CartaoOriginal(destaque = true) {
                Text(h.getString("nome"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(enderecoCentroReal(h))
                h.optString("telefone").takeUnless { it.isBlank() || it == "null" }?.let { telefone ->
                    TextButton(onClick = { abrirContatoCentro(context, "tel:" + telefone.filter { it.isDigit() || it == '+' }) }) { Text(telefone) }
                }
                h.optString("emailContato").takeUnless { it.isBlank() || it == "null" }?.let { email ->
                    TextButton(onClick = { abrirContatoCentro(context, "mailto:$email") }) { Text(email) }
                }
                Text("Consulte os horários de coleta disponíveis no agendamento. O aplicativo não informa tempo de espera presencial.")
            }
            EstoquePublicoReal(h.getLong("id"))
        }
    } else {
        TelaRecurso("Hemocentros próximos", onNavigateBack) {
            OutlinedTextField(nome, { nome = it.take(150) }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cidade, { cidade = it.take(100) }, label = { Text("Cidade") }, modifier = Modifier.weight(2f), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(uf, { uf = it.uppercase().take(2) }, label = { Text("UF") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }
            BotaoOriginal("Pesquisar", enabled = !carregando) { atualizar++ }
            OutlinedButton(enabled = !localizando, onClick = {
                val permitida = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (permitida) localizar++ else permissao.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) { Text(if (localizando) "Buscando localização…" else "Ordenar pela minha localização") }
            avisoLocal?.let { Text(it, fontSize = 12.sp) }
            if (carregando) LinearProgressIndicator(Modifier.fillMaxWidth())
            ErroRecurso(erro)
            if (!carregando && lista.isEmpty()) Text("Nenhum hemocentro encontrado para os filtros informados.")
            lista.sortedBy { distanciaCentroReal(local, it) ?: Double.MAX_VALUE }.forEach { centro ->
                val distancia = distanciaCentroReal(local, centro)
                HemocentroListCard(centro.getString("nome"), "${centro.getString("cidade")} — ${centro.getString("estado")}", Color(0xFF64748B),
                    if (distancia != null) String.format(java.util.Locale.getDefault(), "%.1f km em linha reta", distancia) else if (local != null) "Coordenadas não informadas pela instituição" else "Distância não calculada",
                    onDetalhes = { selecionado = centro }, onAgendar = { onAgendarCentro(centro.getLong("id")) })
            }
        }
    }
}

@Composable
fun HemocentroListCard(nome: String, status: String, statusColor: Color, info: String, onDetalhes: () -> Unit, onAgendar: () -> Unit) {
    Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(nome, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(status, color = statusColor, fontSize = 12.sp)
            Text(info, fontSize = 13.sp, color = Color(0xFF64748B))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDetalhes, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) { Text("Ver detalhes", color = VermelhoRecurso) }
                Button(onClick = onAgendar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = VermelhoRecurso)) { Text("Agendar") }
            }
        }
    }
}

internal fun enderecoCentroReal(h: JSONObject) = "${h.getString("endereco")}, ${h.getString("cidade")} — ${h.getString("estado")}"

private fun distanciaCentroReal(local: Location?, h: JSONObject): Double? {
    if (local == null || h.isNull("latitude") || h.isNull("longitude")) return null
    val lat = h.optDouble("latitude")
    val lon = h.optDouble("longitude")
    if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    val resultado = FloatArray(1)
    Location.distanceBetween(local.latitude, local.longitude, lat, lon, resultado)
    return resultado[0] / 1000.0
}

@Suppress("DEPRECATION", "MissingPermission")
private suspend fun obterLocalizacaoHemocentros(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val precisa = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val providers = manager.getProviders(true).filter { it == LocationManager.NETWORK_PROVIDER || (precisa && it == LocationManager.GPS_PROVIDER) }
    val ultimas = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
    ultimas.filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..600_000_000_000L }.minByOrNull { it.accuracy }?.let { return it }
    if (providers.isEmpty()) return null
    return withTimeoutOrNull(30_000) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
                override fun onProviderEnabled(provider: String) { }
                override fun onProviderDisabled(provider: String) { }
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            try { providers.forEach { manager.requestSingleUpdate(it, listener, Looper.getMainLooper()) } }
            catch (_: SecurityException) { manager.removeUpdates(listener); if (continuation.isActive) continuation.resume(null) }
        }
    }
}

fun abrirMapa(context: Context, endereco: String, pacote: String?) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(endereco)}")).apply { pacote?.let { setPackage(it) } }) }
    catch (_: Exception) { Toast.makeText(context, "App de mapas não encontrado", Toast.LENGTH_SHORT).show() }
}
private fun abrirContatoCentro(context: Context, uri: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) } catch (_: Exception) { Toast.makeText(context, "Aplicativo para este contato não encontrado", Toast.LENGTH_SHORT).show() }
}